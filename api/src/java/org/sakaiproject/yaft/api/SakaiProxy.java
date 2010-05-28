package org.sakaiproject.yaft.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.api.app.profile.Profile;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.user.api.User;

/**
 * All Sakai API calls go in here. If Sakai changes all we have to do if mod
 * this file.
 * 
 * @author Adrian Fish (a.fish@lancaster.ac.uk)
 */
public interface SakaiProxy
{
	public boolean isAutoDDL();
	
	public String getDbVendor();
	
	public String getCurrentSiteId();
	
	public Site getCurrentSite();
	
	public Connection borrowConnection() throws SQLException;
	
	public void returnConnection(Connection connection);

	public String getDisplayNameForUser(String creatorId);

	public String getSakaiHomePath();

	public Profile getProfile(String userId);

	public void registerFunction(String yaftForumCreate);
	
	public boolean addCalendarEntry(String title,String description, String type, long startDate,long endDate);
	
	public boolean removeCalendarEntry(String title,String description);
	
	public void sendEmailMessage(String subject,String body, String user);

	public User getCurrentUser();
	
	public Set<String> getSiteUsers();

	public String getPortalUrl();
	
	public String getServerUrl();

	public String getCurrentPageId();

	public String getCurrentToolId();

	public String getDirectUrl(String string);
	
	public String saveFile(String creatorId,String name,String mimeType, byte[] fileData) throws Exception;
	
	public void getAttachment(Attachment attachment);

	public void deleteFile(String resourceId) throws Exception;

	public String getUserBio(String id);

	public List<Site> getAllSites();

	public ToolConfiguration getFirstInstanceOfTool(String siteId, String string);

	public String[] getSiteIds();
	
	public void registerEntityProducer(EntityProducer entityProducer);
	
	public void postEvent(String event,String reference,boolean modify);

	public byte[] getResourceBytes(String resourceId);

	public void addDigestMessage(String user,String subject, String body);

	public List<String> getOfflineYaftUserIds(String siteId) throws IdUnusedException;
}
