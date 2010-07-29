package org.sakaiproject.yaft.tool.entityprovider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Createable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Inputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.impl.SakaiProxyImpl;

public class YaftDiscussionEntityProvider extends AbstractEntityProvider implements Resolvable, CoreEntityProvider, AutoRegisterEntityProvider, Createable, Inputable, Outputable, Describeable, ActionsExecutable
{
	protected final Logger LOG = Logger.getLogger(getClass());
	
	public final static String ENTITY_PREFIX = "yaft-discussion";
	
	private YaftForumService yaftForumService = null;
	
	public void setYaftForumService(YaftForumService yaftForumService)
	{
		this.yaftForumService = yaftForumService;
	}
	
	private SakaiProxy sakaiProxy  = null;
	
	public void init()
	{
		sakaiProxy = yaftForumService.getSakaiProxy();
	}

	public boolean entityExists(String id)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("entityExists(" + id + ")");
		
		if (id == null)
		{
			return false;
		}

		if ("".equals(id))
			return false;
		
		if("unsubscriptions".equals(id))
			return true;
		
		try
		{
			Discussion discussion = yaftForumService.getDiscussion(id, true);
			return discussion != null;
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting discussion.", e);
			return false;
		}
	}

	public Object getEntity(EntityReference ref)
	{
		if (LOG.isDebugEnabled())
			LOG.debug("getEntity(" + ref.getId() + ")");
		
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null)
			throw new EntityException("Not logged in",ref.getReference(),HttpServletResponse.SC_UNAUTHORIZED);

		String id = ref.getId();

		if (id == null || "".equals(id))
			throw new IllegalArgumentException("No forum id supplied");
		
		if("unsubscriptions".equals(id))
			return yaftForumService.getDiscussionUnsubscriptions(userId);
		
		Discussion discussion = null;

		try
		{
			discussion = yaftForumService.getDiscussion(id, true);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting discussion.", e);
		}

		if (discussion == null)
		{
			throw new IllegalArgumentException("Discussion not found");
		}

		return discussion;
	}
	
	public String[] getHandledInputFormats()
	{
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params)
	{
		String siteId = (String) params.get("siteId");
		String id = (String) params.get("id");
		String subject = (String) params.get("subject");
		String content = (String) params.get("content");
		String forumId = (String) params.get("forumId");
		String startDate= (String) params.get("startDate");
		String endDate= (String) params.get("endDate");
		String lockWritingString = (String) params.get("lockWriting");
		String lockReadingString = (String) params.get("lockReading");
		
		if(subject == null || subject.length() <= 0)
			throw new IllegalArgumentException("You must supply a subject.");
		
		if(content == null || content.length() <= 0)
			throw new IllegalArgumentException("You must supply some content.");
		
		boolean lockWriting = true;
		boolean lockReading = true;
		
		if(lockWritingString != null)
			lockWriting = lockWritingString.equals("true");
		else
			lockWriting = false;
		
		if(lockReadingString != null)
			lockReading = lockReadingString.equals("true");
		else
			lockReading = false;

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Subject: " + subject);
			LOG.debug("Content: " + content);
			LOG.debug("Forum ID: " + forumId);
		}
		
		if(subject == null || subject.length() <= 0)
			throw new IllegalArgumentException("Subject must be supplied.");

		Message message = new Message();
		
		if(id != null)
			message.setId(id);
		
		String currentUserId = developerHelperService.getCurrentUserId();
		
		message.setSubject(subject);
		message.setContent(content);
		message.setSiteId(siteId);
		message.setCreatorId(currentUserId);
		message.setCreatorDisplayName(sakaiProxy.getDisplayNameForUser(currentUserId));
		message.setAttachments(getAttachments(params));
		
		// The first messages in discussions always have the same id as the
		// discussion
		message.setDiscussionId(message.getId());
		
		message.setStatus("READY");
		
		Discussion discussion = new Discussion();
		discussion.setFirstMessage(message);
		discussion.setLockedForWriting(lockWriting);
		discussion.setLockedForReading(lockReading);
		
		if(startDate != null && startDate.length() > 0
				&& endDate != null && endDate.length() > 0)
		{
			try
			{
				long start = Long.parseLong(startDate);
				long end = Long.parseLong(endDate);
				if(start > 0L && end > 0L)
				{
					if(end <= start)
					{
						throw new IllegalArgumentException("The end date MUST come after the start date.");
					}
					else
					{
						discussion.setStart(start);
						discussion.setEnd(end);
					}
				}
			}
			catch(NumberFormatException pe)
			{
				throw new IllegalArgumentException("The start and end dates MUST be supplied in millisecond format.");
			}
		}

		if(yaftForumService.addDiscussion(siteId, forumId, discussion, true) != null)
		{
			//try
			//{
				return discussion.getId();
			//}
			//catch(UnsupportedEncodingException e)
			//{
				//return URLEncoder.encode("<textarea>" + discussion.getId() + "</textarea>");
			//}
		}
		else
			throw new EntityException("Failed to add discussion..",id);
	}
	
	public Object getSampleEntity()
	{
		return new Discussion();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats()
	{
		return new String[] { Formats.JSON };
	}
	
	@EntityCustomAction(action = "readMessages", viewKey = EntityView.VIEW_SHOW)
	public Object handleReadMessages(EntityReference ref,Map<String,Object> params)
	{
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null)
			throw new EntityException("Not logged in",ref.getReference(),HttpServletResponse.SC_UNAUTHORIZED);

		String discussionId = ref.getId();
		
		if (discussionId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the discussion id");
		
		List<String> ids = yaftForumService.getReadMessageIds(discussionId);
		
		if(ids.size() == 0)
			throw new EntityException("No read messages",ref.getReference(),HttpServletResponse.SC_NOT_FOUND);
		
		return ids;
	}
	
	@EntityCustomAction(action = "discussionContainingMessage", viewKey = EntityView.VIEW_LIST)
	public Object handleDiscussionContainingMessage(EntityReference ref,Map<String,Object> params)
	{
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null)
			throw new EntityException("Not logged in",ref.getReference(),HttpServletResponse.SC_UNAUTHORIZED);

		String messageId = (String) params.get("messageId");
		
		if (messageId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the message id");
		
		Message message = yaftForumService.getMessage(messageId);
		
		if(message == null)
			return null;
		
		return yaftForumService.getDiscussion(message.getDiscussionId(),true);
	}
	
	private List<Attachment> getAttachments(Map<String,Object> params)
	{
		List<FileItem> fileItems = new ArrayList<FileItem>();

		try
		{
			FileItem attachment1 = (FileItem) params.get("attachment_0");
			if (attachment1 != null && attachment1.getSize() > 0)
				fileItems.add(attachment1);
			FileItem attachment2 = (FileItem) params.get("attachment_1");
			if (attachment2 != null && attachment2.getSize() > 0)
				fileItems.add(attachment2);
			FileItem attachment3 = (FileItem) params.get("attachment_2");
			if (attachment3 != null && attachment3.getSize() > 0)
				fileItems.add(attachment3);
			FileItem attachment4 = (FileItem) params.get("attachment_3");
			if (attachment4 != null && attachment4.getSize() > 0)
				fileItems.add(attachment4);
			FileItem attachment5 = (FileItem) params.get("attachment_4");
			if (attachment5 != null && attachment5.getSize() > 0)
				fileItems.add(attachment5);
		}
		catch (Exception e)
		{
		}
		
		List<Attachment> attachments = new ArrayList<Attachment>();
		if (fileItems.size() > 0)
		{
		for (Iterator i = fileItems.iterator(); i.hasNext();)
		{
			FileItem fileItem = (FileItem) i.next();

			String name = fileItem.getName();

			if (name.contains("/"))
				name = name.substring(name.lastIndexOf("/") + 1);
			else if (name.contains("\\"))
				name = name.substring(name.lastIndexOf("\\") + 1);

			attachments.add(new Attachment(name, fileItem.getContentType(), fileItem.get()));
		}
		}
		
		return attachments;
	}
}
