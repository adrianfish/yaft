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
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.YaftFunctions;

public class NewMessageNotification extends SiteEmailNotification{
	
	private static ResourceLoader rb = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.newmessagenotification");
	
	public NewMessageNotification() {
	}
	
	/**
     * Construct.
     */
    public NewMessageNotification(String siteId) {
        super(siteId);
    }
    
    protected void addSpecialRecipients(List users, Reference ref) {
    }

	
	protected String plainTextContent(Event event) {
		Reference ref = EntityManager.newReference(event.getResource());
        Message message = (Message) ref.getEntity();
        
		String creatorName = "";
		try {
			creatorName = UserDirectoryService.getUser(message.getCreatorId()).getDisplayName();
		} catch (UserNotDefinedException e) {
			e.printStackTrace();
		}
		
		return rb.getFormattedMessage("noti.neworupdatedmessage", new Object[]{creatorName,message.getSubject(),ServerConfigurationService.getServerUrl() + message.getUrl()});
	}
	
	protected String getSubject(Event event) {
		Reference ref = EntityManager.newReference(event.getResource());
        Message message = (Message) ref.getEntity();
        
        String siteTitle = "";
		try {
			siteTitle = SiteService.getSite(message.getSiteId()).getTitle();
		} catch (IdUnusedException e) {
			e.printStackTrace();
		}
        
        return rb.getFormattedMessage("noti.subject", new Object[]{siteTitle, message.getSubject()});
	}
	
	protected String getResourceAbility() {
		return YaftFunctions.YAFT_MESSAGE_READ;
	}
	
	protected String getTag(String title, boolean shouldUseHtml) {
		return rb.getFormattedMessage("noti.tag", new Object[]{ServerConfigurationService.getString("ui.service", "Sakai"), ServerConfigurationService.getPortalUrl(), title});
    }
	
	protected List getHeaders(Event event) {
        List rv = super.getHeaders(event);
        rv.add("Subject: " + getSubject(event));
        rv.add(getFromAddress(event));
        rv.add(getTo(event));
        return rv;
    }
	
	protected EmailNotification makeEmailNotification() {
        return new NewMessageNotification();
    }
	
	protected List<User> getRecipients(Event event) {
		Reference ref = EntityManager.newReference(event.getResource());
        Message message = (Message) ref.getEntity();
        
        return super.getRecipients(event);
	}
	
    protected String getFromAddress(Event event)
    {
    	System.out.println("getFromAddress");
        String userEmail = "no-reply@" + ServerConfigurationService.getServerName();
        String userDisplay = ServerConfigurationService.getString("ui.service", "Sakai");
        String no_reply= "From: \"" + userDisplay + "\" <" + userEmail + ">";
        String from= getFrom(event);
        // get the message
        Reference ref = EntityManager.newReference(event.getResource());
        Message message = (Message) ref.getEntity();
        String userId=message.getCreatorId();

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
