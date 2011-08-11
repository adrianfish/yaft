package org.sakaiproject.yaft.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Group;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.YaftFunctions;

public class YaftSecurityManager
{
	private SakaiProxy sakaiProxy;
	
	public YaftSecurityManager(SakaiProxy sakaiProxy)
	{
		this.sakaiProxy = sakaiProxy;
	}
	
	public List<Forum> filterFora(List<Forum> fora)
	{
		String siteId = sakaiProxy.getCurrentSiteId();
		
		List<Forum> filtered = new ArrayList<Forum>();
		
		for(Forum forum : fora)
		{
			if(filterForum(forum, siteId) == null)
				continue;
			
			filtered.add(forum);
		}
		
		return filtered;
	}
	
	public List<Discussion> filterDiscussions(List<Discussion> discussions)
	{
		String siteId = sakaiProxy.getCurrentSiteId();
		
		List<Discussion> filtered = new ArrayList<Discussion>();
		
		for(Discussion discussion : discussions)
		{
			if(filterDiscussion(discussion, siteId) == null)
				continue;
			
			filtered.add(discussion);
		}
		
		return filtered;
	}

	public Forum filterForum(Forum forum,String siteId)
	{
		if(forum == null) return null;
		
		if(siteId == null) siteId = sakaiProxy.getCurrentSiteId();
		
		if(!forum.getSiteId().equals(siteId))
			return null;
		
		// Is the current user a member of the site?
		if(!sakaiProxy.isCurrentUserMemberOfSite(siteId)) {
			return null;
		}
		
		List<Group> groups = forum.getGroups();
			
		if(groups.size() > 0
				&& !sakaiProxy.isCurrentUserMemberOfAnyOfTheseGroups(groups)
				&& !sakaiProxy.currentUserHasFunction(YaftFunctions.YAFT_FORUM_VIEW_GROUPS))
		{
			return null;
		}
		
		return forum;
	}
	
	public Discussion filterDiscussion(Discussion discussion,String siteId)
	{
		if(discussion == null) return null;
		
		if(siteId == null) siteId = sakaiProxy.getCurrentSiteId();
		
		// Is the current user a member of the site?
		if(!sakaiProxy.isCurrentUserMemberOfSite(siteId)) {
			return null;
		}
		
		List<Group> groups = discussion.getGroups();
			
		if(groups.size() > 0
				&& !sakaiProxy.isCurrentUserMemberOfAnyOfTheseGroups(groups)
				&& !sakaiProxy.currentUserHasFunction(YaftFunctions.YAFT_FORUM_VIEW_GROUPS))
		{
			return null;
		}
		
		return discussion;
	}
}
