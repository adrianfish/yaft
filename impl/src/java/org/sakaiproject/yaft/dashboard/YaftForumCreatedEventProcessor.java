package org.sakaiproject.yaft.dashboard;

import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.dash.model.NewsItem;
import org.sakaiproject.dash.model.SourceType;
import org.sakaiproject.event.api.Event;

public class YaftForumCreatedEventProcessor extends YaftDashboardEventProcessor {
    
    public static final String IDENTIFIER = "yaft-forum";
    
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
        return YaftForumService.YAFT_FORUM_CREATED_SS;
    }

    /**
     * Process the clog.post.created event. The aim is to create an appropriate
     * Dashboard NewsItem for it to render. 
     */
    public void processEvent(Event event) {

        String resource = event.getResource();
        
        // Parse the post id out of the entity path
        String forumId = resource.substring(resource.lastIndexOf("/") + 1);
        Forum forum;
        try {
            forum = forumService.getUnfilteredForum(forumId, ForumPopulatedStates.EMPTY);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        NewsItem newsItem = dashboardLogic.createNewsItem(forum.getTitle()
                                        ,event.getEventTime()
                                        ,YaftForumService.YAFT_FORUM_CREATED_SS
                                        ,resource
                                        ,dashboardLogic.getContext(forum.getSiteId())
                                        ,sourceType
                                        ,"");
        dashboardLogic.createNewsLinks(newsItem);
    }
}
