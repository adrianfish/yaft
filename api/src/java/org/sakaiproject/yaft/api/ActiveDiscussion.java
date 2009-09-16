package org.sakaiproject.yaft.api;

public class ActiveDiscussion
{
	private int newMessages = 0;
	private String url = "";
	private String subject = "";
	private String latestMessageSubject = "";
	private long lastMessageDate = 0L;
	
	public void setNewMessages(int newMessages)
	{
		this.newMessages = newMessages;
	}
	public int getNewMessages()
	{
		return newMessages;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	public String getSubject()
	{
		return subject;
	}
	public void setLastMessageDate(long lastMessageDate)
	{
		this.lastMessageDate = lastMessageDate;
	}
	public long getLastMessageDate()
	{
		return lastMessageDate;
	}
	public void setLatestMessageSubject(String latestMessageSubject)
	{
		this.latestMessageSubject = latestMessageSubject;
	}
	public String getLatestMessageSubject()
	{
		return latestMessageSubject;
	}
}
