package org.sakaiproject.yaft.api;

public class YaftPermissions
{
	private boolean forumCreate = false;
	private boolean forumDeleteOwn = false;
	private boolean forumDeleteAny = false;
	private boolean discussionCreate = false;
	private boolean discussionDeleteOwn = false;
	private boolean discussionDeleteAny = false;
	private boolean messageCreate = false;
	private boolean messageCensor = false;
	private boolean messageDeleteOwn = false;
	private boolean messageUnDelete = false;
	private boolean messageDeleteAny = false;
	private boolean canModifyPermissions = false;
	private boolean viewInvisible = false;
	private String role = "";
	
	public void setRole(String role)
	{
		this.role = role;
	}
	
	public String getRole()
	{
		return role;
	}

	public void setForumCreate(boolean forumCreate)
	{
		this.forumCreate = forumCreate;
	}

	public boolean isForumCreate()
	{
		return forumCreate;
	}

	public void setDiscussionCreate(boolean discussionCreate)
	{
		this.discussionCreate = discussionCreate;
	}

	public boolean isDiscussionCreate()
	{
		return discussionCreate;
	}

	public void setMessageCreate(boolean messageCreate)
	{
		this.messageCreate = messageCreate;
	}

	public boolean isMessageCreate()
	{
		return messageCreate;
	}

	public void setMessageCensor(boolean messageCensor)
	{
		this.messageCensor = messageCensor;
	}

	public boolean isMessageCensor()
	{
		return messageCensor;
	}

	public void setCanModifyPermissions(boolean canModifyPermissions)
	{
		this.canModifyPermissions = canModifyPermissions;
	}

	public boolean isCanModifyPermissions()
	{
		return canModifyPermissions;
	}

	public void setForumDeleteOwn(boolean forumDeleteOwn)
	{
		this.forumDeleteOwn = forumDeleteOwn;
	}

	public boolean isForumDeleteOwn()
	{
		return forumDeleteOwn;
	}

	public void setForumDeleteAny(boolean forumDeleteAny)
	{
		this.forumDeleteAny = forumDeleteAny;
	}

	public boolean isForumDeleteAny()
	{
		return forumDeleteAny;
	}

	public void setDiscussionDeleteOwn(boolean discussionDeleteOwn)
	{
		this.discussionDeleteOwn = discussionDeleteOwn;
	}

	public boolean isDiscussionDeleteOwn()
	{
		return discussionDeleteOwn;
	}

	public void setDiscussionDeleteAny(boolean discussionDeleteAny)
	{
		this.discussionDeleteAny = discussionDeleteAny;
	}

	public boolean isDiscussionDeleteAny()
	{
		return discussionDeleteAny;
	}

	public void setMessageDeleteOwn(boolean messageDeleteOwn)
	{
		this.messageDeleteOwn = messageDeleteOwn;
	}

	public boolean isMessageDeleteOwn()
	{
		return messageDeleteOwn;
	}

	public void setMessageDeleteAny(boolean messageDeleteAny)
	{
		this.messageDeleteAny = messageDeleteAny;
	}

	public boolean isMessageDeleteAny()
	{
		return messageDeleteAny;
	}

	public void setViewInvisible(boolean viewInvisible)
	{
		this.viewInvisible = viewInvisible;
	}

	public boolean isViewInvisible()
	{
		return viewInvisible;
	}
}
