package org.sakaiproject.yaft.tool.entityprovider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Statisticable;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.YaftForumService;

public class YaftForumEntityProvider extends AbstractEntityProvider implements AutoRegisterEntityProvider,Statisticable
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
	
	/*
	public Object getSampleEntity()
	{
		return new Forum();
	}
	*/

	public String getEntityPrefix()
	{
		return ENTITY_PREFIX;
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
		ResourceLoader msgs = new ResourceLoader("YaftEvents");
		msgs.setContextLocale(locale);
		for (int i = 0; i < EVENT_KEYS.length; i++)
		{
			localeEventNames.put(EVENT_KEYS[i], msgs.getString(EVENT_KEYS[i]));
		}
		return localeEventNames;
	}
}
