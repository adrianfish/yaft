package org.sakaiproject.yaft.impl.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.Message;

public class DefaultSqlGenerator implements SqlGenerator
{
	private Logger logger = Logger.getLogger(DefaultSqlGenerator.class);
	
	protected String VARCHAR = "VARCHAR";
	protected String TIMESTAMP = "DATETIME";
	protected String TEXT = "TEXT";
	
	public List<String> getSetupStatements()
	{
		if (logger.isDebugEnabled()) logger.debug("getSetupStatements()");
		
		List<String> statements = new ArrayList<String>();
		
		statements.add("CREATE TABLE YAFT_FORUM (" 
				+ "FORUM_ID CHAR(36) NOT NULL,"
				+ "SITE_ID " + VARCHAR + "(99) NOT NULL,"
				+ "CREATOR_ID " + VARCHAR + "(99) NOT NULL,"
				+ "TITLE " + VARCHAR + "(255) NOT NULL,"
				+ "DESCRIPTION " + TEXT + " NOT NULL,"
				+ "DISCUSSION_COUNT INT NOT NULL,"
				+ "MESSAGE_COUNT INT NOT NULL,"
				+ "LAST_MESSAGE_DATE " + TIMESTAMP + ","
				+ "STATUS " + VARCHAR + "(36) NOT NULL,"
				+ "PRIMARY KEY (FORUM_ID))");
		
		statements.add("CREATE TABLE YAFT_FORUM_DISCUSSION (" 
				+ "FORUM_ID CHAR(36) NOT NULL,"
				+ "DISCUSSION_ID CHAR(36) NOT NULL,"
				+ "PRIMARY KEY (FORUM_ID,DISCUSSION_ID))");
		
		statements.add("CREATE TABLE YAFT_DISCUSSION (" 
				+ "DISCUSSION_ID CHAR(36) NOT NULL,"
				+ "LAST_MESSAGE_DATE " + TIMESTAMP + " NOT NULL,"
				+ "MESSAGE_COUNT INT NOT NULL,"
				+ "STATUS " + VARCHAR + "(36) NOT NULL,"
				+ "PRIMARY KEY (DISCUSSION_ID))");
		
		statements.add("CREATE TABLE YAFT_MESSAGE (" 
				+ "MESSAGE_ID CHAR(36) NOT NULL,"
				+ "SITE_ID " + VARCHAR + "(99) NOT NULL,"
				+ "PARENT_MESSAGE_ID CHAR(36),"
				+ "DISCUSSION_ID CHAR(36),"
				+ "SUBJECT " + VARCHAR + "(255) NOT NULL,"
				+ "CONTENT " + TEXT + " NOT NULL,"
				+ "CREATOR_ID " + VARCHAR + "(99) NOT NULL,"
				+ "CREATED_DATE " + TIMESTAMP + " NOT NULL,"
				+ "STATUS " + VARCHAR + "(36),"
				+ "PRIMARY KEY (MESSAGE_ID))");
		
		statements.add("CREATE TABLE YAFT_MESSAGE_CHILDREN (" 
				+ "MESSAGE_ID CHAR(36) NOT NULL,"
				+ "CHILD_MESSAGE_ID CHAR(36) NOT NULL,"
				+ "PRIMARY KEY (MESSAGE_ID,CHILD_MESSAGE_ID))");
		
		statements.add("CREATE TABLE YAFT_DISCUSSION_UNSUBS (" 
				+ "DISCUSSION_ID CHAR(36) NOT NULL,"
				+ "USER_ID " + VARCHAR + "(99) NOT NULL,"
				+ "PRIMARY KEY (DISCUSSION_ID,USER_ID))");
		
		statements.add("CREATE TABLE YAFT_READ_MESSAGES (" 
				+ "USER_ID " + VARCHAR + "(99) NOT NULL,"
				+ "MESSAGE_ID CHAR(36) NOT NULL,"
				+ "PRIMARY KEY (USER_ID,MESSAGE_ID))");
		
		statements.add("CREATE TABLE YAFT_MESSAGE_ATTACHMENTS (" 
				+ "MESSAGE_ID CHAR(36) NOT NULL,"
				+ "RESOURCE_ID " + VARCHAR + "(255) NOT NULL,"
				+ "PRIMARY KEY (MESSAGE_ID,RESOURCE_ID))");
		
		statements.add("CREATE TABLE YAFT_FORUM_READ (" 
				+ "FORUM_ID CHAR(36) NOT NULL,"
				+ "USER_ID " + VARCHAR + "(99) NOT NULL,"
				+ "NUMBER_READ INT NOT NULL,"
				+ "PRIMARY KEY (FORUM_ID,USER_ID))");
		
		statements.add("CREATE TABLE YAFT_DISCUSSION_READ (" 
				+ "DISCUSSION_ID CHAR(36) NOT NULL,"
				+ "USER_ID " + VARCHAR + "(99) NOT NULL,"
				+ "NUMBER_READ INT NOT NULL,"
				+ "PRIMARY KEY (DISCUSSION_ID,USER_ID))");
		
		return statements;
	}
	public String getForaSelectStatement()
	{
		return getForaSelectStatement(null);
	}

	public String getForaSelectStatement(String siteId)
	{
		if(siteId == null)
			return "SELECT * FROM YAFT_FORUM";
		else
			return "SELECT * FROM YAFT_FORUM WHERE SITE_ID = '" + siteId + "'";
	}
	
	public String getForumSelectStatement(String forumId)
	{
		return "SELECT * FROM YAFT_FORUM WHERE FORUM_ID = '" + forumId + "'";
	}

	public PreparedStatement getAddOrUpdateForumStatement(Forum forum,Connection connection) throws SQLException
	{
		String testSql = "SELECT * FROM YAFT_FORUM WHERE FORUM_ID = '" + forum.getId() + "'";
		Statement statement = connection.createStatement();
		if(statement.executeQuery(testSql).next())
		{
			String updateSql = "UPDATE YAFT_FORUM SET " 
								+ ColumnNames.TITLE + " = ?,"
								+ ColumnNames.DESCRIPTION + " = ?";
			
			PreparedStatement ps = connection.prepareStatement(updateSql);
			ps.setString(1,forum.getTitle());
			ps.setString(2,forum.getDescription());
			
			statement.close();
			
			return ps;
		}
		else
		{
			String insertSql = "INSERT INTO YAFT_FORUM (" 
								+ ColumnNames.FORUM_ID + ","
								+ ColumnNames.SITE_ID + ","
								+ ColumnNames.CREATOR_ID + ","
								+ ColumnNames.TITLE + ","
								+ ColumnNames.DESCRIPTION + ","
								+ "STATUS,"
								+ ColumnNames.MESSAGE_COUNT + ","
								+ ColumnNames.DISCUSSION_COUNT + ") VALUES(?,?,?,?,?,?,0,0)";
			
			PreparedStatement ps = connection.prepareStatement(insertSql);
			ps.setString(1,forum.getId());
			ps.setString(2,forum.getSiteId());
			ps.setString(3,forum.getCreatorId());
			ps.setString(4,forum.getTitle());
			ps.setString(5,forum.getDescription());
			ps.setString(6,forum.getStatus());
			
			statement.close();
			
			return ps;
		}
	}
	
	public String getMessageSelectStatement(String messageId)
	{
		return "SELECT * FROM YAFT_MESSAGE WHERE MESSAGE_ID  = '" + messageId + "'";
	}
	
	public String getMessageAttachmentsSelectStatement(String messageId)
	{
		return "SELECT * FROM YAFT_MESSAGE_ATTACHMENTS WHERE MESSAGE_ID  = '" + messageId + "'";
	}
	
	/*
	public String getDiscussionsSelectStatement()
	{
		return getDiscussionsSelectStatement(null);
	}
	*/

	public String getDiscussionsSelectStatement(String forumId)
	{
		String sql = "SELECT YAFT_DISCUSSION.*,FORUM_ID"
					+ " FROM YAFT_FORUM_DISCUSSION,YAFT_DISCUSSION";
		
		if(forumId != null)
		{
			sql += " WHERE YAFT_FORUM_DISCUSSION.FORUM_ID = '" + forumId + "'"
					+ " AND YAFT_FORUM_DISCUSSION.DISCUSSION_ID = YAFT_DISCUSSION.DISCUSSION_ID";
		}
		else
			sql += " WHERE YAFT_FORUM_DISCUSSION.DISCUSSION_ID = YAFT_DISCUSSION.DISCUSSION_ID";
		
		return sql;
	}

	public List<PreparedStatement> getAddOrUpdateMessageStatements(String forumId,Message message, Connection connection) throws Exception
	{
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		
		String testSql = "SELECT * FROM YAFT_MESSAGE WHERE MESSAGE_ID = '" + message.getId() + "'";
		Statement testStatement = connection.createStatement();
		ResultSet testRS = testStatement.executeQuery(testSql);
		if(testRS.next())
		{
			// This is an existing message
			
			testRS.close();
			testStatement.close();
				
			// This is an existing message
			String updateSql = "UPDATE YAFT_MESSAGE SET " 
								+ "SUBJECT = ?,CONTENT = ? WHERE MESSAGE_ID = ?";
			
			PreparedStatement ps = connection.prepareStatement(updateSql);
			ps.setString(1,message.getSubject());
			ps.setString(2,message.getContent());
			ps.setString(3,message.getId());
			
			statements.add(ps);
		}
		else
		{
			// This is a new message
			
			testRS.close();
			testStatement.close();
			
			String insertSql = "INSERT INTO YAFT_MESSAGE (" 
								+ "MESSAGE_ID,";
			
			if(message.hasParent())
					insertSql += "PARENT_MESSAGE_ID,";
			
			insertSql += "DISCUSSION_ID,"
						+ "SITE_ID,"
						+ "STATUS,"
						+ "SUBJECT,"
						+ "CONTENT,"
						+ "CREATOR_ID,"
						+ "CREATED_DATE) VALUES(?,";
			
			if(message.hasParent())
				insertSql += "?,";
			
			insertSql += "?,?,?,?,?,?,?)";
			
			PreparedStatement insertMessagePS = connection.prepareStatement(insertSql);
			insertMessagePS.setString(1,message.getId());
			if(message.hasParent())
			{
				// This is a reply
				
				insertMessagePS.setString(2,message.getParent());
				insertMessagePS.setString(3,message.getDiscussionId());
				insertMessagePS.setString(4,message.getSiteId());
				insertMessagePS.setString(5,message.getStatus());
				insertMessagePS.setString(6,message.getSubject());
				insertMessagePS.setString(7,message.getContent());
				insertMessagePS.setString(8,message.getCreatorId());
				insertMessagePS.setTimestamp(9,new Timestamp(message.getCreatedDate()));
				
				String childrenSql = "INSERT INTO YAFT_MESSAGE_CHILDREN VALUES(?,?)";
				
				PreparedStatement childrenPS = connection.prepareStatement(childrenSql);
				childrenPS.setString(1,message.getParent());
				childrenPS.setString(2,message.getId());
				statements.add(childrenPS);
				
				if(message.getStatus().equals("READY"))
				{
					statements.add(getIncrementDiscussionMessageCountStatement(message,connection));
					statements.add(getIncrementForumMessageCountStatement(forumId,message,connection));
				}
			}
			else
			{
				// This is a discussion
				
				// The fact that this message doesn't have a parent means that
				// it is a discussion, or 'top level message'
				insertMessagePS.setString(2,message.getId()); // TODO: Tidy this up. Confusion between getId and getDiscussionId
				insertMessagePS.setString(3,message.getSiteId());
				insertMessagePS.setString(4,"READY");
				insertMessagePS.setString(5,message.getSubject());
				insertMessagePS.setString(6,message.getContent());
				insertMessagePS.setString(7,message.getCreatorId());
				insertMessagePS.setTimestamp(8,new Timestamp(message.getCreatedDate()));
				
				
				insertSql = "INSERT INTO YAFT_DISCUSSION (DISCUSSION_ID,LAST_MESSAGE_DATE,MESSAGE_COUNT,STATUS) VALUES(?,?,1,'READY')";
				PreparedStatement discussionPS = connection.prepareStatement(insertSql);
				discussionPS.setString(1, message.getId());
				discussionPS.setTimestamp(2, new Timestamp(message.getCreatedDate()));
				statements.add(discussionPS);
				
				insertSql = "INSERT INTO YAFT_FORUM_DISCUSSION VALUES(?,?)";
				PreparedStatement forumDiscussionPS = connection.prepareStatement(insertSql);
				forumDiscussionPS.setString(1,forumId);
				forumDiscussionPS.setString(2, message.getId());
				statements.add(forumDiscussionPS);
				
				insertSql = "UPDATE YAFT_FORUM SET DISCUSSION_COUNT = DISCUSSION_COUNT + 1 WHERE FORUM_ID = ?";
				PreparedStatement forumDiscussionCountPS = connection.prepareStatement(insertSql);
				forumDiscussionCountPS.setString(1,forumId);
				statements.add(forumDiscussionCountPS);
				
				statements.add(getIncrementForumMessageCountStatement(forumId,message,connection));
			}
			
			statements.add(insertMessagePS);
		}
		
		for(Attachment attachment : message.getAttachments())
		{
			String insertSql = "INSERT INTO YAFT_MESSAGE_ATTACHMENTS VALUES(?,?)";
			PreparedStatement attachmentPS = connection.prepareStatement(insertSql);
			attachmentPS.setString(1,message.getId());
			attachmentPS.setString(2,attachment.getResourceId());
			statements.add(attachmentPS);
		}
		
		return statements;
	}
	private PreparedStatement getIncrementDiscussionMessageCountStatement(Message message, Connection connection) throws SQLException
	{
		String insertSql = "UPDATE YAFT_DISCUSSION SET MESSAGE_COUNT = MESSAGE_COUNT + 1,LAST_MESSAGE_DATE = ? WHERE DISCUSSION_ID = ?";
		PreparedStatement discussionMessageCountPS = connection.prepareStatement(insertSql);
		discussionMessageCountPS.setTimestamp(1, new Timestamp(message.getCreatedDate()));
		discussionMessageCountPS.setString(2,message.getDiscussionId());
		return discussionMessageCountPS;
	}
	private PreparedStatement getIncrementForumMessageCountStatement(String forumId, Message message,Connection connection) throws SQLException
	{
		String insertSql = "UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT + 1,LAST_MESSAGE_DATE = ? WHERE FORUM_ID = ?";
		PreparedStatement forumMessageCountPS = connection.prepareStatement(insertSql);
		forumMessageCountPS.setTimestamp(1, new Timestamp(message.getCreatedDate()));
		forumMessageCountPS.setString(2,forumId);
		return forumMessageCountPS;
	}
	public String getMessageChildrenSelectStatement(String id)
	{
		return "SELECT * FROM YAFT_MESSAGE WHERE MESSAGE_ID IN " 
				+ "(SELECT CHILD_MESSAGE_ID FROM YAFT_MESSAGE_CHILDREN WHERE MESSAGE_ID = '" + id + "')"
				+ " ORDER BY CREATED_DATE";
	}
	
	public String getDiscussionSelectStatement(String discussionId)
	{
		return "SELECT YAFT_DISCUSSION.*,FORUM_ID"
					+ " FROM YAFT_DISCUSSION,YAFT_FORUM_DISCUSSION"
					+ " WHERE YAFT_DISCUSSION.DISCUSSION_ID = '" + discussionId
					+ "' AND YAFT_DISCUSSION.DISCUSSION_ID = YAFT_FORUM_DISCUSSION.DISCUSSION_ID";
	}
	public List<String> getDeleteForumStatements(String forumId)
	{
		List<String> statements = new ArrayList<String>();
		
		statements.add("DELETE FROM YAFT_MESSAGE WHERE DISCUSSION_ID IN (SELECT " + ColumnNames.DISCUSSION_ID + " FROM YAFT_FORUM_DISCUSSION WHERE FORUM_ID = '" + forumId + "')");
		statements.add("DELETE FROM YAFT_DISCUSSION WHERE DISCUSSION_ID IN (SELECT DISCUSSION_ID FROM YAFT_FORUM_DISCUSSION WHERE FORUM_ID = '" + forumId + "')");
		statements.add("DELETE FROM YAFT_FORUM_DISCUSSION WHERE FORUM_ID = '" + forumId + "'");
		statements.add("DELETE FROM YAFT_FORUM WHERE FORUM_ID = '" + forumId + "'");
		
		return statements;
	}
	public List<String> getDeleteDiscussionStatements(String forumId,String discussionId)
	{
		List<String> statements = new ArrayList<String>();
		
		//statements.add("DELETE FROM YAFT_MESSAGE WHERE DISCUSSION_ID = '" + discussionId + "'");
		//statements.add("DELETE FROM YAFT_FORUM_DISCUSSION  WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_FORUM SET DISCUSSION_COUNT = DISCUSSION_COUNT - 1 WHERE FORUM_ID = '" + forumId + "' AND DISCUSSION_COUNT > 0");
		statements.add("UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT - (SELECT MESSAGE_COUNT FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "') WHERE FORUM_ID = '" + forumId + "'");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = (SELECT MAX(yd.LAST_MESSAGE_DATE) FROM YAFT_FORUM_DISCUSSION AS yfd,YAFT_DISCUSSION AS yd WHERE yfd.DISCUSSION_ID = yd.DISCUSSION_ID AND yfd.FORUM_ID = '" + forumId + "') WHERE FORUM_ID = '" + forumId + "'");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = NULL WHERE FORUM_ID = '" + forumId + "' AND MESSAGE_COUNT  = 0");
		//statements.add("DELETE FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_DISCUSSION SET STATUS = 'DELETED' WHERE DISCUSSION_ID = '" + discussionId + "'");
		
		return statements;
	}
	
	public String getCensorMessageStatement(String messageId)
	{
		return "UPDATE YAFT_MESSAGE SET " + ColumnNames.STATUS + " = 'CENSORED' WHERE " + ColumnNames.MESSAGE_ID + " = '" + messageId + "'";
	}
	
	public List<String> getDeleteMessageStatements(Message message,String forumId)
	{
		String messageId = message.getId();
		String discussionId = message.getDiscussionId();
		List<String> statements = new ArrayList<String>();
		statements.add("UPDATE YAFT_MESSAGE SET STATUS = 'DELETED' WHERE MESSAGE_ID  = '" + messageId + "'");
		statements.add("UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT - 1 WHERE FORUM_ID = '" + forumId + "'");
		statements.add("UPDATE YAFT_DISCUSSION SET MESSAGE_COUNT = MESSAGE_COUNT - 1 WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_DISCUSSION SET LAST_MESSAGE_DATE = (SELECT MAX(CREATED_DATE) FROM YAFT_MESSAGE WHERE STATUS <> 'DELETED' AND DISCUSSION_ID = '" + discussionId + "') WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = (SELECT MAX(yd.LAST_MESSAGE_DATE) FROM YAFT_FORUM_DISCUSSION AS yfd,YAFT_DISCUSSION AS yd WHERE yfd.DISCUSSION_ID = yd.DISCUSSION_ID AND yfd.FORUM_ID = '" + forumId + "') WHERE FORUM_ID = '" + forumId + "'");
		return statements;
	}
	
	public String getShowMessageStatement(Message message)
	{
		return "UPDATE YAFT_MESSAGE SET STATUS = 'READY' WHERE MESSAGE_ID  = '" + message.getId() + "'";
	}
	
	public List<String> getUndeleteMessageStatements(Message message,String forumId)
	{
		String messageId = message.getId();
		String discussionId = message.getDiscussionId();
		List<String> statements = new ArrayList<String>();
		statements.add("UPDATE YAFT_MESSAGE SET STATUS = 'READY' WHERE MESSAGE_ID  = '" + messageId + "'");
		statements.add("UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT + 1 WHERE FORUM_ID = '" + forumId + "'");
		statements.add("UPDATE YAFT_DISCUSSION SET MESSAGE_COUNT = MESSAGE_COUNT + 1 WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_DISCUSSION SET LAST_MESSAGE_DATE = (SELECT MAX(CREATED_DATE) FROM YAFT_MESSAGE WHERE STATUS <> 'DELETED' AND DISCUSSION_ID = '" + discussionId + "')");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = (SELECT MAX(yd.LAST_MESSAGE_DATE) FROM YAFT_FORUM_DISCUSSION AS yfd,YAFT_DISCUSSION AS yd WHERE yfd.DISCUSSION_ID = yd.DISCUSSION_ID AND yfd.FORUM_ID = '" + forumId + "') WHERE FORUM_ID = '" + forumId + "'");
		return statements;
	}
	
	public String getDiscussionUnsubscribersStatement(String discussionId)
	{
		return "SELECT USER_ID FROM YAFT_DISCUSSION_UNSUBS WHERE DISCUSSION_ID = '" + discussionId + "'";
	}
	
	public String getUnsubscribeFromDiscussionStatement(String userId, String discussionId)
	{
		return "INSERT INTO YAFT_DISCUSSION_UNSUBS (DISCUSSION_ID,USER_ID) VALUES('" + discussionId + "','" + userId + "')";
	}
	
	public String getDiscussionUnsubscriptionsStatement(String userId)
	{
		return "SELECT DISCUSSION_ID FROM YAFT_DISCUSSION_UNSUBS"
				+ " WHERE USER_ID = '" + userId + "'";
	}
	public String getSubscribeToDiscussionStatement(String userId, String discussionId)
	{
		return "DELETE FROM YAFT_DISCUSSION_UNSUBS WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'";
	}
	public String getDeleteAttachmentStatement(String resourceId, String messageId)
	{
		return "DELETE FROM YAFT_MESSAGE_ATTACHMENTS WHERE MESSAGE_ID = '" + messageId + "' AND RESOURCE_ID = '" + resourceId + "'";
	}
	
	public String getSelectForumContainingMessageStatement(String messageId)
	{
		return "SELECT FORUM_ID FROM YAFT_DISCUSSION,YAFT_FORUM_DISCUSSION"
				+ " WHERE YAFT_DISCUSSION.DISCUSSION_ID = YAFT_FORUM_DISCUSSION.DISCUSSION_ID"
				+ " AND YAFT_DISCUSSION.DISCUSSION_ID"
				+ " IN (SELECT DISTINCT DISCUSSION_ID FROM YAFT_MESSAGE"
					+ " WHERE MESSAGE_ID = '" + messageId + "')";
	}
	
	public String getSearchStatement(String siteId,List<String> searchTerms)
	{
		String sql = "SELECT * FROM YAFT_MESSAGE WHERE SITE_ID = '" + siteId + "' AND";
		
		for(int i = 0;i < searchTerms.size();i++)
		{
			sql += " CONTENT LIKE '%" + searchTerms.get(i) + "%'";
			if(i < (searchTerms.size() - 1))
				sql += " AND";
		}
		
		return sql;
	}
	
	public String getSelectMessageReadStatement(String userId, String messageId)
	{
		return "SELECT * FROM YAFT_READ_MESSAGES WHERE USER_ID = '" + userId + "' AND MESSAGE_ID = '" + messageId + "'";
	}
	
	public List<String> getMarkMessageReadStatements(String userId, String messageId,String forumId,String discussionId,Connection conn) throws SQLException
	{
		List<String> statements = new ArrayList<String>();
		statements.add("INSERT INTO YAFT_READ_MESSAGES (USER_ID,MESSAGE_ID) VALUES('" + userId + "','" + messageId + "')");
		
		Statement st = null;
		ResultSet rs = null;
		
		try
		{
			st = conn.createStatement();
			rs = st.executeQuery("SELECT * FROM YAFT_FORUM_READ WHERE FORUM_ID = '" + forumId + "' AND USER_ID = '" + userId + "'");
			if(rs.next())
				statements.add("UPDATE YAFT_FORUM_READ SET NUMBER_READ = NUMBER_READ + 1 WHERE FORUM_ID = '" + forumId + "' AND USER_ID = '" + userId + "'");
			else
				statements.add("INSERT INTO YAFT_FORUM_READ (FORUM_ID,USER_ID,NUMBER_READ) VALUES('" + forumId + "','" + userId + "',1)");
			
			rs.close();
			
			rs = st.executeQuery("SELECT * FROM YAFT_DISCUSSION_READ WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'");
			if(rs.next())
				statements.add("UPDATE YAFT_DISCUSSION_READ SET NUMBER_READ = NUMBER_READ + 1 WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'");
			else
				statements.add("INSERT INTO YAFT_DISCUSSION_READ (DISCUSSION_ID,USER_ID,NUMBER_READ) VALUES('" + discussionId + "','" + userId + "',1)");
		}
		finally
		{
			if(rs != null) rs.close();
			if(st != null) st.close();
		}
		
		return statements;
	}
	
	public List<String> getMarkMessageUnReadStatements(String userId, String messageId,String forumId, String discussionId)
	{
		List<String> statements = new ArrayList<String>();
		statements.add("DELETE FROM YAFT_READ_MESSAGES WHERE USER_ID = '" + userId + "' AND MESSAGE_ID = '" + messageId + "'");
		statements.add("UPDATE YAFT_FORUM_READ SET NUMBER_READ = NUMBER_READ - 1 WHERE FORUM_ID = '" + forumId + "' AND USER_ID = '" + userId + "'");
		statements.add("UPDATE YAFT_DISCUSSION_READ SET NUMBER_READ = NUMBER_READ - 1 WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'");
		return statements;
	}
	
	public List<String> getMarkDiscussionReadStatements(String userId, String discussionId,String forumId,Connection conn) throws SQLException
	{
		List<String> statements = new ArrayList<String>();
		
		Statement st = null;
		ResultSet rs = null;
		
		try
		{
			st = conn.createStatement();
			// Get a count of the current read messages in this discussion
			rs = st.executeQuery("SELECT COUNT(*) AS CURRENT_READ"
								+ " FROM YAFT_READ_MESSAGES,YAFT_MESSAGE"
								+ " WHERE YAFT_READ_MESSAGES.MESSAGE_ID = YAFT_MESSAGE.MESSAGE_ID"
								+ " AND DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'");
			rs.next();
			int currentRead = rs.getInt("CURRENT_READ");
			rs.close();
			
			// Now delete the current read messages for this discussion
			statements.add("DELETE FROM YAFT_READ_MESSAGES WHERE MESSAGE_ID IN (SELECT MESSAGE_ID FROM YAFT_MESSAGE WHERE DISCUSSION_ID = '" + discussionId + "') AND USER_ID = '" + userId + "'");
			
			rs = st.executeQuery("SELECT MESSAGE_ID FROM YAFT_MESSAGE WHERE DISCUSSION_ID = '" + discussionId + "'");
			int count = 0;
			while(rs.next())
			{
				statements.add("INSERT INTO YAFT_READ_MESSAGES (MESSAGE_ID,USER_ID) VALUES('" + rs.getString("MESSAGE_ID") + "','" + userId + "')");
				count++;
			}
			rs.close();
			
			int diff = count - currentRead;
			
			rs = st.executeQuery("SELECT * FROM YAFT_FORUM_READ WHERE FORUM_ID = '" + forumId + "' AND USER_ID = '" + userId + "'");
			if(rs.next())
				statements.add("UPDATE YAFT_FORUM_READ SET NUMBER_READ = NUMBER_READ + " + diff + " WHERE FORUM_ID = '" + forumId + "' AND USER_ID = '" + userId +"'");
			else
				statements.add("INSERT INTO YAFT_FORUM_READ (FORUM_ID,USER_ID,NUMBER_READ) VALUES('" + forumId + "','" + userId + "'," + diff +")");
			rs.close();
			
			rs = st.executeQuery("SELECT * FROM YAFT_DISCUSSION_READ WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId + "'");
			if(rs.next())
				statements.add("UPDATE YAFT_DISCUSSION_READ SET NUMBER_READ = NUMBER_READ + " + diff + " WHERE DISCUSSION_ID = '" + discussionId + "' AND USER_ID = '" + userId +"'");
			else
				statements.add("INSERT INTO YAFT_DISCUSSION_READ (DISCUSSION_ID,USER_ID,NUMBER_READ) VALUES('" + discussionId + "','" + userId + "'," + diff +")");
		}
		finally
		{
			if(st != null) st.close();
		}
		
		return statements;
	}
	
	public String getSelectReadMessageIds(String userId, String discussionId)
	{
		return "SELECT MESSAGE_ID FROM YAFT_MESSAGE WHERE DISCUSSION_ID = '" + discussionId + "' AND MESSAGE_ID IN (SELECT MESSAGE_ID FROM YAFT_READ_MESSAGES WHERE USER_ID = '" + userId + "')";
	}
	
	public List<String> getMoveDiscussionStatements(String discussionId, String currentForumId,String newForumId)
	{
		List<String> statements = new ArrayList<String>();
		statements.add("UPDATE YAFT_FORUM_DISCUSSION SET FORUM_ID = '" + newForumId + "' WHERE DISCUSSION_ID = '" + discussionId + "'");
		statements.add("UPDATE YAFT_FORUM SET DISCUSSION_COUNT = DISCUSSION_COUNT - 1 WHERE FORUM_ID = '" + currentForumId + "'");
		statements.add("UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT - (SELECT MESSAGE_COUNT FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "') WHERE FORUM_ID = '" + currentForumId + "'");
		statements.add("UPDATE YAFT_FORUM SET DISCUSSION_COUNT = DISCUSSION_COUNT + 1 WHERE FORUM_ID = '" + newForumId + "'");
		statements.add("UPDATE YAFT_FORUM SET MESSAGE_COUNT = MESSAGE_COUNT + (SELECT MESSAGE_COUNT FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "') WHERE FORUM_ID = '" + newForumId + "'");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = NULL WHERE DISCUSSION_COUNT = 0 AND FORUM_ID = '" + currentForumId + "'");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = (SELECT LAST_MESSAGE_DATE FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "') WHERE FORUM_ID = '" + newForumId + "' AND (SELECT LAST_MESSAGE_DATE FROM YAFT_DISCUSSION WHERE DISCUSSION_ID = '" + discussionId + "') > LAST_MESSAGE_DATE");
		statements.add("UPDATE YAFT_FORUM SET LAST_MESSAGE_DATE = (SELECT MAX(yd.LAST_MESSAGE_DATE) FROM YAFT_FORUM_DISCUSSION AS yfd,YAFT_DISCUSSION AS yd WHERE yfd.DISCUSSION_ID = yd.DISCUSSION_ID AND yfd.FORUM_ID = '" + currentForumId + "') WHERE FORUM_ID = '" + currentForumId + "'");
		return statements;
	}
	
	public List<PreparedStatement> getPublishMessageStatements(String forumId,Message message,Connection connection) throws SQLException
	{
		long currentDate = new Date().getTime();
		
		List<PreparedStatement> statements = new ArrayList<PreparedStatement>();
		
		String sql = "UPDATE YAFT_MESSAGE SET STATUS = 'READY',CREATED_DATE = ? WHERE MESSAGE_ID = ? AND STATUS = 'DRAFT'";
		PreparedStatement updateMessageStatement = connection.prepareStatement(sql);
		updateMessageStatement.setTimestamp(1,new Timestamp(currentDate));
		updateMessageStatement.setString(2,message.getId());
		statements.add(updateMessageStatement);
		
		message.setCreatedDate(currentDate);
		
		statements.add(getIncrementForumMessageCountStatement(forumId,message,connection));
		statements.add(getIncrementDiscussionMessageCountStatement(message,connection));
		
		return statements;
	}
	
	public String getSelectProfile2Picture(String userId)
	{
		return "select RESOURCE_THUMB from PROFILE_IMAGES_T where USER_UUID = '" + userId + "' and IS_CURRENT = true order by ID DESC";
	}
	
	public String getSelectReadMessageCountForAllForaStatement(String userId)
	{
		return "SELECT FORUM_ID,NUMBER_READ FROM YAFT_FORUM_READ WHERE USER_ID = '" + userId + "'";
	}
	
	public PreparedStatement getSelectReadMessageCountForDiscussionStatement(String userId,Connection conn) throws SQLException
	{
		String sql = "SELECT NUMBER_READ FROM YAFT_DISCUSSION_READ WHERE USER_ID = '" + userId + "' AND DISCUSSION_ID = ?";
		return conn.prepareStatement(sql);
	}
	
	public String getSelectDiscussionIdsForForumStatement(String forumId)
	{
		return "SELECT DISCUSSION_ID FROM YAFT_FORUM_DISCUSSION WHERE FORUM_ID = '" + forumId + "'";
	}
	
	public String getMarkForumDeletedStatement(String forumId)
	{
		return "UPDATE YAFT_FORUM SET STATUS = 'DELETED' WHERE FORUM_ID = '" + forumId + "'";
	}
	
	public String getSelectForumIdForTitleStatement(String title)
	{
		return "SELECT FORUM_ID FROM YAFT_FORUM WHERE TITLE = '" + title + "' AND STATUS <> 'DELETED'";
	}
}
