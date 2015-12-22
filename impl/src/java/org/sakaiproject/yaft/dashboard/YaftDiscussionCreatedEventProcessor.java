package org.sakaiproject.yaft.dashboard;

import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.dash.model.NewsItem;
import org.sakaiproject.dash.model.SourceType;
import org.sakaiproject.event.api.Event;

public class YaftDiscussionCreatedEventProcessor extends YaftDashboardEventProcessor {
    
    public static final String IDENTIFIER = "yaft-discussion";
    
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
        return YaftForumService.YAFT_DISCUSSION_CREATED_SS;
    }

    /**
     * Process the clog.post.created event. The aim is to create an appropriate
     * Dashboard NewsItem for it to render. 
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        
        // Parse the post id out of the entity path
        String discussionId = resource.substring(resource.lastIndexOf("/") + 1);
        Discussion discussion;
        try {
            discussion = forumService.getUnfilteredDiscussion(discussionId, false);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        NewsItem newsItem = dashboardLogic.createNewsItem(discussion.getSubject()
                                        ,event.getEventTime()
                                        ,YaftForumService.YAFT_DISCUSSION_CREATED_SS
                                        ,resource
                                        ,dashboardLogic.getContext(discussion.getSiteId())
                                        ,sourceType
                                        ,"");
        dashboardLogic.createNewsLinks(newsItem);
    }
}
