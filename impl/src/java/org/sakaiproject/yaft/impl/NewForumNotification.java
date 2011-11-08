package org.sakaiproject.yaft.impl;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.Notification;
import org.sakaiproject.event.api.NotificationAction;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.EmailNotification;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.SiteEmailNotification;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.YaftFunctions;

public class NewForumNotification extends SiteEmailNotification{
	
	private static ResourceLoader rb = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.newforumnotification");
	
	public NewForumNotification() {
	}
	
	/**
     * Construct.
     */
    public NewForumNotification(String siteId) {
        super(siteId);
    }
    
    protected void addSpecialRecipients(List users, Reference ref) {
    	System.out.println("addSpecialRecipients");
    }

	
	protected String plainTextContent(Event event) {
    	System.out.println("plainTextContent");
		Reference ref = EntityManager.newReference(event.getResource());
        Forum forum = (Forum) ref.getEntity();
        
		String creatorName = "";
		try {
			creatorName = UserDirectoryService.getUser(forum.getCreatorId()).getDisplayName();
		} catch (UserNotDefinedException e) {
			e.printStackTrace();
		}
		
		return rb.getFormattedMessage("noti.neworupdatedforum", new Object[]{creatorName,forum.getTitle(),ServerConfigurationService.getServerUrl() + forum.getUrl()});
	}
	
	protected String getSubject(Event event) {
    	System.out.println("getSubject");
		Reference ref = EntityManager.newReference(event.getResource());
        Forum forum = (Forum) ref.getEntity();
        
        String siteTitle = "";
		try {
			siteTitle = SiteService.getSite(forum.getSiteId()).getTitle();
		} catch (IdUnusedException e) {
			e.printStackTrace();
		}
        
        return rb.getFormattedMessage("noti.subject", new Object[]{siteTitle, forum.getTitle()});
	}
	
	protected String getResourceAbility() {
    	System.out.println("getResourceAbility");
		return YaftFunctions.YAFT_MESSAGE_READ;
	}
	
	protected String getTag(String title, boolean shouldUseHtml) {
    	System.out.println("getTag");
		return rb.getFormattedMessage("noti.tag", new Object[]{ServerConfigurationService.getString("ui.service", "Sakai"), ServerConfigurationService.getPortalUrl(), title});
    }
	
	protected List getHeaders(Event event) {
        List rv = super.getHeaders(event);
        //rv.add("Content-Type: text/plain");
        rv.add("Subject: " + getSubject(event));
        rv.add(getFromAddress(event));
        rv.add(getTo(event));
        return rv;
    }
	
	protected EmailNotification makeEmailNotification() {
    	System.out.println("makeEmailNotification");
        return new NewForumNotification();
    }
	
	public void notify(Notification notification, Event event) {
    	System.out.println("notify");
		super.notify(notification, event);
	}
	
	protected List<User> getRecipients(Event event) {
    	System.out.println("getRecipients");
		Reference ref = EntityManager.newReference(event.getResource());
        Forum forum = (Forum) ref.getEntity();
        
        if(forum.getGroups().size() > 0) {
        	return new ArrayList<User>();
        }
        else {
        	List<User> recipients = super.getRecipients(event);
        	return super.getRecipients(event);
        }
	}
	
	/**
     * Format the announcement notification from address.
     * 
     * @param event
     *        The event that matched criteria to cause the notification.
     * @return the announcement notification from address.
     */
    protected String getFromAddress(Event event)
    {
    	System.out.println("getFromAddress");
        String userEmail = "no-reply@" + ServerConfigurationService.getServerName();
        String userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
        String no_reply= "From: \"" + userDisplay + "\" <" + userEmail + ">";
        String from= getFrom(event);
        // get the message
        Reference ref = EntityManager.newReference(event.getResource());
        Forum msg = (Forum) ref.getEntity();
        String userId=msg.getCreatorId();

        //checks if "from" email id has to be included? and whether the notification is a delayed notification?. SAK-13512
        if ((ServerConfigurationService.getString("emailFromReplyable@org.sakaiproject.event.api.NotificationService").equals("true")) && from.equals(no_reply) && userId !=null){

                try
                {
                    User u = UserDirectoryService.getUser(userId);
                    userDisplay = u.getDisplayName();
                    userEmail = u.getEmail();
                    if ((userEmail != null) && (userEmail.trim().length()) == 0) userEmail = null;

                }
                catch (UserNotDefinedException e)
                {
                }

                // some fallback positions
                if (userEmail == null) userEmail = "no-reply@" + ServerConfigurationService.getServerName();
                if (userDisplay == null) userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
                from="From: \"" + userDisplay + "\" <" + userEmail + ">";
        }

        return from;
    }

}
