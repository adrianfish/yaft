package org.sakaiproject.yaft.api;

import java.util.ArrayList;
import java.util.List;

public class SearchResult
{
	private String url = "";
	private List<String> contextFragments = new ArrayList<String>();
	private String title = "";
	private String messageId = "";
	private String discussionId = "";
	private String forumId = "";
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getTitle()
	{
		return title;
	}
	public void setContextFragments(List<String> contextFragments)
	{
		this.contextFragments = contextFragments;
	}
	public List<String> getContextFragments()
	{
		return contextFragments;
	}
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	public String getMessageId()
	{
		return messageId;
	}
	public void setDiscussionId(String discussionId)
	{
		this.discussionId = discussionId;
	}
	public String getDiscussionId()
	{
		return discussionId;
	}
	public void setForumId(String forumId)
	{
		this.forumId = forumId;
	}
	public String getForumId()
	{
		return forumId;
	}
}
