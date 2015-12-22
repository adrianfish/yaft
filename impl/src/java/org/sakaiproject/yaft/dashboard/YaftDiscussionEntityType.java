package org.sakaiproject.yaft.dashboard;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.dash.entity.DashboardEntityInfo;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

public class YaftDiscussionEntityType extends YaftDashboardEntityType{

    public String getGroupTitle(int numberOfItems, String contextTitle, String labelKey) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        return rl.getString("grouped_discussion_title");
    }

    public String getIdentifier() {
        return YaftDiscussionCreatedEventProcessor.IDENTIFIER;
    }

    public Map<String, Object> getValues(String entityReference, String localeCode) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        String discussionId = entityReference.substring(entityReference.lastIndexOf("/") + 1);
        Discussion discussion;
        try {
            discussion = forumService.getUnfilteredDiscussion(discussionId, false);
        } catch (Exception e) {
            return null;
        }
        List<Map<String,String>> infoList = new ArrayList<Map<String,String>>();
        Map<String,String> infoItem = new HashMap<String,String>();
        infoItem.put(VALUE_INFO_LINK_URL, discussion.getUrl());
        infoItem.put(VALUE_INFO_LINK_TITLE, rl.getString("view_in_site"));
        infoItem.put(VALUE_INFO_LINK_TARGET, "_top");
        infoList.add(infoItem);
        Map<String, Object> values = new HashMap<String, Object>();
        DateFormat df = DateFormat.getDateTimeInstance();
        values.put(VALUE_NEWS_TIME, df.format(new Date(discussion.getCreatedDate())));
        values.put(VALUE_MORE_INFO, infoList);
        values.put(VALUE_TITLE, discussion.getSubject());
        try {
            values.put(VALUE_USER_NAME, userDirectoryService.getUser(discussion.getCreatorId()).getDisplayName());
        } catch (UserNotDefinedException unde) {
        }
        values.put(DashboardEntityInfo.VALUE_ENTITY_TYPE, YaftDiscussionCreatedEventProcessor.IDENTIFIER);
        return values;
    }
}
