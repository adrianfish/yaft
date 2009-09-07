package org.sakaiproject.yaft.tool;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.sakaiproject.api.app.profile.Profile;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.ActiveDiscussion;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.SearchResult;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftPermissions;
import org.sakaiproject.yaft.api.YaftPreferences;

/**
 * This servlet handles all of the REST type stuff. At some point this may all
 * move into an EntityProvider.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class SynopticYaftTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(SynopticYaftTool.class);

	private YaftForumService yaftForumService = null;

	private SakaiProxy sakaiProxy;

	public void destroy()
	{
		logger.info("destroy");

		super.destroy();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (logger.isDebugEnabled()) logger.debug("doGet()");
		
		if(yaftForumService == null || sakaiProxy == null)
			throw new ServletException("yaftForumService and sakaiProxy MUST be initialised.");
		
		User user = sakaiProxy.getCurrentUser();
		
		if(user == null)
		{
			// We are not logged in
			throw new ServletException("getCurrentUser returned null.");
		}
		
		String siteId = sakaiProxy.getCurrentSiteId();
		String placementId = sakaiProxy.getCurrentToolId();
		
		// We need to pass the language code to the JQuery code in the pages.
		Locale locale = (new ResourceLoader(user.getId())).getLocale();
		String languageCode = locale.getLanguage();

		String pathInfo = request.getPathInfo();

		if (pathInfo == null || pathInfo.length() < 1)
		{
			// There's no path info, so this is the initial state
			response.sendRedirect("/sakai-yaft-tool/synoptic_yaft.html?placementId=" + placementId + "&language=" + languageCode);
			return;
		}
		else
		{
			String[] parts = pathInfo.substring(1).split("/");

			if (parts.length >= 1)
			{
				String part1 = parts[0];
				if (logger.isDebugEnabled())
					logger.debug("data=" + part1);

				if ("data".equals(part1))
				{
					handleDataRequest(request, response, parts, pathInfo);
				}
			}
		}
	}

	private void handleDataRequest(HttpServletRequest request, HttpServletResponse response, String[] parts, String pathInfo) throws ServletException, IOException
	{
		if (parts.length >= 2)
		{
			String part1 = parts[1];
			
			if("activeDiscussions".equals(part1))
			{
				List<ActiveDiscussion> activeDiscussions = yaftForumService.getActiveDiscussions();
				
				JSONObject json = new JSONObject();
				json.put("discussions", activeDiscussions);
				
				String jsonString = json.toString();
				if (logger.isDebugEnabled())
					logger.debug("User JSON: " + jsonString);

				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/javascript");
				response.setContentLength(jsonString.getBytes().length);
				response.getWriter().write(jsonString);
				response.getWriter().close();
			}
		}
	}

	/**
	 * Sets up the YaftForumService and SakaiProxy instances
	 */
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);

		if (logger.isDebugEnabled()) logger.debug("init");

		try
		{
			ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
			yaftForumService = (YaftForumService) componentManager.get(YaftForumService.class);
			sakaiProxy = yaftForumService.getSakaiProxy();
		}
		catch (Throwable t)
		{
			throw new ServletException("Failed to initialise SynopticYaftTool servlet.", t);
		}
	}
}
