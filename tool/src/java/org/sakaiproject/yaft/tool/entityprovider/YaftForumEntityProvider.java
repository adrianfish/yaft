package org.sakaiproject.yaft.tool.entityprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.CoreEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.CollectionResolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Resolvable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.search.Restriction;
import org.sakaiproject.entitybroker.entityprovider.search.Search;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.impl.SakaiProxyImpl;

public class YaftForumEntityProvider extends AbstractEntityProvider implements Resolvable, CoreEntityProvider, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable,Statisticable,CollectionResolvable
{
	protected final Logger LOG = Logger.getLogger(getClass());
	
	public final static String ENTITY_PREFIX = "yaft-forum";
	
	private static final String[] EVENT_KEYS
		= new String[] {
			YaftForumService.YAFT_FORUM_CREATED,
			YaftForumService.YAFT_FORUM_DELETED,
			YaftForumService.YAFT_DISCUSSION_CREATED,
			YaftForumService.YAFT_DISCUSSION_DELETED,
			YaftForumService.YAFT_MESSAGE_CREATED,
			YaftForumService.YAFT_MESSAGE_DELETED
			};
	
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
		
		if("allReadMessages".equals(id))
			return true;
		
		if("unsubscriptions".equals(id))
			return true;
		
		if(id .length() > 36)
			id = id.substring(0,36);

		try
		{
			Forum forum = yaftForumService.getForum(id,ForumPopulatedStates.EMPTY);
			return forum != null;
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting forum.", e);
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
		
		if("allReadMessages".equals(id))
			return yaftForumService.getReadMessageCountForAllFora();
		
		if("unsubscriptions".equals(id))
			return yaftForumService.getForumUnsubscriptions(userId);
		
		String state = "";
		if(id .length() > 36)
		{
			state = id.substring(37);
			id = id.substring(0,36);
		}

		Forum forum = null;

		try
		{
			forum = yaftForumService.getForum(id,state);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting forum.", e);
		}

		if (forum == null)
		{
			throw new IllegalArgumentException("Forum not found");
		}

		return forum;
	}
	
	public List<Forum> getEntities(EntityReference ref, Search search)
	{
		List<Forum> fora = new ArrayList<Forum>();

		Restriction locRes = search.getRestrictionByProperty(CollectionResolvable.SEARCH_LOCATION_REFERENCE);
		
		if(locRes == null)
			throw new IllegalArgumentException("You must supply the siteId");

		String siteId = new EntityReference(locRes.getStringValue()).getId();

		try
		{
			fora = yaftForumService.getSiteForums(siteId, false);
		}
		catch (Exception e)
		{
			LOG.error("Caught exception whilst getting fora.", e);
		}

		return fora;
	}

	public Object getSampleEntity()
	{
		return new Forum();
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

		String forumId = ref.getId();
		
		if (forumId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the forum id");
		
		Map<String,Integer> counts = yaftForumService.getReadMessageCountForForum(forumId);
		return counts;
	}
	
	@EntityCustomAction(action = "userPreferences", viewKey = EntityView.VIEW_SHOW)
	public Object handleUserPreferences(EntityReference ref,Map<String,Object> params)
	{
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null)
			throw new EntityException("Not logged in",ref.getReference(),HttpServletResponse.SC_UNAUTHORIZED);

		String siteId = ref.getId();
		
		if (siteId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the site id");
		
		return yaftForumService.getPreferencesForUser(userId,siteId);
	}
	
	@EntityCustomAction(action = "activeDiscussions", viewKey = EntityView.VIEW_SHOW)
	public Object handleActiveDiscussions(EntityReference ref,Map<String,Object> params)
	{
		String userId = developerHelperService.getCurrentUserId();
		
		if(userId == null)
			throw new EntityException("Not logged in",ref.getReference(),HttpServletResponse.SC_UNAUTHORIZED);

		String siteId = ref.getId();
		
		if (siteId == null)
			throw new IllegalArgumentException("Invalid path provided: expect to receive the site id");
		
		return yaftForumService.getActiveDiscussions(siteId);
	}
	
	/**
	 * From Statisticable
	 */
	public String getAssociatedToolId()
	{
		return "sakai.yaft";
	}

	/**
	 * From Statisticable
	 */
	public String[] getEventKeys()
	{
		String[] temp = new String[EVENT_KEYS.length];
		System.arraycopy(EVENT_KEYS, 0, temp, 0, EVENT_KEYS.length);
		return temp;
	}

	/**
	 * From Statisticable
	 */
	public Map<String, String> getEventNames(Locale locale)
	{
		Map<String, String> localeEventNames = new HashMap<String, String>();
		ResourceLoader msgs = new ResourceLoader("Events");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++)
		{
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}
}
