package org.sakaiproject.yaft.tool;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftPreferences;

/**
 * This servlet handles all of the REST type stuff. At some point this may all
 * move into an EntityProvider.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class YaftTool extends HttpServlet
{
	private Logger logger = Logger.getLogger(YaftTool.class);

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
			response.sendRedirect("/yaft-tool/yaft.html?state=forums&siteId=" + siteId + "&placementId=" + placementId + "&language=" + languageCode);
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

				if ("forums".equals(part1))
				{
					if(parts.length == 1)
					{
						response.sendRedirect("/yaft-tool/yaft.html?state=forums&siteId=" + siteId + "&placementId=" + placementId + "&language=" + languageCode);
						return;
					}
					
					if (parts.length >= 2)
					{
						String forumId = parts[1];

						if (logger.isDebugEnabled())
							logger.debug("forumId=" + forumId);
						
						if(parts.length == 2)
						{
							// This is a request for a particular forum
							response.sendRedirect("/yaft-tool/yaft.html?state=forum&forumId=" + forumId + "&siteId=" + siteId + "&placementId=" + placementId + "&language=" + languageCode);
							return;
						}
						else
						{
							String forumOp = parts[2];
						
							if ("delete".equals(forumOp))
							{
								yaftForumService.deleteForum(forumId);
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
							else if ("subscribe".equals(forumOp))
							{
								yaftForumService.subscribeToForum(forumId);
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
							else if ("unsubscribe".equals(forumOp))
							{
								yaftForumService.unsubscribeFromForum(forumId);
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
						}
					}
				}
				
				else if ("discussions".equals(part1))
				{
					if (parts.length >= 2)
					{
						String discussionId = parts[1];
						Forum forum = yaftForumService.getForumContainingMessage(discussionId);
						String forumId = forum.getId();
						
						if(parts.length == 2)
						{
							// This is a request for a discussion and defaults to full view mode
							response.sendRedirect("/yaft-tool/yaft.html?state=full&discussionId=" + discussionId + "&siteId=" + siteId + "&placementId=" + placementId + "&forumId=" + forum.getId() + "&language=" + languageCode);
							return;
						}
						else
						{
							String discussionOp = parts[2];
							if ("delete".equals(discussionOp))
							{
								yaftForumService.deleteDiscussion(discussionId);
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
							else if ("subscribe".equals(discussionOp))
							{
								yaftForumService.subscribeToDiscussion(null, discussionId);
								response.setStatus(HttpServletResponse.SC_NO_CONTENT);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
							else if ("unsubscribe".equals(discussionOp))
							{
								yaftForumService.unsubscribeFromDiscussion(null, discussionId);
								response.setStatus(HttpServletResponse.SC_NO_CONTENT);
								response.setContentType("text/plain");
								response.getWriter().write("success");
								response.getWriter().close();
								return;
							}
							else if ("markRead".equals(discussionOp))
							{
								if(yaftForumService.markDiscussionRead(discussionId, forumId))
								{
									response.setContentType("text/plain");
									response.getWriter().write("success");
									response.getWriter().close();
								}
								else
								{
									response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									response.getWriter().close();
								}
								
								return;
							}
						}
					}
				}
				
				else if ("messages".equals(part1))
				{
					if (parts.length >= 2)
					{
						String messageId = parts[1];
						Message message = yaftForumService.getMessage(messageId);
						Forum forum = yaftForumService.getForumContainingMessage(messageId);
						
						String messageOp = "full";
						if(parts.length >= 3)
							messageOp = parts[2];
						
						if ("full".equals(messageOp))
						{
							response.sendRedirect("/yaft-tool/yaft.html?state=" + messageOp + "&discussionId=" + message.getDiscussionId() + "&siteId=" + siteId + "&placementId=" + placementId + "&forumId=" + forum.getId()  + "&language=" + languageCode + "#message-" + messageId);
							return;
						}
						else if("minimal".equals(messageOp))
						{
							response.sendRedirect("/yaft-tool/yaft.html?state=" + messageOp + "&discussionId=" + message.getDiscussionId() + "&messageId=" + messageId + "&siteId=" + siteId + "&placementId=" + placementId + "&forumId=" + forum.getId()  + "&language=" + languageCode);
							return;
						}
						
						else if ("markRead".equals(messageOp))
						{
							if (yaftForumService.markMessageRead(messageId,forum.getId(),message.getDiscussionId()))
							{
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
							}
							else
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

							response.getWriter().close();
							
							return;
						}
						else if ("markUnRead".equals(messageOp))
						{
							if (yaftForumService.markMessageUnRead(messageId,forum.getId(),message.getDiscussionId()))
							{
								response.setStatus(HttpServletResponse.SC_OK);
								response.setContentType("text/plain");
								response.getWriter().write("success");
							}
							else
							{
								logger.error("Failed to mark message with id '" + messageId + "' as un-read");
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							}
							
							response.getWriter().close();

							return;
						}
						
						else if ("delete".equals(messageOp))
						{
							yaftForumService.deleteMessage(message,forum.getId());
							response.setStatus(HttpServletResponse.SC_OK);
							response.setContentType("text/plain");
							response.getWriter().write("success");
							response.getWriter().close();
							return;
						}
						else if ("undelete".equals(messageOp))
						{
							yaftForumService.undeleteMessage(message,forum.getId());
							response.setStatus(HttpServletResponse.SC_OK);
							response.setContentType("text/plain");
							response.getWriter().write("success");
							response.getWriter().close();
							return;

						}
						else if ("publish".equals(messageOp))
						{
							yaftForumService.publishMessage(forum.getId(),message);
							response.setStatus(HttpServletResponse.SC_OK);
							response.setContentType("text/plain");
							response.getWriter().write("success");
							response.getWriter().close();
							return;
						}
						else if ("attachments".equals(messageOp))
						{
							if(parts.length >= 4)
							{
								String attachmentId = parts[3];
								if(parts.length >= 5)
								{
									String attachmentOp = parts[4];
									
									if("delete".equals(attachmentOp))
									{
										yaftForumService.deleteAttachment(attachmentId, messageId);

										response.setStatus(HttpServletResponse.SC_OK);
										response.setContentType("text/plain");
										response.getWriter().write("success");
										response.getWriter().close();
										return;
									}
								}
							}
						}
					}
				}
			}
			else
			{
				response.sendRedirect("/yaft-tool/yaft.html?state=forums&siteId=" + siteId + "&placementId=" + placementId + "&language=" + languageCode);
				return;
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		logger.info("doPost()");

		String function = request.getParameter("function");

		if (logger.isDebugEnabled())
			logger.debug("function=" + function);

		if (function.equals("setPreferences"))
		{
			String emailPreference = request.getParameter("emailPreference");
			String viewPreference = request.getParameter("viewPreference");
			if (logger.isDebugEnabled())
			{
				logger.debug("emailPreference: " + emailPreference);
				logger.debug("viewPreference: " + viewPreference);
			}
			
			YaftPreferences preferences = new YaftPreferences(emailPreference,viewPreference);
			
			yaftForumService.savePreferences(preferences);
			
			response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId());
			return;
		}
		else if(function.equals("moveDiscussion"))
		{
			String currentForumId = request.getParameter("currentForumId");
			String newForumId = request.getParameter("forumId");
			String discussionId = request.getParameter("discussionId");

			if (logger.isDebugEnabled())
			{
				logger.debug("Current Forum ID: " + currentForumId);
				logger.debug("Forum ID: " + newForumId);
				logger.debug("Discussion ID: " + discussionId);
			}

			yaftForumService.moveDiscussion(discussionId, currentForumId, newForumId);
			response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/forums/" + currentForumId);
			return;
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
			throw new ServletException("Failed to initialise YaftTool servlet.", t);
		}
	}
}
