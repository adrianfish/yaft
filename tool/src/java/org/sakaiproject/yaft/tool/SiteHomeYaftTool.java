package org.sakaiproject.yaft.tool;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.ActiveDiscussion;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;

/**
 * This servlet handles all of the REST type stuff. At some point this may all
 * move into an EntityProvider.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class SiteHomeYaftTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(SiteHomeYaftTool.class);

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
		
		// We need to pass the language code to the JQuery code in the pages.
		Locale locale = (new ResourceLoader(user.getId())).getLocale();
		String languageCode = locale.getLanguage();

		response.sendRedirect("/yaft-tool/sitehome_yaft.html?siteId=" + siteId + "&language=" + languageCode);
		return;
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
