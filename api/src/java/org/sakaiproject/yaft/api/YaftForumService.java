package org.sakaiproject.yaft.api;

import java.util.List;
import java.util.Map;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.exception.IdUnusedException;

public interface YaftForumService extends EntityProducer
{
	public static final String ENTITY_PREFIX = "yaft";
	public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
	public static final String YAFT_MESSAGE_CREATED = "yaft.message.created";
	public static final String YAFT_MESSAGE_DELETED = "yaft.message.deleted";
	public static final String YAFT_FORUM_CREATED = "yaft.forum.created";
	public static final String YAFT_FORUM_DELETED = "yaft.forum.deleted";
	public static final String YAFT_DISCUSSION_CREATED = "yaft.discussion.created";
	public static final String YAFT_DISCUSSION_DELETED = "yaft.discussion.deleted";

	/**
	 * Get the forums for the specified site
	 * 
	 * @param siteId The site we want the associated forums for
	 * @return A list of Forum objects
	 */
	public List<Forum> getSiteForums(String siteId,boolean fully);
	
	/**
	 * Get the discussions for the specified forum.
	 * 
	 * @param forumId The forum we want the associated discussions for
	 * @return The Discussions in the specified forum
	 */
	public Forum getForum(String forumId,String state);
	
	public Forum getForumForTitle(String title,String state,String siteId);
	
	public Discussion getDiscussion(String discussionId,boolean fully);
	
	public SakaiProxy getSakaiProxy();

	public boolean addOrUpdateForum(Forum forum);
	
	public Discussion addDiscussion(String siteId, String forumId,Discussion discussion,boolean sendMail);

	public boolean addOrUpdateMessage(String siteId, String forumId,Message message,boolean sendMail);

	public List<Forum> getFora(boolean fully);

	//public List<Discussion> getDiscussions();

	public List<Discussion> getForumDiscussions(String id,boolean fully);

	public List<Message> getMessages();

	public void deleteForum(String forumId);

	public boolean deleteDiscussion(String discussionId);
	
	public void deleteMessage(Message messageId,String forumId);
	
	public void undeleteMessage(Message messageId,String forumId);

	public void unsubscribeFromDiscussion(String userId, String discussionId);

	public List<String> getDiscussionUnsubscriptions(String userId);

	public void subscribeToDiscussion(String userId, String discussionId);

	public void showMessage(Message messageId);

	public void deleteAttachment(String attachmentId, String messageId);

	public Message getMessage(String messageId);

	public Forum getForumContainingMessage(String messageId);

	public boolean markMessageRead(String messageId,String forumId,String discussionId);

	public boolean markMessageUnRead(String messageId,String forumId,String discussionId);
	
	public boolean markDiscussionRead(String discussionId,String forumId);

	public List<String> getReadMessageIds(String discussionId);

	public void moveDiscussion(String discussionId, String currentForumId,String newForumId);

	public void publishMessage(String forumId,Message message);

	public Map<String,Integer> getReadMessageCountForAllFora();

	public Map<String, Integer> getReadMessageCountForForum(String forumId);

	public void subscribeToForum(String forumId);
	
	public void unsubscribeFromForum(String forumId);

	public List<String> getForumUnsubscriptions(String userId);

	public boolean savePreferences(YaftPreferences preferences);

	public YaftPreferences getPreferencesForCurrentUserAndSite();
	
	public YaftPreferences getPreferencesForUser(String userId,String siteId);

	public List<ActiveDiscussion> getActiveDiscussions(String siteId);

	public String getIdOfSiteContainingMessage(String messageId);
}
