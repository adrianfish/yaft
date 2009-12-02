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

				if ("data".equals(part1))
				{
					handleDataRequest(request, response, parts, pathInfo);
				}
				
				else if ("forums".equals(part1))
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

	private void printUserJSON(HttpServletResponse response, String userId) throws IOException
	{
		User user = null;
		try
		{
			if (userId != null)
				user = sakaiProxy.getUser(userId);
			else
				user = sakaiProxy.getCurrentUser();
		}
		catch (Exception e)
		{
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No user");
		}
		JSONObject json = new JSONObject();
		json.put("id", user.getId());
		json.put("eid", user.getEid());
		json.put("email", user.getEmail());
		json.put("firstName", user.getFirstName());
		json.put("lastName", user.getLastName());
		json.put("bio", sakaiProxy.getUserBio(user.getId()));
		String jsonString = json.toString();
		if (logger.isDebugEnabled())
			logger.debug("User JSON: " + jsonString);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/javascript");
		response.setContentLength(jsonString.getBytes().length);
		response.getWriter().write(jsonString);
		response.getWriter().close();
	}

	private void handleCurrentUserPermissionsRequest(HttpServletResponse response)  throws ServletException, IOException
	{
		YaftPermissions permissions = sakaiProxy.getPermissionsForCurrentUser(null);

		JSONObject json = JSONObject.fromObject(permissions);
		if (logger.isDebugEnabled())
			logger.debug("Permissions JSON: " + json.toString());

		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
		
	}
	
	private void handleCurrentUserPreferencesRequest(HttpServletResponse response)  throws ServletException, IOException
	{
		YaftPreferences preferences = yaftForumService.getPreferencesForCurrentUserAndSite();

		JSONObject json = JSONObject.fromObject(preferences);
		if (logger.isDebugEnabled())
			logger.debug("Preferences JSON: " + json.toString());

		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
		
	}
	
	
	private void handleDataRequest(HttpServletRequest request, HttpServletResponse response, String[] parts, String pathInfo) throws ServletException, IOException
	{
		if (parts.length >= 2)
		{
			String part1 = parts[1];

			if ("currentUser".equals(part1))
			{
				printUserJSON(response, null);
				return;
			}

			else if ("userPermissions".equals(part1))
			{
				if (logger.isDebugEnabled()) logger.debug("userPermissions");
				handleCurrentUserPermissionsRequest(response);
				return;
			}
			
			else if ("userPreferences".equals(part1))
			{
				if (logger.isDebugEnabled()) logger.debug("userPermissions");
				handleCurrentUserPreferencesRequest(response);
				return;
			}

			else if ("users".equals(part1))
			{
				if (logger.isDebugEnabled()) logger.debug("users");
				handleUsersRequest(parts,request,response);
				return;
			}
			else if ("permissions".equals(part1))
			{
				handlePermissionsRequest(response);
				return;
			}

			else if ("search".equals(part1))
			{
				if (parts.length == 3)
				{
					handleSearchRequest(parts,response);
					return;
				}
			}

			else if ("messages".equals(part1))
			{
				if (parts.length >= 3)
				{
					String messageId = parts[2];

					if (logger.isDebugEnabled())
						logger.debug("messageId=" + messageId);

					if (parts.length == 3)
					{
						printMessageJSON(messageId,response);
						return;
					}
				}
			}

			else if ("forums".equals(part1))
			{
				if (parts.length == 2)
				{
					printForumsJSON(response);
					return;
				}
				else if (parts.length >= 3)
				{
					String forumsOp = parts[2];
					
					if("readMessages".equals(forumsOp))
					{
						Map<String,Integer> counts = yaftForumService.getReadMessageCountForAllFora();
						JSONObject json = JSONObject.fromObject(counts);
						response.setContentType("text/javascript");
						json.write(response.getWriter());
						if (logger.isDebugEnabled())
							logger.debug("Read Messages JSON: " + json.toString());
						response.getWriter().close();
						return;
					}

					if (parts.length == 3)
					{
						printForumJSON(forumsOp,ForumPopulatedStates.EMPTY,response);
						return;
					}
					else if (parts.length >= 4)
					{
						String forumOp = parts[3];

						if (parts.length == 4)
						{
							// This is an operation on a particular forum

							if (ForumPopulatedStates.FULL.equals(forumOp))
							{
								// This is a request for a forum in it's fully
								// populated state 
								printForumJSON(forumsOp,ForumPopulatedStates.FULL,response);
								return;
							}
							
							else if (ForumPopulatedStates.PART.equals(forumOp))
							{
								// This is a request for a forum in it's
								// partially populated state 
								printForumJSON(forumsOp,ForumPopulatedStates.PART,response);
								return;
							}
							
							else if("readMessages".equals(forumOp))
							{
								Map<String,Integer> counts = yaftForumService.getReadMessageCountForForum(forumsOp);
								JSONObject json = JSONObject.fromObject(counts);
								response.setContentType("text/javascript");
								json.write(response.getWriter());
								if (logger.isDebugEnabled())
									logger.debug("Read Messages JSON: " + json.toString());
								response.getWriter().close();
								return;
							}
						}
					}
				}
			}

			else if ("discussions".equals(part1))
			{
				if (parts.length >= 3)
				{
					String discussionId = parts[2];

					if (logger.isDebugEnabled())
						logger.debug("discussionId=" + discussionId);

					if (parts.length == 3)
					{
						// This is a request for a particular discussion
						
						try
						{
							Discussion discussion = yaftForumService.getDiscussion(discussionId,true);
							JsonConfig config = new JsonConfig();
							config.setExcludes(new String[] {"properties","reference"});
							JSONObject json = JSONObject.fromObject(discussion,config);

							if (logger.isDebugEnabled())
								logger.debug("Discussion JSON: " + json.toString());

							response.setContentType("text/javascript");
							json.write(response.getWriter());
						}
						catch(IdUnusedException ide)
						{
							logger.error("A valid discussion id must be supplied. Returning BAD REQUEST ...");
							response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						}
						catch(Exception e)
						{
							logger.error("Failed to get discussion.",e);
						}
						
						response.getWriter().close();
						return;
					}
					
					if(parts.length >= 4)
					{
						String discussionOp = parts[3];
						if("readMessages".equals(discussionOp))
						{
							List<String> readMessageIds = yaftForumService.getReadMessageIds(discussionId);
							JSONArray items = new JSONArray();
							items.addAll(readMessageIds);
							JSON json = JSONSerializer.toJSON(items);
							if (logger.isDebugEnabled())
								logger.debug("Read Messages JSON: " + json.toString());
							response.setContentType("text/javascript");
							json.write(response.getWriter());
							response.getWriter().close();
							return;
						}
					}
				}
			}
		}
	}

	private void printForumJSON(String forumId, String state, HttpServletResponse response) throws ServletException,IOException
	{
		Forum forum = yaftForumService.getForum(forumId, state);
		
		JSONObject json = null;
		
		if(state.equals(ForumPopulatedStates.FULL) || state.equals(ForumPopulatedStates.PART))
		{
			JsonConfig config = new JsonConfig();
			config.setExcludes(new String[] {"properties","reference"});
			json = JSONObject.fromObject(forum,config);
		}
		else
			json = JSONObject.fromObject(forum);

		if (logger.isDebugEnabled())
			logger.debug("Forum JSON: " + json.toString());

		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
	}

	private void printForumsJSON(HttpServletResponse response) throws ServletException,IOException
	{
		// This is a request for all forums in the current site
		List<Forum> forums = yaftForumService.getSiteForums(sakaiProxy.getCurrentSiteId(),false);

		JsonConfig config = new JsonConfig();
		config.setExcludes(new String[] {"properties","reference"});
		JSONArray items = JSONArray.fromObject(forums,config);
		JSONObject container = new JSONObject();
		container.put("items", items);
		JSON json = JSONSerializer.toJSON(container,config);
		if (logger.isDebugEnabled())
			logger.debug("Forums JSON: " + json.toString());

		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
	}

	private void printMessageJSON(String messageId, HttpServletResponse response) throws ServletException,IOException
	{
		Message message = yaftForumService.getMessage(messageId);
		JsonConfig config = new JsonConfig();
		config.setExcludes(new String[] {"properties","reference"});
		JSONObject json = JSONObject.fromObject(message,config);
		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
	}

	private void handleSearchRequest(String[] parts, HttpServletResponse response) throws ServletException,IOException
	{
		String searchTerms = parts[2];

		if (logger.isDebugEnabled())
			logger.debug("Search Terms: " + searchTerms);

		List<SearchResult> results = yaftForumService.search(searchTerms);

		JSONArray items = new JSONArray();
		items.addAll(results);
		JSON json = JSONSerializer.toJSON(items);
		if (logger.isDebugEnabled())
			logger.debug("Search Results JSON: " + json.toString());

		response.setContentType("text/javascript");
		json.write(response.getWriter());
		response.getWriter().close();
		return;
	}

	private void handlePermissionsRequest(HttpServletResponse response) throws ServletException,IOException
	{
		List<YaftPermissions> permissions = sakaiProxy.getPermissions(null);

		JSONArray o = JSONArray.fromObject(permissions);

		logger.debug("Permissions JSON: " + o.toString());

		response.setContentType("text/javascript");
		o.write(response.getWriter());
		response.getWriter().close();
		return;
	}

	private void handleUsersRequest(String[] parts,HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException
	{
		if (parts.length >= 3)
		{
			String userId = parts[2];

			if (parts.length == 3)
			{
				try
				{
					printUserJSON(response, userId);
					return;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				String userOp = parts[3];

				if ("picture".equals(userOp))
				{
					Profile profile = sakaiProxy.getProfile(userId);
					if (profile == null)
					{
						RequestDispatcher disp = request.getRequestDispatcher("/images/no_image.gif");
						disp.include(request, response);
						return;
					}

					byte[] bytes = profile.getInstitutionalPicture();
						
					if (bytes == null || bytes.length == 0)
					{
						RequestDispatcher disp = request.getRequestDispatcher("/images/no_image.gif");
						disp.include(request, response);
						return;
					}

					response.setContentType("image/jpeg");
					response.setContentLength(bytes.length);

					response.getOutputStream().write(bytes);
					response.getOutputStream().close();
					return;
				}
				else if ("unsubscriptions".equals(userOp))
				{
					List<String> unsubscriptions = yaftForumService.getDiscussionUnsubscriptions(userId);
					JSONArray items = new JSONArray();
					items.addAll(unsubscriptions);
					JSON json = JSONSerializer.toJSON(items);
					if (logger.isDebugEnabled())
						logger.debug("Unsubscriptions JSON: " + json.toString());
					response.setContentType("text/javascript");
					json.write(response.getWriter());
					response.getWriter().close();
					return;
				}
				else if ("forumUnsubscriptions".equals(userOp))
				{
					List<String> unsubscriptions = yaftForumService.getForumUnsubscriptions(userId);
					JSONArray items = JSONArray.fromObject(unsubscriptions);
					JSON json = JSONSerializer.toJSON(items);
					if (logger.isDebugEnabled())
						logger.debug("Unsubscriptions JSON: " + json.toString());
					response.setContentType("text/javascript");
					json.write(response.getWriter());
					response.getWriter().close();
					return;
				}
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		logger.info("doPost()");

		String function = request.getParameter("function");

		if (logger.isDebugEnabled())
			logger.debug("function=" + function);

		if (function.equals("createForum"))
		{
			String id = request.getParameter("id");
			String title = request.getParameter("title");
			String description = request.getParameter("description");
			String startDateString = request.getParameter("startDate");
			String endDateString = request.getParameter("endDate");
			String startHourString = request.getParameter("startHour");
			String startMinuteString = request.getParameter("startMinute");
			String endHourString = request.getParameter("endHour");
			String endMinuteString = request.getParameter("endMinute");
			
			String lockWritingString = request.getParameter("lockWriting");
			String lockReadingString = request.getParameter("lockReading");
			
			boolean lockWriting = true;
			boolean lockReading = true;
			
			if(lockWritingString != null)
				lockWriting = lockWritingString.equals("true");
			else
				lockWriting = false;
			
			if(lockReadingString != null)
				lockReading = lockReadingString.equals("true");
			else
				lockReading = false;
			
			if (logger.isDebugEnabled())
			{
				logger.debug("Title: " + title);
				logger.debug("Description: " + description);
			}
			
			if(title == null || title.length() <= 0)
			{
				logger.error("Title must be supplied. Returning BAD REQUEST ...");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().close();
				return;
			}
			
			Forum forum = new Forum();
			
			if(id != null)
				forum.setId(id);
			
			forum.setTitle(title);
			forum.setDescription(description);
			forum.setSiteId(sakaiProxy.getCurrentSiteId());
			forum.setCreatorId(sakaiProxy.getCurrentUser().getId());
			forum.setLockedForWriting(lockWriting);
			forum.setLockedForReading(lockReading);
			
			if(startDateString != null && startDateString.length() > 0
					&& startHourString != null && startHourString.length() > 0
					&& startMinuteString != null && startMinuteString.length() > 0
					&& endDateString != null && endDateString.length() > 0
					&& endHourString != null && endHourString.length() > 0
					&& endMinuteString != null && endMinuteString.length() > 0)
			{
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");
				
			Date startDate = null;
			Date endDate = null;
				
			try
			{
				startDate = dateFormat.parse(startDateString);
				endDate = dateFormat.parse(endDateString);
			}
			catch(ParseException pe)
			{
					logger.error("The start and end dates MUST be in dd MM yyyy format. Returning BAD REQUEST ...");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().close();
					return;
			}
			
			int startHours,startMinutes,endHours,endMinutes;
			
			try
			{
				startHours = Integer.parseInt(startHourString);
				startMinutes = Integer.parseInt(startMinuteString);
				endHours = Integer.parseInt(endHourString);
				endMinutes = Integer.parseInt(endMinuteString);
			}
			catch(NumberFormatException nfe)
			{
				logger.error("The hours and minutes MUST be integers. Returning BAD REQUEST ...");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().close();
				return;
			}
			
			Calendar startCal = Calendar.getInstance();
			startCal.setTime(startDate);
			
			startCal.set(Calendar.HOUR_OF_DAY, startHours);
			startCal.set(Calendar.MINUTE, startMinutes);
			
			Calendar endCal = Calendar.getInstance();
			endCal.setTime(endDate);
			
			endCal.set(Calendar.HOUR_OF_DAY, endHours);
			endCal.set(Calendar.MINUTE, endMinutes);


			forum.setStart(startCal.getTimeInMillis());
			forum.setEnd(endCal.getTimeInMillis());
			
			}

			if(yaftForumService.addOrUpdateForum(forum))
			{
				if(id.length() == 0)
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/forums/" + forum.getId());
				else
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId());
			}
			else
			{
				logger.error("Failed to add or update forum. Returning INTERNAl SERVER ERROR ...");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().close();
				return;
				
			}
			
			return;
		}
		else if (function.equals("setPreferences"))
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
		else if (function.equals("moveDiscussion"))
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
		else if (function.equals("startDiscussion"))
		{
			String id = request.getParameter("id");
			String subject = request.getParameter("subject");
			String content = request.getParameter("content");
			String forumId = request.getParameter("forumId");
			String startDateString = request.getParameter("startDate");
			String endDateString = request.getParameter("endDate");
			String startHourString = request.getParameter("startHour");
			String startMinuteString = request.getParameter("startMinute");
			String endHourString = request.getParameter("endHour");
			String endMinuteString = request.getParameter("endMinute");
			String lockWritingString = request.getParameter("lockWriting");
			String lockReadingString = request.getParameter("lockReading");
			
			boolean lockWriting = true;
			boolean lockReading = true;
			
			if(lockWritingString != null)
				lockWriting = lockWritingString.equals("true");
			else
				lockWriting = false;
			
			if(lockReadingString != null)
				lockReading = lockReadingString.equals("true");
			else
				lockReading = false;

			if (logger.isDebugEnabled())
			{
				logger.debug("Subject: " + subject);
				logger.debug("Content: " + content);
				logger.debug("Forum ID: " + forumId);
			}
			
			if(subject == null || subject.length() <= 0)
			{
				logger.error("Subject must be supplied. Returning BAD REQUEST ...");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().close();
				return;
			}

			Message message = new Message();
			
			if(id != null)
				message.setId(id);
			
			message.setSubject(subject);
			message.setContent(content);
			message.setSiteId(sakaiProxy.getCurrentSiteId());
			message.setCreatorId(sakaiProxy.getCurrentUser().getId());
			message.setCreatorDisplayName(sakaiProxy.getDisplayNameForUser(sakaiProxy.getCurrentUser().getId()));
			message.setAttachments(getAttachments(request));
			
			// The first messages in discussions always have the same id as the
			// discussion
			message.setDiscussionId(message.getId());
			
			message.setStatus("READY");
			
			Discussion discussion = new Discussion();
			discussion.setFirstMessage(message);
			discussion.setLockedForWriting(lockWriting);
			discussion.setLockedForReading(lockReading);
			
			if(startDateString != null && startDateString.length() > 0
					&& startHourString != null && startHourString.length() > 0
					&& startMinuteString != null && startMinuteString.length() > 0
					&& endDateString != null && endDateString.length() > 0
					&& endHourString != null && endHourString.length() > 0
					&& endMinuteString != null && endMinuteString.length() > 0)
			{
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");
				
			Date startDate = null;
			Date endDate = null;
				
			try
			{
				startDate = dateFormat.parse(startDateString);
				endDate = dateFormat.parse(endDateString);
			}
			catch(ParseException pe)
			{
					logger.error("The start and end dates MUST be in dd MM yyyy format. Returning BAD REQUEST ...");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().close();
					return;
			}
			
			int startHours,startMinutes,endHours,endMinutes;
			
			try
			{
				startHours = Integer.parseInt(startHourString);
				startMinutes = Integer.parseInt(startMinuteString);
				endHours = Integer.parseInt(endHourString);
				endMinutes = Integer.parseInt(endMinuteString);
			}
			catch(NumberFormatException nfe)
			{
				logger.error("The hours and minutes MUST be integers. Returning BAD REQUEST ...");
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().close();
				return;
			}
			
			Calendar startCal = Calendar.getInstance();
			startCal.setTime(startDate);
			
			startCal.set(Calendar.HOUR_OF_DAY, startHours);
			startCal.set(Calendar.MINUTE, startMinutes);
			
			Calendar endCal = Calendar.getInstance();
			endCal.setTime(endDate);
			
			endCal.set(Calendar.HOUR_OF_DAY, endHours);
			endCal.set(Calendar.MINUTE, endMinutes);

			discussion.setStart(startCal.getTimeInMillis());
			discussion.setEnd(endCal.getTimeInMillis());
			}

			if(yaftForumService.addDiscussion(forumId, discussion, true) != null)
			{
				if(id.length() == 0)
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/discussions/" + discussion.getId());
				else
				response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/forums/" + forumId);
			}
			else
			{
				logger.error("Failed to add discussion. Returning INTERNAl SERVER ERROR ...");
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().close();
				return;
			}

			return;
		}
		else if (function.equals("createMessage"))
		{
			String status = request.getParameter("status");
			String subject = request.getParameter("subject");
			String content = request.getParameter("content");
			String forumId = request.getParameter("forumId");
			String viewMode = request.getParameter("viewMode");
			String messageId = request.getParameter("messageId");
			String messageBeingRepliedTo = request.getParameter("messageBeingRepliedTo");
			String discussionId = request.getParameter("discussionId");

			if (logger.isDebugEnabled())
			{
				logger.debug("Status: " + status);
				logger.debug("Subject: " + subject);
				logger.debug("Content: " + content);
				logger.debug("Forum ID: " + forumId);
				logger.debug("View Mode: " + viewMode);
				logger.debug("Discussion ID: " + discussionId);
				logger.debug("Message Being Replied To: " + messageBeingRepliedTo);
			}
			
			if(viewMode == null || viewMode.length() <= 0)
				viewMode = "full";
			
			Message message = new Message();
			message.setStatus(status);
			message.setSubject(subject);
			message.setContent(content);
			message.setSiteId(sakaiProxy.getCurrentSiteId());
			message.setCreatorId(sakaiProxy.getCurrentUser().getId());
			message.setCreatorDisplayName(sakaiProxy.getDisplayNameForUser(sakaiProxy.getCurrentUser().getId()));
			message.setDiscussionId(discussionId);
			message.setAttachments(getAttachments(request));
			
			if(messageId == null)
				message.setId("");

			if (messageBeingRepliedTo != null && messageBeingRepliedTo.length() > 0)
			{
				// This is a reply message
				message.setParent(messageBeingRepliedTo);
				if(yaftForumService.addOrUpdateMessage(forumId, message, true))
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/messages/" + message.getId() + "/" + viewMode);
				else
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId());
			}
			else if (messageBeingRepliedTo == null || messageBeingRepliedTo.length() <= 0)
			{
				message.setId(messageId);
				if(yaftForumService.addOrUpdateMessage(forumId, message, true))
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId() + "/messages/" + message.getId() + "/" + viewMode);
				else
					response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId());
			}

			return;
		}
		else if ("savePermissions".equals(function))
		{
			String siteId = sakaiProxy.getCurrentSiteId();

			Map params = request.getParameterMap();

			Map<String, YaftPermissions> permissionMap = new HashMap<String, YaftPermissions>();

			for (Iterator i = params.keySet().iterator(); i.hasNext();)
			{
				String name = (String) i.next();

				if (!name.contains(":"))
					continue;

				String value = ((String[]) params.get(name))[0];
				logger.debug("Name: " + name);
				logger.debug("Value: " + value);

				String role = name.substring(0, name.indexOf(":"));
				logger.debug("Role: " + role);
				String permission = name.substring(name.indexOf(":") + 1);
				logger.debug("Permission: " + permission);

				YaftPermissions permissions = permissionMap.get(role);

				if (permissions == null)
				{
					permissions = new YaftPermissions();
					permissions.setRole(role);
				}

				try
				{
					String methodName = "set" + permission;
					if (logger.isDebugEnabled())
						logger.debug("Calling " + methodName + " on permissions object");
					Method setter = YaftPermissions.class.getMethod(methodName, new Class[] { boolean.class });
					setter.invoke(permissions, value.equals("on"));
					permissionMap.put(role, permissions);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}

			try
			{
				sakaiProxy.savePermissions(siteId, permissionMap);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			response.sendRedirect("/portal/tool/" + sakaiProxy.getCurrentToolId());// + "/forums/" + forumId + "/discussions/" + discussionId + "/messages/" + message.getId());
		}
	}
	
	private List<Attachment> getAttachments(HttpServletRequest request)
	{
		List<FileItem> fileItems = new ArrayList<FileItem>();

		String uploadsDone = (String) request.getAttribute(RequestFilter.ATTR_UPLOADS_DONE);

		if (uploadsDone != null && uploadsDone.equals(RequestFilter.ATTR_UPLOADS_DONE))
		{
			logger.debug("UPLOAD STATUS: " + request.getAttribute("upload.status"));

			try
			{
				FileItem attachment1 = (FileItem) request.getAttribute("attachment_0");
				if (attachment1 != null && attachment1.getSize() > 0)
					fileItems.add(attachment1);
				FileItem attachment2 = (FileItem) request.getAttribute("attachment_1");
				if (attachment2 != null && attachment2.getSize() > 0)
					fileItems.add(attachment2);
				FileItem attachment3 = (FileItem) request.getAttribute("attachment_2");
				if (attachment3 != null && attachment3.getSize() > 0)
					fileItems.add(attachment3);
				FileItem attachment4 = (FileItem) request.getAttribute("attachment_3");
				if (attachment4 != null && attachment4.getSize() > 0)
					fileItems.add(attachment4);
				FileItem attachment5 = (FileItem) request.getAttribute("attachment_4");
				if (attachment5 != null && attachment5.getSize() > 0)
					fileItems.add(attachment5);
			}
			catch (Exception e)
			{

			}
		}
		
		List<Attachment> attachments = new ArrayList<Attachment>();
		if (fileItems.size() > 0)
		{
		for (Iterator i = fileItems.iterator(); i.hasNext();)
		{
			FileItem fileItem = (FileItem) i.next();

			String name = fileItem.getName();

			if (name.contains("/"))
				name = name.substring(name.lastIndexOf("/") + 1);
			else if (name.contains("\\"))
				name = name.substring(name.lastIndexOf("\\") + 1);

			attachments.add(new Attachment(name, fileItem.getContentType(), fileItem.get()));
		}
		}
		
		return attachments;
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
