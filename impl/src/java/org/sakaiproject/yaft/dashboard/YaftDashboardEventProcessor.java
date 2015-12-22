package org.sakaiproject.yaft.dashboard;

import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.dash.listener.EventProcessor;
import org.sakaiproject.dash.logic.DashboardLogic;

import lombok.Setter;

/**
 * Base class so we can get the dependencies in
 * 
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter
public abstract class YaftDashboardEventProcessor implements EventProcessor{
    
    protected YaftForumService forumService;
    protected DashboardLogic dashboardLogic;
}
