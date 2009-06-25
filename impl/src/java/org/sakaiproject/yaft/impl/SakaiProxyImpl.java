package org.sakaiproject.yaft.impl;

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.sakaiproject.api.app.profile.Profile;
import org.sakaiproject.api.app.profile.ProfileManager;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.calendar.api.Calendar;
import org.sakaiproject.calendar.api.CalendarEvent;
import org.sakaiproject.calendar.api.CalendarEventEdit;
import org.sakaiproject.calendar.api.CalendarService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.cover.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.BaseResourceProperties;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftFunctions;
import org.sakaiproject.yaft.api.YaftPermissions;

/**
 * All Sakai API calls go in here. If Sakai changes all we have to do if mod
 * this file.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class SakaiProxyImpl implements SakaiProxy
{
	private Logger logger = Logger.getLogger(SakaiProxyImpl.class);
	
	private ServerConfigurationService serverConfigurationService = null;
	private UserDirectoryService userDirectoryService = null;
	private SqlService sqlService = null;
	private SiteService siteService = null;
	private ToolManager toolManager;
	private ProfileManager profileManager;
	private FunctionManager functionManager;
	private AuthzGroupService authzGroupService;
	private EmailService emailService;
	private ContentHostingService contentHostingService;
	private SecurityService securityService;
	private EntityManager entityManager;
	private EventTrackingService eventTrackingService;
	private CalendarService calendarService;
	private TimeService timeService;
	
	public SakaiProxyImpl()
	{
		if(logger.isDebugEnabled()) logger.debug("SakaiProxy()");
		
		ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		serverConfigurationService = (ServerConfigurationService) componentManager.get(ServerConfigurationService.class);
		userDirectoryService = (UserDirectoryService) componentManager.get(UserDirectoryService.class);
		sqlService = (SqlService) componentManager.get(SqlService.class);
		siteService = (SiteService) componentManager.get(SiteService.class);
		toolManager = (ToolManager) componentManager.get(ToolManager.class);
		profileManager = (ProfileManager) componentManager.get(ProfileManager.class);
		functionManager = (FunctionManager) componentManager.get(FunctionManager.class);
		authzGroupService = (AuthzGroupService) componentManager.get(AuthzGroupService.class);
		emailService = (EmailService) componentManager.get(EmailService.class);
		securityService = (SecurityService) componentManager.get(SecurityService.class);
		contentHostingService = (ContentHostingService) componentManager.get(ContentHostingService.class);
		eventTrackingService = (EventTrackingService) componentManager.get(EventTrackingService.class);
		timeService = (TimeService) componentManager.get(TimeService.class);
		calendarService = (CalendarService) componentManager.get(CalendarService.class);
		entityManager = (EntityManager) componentManager.get(EntityManager.class);
	}
	
	public boolean isAutoDDL()
	{
		if(logger.isDebugEnabled()) logger.debug("isAutoDDL()");
		
		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}
	
	public String getDbVendor()
	{
		if(logger.isDebugEnabled()) logger.debug("getDbVendor()");
		
		return sqlService.getVendor();
	}
	
	public String getCurrentSiteId()
	{
		Placement placement = toolManager.getCurrentPlacement();
		if(placement == null)
		{
			logger.warn("Current tool placement is null.");
			return null;
		}
		
		return placement.getContext();
	}
	
	public Connection borrowConnection() throws SQLException
	{
		if(logger.isDebugEnabled()) logger.debug("borrowConnection()");
		return sqlService.borrowConnection();
	}
	
	public void returnConnection(Connection connection)
	{
		if(logger.isDebugEnabled()) logger.debug("returnConnection()");
		sqlService.returnConnection(connection);
	}

	public String getDisplayNameForUser(String creatorId)
	{
		try
		{
			User sakaiUser = userDirectoryService.getUser(creatorId);
			return sakaiUser.getDisplayName();
		}
		catch (Exception e)
		{
			return creatorId; // this can happen if the user does not longer exist in the system
		}
	}
	
	public void registerFunction(String function)
	{
		List functions = functionManager.getRegisteredFunctions("yaft.");
		
		if(!functions.contains(function))
			functionManager.registerFunction(function);
	}

	public String getSakaiHomePath()
	{
		return serverConfigurationService.getSakaiHomePath();
	}

	public Profile getProfile(String userId)
	{
		return profileManager.getUserProfileById(userId);
	} 
	
	public boolean addCalendarEntry(String title,String description, String type, long startDate,long endDate)
	{
		try
		{
			Calendar cal = calendarService.getCalendar("/calendar/calendar/" + getCurrentSiteId() + "/main");
			CalendarEventEdit edit = cal.addEvent();
			TimeRange range = timeService.newTimeRange(startDate, endDate - startDate);
			edit.setRange(range);
			edit.setDescriptionFormatted(description);
			edit.setDisplayName(title);
			edit.setType(type);
			cal.commitEvent(edit);
		
			return true;
		}
		catch(Exception e)
		{
			logger.error("Failed to add calendar entry. Returning false ...",e);
			return false;
		}
	}
	
	public boolean removeCalendarEntry(String title,String description)
	{
		try
		{
			Calendar cal = calendarService.getCalendar("/calendar/calendar/" + getCurrentSiteId() + "/main");
			List<CalendarEvent> events = cal.getEvents(null,null);
			for(CalendarEvent event : events)
			{
				if(event.getDisplayName().equals(title) && event.getDescription().equals(description))
				{
					CalendarEventEdit edit = cal.getEditEvent(event.getId(), CalendarService.SECURE_REMOVE);
					cal.removeEvent(edit);
					return true;
				}
			}
			
			return true;
		
		}
		catch(Exception e)
		{
			logger.error("Failed to add calendar entry. Returning false ...",e);
			return false;
		}
	}
	
	public String getServerUrl()
	{
		return serverConfigurationService.getServerUrl();
	}
	
	public List<YaftPermissions> getPermissions(String siteId)
	{
		try
		{
			Site site = null;
			
			if(siteId != null)
				site = siteService.getSite(siteId);
			else
				site = siteService.getSite(getCurrentSiteId());
			
			AuthzGroup realm = authzGroupService.getAuthzGroup(site.getReference());
			Set<Role> roles = realm.getRoles();
			
			List<YaftPermissions> list = new ArrayList<YaftPermissions>(roles.size());
			
			for(Role role : roles)
			{
				YaftPermissions permissions = new YaftPermissions();
				permissions.setRole(role.getId());
				permissions.setForumCreate(role.isAllowed(YaftFunctions.YAFT_FORUM_CREATE));
				permissions.setForumDeleteOwn(role.isAllowed(YaftFunctions.YAFT_FORUM_DELETE_OWN));
				permissions.setForumDeleteAny(role.isAllowed(YaftFunctions.YAFT_FORUM_DELETE_ANY));
				permissions.setDiscussionCreate(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_CREATE));
				permissions.setDiscussionDeleteOwn(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_DELETE_OWN));
				permissions.setDiscussionDeleteAny(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_DELETE_ANY));
				permissions.setMessageCreate(role.isAllowed(YaftFunctions.YAFT_MESSAGE_CREATE));
				permissions.setMessageCensor(role.isAllowed(YaftFunctions.YAFT_MESSAGE_CENSOR));
				permissions.setMessageDeleteOwn(role.isAllowed(YaftFunctions.YAFT_MESSAGE_DELETE_OWN));
				permissions.setMessageDeleteAny(role.isAllowed(YaftFunctions.YAFT_MESSAGE_DELETE_ANY));
				permissions.setViewInvisible(role.isAllowed(YaftFunctions.YAFT_VIEW_INVISIBLE));
				
				list.add(permissions);
			}
			
			return list;
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst building permissions list",e);
		}
		
		return null;
	}

	public YaftPermissions getPermissionsForCurrentUser(String siteId)
	{
		try
		{
			String userId = userDirectoryService.getCurrentUser().getId();
			Site site = null;
			
			if(siteId != null)
				site = siteService.getSite(siteId);
			else
				site = siteService.getSite(getCurrentSiteId());
			
			AuthzGroup realm = authzGroupService.getAuthzGroup(site.getReference());
			Role role = realm.getUserRole(userId);
			
			YaftPermissions permissions = new YaftPermissions();
			permissions.setRole(role.getId());
			permissions.setForumCreate(role.isAllowed(YaftFunctions.YAFT_FORUM_CREATE));
			permissions.setForumDeleteOwn(role.isAllowed(YaftFunctions.YAFT_FORUM_DELETE_OWN));
			permissions.setForumDeleteAny(role.isAllowed(YaftFunctions.YAFT_FORUM_DELETE_ANY));
			permissions.setDiscussionCreate(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_CREATE));
			permissions.setDiscussionDeleteOwn(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_DELETE_OWN));
			permissions.setDiscussionDeleteAny(role.isAllowed(YaftFunctions.YAFT_DISCUSSION_DELETE_ANY));
			permissions.setMessageCreate(role.isAllowed(YaftFunctions.YAFT_MESSAGE_CREATE));
			permissions.setMessageDeleteOwn(role.isAllowed(YaftFunctions.YAFT_MESSAGE_DELETE_OWN));
			permissions.setMessageDeleteAny(role.isAllowed(YaftFunctions.YAFT_MESSAGE_DELETE_ANY));
			permissions.setMessageCensor(role.isAllowed(YaftFunctions.YAFT_MESSAGE_CENSOR));
			permissions.setViewInvisible(role.isAllowed(YaftFunctions.YAFT_VIEW_INVISIBLE));
				
			return permissions;
				
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst building permissions list",e);
		}
		
		return null;
	}

	public void savePermissions(String siteId,Map<String, YaftPermissions> permissionMap) throws Exception
	{
		Site site = siteService.getSite(siteId);
		AuthzGroup realm = authzGroupService.getAuthzGroup(site.getReference());
		
		Set roles = realm.getRoles();
		
		for(Iterator i = roles.iterator();i.hasNext();)
		{
			Role role = (Role) i.next();
			
			if(!permissionMap.containsKey(role.getId()))
				permissionMap.put(role.getId(), new YaftPermissions());
		}
		
		for(String roleName : permissionMap.keySet())
		{
			YaftPermissions permissions = permissionMap.get(roleName);
			
			Role role = realm.getRole(roleName);
			
			if(role == null)
				throw new Exception("Role '" + roleName + "' has not been setup for this site");
			
			if(permissions.isForumCreate())
				role.allowFunction(YaftFunctions.YAFT_FORUM_CREATE);
			else
				role.disallowFunction(YaftFunctions.YAFT_FORUM_CREATE);
			
			if(permissions.isForumDeleteAny())
				role.allowFunction(YaftFunctions.YAFT_FORUM_DELETE_ANY);
			else
				role.disallowFunction(YaftFunctions.YAFT_FORUM_DELETE_ANY);
			
			if(permissions.isForumDeleteOwn())
				role.allowFunction(YaftFunctions.YAFT_FORUM_DELETE_OWN);
			else
				role.disallowFunction(YaftFunctions.YAFT_FORUM_DELETE_OWN);
			
			if(permissions.isDiscussionCreate())
				role.allowFunction(YaftFunctions.YAFT_DISCUSSION_CREATE);
			else
				role.disallowFunction(YaftFunctions.YAFT_DISCUSSION_CREATE);
			
			if(permissions.isDiscussionDeleteOwn())
				role.allowFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_OWN);
			else
				role.disallowFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_OWN);
			
			if(permissions.isDiscussionDeleteAny())
				role.allowFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_ANY);
			else
				role.disallowFunction(YaftFunctions.YAFT_DISCUSSION_DELETE_ANY);
			
			if(permissions.isMessageCreate())
				role.allowFunction(YaftFunctions.YAFT_MESSAGE_CREATE);
			else
				role.disallowFunction(YaftFunctions.YAFT_MESSAGE_CREATE);
			
			if(permissions.isMessageCensor())
				role.allowFunction(YaftFunctions.YAFT_MESSAGE_CENSOR);
			else
				role.disallowFunction(YaftFunctions.YAFT_MESSAGE_CENSOR);
			
			if(permissions.isMessageDeleteOwn())
				role.allowFunction(YaftFunctions.YAFT_MESSAGE_DELETE_OWN);
			else
				role.disallowFunction(YaftFunctions.YAFT_MESSAGE_DELETE_OWN);
			
			if(permissions.isMessageDeleteAny())
				role.allowFunction(YaftFunctions.YAFT_MESSAGE_DELETE_ANY);
			else
				role.disallowFunction(YaftFunctions.YAFT_MESSAGE_DELETE_ANY);
			
			if(permissions.isViewInvisible())
				role.allowFunction(YaftFunctions.YAFT_VIEW_INVISIBLE);
			else
				role.disallowFunction(YaftFunctions.YAFT_VIEW_INVISIBLE);
		}
		
		authzGroupService.save(realm);
	}
	
	private String getEmailForTheUser(String userId)
	{
		try
		{
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getEmail();
		}
		catch (Exception e)
		{
			return ""; // this can happen if the user does not longer exist in the system
		}
	}

	public void sendEmailMessageToSiteUsers(String subject,String message, List<String> exclusions)
	{
		class EmailSender implements Runnable
		{
			public final String MULTIPART_BOUNDARY = "======sakai-multi-part-boundary======";
			public final String BOUNDARY_LINE = "\n\n--"+MULTIPART_BOUNDARY+"\n";
			public final String TERMINATION_LINE = "\n\n--"+MULTIPART_BOUNDARY+"--\n\n";
			public final String MIME_ADVISORY = "This message is for MIME-compliant mail readers.";
			public final String PLAIN_TEXT_HEADERS= "Content-Type: text/plain\n\n";
			public final String HTML_HEADERS = "Content-Type: text/html; charset=ISO-8859-1\n\n";
			public final String HTML_END = "\n  </body>\n</html>\n";

			private Thread runner;

			private String sender;

			private String subject;

			private String text;

			private Set<String> participants;

			public EmailSender(String from, Set<String> to, String subject, String text)
			{
				this.sender = from;
				this.participants = to;
				this.text = text;
				this.subject = subject;
				runner = new Thread(this,"Yaft Emailer Thread");
				runner.start();
			}

			public synchronized void run()
			{
				try
				{
					String emailText = "<html><body>";
					emailText += text;
					emailText += "</body></html>";

					List<String> additionalHeader = new ArrayList<String>();
					additionalHeader.add("Content-Type: text/html; charset=ISO-8859-1");
					
					String formattedSubject = formatSubject(subject);
					String formattedMessage = formatMessage(subject,text);

					for (String userId : participants)
					{
						String emailParticipant = getEmailForTheUser(userId);
						try
						{
							// TODO: This should all be parameterised and internationalised.
							// logger.info("Sending email to " + participantId + " ...");
							//emailService.send(sender, emailParticipant, formattedSubject, emailText, emailParticipant, sender, getHeaders(emailParticipant, formattedSubject));
							emailService.send(sender, emailParticipant, subject, emailText, emailParticipant, sender, additionalHeader);
						}
						catch (Exception e)
						{
							System.out.println("Failed to send email to '" + userId + "'. Message: " + e.getMessage());
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			
			private List<String> getHeaders(String emailTo, String subject){
				List<String> headers = new ArrayList<String>();
				headers.add("MIME-Version: 1.0");
				headers.add("Content-Type: multipart/alternative; boundary=\""+MULTIPART_BOUNDARY+"\"");
				headers.add(subject);
				headers.add(getFrom());
				if (StringUtil.trimToNull(emailTo) != null) {
					headers.add("To: " + emailTo);
				}
				
				return headers;
			}
			
			private String getFrom(){
				StringBuilder sb = new StringBuilder();
				sb.append("From: ");
				sb.append(serverConfigurationService.getString("ui.service","Sakai"));
				sb.append(" <sakai-forum@");
				sb.append(serverConfigurationService.getServerName());
				sb.append(">");
				
				return sb.toString();
			}
			
			private String formatSubject(String subject) {
				StringBuilder sb = new StringBuilder();
				sb.append("Subject: ");
				sb.append(subject);
				
				return sb.toString();
			}
			
			private String htmlPreamble(String subject) {
				StringBuilder sb = new StringBuilder();
				sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
				sb.append("\"http://www.w3.org/TR/html4/loose.dtd\">\n");
				sb.append("<html>\n");
				sb.append("<head><title>");
				sb.append(subject);
				sb.append("</title></head>\n");
				sb.append("<body>\n");
				
				return sb.toString();
			}

			
			/** helper methods for formatting the message */
			private String formatMessage(String subject, String message) {
				StringBuilder sb = new StringBuilder();
				sb.append(MIME_ADVISORY);
				sb.append(BOUNDARY_LINE);
				sb.append(PLAIN_TEXT_HEADERS);
				sb.append(Validator.escapeHtmlFormattedText(message));
				sb.append(BOUNDARY_LINE);
				sb.append(HTML_HEADERS);
				sb.append(htmlPreamble(subject));
				sb.append(message);
				sb.append(HTML_END);
				sb.append(TERMINATION_LINE);
				
				return sb.toString();
			}

		}
		
		Set<String> siteUsers = getSiteUsers();
		
		if(exclusions != null && exclusions.size() > 0)
		{
			for(String excludedId : exclusions)
				siteUsers.remove(excludedId);
		}
		
		new EmailSender("sakai-forum@" + serverConfigurationService.getServerName(), siteUsers, subject, message);
	}
	private Set<String> getSiteUsers()
	{
		try
		{
			Site site = siteService.getSite(getCurrentSiteId());
			return site.getUsers();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public User getCurrentUser()
	{
		try
		{
			return userDirectoryService.getCurrentUser();
		}
		catch(Throwable t)
		{
			logger.error("Exception caught whilst getting current user.",t);
			if(logger.isDebugEnabled()) logger.debug("Returning null ...");
			return null;
		}
	}
	
	public String getPortalUrl()
	{
		return serverConfigurationService.getServerUrl() + "/portal";
	}
	
	public String getCurrentPageId()
	{
		Placement placement = toolManager.getCurrentPlacement();
		
		if(placement instanceof ToolConfiguration)
			return ((ToolConfiguration) placement).getPageId();
		
		return null;
	}
	
	public String getCurrentToolId()
	{
		return toolManager.getCurrentPlacement().getId();
	}

	public String getDirectUrl(String string)
	{
		String portalUrl = getPortalUrl();
		String pageId = getCurrentPageId();
		String siteId = getCurrentSiteId();
		String toolId = getCurrentToolId();
				
		try
		{
			String url = portalUrl
						+ "/site/" + siteId
						+ "/page/" + pageId
						+ "?toolstate-" + toolId + "="
							+ URLEncoder.encode(string,"UTF-8");
		
			return url;
		}
		catch(Exception e)
		{
			logger.error("Caught exception whilst building direct URL.",e);
			return null;
		}
	}
	
	private void enableSecurityAdvisor()
    {
		securityService.pushAdvisor(new SecurityAdvisor()
        {
            public SecurityAdvice isAllowed(String userId, String function, String reference)
            {
                return SecurityAdvice.ALLOWED;
            }
        });
    }
	
	/**
	 * Saves the file to Sakai's content hosting
	 */
	public String saveFile(String creatorId,String name,String mimeType, byte[] fileData) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("saveFile(" + name + "," + mimeType + ",[BINARY FILE DATA])");
		
		if(name == null | name.length()  == 0)
			throw new IllegalArgumentException("The name argument must be populated.");
		
		String uuid = UUID.randomUUID().toString();
		
		String id = "/group/" + getCurrentSiteId() + "/yaft-files/" + uuid;

		try
		{
			enableSecurityAdvisor();

			ContentResourceEdit resource = contentHostingService.addResource(id);
			resource.setContentType(mimeType);
			resource.setContent(fileData);
			ResourceProperties props = new BaseResourceProperties();
			props.addProperty(ResourceProperties.PROP_CONTENT_TYPE, mimeType);
			props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);
			props.addProperty(ResourceProperties.PROP_CREATOR, creatorId);
			props.addProperty(ResourceProperties.PROP_ORIGINAL_FILENAME, name);
			resource.getPropertiesEdit().set(props);
			contentHostingService.commitResource(resource, NotificationService.NOTI_NONE);
			
			//return resource.getId();
			return uuid;
		}
		catch(IdUsedException e)
		{
			if(logger.isInfoEnabled()) logger.info("A resource with id '" + id + "' exists already. Returning id without recreating ...");
			return uuid;
			//return id;
		}
	}
	
	public void getAttachment(Attachment attachment)
	{
		try
		{
			enableSecurityAdvisor();
			String id = "/group/" + getCurrentSiteId() + "/yaft-files/" + attachment.getResourceId();
			//ContentResource resource = contentHostingService.getResource(attachment.getResourceId());
			ContentResource resource = contentHostingService.getResource(id);
			ResourceProperties properties = resource.getProperties();
			attachment.setMimeType(properties.getProperty(ResourceProperties.PROP_CONTENT_TYPE));
			attachment.setName(properties.getProperty(ResourceProperties.PROP_DISPLAY_NAME));
			attachment.setUrl(resource.getUrl());
		}
		catch(Exception e)
		{
			if(logger.isDebugEnabled()) e.printStackTrace();
			
			logger.error("Caught an exception with message '" + e.getMessage()+ "'");
		}
	}

	public void deleteFile(String resourceId) throws Exception
	{
		enableSecurityAdvisor();
		String id = "/group/" + getCurrentSiteId() + "/yaft-files/" + resourceId;
		contentHostingService.removeResource(id);
	}

	public String getUserBio(String id)
	{
		Profile profile = profileManager.getUserProfileById(id);
		if(profile != null)
			return profile.getOtherInformation();
		
		return "";
	}

	public User getUser(String userId) throws Exception
	{
		return userDirectoryService.getUser(userId);
	}

	public List<Site> getAllSites()
	{
		return siteService.getSites(SiteService.SelectionType.NON_USER, null, null, null, null, null);
	}

	public ToolConfiguration getFirstInstanceOfTool(String siteId, String toolId)
	{
		try
		{
			return siteService.getSite(siteId).getToolForCommonId(toolId);
		}
		catch (IdUnusedException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public String[] getSiteIds()
	{
		return null;
	}
	
	public void registerEntityProducer(EntityProducer entityProducer)
	{
		entityManager.registerEntityProducer(entityProducer, YaftForumService.ENTITY_PREFIX);
	}

	public void postEvent(String event,String reference,boolean modify)
	{
		eventTrackingService.post(eventTrackingService.newEvent(event,reference,modify));
	}

	public byte[] getResourceBytes(String resourceId)
	{
		try
		{
			enableSecurityAdvisor();
			ContentResource resource = contentHostingService.getResource(resourceId);
			return resource.getContent();
		}
		catch(Exception e)
		{
			logger.error("Caught an exception with message '" + e.getMessage()+ "'");
		}
		
		return null;
	}
}
