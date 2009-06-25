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

	PreparedStatement getAddOrUpdateForumStatement(Forum forum,Connection connection) throws SQLException;

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

	List<String> getDeleteMessageStatements(Message message,String forumId);
	
	List<String> getUndeleteMessageStatements(Message message,String forumId);
	
	String getCensorMessageStatement(String messageId);
	String getDiscussionUnsubscribersStatement(String discussionId);

	String getUnsubscribeFromDiscussionStatement(String userId, String discussionId);

	String getDiscussionUnsubscriptionsStatement(String userId);

	String getSubscribeToDiscussionStatement(String userId, String discussionId);
	
	public String getMessageAttachmentsSelectStatement(String messageId);

	String getDeleteAttachmentStatement(String attachmentId, String messageId);

	String getSelectForumContainingMessageStatement(String messageId);

	String getSearchStatement(String siteId, List<String> searchTerms);

	List<String> getMarkMessageReadStatements(String userId, String messageId,String forumId,String discussionId,Connection conn) throws SQLException;

	List<String> getMarkMessageUnReadStatements(String id, String messageId,String forumId,String discussionId);
	
	List<String> getMarkDiscussionReadStatements(String userId, String discussionId,String forumId,Connection conn) throws SQLException;
	
	public String getSelectMessageReadStatement(String userId, String messageId);

	String getSelectReadMessageIds(String id, String discussionId);

	List<String> getMoveDiscussionStatements(String discussionId, String currentForumId,String newForumId);

	List<PreparedStatement> getPublishMessageStatements(String forumId,Message message,Connection connection) throws SQLException;

	String getSelectProfile2Picture(String userId);

	String getShowMessageStatement(Message message);

	String getSelectReadMessageCountForAllForaStatement(String userId);

	PreparedStatement getSelectReadMessageCountForDiscussionStatement(String userId,Connection conn) throws SQLException;

	String getSelectDiscussionIdsForForumStatement(String forumId);

	String getMarkForumDeletedStatement(String forumId);

	String getSelectForumIdForTitleStatement(String title,String siteId);

	List<String> getSubscribeToForumStatements(String userId, Forum forum);

	List<String> getUnsubscribeFromForumStatements(String userId, Forum forum);

	String getForumUnsubscriptionsStatement(String userId);

	PreparedStatement getSetDiscussionDatesStatement(Discussion discussion,Connection conn) throws Exception;
}
