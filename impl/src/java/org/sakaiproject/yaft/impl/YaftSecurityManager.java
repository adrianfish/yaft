package org.sakaiproject.yaft.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.yaft.api.Group;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.Forum;

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
			if(!forum.getSiteId().equals(siteId))
				continue;
			
			List<Group> groups = forum.getGroups();
			
			if(groups.size() > 0 && !sakaiProxy.isCurrentUserMemberOfAnyOfTheseGroups(groups))
				continue;
			
			filtered.add(forum);
		}
		
		return filtered;
	}

	public Forum filterForum(Forum forum)
	{
		String siteId = sakaiProxy.getCurrentSiteId();
		
		if(forum.getSiteId().equals(siteId))
			return forum;
		
		return null;
	}
}
