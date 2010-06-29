package org.sakaiproject.yaft.tool.entityprovider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;

public class YaftMessageEntityProvider extends AbstractEntityProvider implements Resolvable, CoreEntityProvider, AutoRegisterEntityProvider, Createable, Inputable, Outputable, Describeable, ActionsExecutable
{
	protected final Logger LOG = Logger.getLogger(getClass());
	
	public final static String ENTITY_PREFIX = "yaft-message";
	
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
		
		if (id == null || "".equals(id))
			return false;
		
		try
		{
			Message message = yaftForumService.getMessage(id);
			return message != null;
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting message.", e);
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
			throw new IllegalArgumentException("No message id supplied");
		
		Message message = null;

		try
		{
			message = yaftForumService.getMessage(id);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting message.", e);
		}

		if (message == null)
			throw new IllegalArgumentException("Message not found");

		return message;
	}
	
	public String[] getHandledInputFormats()
	{
		return new String[] { Formats.HTML, Formats.JSON, Formats.FORM };
	}

	public String createEntity(EntityReference ref, Object entity, Map<String, Object> params)
	{
		String siteId = (String) params.get("siteId");
		String status = (String) params.get("status");
		String subject = (String) params.get("subject");
		String content = (String) params.get("content");
		String forumId = (String) params.get("forumId");
		String viewMode = (String) params.get("viewMode");
		String messageId = (String) params.get("messageId");
		String messageBeingRepliedTo = (String) params.get("messageBeingRepliedTo");
		String discussionId = (String) params.get("discussionId");

		if (LOG.isDebugEnabled())
		{
			LOG.debug("Status: " + status);
			LOG.debug("Subject: " + subject);
			LOG.debug("Content: " + content);
			LOG.debug("Forum ID: " + forumId);
			LOG.debug("View Mode: " + viewMode);
			LOG.debug("Discussion ID: " + discussionId);
			LOG.debug("Message Being Replied To: " + messageBeingRepliedTo);
		}
		
		if(viewMode == null || viewMode.length() <= 0)
			viewMode = "full";
		
		String currentUserId = developerHelperService.getCurrentUserId();
		
		Message message = new Message();
		message.setStatus(status);
		message.setSubject(subject);
		message.setContent(content);
		message.setSiteId(siteId);
		message.setCreatorId(currentUserId);
		message.setCreatorDisplayName(sakaiProxy.getDisplayNameForUser(currentUserId));
		message.setDiscussionId(discussionId);
		message.setAttachments(getAttachments(params));
		
		// If no message id has been supplied this must be a new message, so
		// we set the message id to empty
		if(messageId == null)
			message.setId("");

		if (messageBeingRepliedTo != null && messageBeingRepliedTo.length() > 0)
			message.setParent(messageBeingRepliedTo);
		else if (messageBeingRepliedTo == null || messageBeingRepliedTo.length() <= 0)
		{
			// This is a discussion, or top level message
			message.setId(messageId);
		}
		
		if(yaftForumService.addOrUpdateMessage(siteId, forumId, message, true))
			return message.getId();
		else
			throw new EntityException("Failed to add message.",messageId);
	}
	
	public Object getSampleEntity()
	{
		return new Message();
	}

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
	}

	public String[] getHandledOutputFormats()
	{
		return new String[] { Formats.JSON };
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
