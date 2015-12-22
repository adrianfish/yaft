package org.sakaiproject.yaft.dashboard;

import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.dash.model.NewsItem;
import org.sakaiproject.dash.model.SourceType;
import org.sakaiproject.event.api.Event;

public class YaftMessageCreatedEventProcessor extends YaftDashboardEventProcessor {
    
    public static final String IDENTIFIER = "yaft-message";
    
    /** Gets used by createNewsItem */
    private SourceType sourceType;
    
    public void init() {
        sourceType = dashboardLogic.createSourceType(IDENTIFIER);
    }
    
    /**
     * This tells the dashboard observer to call our processEvent method
     * when clog.post.created events are observed
     */
    public String getEventIdentifer() {
        return YaftForumService.YAFT_MESSAGE_CREATED_SS;
    }

    /**
     * Process the clog.post.created event. The aim is to create an appropriate
     * Dashboard NewsItem for it to render. 
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        
        // Parse the post id out of the entity path
        String messageId = resource.substring(resource.lastIndexOf("/") + 1);
        Message message;
        try {
            message = forumService.getUnfilteredMessage(messageId);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        NewsItem newsItem = dashboardLogic.createNewsItem(message.getSubject()
                                        ,event.getEventTime()
                                        ,YaftForumService.YAFT_MESSAGE_CREATED_SS
                                        ,resource
                                        ,dashboardLogic.getContext(message.getSiteId())
                                        ,sourceType
                                        ,"");
        dashboardLogic.createNewsLinks(newsItem);
    }
}
