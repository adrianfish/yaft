package org.sakaiproject.yaft.dashboard;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.dash.entity.DashboardEntityInfo;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

public class YaftForumEntityType extends YaftDashboardEntityType{

    public String getGroupTitle(int numberOfItems, String contextTitle, String labelKey) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        return rl.getString("grouped_forum_title");
    }

    public String getIdentifier() {
        return YaftForumCreatedEventProcessor.IDENTIFIER;
    }

    public Map<String, Object> getValues(String entityReference, String localeCode) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        String forumId = entityReference.substring(entityReference.lastIndexOf("/") + 1);
        Forum forum;
        try {
            forum = forumService.getUnfilteredForum(forumId, ForumPopulatedStates.EMPTY);
        } catch (Exception e) {
            return null;
        }
        List<Map<String,String>> infoList = new ArrayList<Map<String,String>>();
        Map<String,String> infoItem = new HashMap<String,String>();
        infoItem.put(VALUE_INFO_LINK_URL, forum.getUrl());
        infoItem.put(VALUE_INFO_LINK_TITLE, rl.getString("view_in_site"));
        infoItem.put(VALUE_INFO_LINK_TARGET, "_top");
        infoList.add(infoItem);
        Map<String, Object> values = new HashMap<String, Object>();
        DateFormat df = DateFormat.getDateTimeInstance();
        values.put(VALUE_NEWS_TIME, df.format(new Date(forum.getCreatedDate())));
        values.put(VALUE_MORE_INFO, infoList);
        values.put(VALUE_TITLE, forum.getTitle());
        try {
            values.put(VALUE_USER_NAME, userDirectoryService.getUser(forum.getCreatorId()).getDisplayName());
        } catch (UserNotDefinedException unde) {
        }
        values.put(DashboardEntityInfo.VALUE_ENTITY_TYPE, YaftForumCreatedEventProcessor.IDENTIFIER);
        return values;
    }
}
