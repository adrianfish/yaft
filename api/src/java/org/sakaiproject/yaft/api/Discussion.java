package org.sakaiproject.yaft.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Discussion
{
	/**
	 * The number of messages in this discussion
	 */
	private int messageCount = 0;
	
	private long lastMessageDate;

	/** Top level message */
	private Message firstMessage;
	
	private String forumId;
	
	private String url;
	
	private String status = "READY";
	
	private List<Attachment> attachments = new ArrayList<Attachment>();
	
	// We need this to build direct urls in the rendered pages. Bogus, but necessary.
	private String pageId;
	
	public void setFirstMessage(Message firstMessage)
	{
		this.firstMessage = firstMessage;
	}

	public String getId()
	{
		return firstMessage.getId();
	}

	public void setMessageCount(int messageCount)
	{
		this.messageCount = messageCount;
	}

	public int getMessageCount()
	{
		return messageCount;
	}

	public String getSubject()
	{
		return firstMessage.getSubject();
	}

	public long getCreatedDate()
	{
		return firstMessage.getCreatedDate();
	}

	public String getCreatorId()
	{
		return firstMessage.getCreatorId();
	}

	public String getCreatorDisplayName()
	{
		return firstMessage.getCreatorDisplayName();
	}
	
	public Message getFirstMessage()
	{
		return firstMessage;
	}

	public void setForumId(String forumId)
	{
		this.forumId = forumId;
	}

	public String getForumId()
	{
		return forumId;
	}

	public void setLastMessageDate(long lastMessageDate)
	{
		this.lastMessageDate = lastMessageDate;
	}

	public long getLastMessageDate()
	{
		return lastMessageDate;
	}

	public void setPageId(String pageId)
	{
		this.pageId = pageId;
	}

	public String getPageId()
	{
		return pageId;
	}

	public String getUrl()
	{
		return "/portal/tool/" + getPlacementId() + "/discussions/" + getId();//firstMessage.getUrl();
	}

	public String getPlacementId()
	{
		return firstMessage.getPlacementId();
	}
	
	public Element toXml(Document doc,Stack stack)
	{
		Element discussionElement = doc.createElement(XmlDefs.DISCUSSION);

		if (stack.isEmpty())
		{
			doc.appendChild(discussionElement);
		}
		else
		{
			((Element) stack.peek()).appendChild(discussionElement);
		}

		//stack.push(discussionElement);

		discussionElement.setAttribute(XmlDefs.ID, getId());
		discussionElement.setAttribute(XmlDefs.CREATED_DATE, Long.toString(getCreatedDate()));
		discussionElement.setAttribute(XmlDefs.CREATOR_ID, getCreatorId());
		discussionElement.setAttribute(XmlDefs.LAST_MESSAGE_DATE, Long.toString(lastMessageDate));
		discussionElement.setAttribute(XmlDefs.MESSAGE_COUNT, Integer.toString(messageCount));
		
		/*
		Element subjectElement = doc.createElement(XmlDefs.SUBJECT);
		subjectElement.setTextContent(getSubject());
		discussionElement.appendChild(subjectElement);
		*/
		
		Element messagesElement = doc.createElement(XmlDefs.MESSAGES);
		discussionElement.appendChild(messagesElement);
		stack.push(messagesElement);
		
		firstMessage.toXml(doc,stack);
		
		stack.pop();
		
		return discussionElement;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public String getStatus()
	{
		return status;
	}
}
