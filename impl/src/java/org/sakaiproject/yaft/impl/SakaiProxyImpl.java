package org.sakaiproject.yaft.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.sakaiproject.api.app.profile.Profile;
import org.sakaiproject.api.app.profile.ProfileManager;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.FunctionManager;
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
import org.sakaiproject.email.api.DigestService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.emailtemplateservice.model.EmailTemplate;
import org.sakaiproject.emailtemplateservice.model.RenderedTemplate;
import org.sakaiproject.emailtemplateservice.service.EmailTemplateService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.BaseResourceProperties;
import org.sakaiproject.util.Validator;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * All Sakai API calls go in here. If Sakai changes all we have to do if mod this file.
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

	private DigestService digestService;

	private ContentHostingService contentHostingService;

	private SecurityService securityService;

	private EntityManager entityManager;

	private EventTrackingService eventTrackingService;

	private CalendarService calendarService;

	private TimeService timeService;

	private UsageSessionService usageSessionService;
	
	private SessionManager sessionManager;
	
	private EmailTemplateService emailTemplateService;
	
	public SakaiProxyImpl()
	{
		if (logger.isDebugEnabled())
			logger.debug("SakaiProxy()");

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
		digestService = (DigestService) componentManager.get(DigestService.class);
		securityService = (SecurityService) componentManager.get(SecurityService.class);
		contentHostingService = (ContentHostingService) componentManager.get(ContentHostingService.class);
		eventTrackingService = (EventTrackingService) componentManager.get(EventTrackingService.class);
		timeService = (TimeService) componentManager.get(TimeService.class);
		calendarService = (CalendarService) componentManager.get(CalendarService.class);
		entityManager = (EntityManager) componentManager.get(EntityManager.class);
		usageSessionService = (UsageSessionService) componentManager.get(UsageSessionService.class);
		sessionManager = (SessionManager) componentManager.get(SessionManager.class);
		emailTemplateService = (EmailTemplateService) componentManager.get(EmailTemplateService.class);
		
		List<String> emailTemplates = (List<String>) componentManager.get("org.sakaiproject.yaft.api.emailtemplates.List");
		//emailTemplateService.processEmailTemplates(emailTemplates);
		for(String templatePath : emailTemplates)
			processEmailTemplate(templatePath);
	}

	public boolean isAutoDDL()
	{
		if (logger.isDebugEnabled())
			logger.debug("isAutoDDL()");

		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}

	public String getDbVendor()
	{
		if (logger.isDebugEnabled())
			logger.debug("getDbVendor()");

		return sqlService.getVendor();
	}

	public Site getCurrentSite()
	{
		try
		{
			return siteService.getSite(getCurrentSiteId());
		}
		catch (Exception e)
		{
			logger.error("Failed to get current site.", e);
			return null;
		}
	}

	public String getCurrentSiteId()
	{
		Placement placement = toolManager.getCurrentPlacement();
		if (placement == null)
		{
			logger.warn("Current tool placement is null.");
			return null;
		}

		return placement.getContext();
	}

	public Connection borrowConnection() throws SQLException
	{
		if (logger.isDebugEnabled())
			logger.debug("borrowConnection()");
		return sqlService.borrowConnection();
	}

	public void returnConnection(Connection connection)
	{
		if (logger.isDebugEnabled())
			logger.debug("returnConnection()");
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

		if (!functions.contains(function))
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

	public boolean addCalendarEntry(String title, String description, String type, long startDate, long endDate)
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
		catch (Exception e)
		{
			logger.error("Failed to add calendar entry. Returning false ...", e);
			return false;
		}
	}

	public boolean removeCalendarEntry(String title, String description)
	{
		try
		{
			Calendar cal = calendarService.getCalendar("/calendar/calendar/" + getCurrentSiteId() + "/main");
			List<CalendarEvent> events = cal.getEvents(null, null);
			for (CalendarEvent event : events)
			{
				if (event.getDisplayName().equals(title) && event.getDescription().equals(description))
				{
					CalendarEventEdit edit = cal.getEditEvent(event.getId(), CalendarService.SECURE_REMOVE);
					cal.removeEvent(edit);
					return true;
				}
			}

			return true;

		}
		catch (Exception e)
		{
			logger.error("Failed to add calendar entry. Returning false ...", e);
			return false;
		}
	}

	public String getServerUrl()
	{
		return serverConfigurationService.getServerUrl();
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
	
	public void sendEmail(final String userId, final String subject, String message)
	{
		class EmailSender implements Runnable
		{
			private Thread runner;
			private String userId;
			private String subject;
			private String message;
			
			public final String MULTIPART_BOUNDARY = "======sakai-multi-part-boundary======";
			public final String BOUNDARY_LINE = "\n\n--"+MULTIPART_BOUNDARY+"\n";
			public final String TERMINATION_LINE = "\n\n--"+MULTIPART_BOUNDARY+"--\n\n";
			public final String MIME_ADVISORY = "This message is for MIME-compliant mail readers.";
			public final String PLAIN_TEXT_HEADERS= "Content-Type: text/plain\n\n";
			public final String HTML_HEADERS = "Content-Type: text/html; charset=ISO-8859-1\n\n";
			public final String HTML_END = "\n  </body>\n</html>\n";

			public EmailSender(String userId, String subject, String message)
			{
				this.userId = userId;
				this.subject = subject;
				this.message = message;
				runner = new Thread(this,"YAFT EmailSender thread");
				runner.start();
			}

			//do it!
			public synchronized void run()
			{
				try
				{

					//get User to send to
					User user = userDirectoryService.getUser(userId);
					
					String email = user.getEmail();
					
					if (email == null || email.length() == 0)
					{
						logger.error("SakaiProxy.sendEmail() failed. No email for userId: " + userId);
						return;
					}
					
					List<User> receivers = new ArrayList<User>();
					receivers.add(user);
					
					//do it
					emailService.sendToUsers(receivers, getHeaders(user.getEmail(), subject), formatMessage(subject, message));
					
					logger.info("Email sent to: " + userId);
				}
				catch (Exception e)
				{
					logger.error("SakaiProxy.sendEmail() failed for userId: " + userId + " : " + e.getClass() + " : " + e.getMessage());
				}
			}
			
			/** helper methods for formatting the message */
			private String formatMessage(String subject, String message)
			{
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
			
			private String htmlPreamble(String subject)
			{
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
			
			private List<String> getHeaders(String emailTo, String subject)
			{
				List<String> headers = new ArrayList<String>();
				headers.add("MIME-Version: 1.0");
				headers.add("Content-Type: multipart/alternative; boundary=\""+MULTIPART_BOUNDARY+"\"");
				headers.add(formatSubject(subject));
				headers.add(getFrom());
				if (emailTo != null && emailTo.length() > 0)
				{
					headers.add("To: " + emailTo);
				}
				
				return headers;
			}
			
			private String getFrom()
			{
				StringBuilder sb = new StringBuilder();
				sb.append("From: ");
				sb.append(serverConfigurationService.getString("ui.service", "Sakai"));
				sb.append(" <no-reply@");
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
			
			
		}
		
		//instantiate class to format, then send the mail
		new EmailSender(userId, subject, message);
	}

	public void addDigestMessage(String user, String subject, String body)
	{
		try
		{
			try
			{
				digestService.digest(user, subject, body);
			}
			catch (Exception e)
			{
				logger.error("Failed to add message to digest. Message: " + e.getMessage());
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public User getCurrentUser()
	{
		try
		{
			return userDirectoryService.getCurrentUser();
		}
		catch (Throwable t)
		{
			logger.error("Exception caught whilst getting current user.", t);
			if (logger.isDebugEnabled())
				logger.debug("Returning null ...");
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

		if (placement instanceof ToolConfiguration)
			return ((ToolConfiguration) placement).getPageId();

		return null;
	}

	public String getCurrentToolId()
	{
		return toolManager.getCurrentPlacement().getId();
	}
	
	public String getYaftPageId(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			return tc.getPageId();
		}
		catch (Exception e)
		{
			return "";
		}
	}
	
	public String getYaftToolId(String siteId)
	{
		try
		{
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			return tc.getId();
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public String getDirectUrl(String siteId, String string)
	{
		String portalUrl = getPortalUrl();
		
		String pageId = null;
		String toolId = null;
		
		if(siteId == null) 
		{
			siteId = getCurrentSiteId();
			pageId = getCurrentPageId();
			toolId = getCurrentToolId();
		}
		else
		{
			pageId = getYaftPageId(siteId);
			toolId = getYaftToolId(siteId);
		}

		try
		{
			String url = portalUrl + "/site/" + siteId + "/page/" + pageId + "?toolstate-" + toolId + "=" + URLEncoder.encode(string, "UTF-8");

			return url;
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst building direct URL.", e);
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
	public String saveFile(String siteId, String creatorId, String name, String mimeType, byte[] fileData) throws Exception
	{
		if (logger.isDebugEnabled())
			logger.debug("saveFile(" + name + "," + mimeType + ",[BINARY FILE DATA])");

		if (name == null | name.length() == 0)
			throw new IllegalArgumentException("The name argument must be populated.");

		if (name.endsWith(".doc"))
			mimeType = "application/msword";
		else if (name.endsWith(".xls"))
			mimeType = "application/excel";

		// String uuid = UUID.randomUUID().toString();
		
		if(siteId == null)
			siteId = getCurrentSiteId();

		String id = "/group/" + siteId + "/yaft-files/" + name;

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

			// return resource.getId();
			return name;
		}
		catch (IdUsedException e)
		{
			if (logger.isInfoEnabled())
				logger.info("A resource with id '" + id + "' exists already. Returning id without recreating ...");
			return name;
		}
	}

	public void getAttachment(String siteId, Attachment attachment)
	{
		if(siteId == null)
			siteId = getCurrentSiteId();
		
		try
		{
			enableSecurityAdvisor();
			String id = "/group/" + siteId + "/yaft-files/" + attachment.getResourceId();
			// ContentResource resource = contentHostingService.getResource(attachment.getResourceId());
			ContentResource resource = contentHostingService.getResource(id);
			ResourceProperties properties = resource.getProperties();
			attachment.setMimeType(properties.getProperty(ResourceProperties.PROP_CONTENT_TYPE));
			attachment.setName(properties.getProperty(ResourceProperties.PROP_DISPLAY_NAME));
			attachment.setUrl(resource.getUrl());
		}
		catch (Exception e)
		{
			if (logger.isDebugEnabled())
				e.printStackTrace();

			logger.error("Caught an exception with message '" + e.getMessage() + "'");
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
		try
		{
			Profile profile = profileManager.getUserProfileById(id);
			if (profile != null)
				return profile.getOtherInformation();
		}
		catch(SecurityException se)
		{
		}

		return "";
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

	public void postEvent(String event, String reference, boolean modify)
	{
		eventTrackingService.post(eventTrackingService.newEvent(event, reference, modify));
	}

	public byte[] getResourceBytes(String resourceId)
	{
		try
		{
			enableSecurityAdvisor();
			ContentResource resource = contentHostingService.getResource(resourceId);
			return resource.getContent();
		}
		catch (Exception e)
		{
			logger.error("Caught an exception with message '" + e.getMessage() + "'");
		}

		return null;
	}
	
	/**
	 * Process the supplied template XML into an EmailTemplate object and save it
	 * @param templatePath
	 * @return
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	private void processEmailTemplate(String templatePath) 
	{
		final String ELEM_SUBJECT = "subject";
		final String ELEM_MESSAGE = "message";
		final String ELEM_HTML_MESSAGE = "htmlMessage";
		final String ELEM_LOCALE = "locale";
		final String ELEM_VERSION = "version";
		final String ELEM_OWNER = "owner";
		final String ELEM_KEY = "key";
		final String ADMIN = "admin";
		
		InputStream in = SakaiProxy.class.getClassLoader().getResourceAsStream(templatePath);
		XMLInputFactory factory = (XMLInputFactory)XMLInputFactory.newInstance();
		XMLStreamReader staxXmlReader = null;
		EmailTemplate template = new EmailTemplate();

		try
		{
			staxXmlReader = (XMLStreamReader) factory.createXMLStreamReader(in);
		
			for (int event = staxXmlReader.next(); event != XMLStreamConstants.END_DOCUMENT; event = staxXmlReader.next())
			{
				if (event == XMLStreamConstants.START_ELEMENT)
				{
					String element = staxXmlReader.getLocalName();
			    
				    //subject
				    if(StringUtils.equals(element, ELEM_SUBJECT))
				    {
				    	template.setSubject(staxXmlReader.getElementText());
				    }
				    //message
				    if(StringUtils.equals(element, ELEM_MESSAGE))
				    {
				    	template.setMessage(staxXmlReader.getElementText());
				    }
				    //html
				    if(StringUtils.equals(element, ELEM_HTML_MESSAGE)) {
				    	template.setHtmlMessage(staxXmlReader.getElementText());
				    }
				    //locale
				    if(StringUtils.equals(element, ELEM_LOCALE)) {
				    	template.setLocale(staxXmlReader.getElementText());
				    }
				    //version - SAK-17637
				    if(StringUtils.equals(element, ELEM_VERSION)) {
				    	//set as integer version of value, or default to 0
				    	template.setVersion(Integer.valueOf(NumberUtils.toInt(staxXmlReader.getElementText(), 0)));
				    }
				    
				    //owner
				    if(StringUtils.equals(element, ELEM_OWNER)) {
				    	template.setOwner(staxXmlReader.getElementText());
				    }
				    //key
				    if(StringUtils.equals(element, ELEM_KEY)) {
				    	template.setKey(staxXmlReader.getElementText());
				    }
				}
			}
		}
		catch (XMLStreamException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				staxXmlReader.close();
			}
			catch (XMLStreamException e)
			{
				e.printStackTrace();
			}
			try
			{
				in.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		//check if we have an existing template of this key and locale
		EmailTemplate existingTemplate = emailTemplateService.getEmailTemplate(template.getKey(), new Locale(template.getLocale()));
		if(existingTemplate == null)
		{
			//no existing, save this one
			Session sakaiSession = sessionManager.getCurrentSession();
			sakaiSession.setUserId(ADMIN);
			sakaiSession.setUserEid(ADMIN);
			emailTemplateService.saveTemplate(template);
			sakaiSession.setUserId(null);
			sakaiSession.setUserId(null);
			logger.info("Saved email template: " + template.getKey() + " with locale: " + template.getLocale());
			return;
		} 
		
		//check version, if local one newer than persisted, update it - SAK-17679
		int existingTemplateVersion = existingTemplate.getVersion() != null ? existingTemplate.getVersion().intValue() : 0;
		if(template.getVersion() > existingTemplateVersion)
		{
			existingTemplate.setSubject(template.getSubject());
			existingTemplate.setMessage(template.getMessage());
			existingTemplate.setHtmlMessage(template.getHtmlMessage());
			existingTemplate.setVersion(template.getVersion());
			existingTemplate.setOwner(template.getOwner());

			Session sakaiSession = sessionManager.getCurrentSession();
			sakaiSession.setUserId(ADMIN);
			sakaiSession.setUserEid(ADMIN);
			emailTemplateService.updateTemplate(existingTemplate);
			sakaiSession.setUserId(null);
			sakaiSession.setUserId(null);
			logger.info("Updated email template: " + template.getKey() + " with locale: " + template.getLocale());
		}
	}

	public Site getSite(String siteId)
	{
		try
		{
			return siteService.getSite(siteId);
		}
		catch(IdUnusedException e)
		{
			logger.error("No site with id of '" + siteId + "'. Returning null ...");
			return null;
		}
	}

	public List<User> getUsers(Collection<String> userIds)
	{
		return userDirectoryService.getUsers(userIds);
	}

	public RenderedTemplate getRenderedTemplateForUser(String emailTemplateKey, String reference, Map<String, String> replacementValues)
	{
		return emailTemplateService.getRenderedTemplateForUser(emailTemplateKey, reference, replacementValues);
	}
}
