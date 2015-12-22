/** * Copyright 2009 The Sakai Foundation
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
package org.sakaiproject.yaft.dashboard;

import org.apache.log4j.Logger;

import org.sakaiproject.yaft.api.YaftForumService;
import org.sakaiproject.dash.logic.DashboardLogic;
import org.sakaiproject.user.api.UserDirectoryService;

import lombok.Setter;

@Setter
public class YaftDashboardIntegration {

    private final Logger logger = Logger.getLogger(YaftDashboardIntegration.class);

    private YaftForumService forumService;
    private DashboardLogic dashboardLogic;
    private UserDirectoryService userDirectoryService;

    /**
     * Register all the CLOG event processors with the Dashboard 
     */
    public void init() {

        logger.debug("init()");
        
        YaftForumEntityType fet = new YaftForumEntityType();
        fet.setForumService(forumService);
        fet.setUserDirectoryService(userDirectoryService);
        dashboardLogic.registerEntityType(fet);

        YaftForumCreatedEventProcessor fceProc = new YaftForumCreatedEventProcessor();
        fceProc.setForumService(forumService);
        fceProc.setDashboardLogic(dashboardLogic);
        fceProc.init();
        dashboardLogic.registerEventProcessor(fceProc);

        YaftDiscussionEntityType det = new YaftDiscussionEntityType();
        det.setForumService(forumService);
        det.setUserDirectoryService(userDirectoryService);
        dashboardLogic.registerEntityType(det);

        YaftDiscussionCreatedEventProcessor dceProc = new YaftDiscussionCreatedEventProcessor();
        dceProc.setForumService(forumService);
        dceProc.setDashboardLogic(dashboardLogic);
        dceProc.init();
        dashboardLogic.registerEventProcessor(dceProc);

        YaftMessageEntityType met = new YaftMessageEntityType();
        met.setForumService(forumService);
        met.setUserDirectoryService(userDirectoryService);
        dashboardLogic.registerEntityType(met);

        YaftMessageCreatedEventProcessor mceProc = new YaftMessageCreatedEventProcessor();
        mceProc.setForumService(forumService);
        mceProc.setDashboardLogic(dashboardLogic);
        mceProc.init();
        dashboardLogic.registerEventProcessor(mceProc);
    }
}
