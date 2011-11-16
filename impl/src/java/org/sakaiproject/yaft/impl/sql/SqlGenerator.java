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
package org.sakaiproject.yaft.impl.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.Message;

public interface SqlGenerator
{
	public String TEXT  = "TEXT";
	
	List<String> getSetupStatements();

	String getForumSelectStatement(String forumId);

	List<PreparedStatement> getAddOrUpdateForumStatements(Forum forum,Connection connection) throws SQLException;

	String getForaSelectStatement(String siteId);

	//String getDiscussionsSelectStatement();
	
	String getDiscussionsSelectStatement(String forumId);

	List<PreparedStatement> getAddOrUpdateMessageStatements(String forumId,Message message, Connection connection) throws Exception;

	String getForaSelectStatement();
	
	public String getMessageSelectStatement(String messageId);

	String getMessageChildrenSelectStatement(String id);

	String getDiscussionSelectStatement(String discussionId);

	List<String> getDeleteForumStatements(String forumId);

	List<String> getDeleteDiscussionStatements(String forumId,String discussionId);

	List<String> getDeleteMessageStatements(Message message,String forumId,Connection conn);
	
	List<String> getUndeleteMessageStatements(Message message,String forumId);
	
	public String getMessageAttachmentsSelectStatement(String messageId);

	String getDeleteAttachmentStatement(String attachmentId, String messageId);

	String getSelectForumContainingMessageStatement(String messageId);

	List<String> getMarkMessageReadStatements(String userId, String messageId,String forumId,String discussionId,Connection conn) throws SQLException;

	List<String> getMarkMessageUnReadStatements(String id, String messageId,String forumId,String discussionId);
	
	List<String> getMarkDiscussionReadStatements(String userId, String discussionId,String forumId,Connection conn) throws SQLException;
	
	public String getSelectMessageReadStatement(String userId, String messageId);

	String getSelectReadMessageIds(String id, String discussionId);

	List<String> getMoveDiscussionStatements(String discussionId, String currentForumId,String newForumId,Connection conn);

	List<PreparedStatement> getPublishMessageStatements(String forumId,Message message,Connection connection) throws SQLException;

	String getShowMessageStatement(Message message);

	String getSelectReadMessageCountForAllForaStatement(String userId);

	PreparedStatement getSelectReadMessageCountForDiscussionStatement(String userId,Connection conn) throws SQLException;

	String getSelectDiscussionIdsForForumStatement(String forumId);

	String getMarkForumDeletedStatement(String forumId);

	String getSelectForumIdForTitleStatement(String title,String siteId);

	PreparedStatement getSetDiscussionDatesStatement(Discussion discussion,Connection conn) throws Exception;

	String getSelectActiveDiscussionsStatement();

	List<PreparedStatement> getAddNewMessageToActiveDiscussionsStatements(Message message, Connection connection) throws Exception;

	String getDeleteFromActiveDiscussionStatement(String discussionId);

	String getSelectIdOfSiteContainingMessage(String messageId);

	String getForumGroupsSelectStatement(String forumId);

	PreparedStatement getSelectSiteAuthors(String siteId, Connection conn) throws Exception;

	PreparedStatement getSelectMessagesForAuthorInSite(String authorId,
			String currentSiteId, Connection connection) throws Exception;

	PreparedStatement getSelectDiscussionAuthors(String discussionId, Connection conn) throws Exception;

	PreparedStatement getSelectMessagesForAuthorInDiscussion(String authorId, String discussionId, Connection conn) throws Exception;

	List<PreparedStatement> getSetDiscussionGroupsStatements(Discussion discussion, Connection connection) throws Exception;

	String getDiscussionGroupsSelectStatement(String discussionId);
}
