/**
 * Copyright 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.yaft.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.AuthzPermissionException;
import org.sakaiproject.authz.api.FunctionManager;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.authz.api.Member;
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
import org.sakaiproject.delegatedaccess.logic.ProjectLogic;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.search.api.SearchList;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.service.gradebook.shared.GradeDefinition;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.time.api.TimeRange;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.BaseResourceProperties;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Group;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.YaftFunctions;
import org.sakaiproject.yaft.api.YaftGBAssignment;

/**
 * All Sakai API calls go in here. If Sakai changes all we have to do if mod
 * this file.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public class SakaiProxyImpl implements SakaiProxy {

	private Logger logger = Logger.getLogger(SakaiProxyImpl.class);

	private ServerConfigurationService serverConfigurationService = null;

	private UserDirectoryService userDirectoryService = null;

	private SqlService sqlService = null;

	private SiteService siteService = null;

	private ToolManager toolManager;

	private FunctionManager functionManager;

	private AuthzGroupService authzGroupService;

	private ContentHostingService contentHostingService;

	private SecurityService securityService;

	private EntityManager entityManager;

	private EventTrackingService eventTrackingService;
	
	private NotificationService notificationService;

	private CalendarService calendarService;

	private TimeService timeService;

	private UsageSessionService usageSessionService;

	private SearchService searchService;

	private GradebookService gradebookService;

	private ProjectLogic projectLogic;

	public SakaiProxyImpl() {

		ComponentManager componentManager = org.sakaiproject.component.cover.ComponentManager.getInstance();
		serverConfigurationService = (ServerConfigurationService) componentManager.get(ServerConfigurationService.class);
		userDirectoryService = (UserDirectoryService) componentManager.get(UserDirectoryService.class);
		sqlService = (SqlService) componentManager.get(SqlService.class);
		siteService = (SiteService) componentManager.get(SiteService.class);
		toolManager = (ToolManager) componentManager.get(ToolManager.class);
		functionManager = (FunctionManager) componentManager.get(FunctionManager.class);
		authzGroupService = (AuthzGroupService) componentManager.get(AuthzGroupService.class);
		securityService = (SecurityService) componentManager.get(SecurityService.class);
		contentHostingService = (ContentHostingService) componentManager.get(ContentHostingService.class);
		eventTrackingService = (EventTrackingService) componentManager.get(EventTrackingService.class);
		notificationService = (NotificationService) componentManager.get(NotificationService.class);
		timeService = (TimeService) componentManager.get(TimeService.class);
		calendarService = (CalendarService) componentManager.get(CalendarService.class);
		entityManager = (EntityManager) componentManager.get(EntityManager.class);
		usageSessionService = (UsageSessionService) componentManager.get(UsageSessionService.class);
		searchService = (SearchService) componentManager.get(SearchService.class);
		gradebookService = (GradebookService) componentManager.get("org_sakaiproject_service_gradebook_GradebookService");
		projectLogic = (ProjectLogic) componentManager.get(ProjectLogic.class);
		
		NotificationEdit ne = notificationService.addTransientNotification();
		ne.setResourceFilter(YaftForumService.REFERENCE_ROOT);
		ne.setFunction(YaftForumService.YAFT_FORUM_CREATED);
		NewForumNotification yn = new NewForumNotification();
		yn.setSakaiProxy(this);
		ne.setAction(yn);
		
		NotificationEdit ne2 = notificationService.addTransientNotification();
		ne2.setResourceFilter(YaftForumService.REFERENCE_ROOT);
		ne2.setFunction(YaftForumService.YAFT_DISCUSSION_CREATED);
		NewDiscussionNotification dn = new NewDiscussionNotification();
		dn.setSakaiProxy(this);
		ne2.setAction(dn);
		
		NotificationEdit ne3 = notificationService.addTransientNotification();
		ne3.setResourceFilter(YaftForumService.REFERENCE_ROOT);
		ne3.setFunction(YaftForumService.YAFT_MESSAGE_CREATED);
		NewMessageNotification mn = new NewMessageNotification();
		mn.setSakaiProxy(this);
		ne3.setAction(mn);
	}

	public boolean isAutoDDL(){

		String autoDDL = serverConfigurationService.getString("auto.ddl");
		return autoDDL.equals("true");
	}

	public String getDbVendor() {
		return sqlService.getVendor();
	}

	public Site getCurrentSite() {

		try {
			return siteService.getSite(getCurrentSiteId());
		} catch (Exception e) {
			logger.error("Failed to get current site.", e);
			return null;
		}
	}

	public String getCurrentSiteId() {

		Placement placement = toolManager.getCurrentPlacement();
		if (placement == null) {
			logger.warn("Current tool placement is null.");
			return null;
		}

		return placement.getContext();
	}

	public Connection borrowConnection() throws SQLException {
		return sqlService.borrowConnection();
	}

	public void returnConnection(Connection connection) {
		sqlService.returnConnection(connection);
	}

	public String getDisplayNameForUser(String creatorId) {

		try {
			User sakaiUser = userDirectoryService.getUser(creatorId);
			return sakaiUser.getDisplayName();
		} catch (Exception e) {
			return creatorId; // this can happen if the user does not longer
			// exist in the system
		}
	}

	public void registerFunction(String function) {

		List functions = functionManager.getRegisteredFunctions("yaft.");

		if (!functions.contains(function)) {
			functionManager.registerFunction(function, true);
        }
	}

	public String getSakaiHomePath() {
		return serverConfigurationService.getSakaiHomePath();
	}

	public boolean addCalendarEntry(String title, String description, String type, long startDate, long endDate) {

		try {
			Calendar cal = calendarService.getCalendar("/calendar/calendar/" + getCurrentSiteId() + "/main");
			CalendarEventEdit edit = cal.addEvent();
			TimeRange range = timeService.newTimeRange(startDate, endDate - startDate);
			edit.setRange(range);
			edit.setDescriptionFormatted(description);
			edit.setDisplayName(title);
			edit.setType(type);
			cal.commitEvent(edit);
			return true;
		} catch (Exception e) {
			logger.error("Failed to add calendar entry. Returning false ...", e);
			return false;
		}
	}

	public boolean removeCalendarEntry(String title, String description) {

		try {
			Calendar cal = calendarService.getCalendar("/calendar/calendar/" + getCurrentSiteId() + "/main");
			List<CalendarEvent> events = cal.getEvents(null, null);
			for (CalendarEvent event : events) {
				if (event.getDisplayName().equals(title) && event.getDescription().equals(description)) {
					CalendarEventEdit edit = cal.getEditEvent(event.getId(), CalendarService.SECURE_REMOVE);
					cal.removeEvent(edit);
					return true;
				}
			}

			return true;

		} catch (Exception e) {
			logger.error("Failed to add calendar entry. Returning false ...", e);
			return false;
		}
	}

	public String getServerUrl() {
		return serverConfigurationService.getServerUrl();
	}

	private String getEmailForTheUser(String userId) {

		try {
			User sakaiUser = userDirectoryService.getUser(userId);
			return sakaiUser.getEmail();
		} catch (Exception e) {
			return ""; // this can happen if the user does not longer exist in
			// the system
		}
	}

	public User getCurrentUser() {

		try {
			return userDirectoryService.getCurrentUser();
		} catch (Throwable t) {
			logger.error("Exception caught whilst getting current user. Returning null ...", t);
			return null;
		}
	}

	public String getPortalUrl() {
		return serverConfigurationService.getString("serverUrl") + serverConfigurationService.getString("portalPath");
	}

	public String getCurrentPageId() {

		Placement placement = toolManager.getCurrentPlacement();

		if (placement instanceof ToolConfiguration) {
			return ((ToolConfiguration) placement).getPageId();
        }

		return null;
	}

	public String getCurrentToolId() {
		return toolManager.getCurrentPlacement().getId();
	}

	public String getYaftPageId(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			return tc.getPageId();
		} catch (Exception e) {
            logger.error("Exception whilst getting the YAFT page id. Returning an empty string ...", e);
			return "";
		}
	}

	public String getYaftToolId(String siteId) {

		try {
			Site site = siteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			return tc.getId();
		} catch (Exception e) {
            logger.error("Exception whilst getting the YAFT tool id. Returning an empty string ...", e);
			return "";
		}
	}

	public String getDirectUrl(String siteId, String string) {

		String portalUrl = getPortalUrl();

		String pageId = null;
		String toolId = null;

		if (siteId == null) {
			siteId = getCurrentSiteId();
			pageId = getCurrentPageId();
			toolId = getCurrentToolId();
		} else {
			pageId = getYaftPageId(siteId);
			toolId = getYaftToolId(siteId);
		}

		try {
			String url = portalUrl + "/directtool/" + toolId + string;
			return url;
		} catch (Exception e) {
			logger.error("Caught exception whilst building direct URL.", e);
			return null;
		}
	}

	private void enableSecurityAdvisor() {

		securityService.pushAdvisor(new SecurityAdvisor() {

			public SecurityAdvice isAllowed(String userId, String function, String reference) {
				return SecurityAdvice.ALLOWED;
			}
		});
	}

	/**
	 * Saves the file to Sakai's content hosting
	 */
	public String saveFile(String siteId, String creatorId, String name, String mimeType, byte[] fileData) throws Exception {

		if (name == null | name.length() == 0) {
			throw new IllegalArgumentException("The name argument must be populated.");
        }

		if (name.endsWith(".doc")) {
			mimeType = "application/msword";
        } else if (name.endsWith(".xls")) {
			mimeType = "application/excel";
        }

		if (siteId == null) {
			siteId = getCurrentSiteId();
        }

		String id = "/group/" + siteId + "/yaft-files/" + name;

		try {
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
		} catch (IdUsedException e) {
			if (logger.isInfoEnabled()) {
				logger.info("A resource with id '" + id + "' exists already. Returning id without recreating ...");
            }
			return name;
		}
	}

	public void getAttachment(String siteId, Attachment attachment) {

		if (siteId == null) {
			siteId = getCurrentSiteId();
        }

		try {
			enableSecurityAdvisor();
			String id = "/group/" + siteId + "/yaft-files/" + attachment.getResourceId();
			// ContentResource resource =
			// contentHostingService.getResource(attachment.getResourceId());
			ContentResource resource = contentHostingService.getResource(id);
			ResourceProperties properties = resource.getProperties();
			attachment.setMimeType(properties.getProperty(ResourceProperties.PROP_CONTENT_TYPE));
			attachment.setName(properties.getProperty(ResourceProperties.PROP_DISPLAY_NAME));
			attachment.setUrl(resource.getUrl());
		} catch (Exception e) {
			logger.error("Caught an exception with message '" + e.getMessage() + "'", e);
		}
	}

	public void deleteFile(String resourceId) throws Exception {

		enableSecurityAdvisor();
		String id = "/group/" + getCurrentSiteId() + "/yaft-files/" + resourceId;
		contentHostingService.removeResource(id);
	}

	public List<Site> getAllSites() {
		return siteService.getSites(SiteService.SelectionType.NON_USER, null, null, null, null, null);
	}

	public ToolConfiguration getFirstInstanceOfTool(String siteId, String toolId) {

		try {
			return siteService.getSite(siteId).getToolForCommonId(toolId);
		} catch (IdUnusedException e) {
			logger.warn("Exception while getting first instance of '" + toolId + "'. Returning null ...", e);
			return null;
		}
	}

	public String[] getSiteIds() {
		return null;
	}

	public void registerEntityProducer(EntityProducer entityProducer) {
		entityManager.registerEntityProducer(entityProducer, YaftForumService.REFERENCE_ROOT);
	}

	public void postEvent(String event, String reference) {

		UsageSession usageSession = usageSessionService.getSession();
		eventTrackingService.post(eventTrackingService.newEvent(event, reference, true), usageSession);
	}

	public Site getSite(String siteId) {

		try {
			return siteService.getSite(siteId);
		} catch (IdUnusedException e) {
			logger.warn("No site with id of '" + siteId + "'. Returning null ...");
			return null;
		}
	}

	public List<User> getUsers(Collection<String> userIds) {
		return userDirectoryService.getUsers(userIds);
	}

	public Set<String> getPermissionsForCurrentUserAndSite() {

		String userId = getCurrentUser().getId();

		if (userId == null) {
			throw new SecurityException("This action (userPerms) is not accessible to anon and there is no current user.");
		}

		Set<String> filteredFunctions = new TreeSet<String>();

		if (securityService.isSuperUser(userId)) {
			// Special case for the super admin
			filteredFunctions.addAll(functionManager.getRegisteredFunctions("yaft"));
			filteredFunctions.add("gradebook.gradeAll");
		} else {
			Site site = null;
			AuthzGroup siteHelperRealm = null;
            String siteId = getCurrentSiteId();

			try {
				site = siteService.getSite(siteId);
				siteHelperRealm = authzGroupService.getAuthzGroup("!site.helper");
			} catch (Exception e) {
				// This should probably be logged but not rethrown.
			}

            String[] delegatedAccess = projectLogic.getCurrentUsersAccessToSite("/site/" + siteId);

			Role currentUserRole = null;
            if (delegatedAccess != null && delegatedAccess.length >= 2) {
                currentUserRole = site.getRole(delegatedAccess[1]);
            } else {
			    currentUserRole = site.getUserRole(userId);
            }

            if (currentUserRole != null) {
                Role siteHelperRole = siteHelperRealm.getRole(currentUserRole.getId());

                Set<String> functions = currentUserRole.getAllowedFunctions();

                if (siteHelperRole != null) {
                    // Merge in all the functions from the same role in !site.helper
                    functions.addAll(siteHelperRole.getAllowedFunctions());
                }

                for (String function : functions) {
                    if (function.startsWith("yaft") || "gradebook.gradeAll".equals(function)) {
                        filteredFunctions.add(function);
                    }
                }

                if (functions.contains("site.upd")) {
                    filteredFunctions.add(YaftFunctions.YAFT_MODIFY_PERMISSIONS);
                }
            } else {
                logger.warn("Failed to get current user role in site. An empty permissions set will be returned ...");
            }
		}

		return filteredFunctions;
	}

	public Map<String, Set<String>> getPermsForCurrentSite() {

		Map<String, Set<String>> perms = new HashMap<String, Set<String>>();

		String userId = getCurrentUser().getId();

		if (userId == null) {
			throw new SecurityException("This action (perms) is not accessible to anon and there is no current user.");
		}

		String siteId = getCurrentSiteId();
		Site site = null;

		try {
			site = siteService.getSite(siteId);

			Set<Role> roles = site.getRoles();
			for (Role role : roles) {
				Set<String> functions = role.getAllowedFunctions();
				Set<String> filteredFunctions = new TreeSet<String>();
				for (String function : functions) {
					if (function.startsWith("yaft")) {
						filteredFunctions.add(function);
                    }
				}

				perms.put(role.getId(), filteredFunctions);
			}
		} catch (Exception e) {
			logger.error("Failed to get current site permissions.", e);
		}

		return perms;
	}

	public boolean setPermsForCurrentSite(Map<String, String[]> params) {

		String userId = getCurrentUser().getId();

		if (userId == null) {
			throw new SecurityException("This action (setPerms) is not accessible to anon and there is no current user.");
        }

		String siteId = getCurrentSiteId();

		Site site = null;

		try {
			site = siteService.getSite(siteId);
		} catch (IdUnusedException ide) {
			logger.warn(userId + " attempted to update YAFT permissions for unknown site " + siteId);
			return false;
		}

		try {
			AuthzGroup authzGroup = authzGroupService.getAuthzGroup(site.getReference());
			Role siteRole = authzGroup.getUserRole(userId);
			AuthzGroup siteHelperAuthzGroup = authzGroupService.getAuthzGroup("!site.helper");
			Role siteHelperRole = siteHelperAuthzGroup.getRole(siteRole.getId());

			if (!securityService.isSuperUser() && !siteRole.isAllowed(YaftFunctions.YAFT_MODIFY_PERMISSIONS) && !siteHelperRole.isAllowed(YaftFunctions.YAFT_MODIFY_PERMISSIONS) && !siteRole.isAllowed("site.upd")) {
				logger.warn(userId + " attempted to update YAFT permissions for site " + site.getTitle());
				return false;
			}

			boolean changed = false;

			for (String name : params.keySet()) {
				if (!name.contains(":")) {
					continue;
                }

				String value = params.get(name)[0];

				String roleId = name.substring(0, name.indexOf(":"));

				Role role = authzGroup.getRole(roleId);
				if (role == null) {
					throw new IllegalArgumentException("Invalid role id '" + roleId + "' provided in POST parameters.");
				}
				String function = name.substring(name.indexOf(":") + 1);

				if ("true".equals(value)) {
					role.allowFunction(function);
                } else {
					role.disallowFunction(function);
                }

				changed = true;
			}

			if (changed) {
				try {
					authzGroupService.save(authzGroup);
				} catch (AuthzPermissionException ape) {
					throw new SecurityException("The permissions for this site (" + siteId + ") cannot be updated by the current user.");
				}
			}

			return true;
		} catch (GroupNotDefinedException gnde) {
			logger.error("No realm defined for site (" + siteId + ").");
		}

		return false;
	}

	public List<SearchResult> searchInCurrentSite(String searchTerms) {

		List<SearchResult> results = new ArrayList<SearchResult>();

		List<String> contexts = new ArrayList<String>(1);
		contexts.add(getCurrentSiteId());

		try {
			SearchList sl = searchService.search(searchTerms, contexts, 0, 50, "normal", "normal");
			for (SearchResult sr : sl) {
				if ("Discussions".equals(sr.getTool())) {
					results.add(sr);
                }
			}

		} catch (Exception e) {
			logger.error("Caught exception whilst searching", e);
		}

		return results;
	}

	public boolean isCurrentUserMemberOfAnyOfTheseGroups(List<Group> groups) {

		String userId = getCurrentUser().getId();

		for (Group group : groups) {
			try {
				AuthzGroup ag = authzGroupService.getAuthzGroup("/site/" + getCurrentSiteId() + "/group/" + group.getId());

				if (ag.getMember(userId) != null) {
					return true;
                }
			} catch (GroupNotDefinedException gnde) {
                logger.warn("No group for id '/group/" + group.getId() + "'");
			}
		}

		return false;
	}

	public List<User> getGroupUsers(List<Group> groups) {

		List<User> users = new ArrayList<User>();

		for (Group group : groups) {
			String groupId = "/site/" + getCurrentSiteId() + "/group/" + group.getId();
			try {
				AuthzGroup ag = authzGroupService.getAuthzGroup(groupId);
				Set<Member> groupMembers = ag.getMembers();
				for (Member member : groupMembers) {
					try {
						users.add(userDirectoryService.getUser(member.getUserId()));
					} catch (UserNotDefinedException e) {
						logger.error("No user for id '" + member.getUserId() + "'");
					}
				}
			} catch (GroupNotDefinedException e) {
                logger.warn("No group for id '" + groupId + "'");
			}
		}

		return users;
	}

	public List<Group> getCurrentSiteGroups() {

		List<Group> groups = new ArrayList<Group>();

		Collection<org.sakaiproject.site.api.Group> sakaiGroups = getCurrentSite().getGroups();

		for (org.sakaiproject.site.api.Group sakaiGroup : sakaiGroups) {
			groups.add(new Group(sakaiGroup.getId(), sakaiGroup.getTitle()));
        }

		return groups;
	}

	public boolean currentUserHasFunctionInCurrentSite(String function) {

		String siteId = getCurrentSiteId();
		return securityService.unlock(function, "/site/" + siteId);
	}

	public boolean userHasFunctionInCurrentSite(String userId, String function) {

		String siteId = getCurrentSiteId();
		return securityService.unlock(userId, function, "/site/" + siteId);
	}

	public boolean scoreAssignment(int assignmentId, String studentId, String score) {

		String siteId = this.getCurrentSiteId();

		Assignment assignment = gradebookService.getAssignment(siteId, (long) assignmentId);

		try {
			gradebookService.setAssignmentScoreString(siteId, assignment.getName(), studentId, score, "YAFT");
			return true;
		} catch (Exception e) {
			logger.error("Failed to score assignment '" + assignment.getName() + "'. Returning false ...", e);
			return false;
		}
	}

	public String getSakaiSkin() {
        return siteService.getSiteSkin(getCurrentSiteId());
	}

	public List<YaftGBAssignment> getGradebookAssignments() {

		List<YaftGBAssignment> yaftGBAssignments = new ArrayList<YaftGBAssignment>();
		try {
			List<Assignment> gbAssignments = gradebookService.getAssignments(getCurrentSiteId());
			for (Assignment gbAss : gbAssignments) {
				yaftGBAssignments.add(new YaftGBAssignment(gbAss.getId(),gbAss.getName(), gbAss.getPoints()));
			}
		} catch (GradebookNotFoundException gnfe) {
			// Normal. GB has not been added to the site yet.
		} catch (Exception e) {
			logger.error("Caught exception whilst getting gradebook assignments for current site. Message: " + e.getMessage());
		}

		return yaftGBAssignments;
	}

	public YaftGBAssignment getGradebookAssignment(int gradebookAssignmentId) {

		try {
			Assignment gbAss = gradebookService.getAssignment(getCurrentSiteId(), (long) gradebookAssignmentId);
			return new YaftGBAssignment(gbAss.getId(),gbAss.getName(), gbAss.getPoints());
		} catch (AssessmentNotFoundException e) {
			return null;
		}
	}

	public GradeDefinition getAssignmentGrade(String userId, long assignmentId) {

		if (!gradebookService.isUserAbleToViewItemForStudent(getCurrentSiteId(), (long) assignmentId, userId)) {
			logger.warn("Current user allowed to view grades for user '" + userId + "'. Returning null ...");
			return null;
		}

		try {
			return gradebookService.getGradeDefinitionForStudentForItem(getCurrentSiteId(), (long) assignmentId, userId);
		} catch (Exception e) {
			logger.error("Failed to get grade for user '" + userId + "'. Returning null ...", e);
			return null;
		}
	}

	public List<User> getCurrentSiteMaintainers() {

		Site currentSite = getCurrentSite();
		Set<String> userIds = currentSite.getUsersHasRole(currentSite.getMaintainRole());
		List<User> maintainers = new ArrayList<User>();
		for (String userId : userIds) {
			try {
				maintainers.add(userDirectoryService.getUser(userId));
			} catch (UserNotDefinedException e) {
				logger.error("No user for id '" + userId + "'");
			}
		}
		return maintainers;
	}

	public boolean getIncludeMessageBodyInEmailSetting() {
		return serverConfigurationService.getBoolean("yaft.includeMessageBodyInEmail", false);
	}
	
	public String getWysiwygEditor() {
		return serverConfigurationService.getString("wysiwyg.editor");
	}

	public boolean isCurrentUserMemberOfSite(String siteId) {
		
		Site site = null;
		if (siteId == null) {
			site = getCurrentSite();
		} else {
			try {
				site = siteService.getSite(siteId);
			} catch (IdUnusedException e) {
				e.printStackTrace();
			}
		}
		
		if (site == null) {
            logger.warn("Failed to look up site. Returning false ...");
            return false;
        }

		return (site.getMember(userDirectoryService.getCurrentUser().getId()) != null);
	}

	public void pushSecurityAdvisor(SecurityAdvisor advisor) {
		securityService.pushAdvisor(advisor);
	}

	public boolean isCurrentUserSuperUser() {
		return securityService.isSuperUser();
	}

	public String getString(String property, String defaultValue) {
		return serverConfigurationService.getString(property, defaultValue);
	}

	public boolean canCurrentUserSendAlerts() {
		return authzGroupService.isAllowed(getCurrentUser().getId(), YaftFunctions.YAFT_SEND_ALERTS, "/site/" + getCurrentSiteId());
	}
}
