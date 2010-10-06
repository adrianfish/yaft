/**
 * Copyright 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.yaft.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.sakaiproject.emailtemplateservice.model.EmailTemplate;
import org.sakaiproject.emailtemplateservice.model.RenderedTemplate;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.HttpAccess;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.user.api.User;
import org.sakaiproject.yaft.api.ActiveDiscussion;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.yaft.api.Group;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.XmlDefs;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftFunctions;
import org.sakaiproject.yaft.api.YaftPreferences;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class YaftForumServiceImpl implements YaftForumService
{
	private Logger logger = Logger.getLogger(YaftForumServiceImpl.class);

	private SakaiProxy sakaiProxy = null;

	private YaftPersistenceManager persistenceManager = null;

	private boolean useSynopticFunctionality = true;

	private YaftSecurityManager securityManager;

	public void init()
	{
		if (logger.isDebugEnabled())
			logger.debug("init()");

		sakaiProxy = new SakaiProxyImpl();
		
		securityManager = new YaftSecurityManager(sakaiProxy);

		logger.info("Registering Yaft functions ...");

		sakaiProxy.registerFunction(YaftFunctions.YAFT_MODIFY_PERMISSIONS);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_FORUM_CREATE);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_FORUM_DELETE_OWN);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_FORUM_DELETE_ANY);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_FORUM_VIEW_GROUPS);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_DISCUSSION_CREATE);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_OWN);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_ANY);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_MESSAGE_CREATE);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_MESSAGE_CENSOR);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_MESSAGE_DELETE_OWN);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_MESSAGE_DELETE_ANY);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_MESSAGE_READ);
		sakaiProxy.registerFunction(YaftFunctions.YAFT_VIEW_INVISIBLE);

		persistenceManager = new YaftPersistenceManager();
		persistenceManager.setSakaiProxy(sakaiProxy);
		persistenceManager.init();
		persistenceManager.setupTables();
		persistenceManager.setUseSynopticFunctionality(useSynopticFunctionality);

		sakaiProxy.registerEntityProducer(this);
	}

	public Forum getForum(String forumId, String state)
	{
		if (logger.isDebugEnabled())
			logger.debug("getForum()");

		return securityManager.filterForum(persistenceManager.getForum(forumId, state),null);
	}

	public Discussion getDiscussion(String discussionId, boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getDiscussion()");

		return persistenceManager.getDiscussion(discussionId, fully);
	}

	public List<Forum> getSiteForums(String siteId, boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getSiteForums()");
		
		return securityManager.filterFora(persistenceManager.getFora(siteId, fully));
	}
	
	public boolean addOrUpdateForum(Forum forum)
	{
		return addOrUpdateForum(forum, true);
	}

	public boolean addOrUpdateForum(Forum forum, boolean sendEmail)
	{
		if (logger.isDebugEnabled())
			logger.debug("addOrUpdateForum()");

		// Every forum needs a title
		if (forum.getTitle() == null || forum.getTitle().length() <= 0)
			return false;

		boolean creating = (forum.getId().length() == 0);

		boolean succeeded = persistenceManager.addOrUpdateForum(forum);

		if (succeeded && creating)
		{
			String reference = YaftForumService.REFERENCE_ROOT + "/" + forum.getSiteId() + "/forums/" + forum.getId();
			sakaiProxy.postEvent(YAFT_FORUM_CREATED, reference, true);
		}
		
		if(sendEmail)
			sendEmail(forum.getSiteId(), forum.getId(), null, false);

		return succeeded;
	}

	public SakaiProxy getSakaiProxy()
	{
		return sakaiProxy;
	}

	public boolean addOrUpdateMessage(String siteId, String forumId, Message message, boolean sendMail)
	{
		if (logger.isDebugEnabled())
			logger.debug("addOrUpdateMessage()");

		String discussionId = message.getDiscussionId();

		if (!persistenceManager.addOrUpdateMessage(siteId, forumId, message,null))
			return false;

		// persistenceManager.markMessageRead(message.getId(), forumId, message.getDiscussionId());

		String reference = YaftForumService.REFERENCE_ROOT + "/" + siteId + "/messages/" + message.getId();

		sakaiProxy.postEvent(YAFT_MESSAGE_CREATED, reference, true);

		if (sendMail && "READY".equals(message.getStatus()))
			sendEmail(siteId, forumId, message, false);

		return true;
	}

	public Discussion addDiscussion(String siteId, String forumId, Discussion discussion, boolean sendMail)
	{
		if (logger.isDebugEnabled())
			logger.debug("addDiscussion()");

		Message message = discussion.getFirstMessage();

		// Get this before calling addDiscussion as it will get set by it.
		String id = discussion.getId();

		if(persistenceManager.addDiscussion(siteId, forumId, discussion))
		{
			if (id.length() == 0)
			{
				// From the empty id we know this is a new discussion
				String reference = YaftForumService.REFERENCE_ROOT + "/" + siteId + "/discussions/" + message.getId();
				sakaiProxy.postEvent(YAFT_DISCUSSION_CREATED, reference, true);
			}

			if (sendMail)
				sendEmail(siteId, forumId, message, true);
		}

		return discussion;
	}

	public List<Forum> getFora(boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getFora()");

		List<Forum> fora = securityManager.filterFora(persistenceManager.getFora());

		for (Forum forum : fora)
		{
			forum.setDiscussions(getForumDiscussions(forum.getId(), fully));
			// forum.setUrl(sakaiProxy.getDirectUrl("/forums/" + forum.getId()));
		}

		return fora;
	}

	public List<Discussion> getForumDiscussions(String forumId, boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getForumDiscussions(" + forumId + ")");

		return persistenceManager.getForumDiscussions(forumId, fully);
	}

	public List<Message> getMessages()
	{
		if (logger.isDebugEnabled())
			logger.debug("getMessages()");

		return persistenceManager.getMessages();
	}

	public void deleteForum(String forumId)
	{
		persistenceManager.deleteForum(forumId);
		String reference = YaftForumService.REFERENCE_ROOT + "/" + sakaiProxy.getCurrentSiteId() + "/forums/" + forumId;
		sakaiProxy.postEvent(YAFT_FORUM_DELETED, reference, true);
	}

	public boolean deleteDiscussion(String discussionId)
	{
		try
		{
			Discussion discussion = getDiscussion(discussionId, false);

			if (persistenceManager.deleteDiscussion(discussionId))
			{
				sakaiProxy.removeCalendarEntry("Start of '" + discussion.getSubject() + "'", "Start of '" + discussion.getSubject() + "' Discussion (Click to launch)");
				sakaiProxy.removeCalendarEntry("End of '" + discussion.getSubject() + "'", "End of '" + discussion.getSubject() + "' Discussion");

				String reference = YaftForumService.REFERENCE_ROOT + "/" + sakaiProxy.getCurrentSiteId() + "/discussions/" + discussionId;
				sakaiProxy.postEvent(YAFT_DISCUSSION_DELETED, reference, true);

				return true;
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to delete discussion.", e);
		}

		return false;
	}

	public void deleteMessage(Message message, String forumId)
	{
		persistenceManager.deleteMessage(message, forumId);
		String reference = YaftForumService.REFERENCE_ROOT + "/" + sakaiProxy.getCurrentSiteId() + "/messages/" + message.getId();
		sakaiProxy.postEvent(YAFT_MESSAGE_DELETED, reference, true);
	}

	public void undeleteMessage(Message message, String forumId)
	{
		persistenceManager.undeleteMessage(message, forumId);
	}

	public void unsubscribeFromDiscussion(String userId, String discussionId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		persistenceManager.unsubscribeFromDiscussion(userId, discussionId);
	}

	public List<String> getDiscussionUnsubscriptions(String userId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		return persistenceManager.getDiscussionUnsubscriptions(userId);
	}

	public void subscribeToDiscussion(String userId, String discussionId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		persistenceManager.subscribeToDiscussion(userId, discussionId);
	}

	public void subscribeToForum(String forumId)
	{
		String userId = sakaiProxy.getCurrentUser().getId();

		persistenceManager.subscribeToForum(userId, forumId);
	}

	public void unsubscribeFromForum(String forumId)
	{
		String userId = sakaiProxy.getCurrentUser().getId();

		persistenceManager.unsubscribeFromForum(userId, forumId);
	}

	public void showMessage(Message message)
	{
		persistenceManager.showMessage(message);
	}

	public void deleteAttachment(String attachmentId, String messageId)
	{
		persistenceManager.deleteAttachment(attachmentId, messageId);
	}

	public Message getMessage(String messageId)
	{
		return persistenceManager.getMessage(messageId);
	}

	public Forum getForumContainingMessage(String messageId)
	{
		return securityManager.filterForum(persistenceManager.getForumContainingMessage(messageId), null);
	}

	public boolean markMessageRead(String messageId, String forumId, String discussionId)
	{
		return persistenceManager.markMessageRead(messageId, forumId, discussionId);
	}

	public boolean markMessageUnRead(String messageId, String forumId, String discussionId)
	{
		return persistenceManager.markMessageUnRead(messageId, forumId, discussionId);
	}

	public boolean markDiscussionRead(String discussionId, String forumId)
	{
		return persistenceManager.markDiscussionRead(discussionId, forumId);
	}

	public List<String> getReadMessageIds(String discussionId)
	{
		return persistenceManager.getReadMessageIds(discussionId);
	}

	public void moveDiscussion(String discussionId, String currentForumId, String newForumId)
	{
		persistenceManager.moveDiscussion(discussionId, currentForumId, newForumId);
	}

	private void sendEmail(String siteId, String forumId, Message message, boolean newDiscussion)
	{
		try
		{
			Site site = null;

			if (siteId == null)
				site = sakaiProxy.getCurrentSite();
			else
				site = sakaiProxy.getSite(siteId);

			String siteTitle = "";

			if (site != null)
				siteTitle = site.getTitle();
			
			Set<String> users = null;
			
			boolean newForum = false;
			
			if(message == null) newForum = true;
			
			Forum forum = persistenceManager.getForum(forumId, ForumPopulatedStates.EMPTY);
			
			List<Group> groups = forum.getGroups();
			
			if(groups.size() > 0)
			{
				users = sakaiProxy.getGroupMemberIds(groups);
			}
			else
			{
				users = site.getUsers();
			}
			
			// Make sure the current user is included
			users.add(sakaiProxy.getCurrentUser().getId());
			
			String url = "";

			if(!newForum)
			{
				List<String> unsubscribers = persistenceManager.getDiscussionUnsubscribers(message.getDiscussionId());

				for (String excludedId : unsubscribers)
					users.remove(excludedId);

				url = sakaiProxy.getDirectUrl(siteId, "/messages/" + message.getId() + ".html");
			}
			else
				url = sakaiProxy.getDirectUrl(siteId, "/forums/" + forumId + ".html");

			Map<String, String> replacementValues = new HashMap<String, String>();
			
			String templateKey = "";

			if (newDiscussion)
			{
				templateKey = "yaft.newDiscussion";
				replacementValues.put("siteTitle", siteTitle);
				replacementValues.put("forumMessage", "Forum Message");
				replacementValues.put("discussionSubject", message.getSubject());
				replacementValues.put("creator", message.getCreatorDisplayName());
				replacementValues.put("url", url);
				replacementValues.put("messageContent", message.getContent());
			}
			else if (newForum)
			{
				templateKey = "yaft.newForum";
				replacementValues.put("siteTitle", siteTitle);
				replacementValues.put("forumMessage", "Forum Message");
				replacementValues.put("forumTitle", forum.getTitle());
				replacementValues.put("forumDescription", forum.getDescription());
				replacementValues.put("creator", sakaiProxy.getDisplayNameForUser(forum.getCreatorId()));
				replacementValues.put("url", url);
				
			}
			else
			{
				templateKey = "yaft.newMessage";
				replacementValues.put("siteTitle", siteTitle);
				replacementValues.put("forumMessage", "Forum Message");
				replacementValues.put("messageSubject", message.getSubject());
				replacementValues.put("creator", message.getCreatorDisplayName());
				replacementValues.put("url", url);
				replacementValues.put("messageContent", message.getContent());
			}

			List<User> sakaiUsers = sakaiProxy.getUsers(users);
			
			boolean canSetHtml = false;
			boolean canSetVersion = false;
		
			// The 2.6.x version of EmailTemplateService doesn't have html
			// methods. We need to test for it or else we'll get a runtime error
			Class templateClass = EmailTemplate.class;
		
			try
			{
				templateClass.getDeclaredMethod("setHtmlMessage", new Class[] {String.class});
				canSetHtml = true;
			}
			catch(NoSuchMethodException nsme) {}
		
			try
			{
				templateClass.getDeclaredMethod("setVersion", new Class[] {int.class});
				canSetVersion = true;
			}
			catch(NoSuchMethodException nsme) {}

			// get the rendered template for each user
			for (User user : sakaiUsers)
			{
				YaftPreferences prefs = persistenceManager.getPreferencesForUser(user.getId(), siteId);

				String emailPref = prefs.getEmail();

				if (YaftPreferences.NEVER.equals(emailPref))
					continue;

				if (logger.isInfoEnabled())
					logger.info("SakaiProxy.sendEmail() attempting to send email to: " + user.getId());
				
				RenderedTemplate template = sakaiProxy.getRenderedTemplateForUser(templateKey, user.getReference(), replacementValues);
			
				if (template == null)
				{
					logger.error("SakaiProxy.sendEmail() no template with key: " + templateKey);
					return; // no template
				}

				try
				{
					if (emailPref.equals(YaftPreferences.EACH))
					{
						if(canSetHtml)
							sakaiProxy.sendEmail(user.getId(), template.getRenderedSubject(), template.getRenderedHtmlMessage());
						else
							sakaiProxy.sendEmail(user.getId(), template.getRenderedSubject(), template.getRenderedMessage());
					}
					else if (emailPref.equals(YaftPreferences.DIGEST))
						sakaiProxy.addDigestMessage(user.getId(), template.getRenderedSubject(), template.getRenderedMessage());
				}
				catch (Exception e)
				{
					logger.error("SakaiProxy.sendEmail() error retrieving template for user: " + user.getId() + " with key: " + templateKey + " : " + e.getClass() + " : " + e.getMessage());
					continue; // try next user
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Failed to send message email.", e);
		}
	}

	public void publishMessage(String forumId, Message message)
	{
		persistenceManager.publishMessage(forumId, message.getId());

		sendEmail(null, forumId, message, false);
	}

	/** START EntityProducer IMPLEMENTATION */

	public String archive(String siteId, Document doc, Stack stack, String archivePath, List attachments)
	{
		if (logger.isDebugEnabled())
			logger.debug("archive(siteId:" + siteId + ",archivePath:" + archivePath + ")");

		StringBuilder results = new StringBuilder();

		results.append(getLabel() + ": Started.\n");

		int forumCount = 0;

		try
		{
			// start with an element with our very own (service) name
			Element element = doc.createElement(YaftForumService.class.getName());
			element.setAttribute("version", "2.5.x");
			((Element) stack.peek()).appendChild(element);
			stack.push(element);

			Element forums = doc.createElement("forums");
			element.appendChild(forums);
			stack.push(forums);
			List<Forum> fora = getSiteForums(siteId, true);
			if (fora != null && fora.size() > 0)
			{
				for (Forum forum : fora)
				{
					forum.toXml(doc, stack);
					forumCount++;
				}
			}

			stack.pop();

			results.append(getLabel() + ": Finished. " + forumCount + " forum(s) archived.\n");
		}
		catch (Exception any)
		{
			results.append(getLabel() + ": exception caught. Message: " + any.getMessage());
			logger.warn(getLabel() + " exception caught. Message: " + any.getMessage());
		}

		stack.pop();

		return results.toString();
	}

	public Entity getEntity(Reference reference)
	{
		// TODO Auto-generated method stub
		String referenceString = reference.getReference();
		String[] parts = referenceString.split(Entity.SEPARATOR);

		if (!parts[0].equals(REFERENCE_ROOT))
			return null;

		String type = parts[1];

		String id = parts[2];

		if ("messages".equals(type))
		{
			return getMessage(id);
		}

		return null;
	}

	public Collection getEntityAuthzGroups(Reference arg0, String arg1)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getEntityDescription(Reference arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ResourceProperties getEntityResourceProperties(Reference arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getEntityUrl(Reference reference)
	{
		String referenceString = reference.getReference();
		return null;
	}

	public HttpAccess getHttpAccess()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String getLabel()
	{
		return ENTITY_PREFIX;
	}

	public String merge(String siteId, Element root, String archivePath, String fromSiteId, Map attachmentNames, Map userIdTrans, Set userListAllowImport)
	{
		logger.debug("merge(siteId:" + siteId + ",root tagName:" + root.getTagName() + ",archivePath:" + archivePath + ",fromSiteId:" + fromSiteId);

		StringBuilder results = new StringBuilder();

		try
		{
			int forumCount = 0;

			NodeList forumNodes = root.getElementsByTagName(XmlDefs.FORUM);
			final int numberForums = forumNodes.getLength();

			for (int i = 0; i < numberForums; i++)
			{
				Node child = forumNodes.item(i);
				if (child.getNodeType() != Node.ELEMENT_NODE)
				{
					// Problem
					continue;
				}

				Element forumElement = (Element) child;

				Forum forum = new Forum();
				forum.fromXml(forumElement);
				forum.setSiteId(siteId);

				addOrUpdateForum(forum);

				NodeList discussionNodes = forumElement.getElementsByTagName(XmlDefs.DISCUSSION);

				for (int j = 0; j < discussionNodes.getLength(); j++)
				{
					Node discussionNode = discussionNodes.item(j);
					NodeList discussionChildNodes = discussionNode.getChildNodes();
					for (int k = 0; k < discussionChildNodes.getLength(); k++)
					{
						Node discussionChildNode = discussionChildNodes.item(k);
						if (discussionChildNode.getNodeType() == Node.ELEMENT_NODE && XmlDefs.MESSAGES.equals(((Element) discussionChildNode).getTagName()))
						{
							NodeList messageNodes = discussionChildNode.getChildNodes();
							mergeDescendantMessages(siteId, forum.getId(), null, null, messageNodes, results);
							break;
						}
					}
				}

				forumCount++;
			}

			results.append("Stored " + forumCount + " forums.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return results.toString();
	}

	private void mergeDescendantMessages(String siteId, String forumId, String discussionId, String parentId, NodeList messageNodes, StringBuilder results)
	{
		for (int i = 0; i < messageNodes.getLength(); i++)
		{
			Node node = messageNodes.item(i);

			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;

			Element messageElement = (Element) node;

			if (!"message".equals(messageElement.getTagName()))
				continue;

			Message message = new Message();
			message.fromXml(messageElement);
			message.setParent(parentId);
			message.setSiteId(siteId);

			if (parentId == null)
				discussionId = message.getId();

			message.setDiscussionId(discussionId);

			addOrUpdateMessage(siteId, forumId, message, false);

			NodeList repliesNodes = messageElement.getElementsByTagName(XmlDefs.REPLIES);
			if (repliesNodes.getLength() >= 1)
			{
				Node repliesNode = repliesNodes.item(0);
				NodeList replyMessageNodes = repliesNode.getChildNodes();
				mergeDescendantMessages(siteId, forumId, discussionId, message.getId(), replyMessageNodes, results);
			}
		}
	}

	public boolean parseEntityReference(String referenceString, Reference reference)
	{
		String[] parts = referenceString.split(Entity.SEPARATOR);

		if (!parts[0].equals(REFERENCE_ROOT))
			return false;

		String type = parts[1];

		String id = parts[2];

		if ("messages".equals(type))
		{
			// Message message = getMessage(id);
			return true;
		}

		return false;
	}

	public boolean willArchiveMerge()
	{
		return true;
	}

	/** END EntityProducer IMPLEMENTATION */

	public Map<String, Integer> getReadMessageCountForAllFora()
	{
		return persistenceManager.getReadMessageCountForAllFora(sakaiProxy.getCurrentUser().getId());
	}

	public Map<String, Integer> getReadMessageCountForForum(String forumId)
	{
		return persistenceManager.getReadMessageCountForForum(sakaiProxy.getCurrentUser().getId(), forumId);
	}

	public Forum getForumForTitle(String title, String state, String siteId)
	{
		return securityManager.filterForum(persistenceManager.getForumForTitle(title, state, siteId),siteId);
	}

	public List<String> getForumUnsubscriptions(String userId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		return persistenceManager.getForumUnsubscriptions(userId);
	}

	public boolean savePreferences(YaftPreferences preferences)
	{
		return persistenceManager.savePreferences(preferences);
	}

	public YaftPreferences getPreferencesForUser(String user, String siteId)
	{
		return persistenceManager.getPreferencesForUser(user, siteId);
	}

	public YaftPreferences getPreferencesForCurrentUserAndSite()
	{
		return persistenceManager.getPreferencesForCurrentUserAndSite();
	}

	public List<ActiveDiscussion> getActiveDiscussions(String siteId)
	{
		return persistenceManager.getActiveDiscussions(siteId);
	}

	public String getIdOfSiteContainingMessage(String messageId)
	{
		return persistenceManager.getIdOfSiteContainingMessage(messageId);
	}

	public void setUseSynopticFunctionality(boolean useSynopticFunctionality)
	{
		this.useSynopticFunctionality = useSynopticFunctionality;
	}

	public boolean isUseSynopticFunctionality()
	{
		return useSynopticFunctionality;
	}
}
