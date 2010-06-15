package org.sakaiproject.yaft.tool.entityprovider;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.impl.SakaiProxyImpl;

public class YaftDiscussionEntityProvider extends AbstractEntityProvider implements Resolvable, CoreEntityProvider, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable
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
		sakaiProxy = new SakaiProxyImpl();
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
		return ids;
	}
}
