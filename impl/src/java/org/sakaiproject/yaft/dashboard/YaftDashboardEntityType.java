package org.sakaiproject.yaft.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.dash.entity.DashboardEntityInfo;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.yaft.api.Group;
import org.sakaiproject.yaft.api.YaftForumService;

import lombok.Setter;

@Setter
public abstract class YaftDashboardEntityType implements DashboardEntityInfo {
    
    protected YaftForumService forumService;
    protected UserDirectoryService userDirectoryService;

    public final String getIconUrl(String subtype) {
        return "/library/image/silk/book_edit.png";
    }

    public final List<List<String>> getOrder(String entityReference, String localeCode) {

        List<List<String>> order = new ArrayList<List<String>>();
        List<String> section0 = new ArrayList<String>();
        section0.add(VALUE_TITLE);
        order.add(section0);
        List<String> section1 = new ArrayList<String>();
        section1.add("yaft_metadata-label");
        order.add(section1);
        List<String> section3 = new ArrayList<String>();
        section3.add(VALUE_MORE_INFO);
        order.add(section3);
        return order;
    }

    public final Map<String, String> getProperties(String entityReference, String localeCode) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        Map<String,String> props = new HashMap<String,String>();
        props.put("yaft_metadata-label", rl.getString("yaft.metadata"));
        return props;
    }

    /**
     * Implement this to map Sakai events to dashboard display strings
     */
    public final String getEventDisplayString(String key, String dflt) {

        ResourceLoader rl = new ResourceLoader("org.sakaiproject.yaft.impl.bundle.dashboard");
        
        if (YaftForumService.YAFT_FORUM_CREATED_SS.equals(key)) {
            return rl.getString("new_forum");
        } else if (YaftForumService.YAFT_DISCUSSION_CREATED_SS.equals(key)) {
            return rl.getString("new_discussion");
        } else if (YaftForumService.YAFT_MESSAGE_CREATED_SS.equals(key)) {
            return rl.getString("new_message");
        }
        
        return null;
    }

    public final boolean isAvailable(String entityReference) {
        return true;
    }

    public List<String> getUsersWithAccess(String reference) {

        String[] parts = reference.split(Entity.SEPARATOR);

        String siteId = parts[2];
        String type = parts[3];
        String id = reference.substring(reference.lastIndexOf("/") + 1);

        try {
            List<String> users = new ArrayList<String>();

            Site site = SiteService.getSite(siteId);

            if (type.equals("forums")) {
                Forum forum = forumService.getForum(id, ForumPopulatedStates.EMPTY);
                users.addAll(getUsers(forum.getGroups(), site));
            } else if (type.equals("discussions")) {
                Discussion discussion = forumService.getDiscussion(id, false);
                users.addAll(getUsers(discussion.getGroups(), site));
            } else if (type.equals("messages")) {
                Discussion discussion = forumService.getDiscussion(forumService.getMessage(id).getDiscussionId(), false);
                users.addAll(getUsers(discussion.getGroups(), site));
            }

            return users;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<String> getUsers(List<Group> yaftGroups, Site site) {

        List<String> users = new ArrayList<String>();
        if (yaftGroups.size() > 0) {
            for (Group yaftGroup : yaftGroups) {
                org.sakaiproject.site.api.Group sakaiGroup = site.getGroup(yaftGroup.getId());
                users.addAll(sakaiGroup.getUsers());
            }
        } else {
            users.addAll(site.getUsers());
        }
        return users;
    }

    public boolean isUserPermitted(String sakaiUserId, String entityReference, String contextId) {
        return true;
    }
}
