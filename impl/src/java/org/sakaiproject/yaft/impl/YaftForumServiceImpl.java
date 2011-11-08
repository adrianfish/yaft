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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.SecurityAdvisor;
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
import org.sakaiproject.yaft.api.Author;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.XmlDefs;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftFunctions;
import org.sakaiproject.yaft.api.YaftPreferences;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class YaftForumServiceImpl implements YaftForumService, SecurityAdvisor
{
	private Logger logger = Logger.getLogger(YaftForumServiceImpl.class);

	private SakaiProxy sakaiProxy = null;

	private YaftPersistenceManager persistenceManager = null;

	private YaftSecurityManager securityManager;
	
	private boolean includeMessageBodyInEmail = false;

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
		persistenceManager.setSecurityManager(securityManager);
		persistenceManager.init();
		persistenceManager.setupTables();

		sakaiProxy.registerEntityProducer(this);
		sakaiProxy.pushSecurityAdvisor(this);
		
		includeMessageBodyInEmail = sakaiProxy.getIncludeMessageBodyInEmailSetting();
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
			// SiteStats/Search etc event
			sakaiProxy.postEvent(YAFT_FORUM_CREATED_SS, forum.getReference(), true);
			
			if(sendEmail) {
				// NotificationService event
				sakaiProxy.postEvent(YAFT_FORUM_CREATED, forum.getReference(), true);
			}
		}

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

		if (!persistenceManager.addOrUpdateMessage(siteId, forumId, message,null))
			return false;

		// persistenceManager.markMessageRead(message.getId(), forumId, message.getDiscussionId());

		// SiteStats/Search etc event
		sakaiProxy.postEvent(YAFT_MESSAGE_CREATED_SS, message.getReference(), true);

		if (sendMail && "READY".equals(message.getStatus()))
				sakaiProxy.postEvent(YAFT_MESSAGE_CREATED, message.getReference(), true);

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
				
				// SiteStats/Search etc event
				sakaiProxy.postEvent(YAFT_DISCUSSION_CREATED_SS, discussion.getReference(), true);
			}

			if (sendMail)
				sakaiProxy.postEvent(YAFT_DISCUSSION_CREATED, discussion.getReference(), true);
		}

		return discussion;
	}

	public List<Forum> getFora(boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getFora()");

		return securityManager.filterFora(persistenceManager.getFora(fully));
	}

	/**
	 * Used by LessonBuilder
	 */
	public List<Discussion> getForumDiscussions(String forumId, boolean fully)
	{
		if (logger.isDebugEnabled())
			logger.debug("getForumDiscussions(" + forumId + ")");

		Forum forum = persistenceManager.getForum(forumId, ForumPopulatedStates.PART);
		return securityManager.filterDiscussions(forum.getDiscussions());
	}

	public void deleteForum(String forumId)
	{
		persistenceManager.deleteForum(forumId);
		String reference = YaftForumService.REFERENCE_ROOT + "/" + sakaiProxy.getCurrentSiteId() + "/forums/" + forumId;
		sakaiProxy.postEvent(YAFT_FORUM_DELETED_SS, reference, true);
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
				sakaiProxy.postEvent(YAFT_DISCUSSION_DELETED_SS, reference, true);

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
		sakaiProxy.postEvent(YAFT_MESSAGE_DELETED_SS, reference, true);
	}

	public void undeleteMessage(Message message, String forumId)
	{
		persistenceManager.undeleteMessage(message, forumId);
	}

	public boolean unsubscribeFromDiscussion(String userId, String discussionId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		return persistenceManager.unsubscribeFromDiscussion(userId, discussionId);
	}

	public List<String> getDiscussionUnsubscriptions(String userId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		return persistenceManager.getDiscussionUnsubscriptions(userId);
	}

	public boolean subscribeToDiscussion(String userId, String discussionId)
	{
		if (userId == null)
			userId = sakaiProxy.getCurrentUser().getId();

		return persistenceManager.subscribeToDiscussion(userId, discussionId);
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
			    // This forum is limited to groups. Make sure the alert only goes
			    // to the group members
			    users = sakaiProxy.getGroupMemberIds(groups);
			    
			    // Maintainers need to get emails also.
			    users.addAll(sakaiProxy.getCurrentSiteMaintainers());
			}
			else
			{
				users = site.getUsers();
			}
			
			// Make sure the current user is included
			users.add(sakaiProxy.getCurrentUser().getId());
		
			Map<String, String> replacementValues = new HashMap<String, String>();
			
			String templateKey = "";

			if (newDiscussion)
			{
			   	templateKey = "yaft.newDiscussion";
				replacementValues.put("discussionSubject", message.getSubject());
			}
			else if (newForum)
			{
				templateKey = "yaft.newForum";
				
				replacementValues.put("forumTitle", forum.getTitle());
				replacementValues.put("forumDescription", forum.getDescription());
				
			}
			else
			{
			   	templateKey = "yaft.newMessage";
				replacementValues.put("messageSubject", message.getSubject());
			}
			
			replacementValues.put("forumMessage", "Forum Message");
			replacementValues.put("siteTitle", siteTitle);
			replacementValues.put("creator", sakaiProxy.getCurrentUser().getDisplayName());
			
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
			
			replacementValues.put("url", url);
			
			String unsubscribeUrl = sakaiProxy.getDirectUrl(siteId, "/unsubscribe");
			replacementValues.put("unsubscribeUrl", unsubscribeUrl);

			List<User> sakaiUsers = sakaiProxy.getUsers(users);

			// get the rendered template for each user
			for (User user : sakaiUsers)
			{
				YaftPreferences prefs = persistenceManager.getPreferencesForUser(user.getId(), siteId);

				String emailPref = prefs.getEmail();

				if (YaftPreferences.NEVER.equals(emailPref))
					continue;

				if (logger.isInfoEnabled())
					logger.info("Attempting to send email to: " + user.getId());
				
				RenderedTemplate template = sakaiProxy.getRenderedTemplateForUser(templateKey, user.getReference(), replacementValues);
			
				if (template == null)
				{
					logger.error("No template with key: " + templateKey);
					return; // no template
				}

				try
				{
					if (emailPref.equals(YaftPreferences.EACH))
					{
						sakaiProxy.sendEmail(user.getId(), template.getRenderedSubject(), template.getRenderedMessage(),false);
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
		String referenceString = reference.getReference();
		String[] parts = referenceString.split(Entity.SEPARATOR);

		if (!parts[1].equals("yaft"))
			return null;

		String type = parts[3];

		String entityId = parts[4];

		if ("forums".equals(type)) {
			return getForum(entityId,ForumPopulatedStates.EMPTY);
		} else if ("discussions".equals(type)) {
			return getDiscussion(entityId,false);
		} else if ("messages".equals(type)) {
			return getMessage(entityId);
		}

		return null;
	}

	public Collection getEntityAuthzGroups(Reference ref, String userId) {
		List ids = new ArrayList();
		ids.add("/site/" + ref.getContext());
		return ids;
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

		if (!parts[1].equals("yaft") || parts.length != 5) // Leading slash adds an empty element
			return false;
		
		String siteId = parts[2];
		String type = parts[3];
		String entityId = parts[4];
		
		if ("forums".equals(type)) {
			reference.set("yaft","forums" , entityId, null, siteId);
			return true;
		}
		else if ("discussions".equals(type)) {
			reference.set("yaft","discussions" , entityId, null, siteId);
			return true;
		}
		else if ("messages".equals(type)) {
			reference.set("yaft","messages" , entityId, null, siteId);
			return true;
		}

		return false;
	}

	public boolean willArchiveMerge()
	{
		return true;
	}

	/** END EntityProducer IMPLEMENTATION */
	
	public SecurityAdvice isAllowed(String userId, String function, String reference) {
		return null;
	}

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
	
	public List<Author> getAuthorsForCurrentSite() {
		return persistenceManager.getAuthorsForCurrentSite();
	}

	public List<Message> getMessagesForAuthorInCurrentSite(String authorId) {
		return persistenceManager.getMessagesForAuthorInCurrentSite(authorId);
	}

	public List<Author> getAuthorsForDiscussion(String discussionId) {
		return persistenceManager.getAuthorsForDiscussion(discussionId);
	}

	public List<Message> getMessagesForAuthorInDiscussion(String authorId, String discussionId) {
		return persistenceManager.getMessagesForAuthorInDiscussion(authorId,discussionId);
	}

	public boolean setDiscussionGroups(String discussionId, Collection<String> groups) {
		return persistenceManager.setDiscussionGroups(discussionId,groups);
	}

}
