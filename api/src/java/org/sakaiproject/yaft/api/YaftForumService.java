/**
 * Copyright 2009 The Sakai Foundation
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
package org.sakaiproject.yaft.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityProducer;

public interface YaftForumService extends EntityProducer
{
	
	
	public static final String ENTITY_PREFIX = "yaft";
	public static final String REFERENCE_ROOT = Entity.SEPARATOR + ENTITY_PREFIX;
	
	// Events. The _SS ones are for SiteStats,Search etc. The others are for notifications
	// and are conditional on the send email box being checked
	public static final String YAFT_MESSAGE_CREATED = "yaft.message.created";
	public static final String YAFT_MESSAGE_CREATED_SS = "yaft.message.created.ss";
	public static final String YAFT_MESSAGE_DELETED_SS = "yaft.message.deleted.ss";
	public static final String YAFT_FORUM_CREATED = "yaft.forum.created";
	public static final String YAFT_FORUM_CREATED_SS = "yaft.forum.created.ss";
	public static final String YAFT_FORUM_DELETED_SS = "yaft.forum.deleted.ss";
	public static final String YAFT_DISCUSSION_CREATED = "yaft.discussion.created";
	public static final String YAFT_DISCUSSION_CREATED_SS = "yaft.discussion.created.ss";
	public static final String YAFT_DISCUSSION_DELETED_SS = "yaft.discussion.deleted.ss";

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
	public boolean addOrUpdateForum(Forum forum,boolean sendEmail);
	
	public Discussion addDiscussion(String siteId, String forumId,Discussion discussion,boolean sendMail);

	public boolean addOrUpdateMessage(String siteId, String forumId,Message message,boolean sendMail);

	public List<Forum> getFora(boolean fully);

	//public List<Discussion> getDiscussions();

	public List<Discussion> getForumDiscussions(String id,boolean fully);

	public void deleteForum(String forumId);

	public boolean deleteDiscussion(String discussionId);
	
	public void deleteMessage(Message messageId,String forumId);
	
	public void undeleteMessage(Message messageId,String forumId);

	public void showMessage(Message messageId);

	public void deleteAttachment(String attachmentId, String messageId);

	public Message getMessage(String messageId);

	public Forum getForumContainingMessage(String messageId);

	public boolean markMessageRead(String messageId,String forumId,String discussionId);

	public boolean markMessageUnRead(String messageId,String forumId,String discussionId);
	
	public boolean markDiscussionRead(String discussionId,String forumId);

	public List<String> getReadMessageIds(String discussionId);

	public void moveDiscussion(String discussionId, String currentForumId,String newForumId);

	public Map<String,Integer> getReadMessageCountForAllFora();

	public Map<String, Integer> getReadMessageCountForForum(String forumId);

	public List<ActiveDiscussion> getActiveDiscussions();

	public String getIdOfSiteContainingMessage(String messageId);
	
	public List<Author> getAuthorsForCurrentSite();

	public List<Message> getMessagesForAuthorInCurrentSite(String authorId);

	public List<Author> getAuthorsForDiscussion(String discussionId);

	public List<Message> getMessagesForAuthorInDiscussion(String authorId,
			String discussionId);
	
	public boolean setDiscussionGroups(String discussionId,Collection<String> groups);
}
