package org.sakaiproject.yaft.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.yaft.api.Attachment;
import org.sakaiproject.yaft.api.Discussion;
import org.sakaiproject.yaft.api.Forum;
import org.sakaiproject.yaft.api.ForumPopulatedStates;
import org.sakaiproject.yaft.api.Message;
import org.sakaiproject.yaft.api.SakaiProxy;
import org.sakaiproject.yaft.api.SearchResult;
import org.sakaiproject.yaft.impl.sql.ColumnNames;
import org.sakaiproject.yaft.impl.sql.DefaultSqlGenerator;
import org.sakaiproject.yaft.impl.sql.HypersonicGenerator;
import org.sakaiproject.yaft.impl.sql.MySQLGenerator;
import org.sakaiproject.yaft.impl.sql.OracleGenerator;
import org.sakaiproject.yaft.impl.sql.SqlGenerator;

public class YaftPersistenceManager
{
	private Logger logger = Logger.getLogger(YaftPersistenceManager.class);
	
	private SakaiProxy sakaiProxy = null;
	private SqlGenerator sqlGenerator = null;
	
	public YaftPersistenceManager()
	{
		if(logger.isDebugEnabled()) logger.debug("YaftPersistenceManager()");
	}
	
	public void init()
	{
		if(logger.isDebugEnabled()) logger.debug("init()");
		
		String dbVendor = sakaiProxy.getDbVendor();
		if(dbVendor.equals("mysql"))
			sqlGenerator = new MySQLGenerator();
		else if(dbVendor.equals("oracle"))
			sqlGenerator = new OracleGenerator();
		else if(dbVendor.equals("hsqldb"))
			sqlGenerator = new HypersonicGenerator();
		else
		{
			logger.warn("'" + dbVendor + "' not directly supported. Defaulting to DefaultSqlGenerator.");
			sqlGenerator = new DefaultSqlGenerator();
		}
	}
	
	/**
	 * If auto.ddl is set the yaft forum tables are created
	 */
	public void setupTables()
	{
		if(logger.isDebugEnabled()) logger.debug("setupTables()");
		
		if(!sakaiProxy.isAutoDDL())
		{
			if(logger.isDebugEnabled())
				logger.debug("auto.ddl is set to false. Returning ...");
			
			return;
		}

		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getSetupStatements();

				Statement statement = connection.createStatement();

				for (String sql : statements)
					statement.executeUpdate(sql);

				connection.commit();
			}
			catch (SQLException sqle)
			{
				logger.error("Caught exception whilst setting up tables. Rolling back ...", sqle);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst setting up tables", e);
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
	}

	/**
	 * Get the Forum with the specified id
	 * 
	 * @param forumId The forum that we want
	 * @return A Forum
	 */
	public Forum getForum(String forumId,String state)
	{
		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			return getForum(forumId,state,connection);
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
			return null;
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
	}
	
	private Forum getForum(String forumId,String state, Connection connection) throws Exception
	{
		if(logger.isDebugEnabled()) logger.debug("getForum()");
		
		Forum forum = null;
		
		String sql = sqlGenerator.getForumSelectStatement(forumId);
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(sql);
		if(rs.next())
		{
			forum = getForumFromResults(rs, connection);
		}
			
		rs.close();
		st.close();
			
		if(ForumPopulatedStates.FULL.equals(state))
			forum.setDiscussions(getForumDiscussions(forumId,true,connection));
		else if(ForumPopulatedStates.PART.equals(state))
			forum.setDiscussions(getForumDiscussions(forumId,false,connection));
		
		return forum;
	}

	/**
	 * Get the Forums for the specified site
	 * 
	 * @param siteId The site that we want the Forums for
	 * @return A list of Forum objects
	 */
	public List<Forum> getFora(String siteId,boolean fully)
	{
		if(logger.isDebugEnabled()) logger.debug("getFora()");
		
		List<Forum> fora = new ArrayList<Forum>();
		
		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			String sql = sqlGenerator.getForaSelectStatement(siteId);
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(sql);
			while(rs.next())
			{
				Forum forum = getForumFromResults(rs, connection);
				
				// We do not want to return deleted fora
				if("DELETED".equals(forum.getStatus())) continue;
				
				if(fully)
				{
					// Add the discussions
					forum.setDiscussions(getForumDiscussions(forum.getId(), true));
				}
				fora.add(forum);
			}
			
			rs.close();
			st.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting fora.", e);
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
		
		return fora;
	}

	public void setSakaiProxy(SakaiProxy sakaiProxy)
	{
		if(logger.isDebugEnabled()) logger.debug("setSakaiProxy()");
		
		this.sakaiProxy = sakaiProxy;
	}

	public void addOrUpdateForum(Forum forum)
	{
		if(logger.isDebugEnabled()) logger.debug("addOrUpdateForum()");
		
		Connection connection = null;
		PreparedStatement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();

			statement = sqlGenerator.getAddOrUpdateForumStatement(forum,connection);
			statement.executeUpdate();
			connection.commit();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst adding or updating forum", e);
		}
		finally
		{
			if(statement != null)
			{
				try
				{
					statement.close();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public void addOrUpdateMessage(String forumId,Message message)
	{
		if(logger.isDebugEnabled()) logger.debug("addOrUpdateMessage()");
		
		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				for(Attachment attachment : message.getAttachments())
				{
					if(attachment.getResourceId().length() == 0)
					{
						String resourceId = sakaiProxy.saveFile(message.getCreatorId(),attachment.getName(),attachment.getMimeType(),attachment.getData());
						attachment.setResourceId(resourceId);
					}
				}
				
				List<PreparedStatement> statements = sqlGenerator.getAddOrUpdateMessageStatements(forumId,message,connection);
				
				for(PreparedStatement statement : statements)
					statement.executeUpdate();
				
				connection.commit();
				
				if(!"DRAFT".equals(message.getStatus()))
					markMessageRead(message.getId(), forumId, message.getDiscussionId());
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst adding or updating message. Rolling back ...", e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst adding or updating message", e);
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
		
	}

	public List<Forum> getFora()
	{
		if(logger.isDebugEnabled()) logger.debug("getForums()");
		
		List<Forum> fora = new ArrayList<Forum>();
		
		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			String sql = sqlGenerator.getForaSelectStatement();
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(sql);
			
			while(rs.next())
			{
				Forum forum = getForumFromResults(rs, connection);
				
				// Only add this forum to the list if is has not been deleted
				if(!"DELETED".equals(forum.getStatus())) fora.add(forum);
			}
			
			rs.close();
			st.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
		
		return fora;
	}

	private void getMessageChildren(Message message,Connection connection) throws Exception
	{
		Statement statement = connection.createStatement();
		String sql = sqlGenerator.getMessageChildrenSelectStatement(message.getId());
	
		ResultSet rs = statement.executeQuery(sql);
		while(rs.next())
		{
			Message child = getMessageFromResults(rs,connection);
			getMessageChildren(child,connection);
			message.addChild(child);
		}
		
		rs.close();
		statement.close();
	}
	
	public List<Discussion> getForumDiscussions(String forumId,boolean fully)
	{
		Connection connection = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			return getForumDiscussions(forumId,fully, connection);
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
			return null;
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
	}

	private List<Discussion> getForumDiscussions(String forumId,boolean fully,Connection connection) throws Exception
	{
		List<Discussion> discussions = new ArrayList<Discussion>();

		String sql = sqlGenerator.getDiscussionsSelectStatement(forumId);
		Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(sql);
			
		while(rs.next())
		{
			Discussion discussion = getDiscussionFromResults(rs,fully,connection);
			
			if(!"DELETED".equals(discussion.getStatus()))
				discussions.add(discussion);
		}
		
		rs.close();
		st.close();
		
		return discussions;
	}
	
	private Discussion getDiscussionFromResults(ResultSet rs,boolean fully,Connection connection) throws Exception
	{
		String discussionId = rs.getString(ColumnNames.DISCUSSION_ID);
		
		// Get the first message. It has the same id as the discussion.
		Statement st = connection.createStatement();
		String sql = sqlGenerator.getMessageSelectStatement(discussionId);
		ResultSet messageRS = st.executeQuery(sql);
		
		if(!messageRS.next())
		{
			messageRS.close();
			st.close();
			throw new Exception("The database is inconsistent. There is no first message for discussion '" + discussionId + "'");
		}
		
		Message firstMessage = getMessageFromResults(messageRS,connection);
		
		messageRS.close();
		st.close();
		
		if(fully)
			getMessageChildren(firstMessage, connection);
			
		Discussion discussion = new Discussion();
		discussion.setFirstMessage(firstMessage);
		discussion.setForumId(rs.getString(ColumnNames.FORUM_ID));
		discussion.setStatus(rs.getString("STATUS"));
		discussion.setMessageCount(rs.getInt(ColumnNames.MESSAGE_COUNT));
		discussion.setLastMessageDate(rs.getTimestamp(ColumnNames.LAST_MESSAGE_DATE).getTime());
		return discussion;
	}
	
	public Discussion getDiscussion(String discussionId,boolean fully)
	{
		Connection connection = null;
		Statement st = null;
		
		Discussion discussion = new Discussion();

		try
		{
			connection = sakaiProxy.borrowConnection();
			st = connection.createStatement();
			String sql = sqlGenerator.getDiscussionSelectStatement(discussionId);
			ResultSet rs = st.executeQuery(sql);
			
			if(rs.next())
				discussion = getDiscussionFromResults(rs,fully, connection);
			else
				logger.error("No discussion with id: " + discussionId);
			
			rs.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting comment.", e);
		}
		finally
		{
			try
			{
				st.close();
			}
			catch(Exception e)
			{
				// We tried ...
			}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return discussion;
	}
	
	private Message getMessageFromResults(ResultSet rs, Connection connection) throws Exception
	{
		Message message = new Message();
		message.setId(rs.getString(ColumnNames.MESSAGE_ID));
		message.setSubject(rs.getString(ColumnNames.SUBJECT));
		message.setSiteId(rs.getString(ColumnNames.SITE_ID));
		message.setDiscussionId(rs.getString(ColumnNames.DISCUSSION_ID));
		message.setContent(rs.getString(ColumnNames.CONTENT));
		Timestamp createdTimestamp = rs.getTimestamp(ColumnNames.CREATED_DATE);
		message.setCreatedDate(createdTimestamp.getTime());
		String creatorId = rs.getString(ColumnNames.CREATOR_ID);
		message.setCreatorId(creatorId);
		message.setStatus(rs.getString(ColumnNames.STATUS));
		message.setCreatorDisplayName(sakaiProxy.getDisplayNameForUser(creatorId));
		message.setCreatorBio(sakaiProxy.getUserBio(creatorId));
		
		String sql = sqlGenerator.getMessageAttachmentsSelectStatement(message.getId());
		
		ResultSet rs2 = null;
		Statement st = null;
		
		List<Attachment> attachments = new ArrayList<Attachment>();
		
		try
		{
			st = connection.createStatement();
			rs2 = st.executeQuery(sql);
		
			while(rs2.next())
			{
				String resourceId = rs2.getString(ColumnNames.RESOURCE_ID);
				
				Attachment attachment = new Attachment();
				attachment.setResourceId(resourceId);
				
				sakaiProxy.getAttachment(attachment);
				
				attachments.add(attachment);
			}
		}
		finally
		{
			rs2.close();
			st.close();
		}
		
		message.setAttachments(attachments);
		
		return message;
	}
	
	private Forum getForumFromResults(ResultSet rs,Connection connection) throws Exception
	{
		Forum forum = new Forum();
		forum.setId(rs.getString(ColumnNames.FORUM_ID));
		forum.setSiteId(rs.getString(ColumnNames.SITE_ID));
		forum.setTitle(rs.getString(ColumnNames.TITLE));
		forum.setCreatorId(rs.getString(ColumnNames.CREATOR_ID));
		forum.setDescription(rs.getString(ColumnNames.DESCRIPTION));
		Timestamp ts = rs.getTimestamp(ColumnNames.LAST_MESSAGE_DATE);
		if(ts != null)
			forum.setLastMessageDate(ts.getTime());
		else
			forum.setLastMessageDate(-1);
		forum.setDiscussionCount(rs.getInt(ColumnNames.DISCUSSION_COUNT));
		forum.setMessageCount(rs.getInt(ColumnNames.MESSAGE_COUNT));
		forum.setStatus(rs.getString("STATUS"));
		return forum;
	}

	public List<Message> getMessages()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean reallyDeleteForum(String forumId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getDeleteForumStatements(forumId);
				
				statement = connection.createStatement();
				
				for(String sql : statements)
					statement.executeUpdate(sql);
				
				connection.commit();
				
				return true;
			}
			catch (SQLException sqle)
			{
				logger.error("Caught exception whilst adding or updating message. Rolling back ...", sqle);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst adding or updating message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean deleteForum(String forumId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();

			String sql = sqlGenerator.getMarkForumDeletedStatement(forumId);
				
			statement = connection.createStatement();
				
			statement.executeUpdate(sql);
				
			return true;
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst marking forum deleted", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean deleteDiscussion(String forumId,String discussionId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getDeleteDiscussionStatements(forumId,discussionId);
				
				statement = connection.createStatement();
				
				for(String sql : statements)
					statement.executeUpdate(sql);
				
				connection.commit();
				
				return true;
			}
			catch (SQLException sqle)
			{
				logger.error("Caught exception whilst adding or updating message. Rolling back ...", sqle);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst adding or updating message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean censorMessage(String messageId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			
			String sql = sqlGenerator.getCensorMessageStatement(messageId);
			
			statement = connection.createStatement();
				
			statement.executeUpdate(sql);
				
			return true;
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst censoring message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}
	
	public boolean deleteMessage(Message message,String forumId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getDeleteMessageStatements(message,forumId);
				
				statement = connection.createStatement();
				
				for(String sql : statements)
					statement.executeUpdate(sql);
				
				connection.commit();
				
				return true;
			}
			catch (SQLException sqle)
			{
				logger.error("Caught exception whilst deleting message. Rolling back ...", sqle);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst deleting message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}
	
	public boolean undeleteMessage(Message message,String forumId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getUndeleteMessageStatements(message,forumId);
				
				statement = connection.createStatement();
				
				for(String sql : statements)
					statement.executeUpdate(sql);
				
				connection.commit();
				
				return true;
			}
			catch (SQLException sqle)
			{
				logger.error("Caught exception whilst un-deleting message. Rolling back ...", sqle);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst un-deleting message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public List<String> getDiscussionUnsubscribers(String discussionId)
	{
		List<String> unsubscribers = new ArrayList<String>();
		
		String sql = sqlGenerator.getDiscussionUnsubscribersStatement(discussionId);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			
			while(rs.next())
				unsubscribers.add(rs.getString(ColumnNames.USER_ID));
			
			rs.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting discussion unsubscribers.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return unsubscribers;
	}

	public void unsubscribeFromDiscussion(String userId, String discussionId)
	{
		Connection connection = null;
		Statement statement = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			statement.executeUpdate(sqlGenerator.getUnsubscribeFromDiscussionStatement(userId,discussionId));
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting discussion unsubscribers.", e);
		}
		finally
		{
			try
			{
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public List<String> getDiscussionUnsubscriptions(String userId)
	{
		List<String> discussions = new ArrayList<String>();
		
		String sql = sqlGenerator.getDiscussionUnsubscriptionsStatement(userId);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			
			while(rs.next())
				discussions.add(rs.getString(ColumnNames.DISCUSSION_ID));
			
			rs.close();
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting discussion unsubscribers.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return discussions;
	}

	public void subscribeToDiscussion(String userId, String discussionId)
	{
		Connection connection = null;
		Statement statement = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			statement.executeUpdate(sqlGenerator.getSubscribeToDiscussionStatement(userId,discussionId));
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting discussion unsubscribers.", e);
		}
		finally
		{
			try
			{
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean showMessage(Message message)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();

			String sql = sqlGenerator.getShowMessageStatement(message);
				
			statement = connection.createStatement();
				
			statement.executeUpdate(sql);
			
			return true;
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst showing message", e);
			return false;
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public void deleteAttachment(String attachmentId, String messageId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				String sql = sqlGenerator.getDeleteAttachmentStatement(attachmentId,messageId);
				
				statement = connection.createStatement();
				
				statement.executeUpdate(sql);
				
				connection.commit();
				
				sakaiProxy.deleteFile(attachmentId);
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst deleting attachment. Rolling back ...", e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst adding or updating message", e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public Message getMessage(String messageId)
	{
		String sql = sqlGenerator.getMessageSelectStatement(messageId);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			
			if(rs.next())
				return getMessageFromResults(rs, connection);
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting message.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return null;
	}

	public Forum getForumContainingMessage(String messageId)
	{
		String sql = sqlGenerator.getSelectForumContainingMessageStatement(messageId);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			if(rs.next())
			{
				String forumId = rs.getString(ColumnNames.FORUM_ID);
				return getForum(forumId, ForumPopulatedStates.EMPTY);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting forum.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return null;
	}

	public List<SearchResult> search(String searchTerms)
	{
		List<Site> sites = sakaiProxy.getAllSites();
		List<String> siteIds = new ArrayList<String>(sites.size());
		for(Site site : sites)
			siteIds.add(site.getId());
		
		// Look for phrases
		Pattern p = Pattern.compile("\"([ a-zA-Z0-9-]*)\"");
		Matcher m = p.matcher(searchTerms);
		List<String> phrases = new ArrayList<String>();
		String words = "";
		
		if(!m.find())
			words = searchTerms;
		else
		{
			int start = 0;
			do
			{
				int matchStart = m.start();
				words += searchTerms.substring(start,matchStart);
				phrases.add(m.group(1));
				start = m.end();
			}
			while(m.find());
		}
		
		String[] terms = words.split(" ");
		for(String term : terms)
			phrases.add(term);
		
		String sql = sqlGenerator.getSearchStatement(sakaiProxy.getCurrentSiteId(),phrases);
		
		List<SearchResult> results = new ArrayList<SearchResult>();
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			while(rs.next())
			{
				Message message = getMessageFromResults(rs, connection);
				SearchResult result = new SearchResult();
				result.setUrl(message.getUrl());
				result.setTitle(message.getSubject());
				result.setMessageId(message.getId());
				result.setDiscussionId(message.getDiscussionId());
				Forum forum = getForumContainingMessage(message.getId());
				result.setForumId(forum.getId());
				
				String content = message.getContent();
				
				List<String> fragments = new ArrayList<String>(phrases.size());
				
				for(String term : phrases)
				{
					int start = content.indexOf(term);
					int end = start + term.length();
					
					int fragmentStart = start - 20;
					if(fragmentStart < 0) fragmentStart = 0;
					
					int fragmentEnd = end + 20;
					if(fragmentEnd > (content.length() - 1))
						fragmentEnd = content.length() - 1;
					
					String fragment = content.substring(fragmentStart,fragmentEnd);
					fragment = fragment.replaceAll("<?p>","");
					fragments.add(fragment);
				}
				
				result.setContextFragments(fragments);
				
				results.add(result);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst searching.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		return results;
	}

	public boolean markMessageRead(String messageId,String forumId,String discussionId)
	{
		String testSql = sqlGenerator.getSelectMessageReadStatement(sakaiProxy.getCurrentUser().getId(),messageId);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs  =  null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statement = connection.createStatement();
				rs = statement.executeQuery(testSql);
				if(rs.next())
					return true;
				
				List<String> updateSql = sqlGenerator.getMarkMessageReadStatements(sakaiProxy.getCurrentUser().getId(),messageId,forumId,discussionId,connection);
				
				for(String sql : updateSql)
					statement.executeUpdate(sql);
				
				connection.commit();
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst marking message read. Rolling back ...", e);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst marking message read", e);
			return false;
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean markMessageUnRead(String messageId,String forumId,String discussionId)
	{
		Connection connection = null;
		Statement statement = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statement = connection.createStatement();
				
				List<String> updateSql = sqlGenerator.getMarkMessageUnReadStatements(sakaiProxy.getCurrentUser().getId(),messageId,forumId,discussionId);
				
				for(String sql : updateSql)
					statement.executeUpdate(sql);
				
				connection.commit();
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst marking message unread. Rolling back ...", e);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst marking message unread", e);
			return false;
		}
		finally
		{
			try
			{
				if(statement != null) statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}
	
	public boolean markDiscussionRead(String discussionId,String forumId)
	{
		if(logger.isInfoEnabled())
			logger.info("markDiscussionRead(" + discussionId + "," + forumId + ")");
		
		Connection connection = null;
		Statement statement = null;
		
		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				statement = connection.createStatement();
				
				List<String> updateSql = sqlGenerator.getMarkDiscussionReadStatements(sakaiProxy.getCurrentUser().getId(),discussionId,forumId,connection);
				
				for(String sql : updateSql)
					statement.executeUpdate(sql);
				
				connection.commit();
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst marking discussion read. Rolling back ...", e);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst marking discussion unread", e);
			return false;
		}
		finally
		{
			try
			{
				if(statement != null) statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public List<String> getReadMessageIds(String discussionId)
	{
		String sql = sqlGenerator.getSelectReadMessageIds(sakaiProxy.getCurrentUser().getId(),discussionId);
		
		List<String> results = new ArrayList<String>();
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			while(rs.next())
				results.add(rs.getString(ColumnNames.MESSAGE_ID));
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting read message ids.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return results;
	}

	public void moveDiscussion(String discussionId, String currentForumId,String newForumId)
	{
		Connection connection = null;
		Statement statement = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				List<String> statements = sqlGenerator.getMoveDiscussionStatements(discussionId, currentForumId, newForumId);
				
				statement = connection.createStatement();
				
				for(String sql : statements)
					statement.executeUpdate(sql);
				
				connection.commit();
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst moving discussion. Rolling back ...", e);
				connection.rollback();
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst moving discussion.", e);
		}
		finally
		{
			try
			{
				statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
	}

	public boolean publishMessage(String forumId,String messageId)
	{
		Connection connection = null;

		try
		{
			connection = sakaiProxy.borrowConnection();
			boolean oldAutoCommitFlag = connection.getAutoCommit();
			connection.setAutoCommit(false);

			try
			{
				Message message = getMessage(messageId);
				List<PreparedStatement> statements = sqlGenerator.getPublishMessageStatements(forumId, message, connection);
				
				for(PreparedStatement statement : statements)
					statement.execute();
				
				connection.commit();
				
				markMessageRead(messageId, forumId, message.getDiscussionId());
				
				return true;
			}
			catch (Exception e)
			{
				logger.error("Caught exception whilst moving discussion. Rolling back ...", e);
				connection.rollback();
				return false;
			}
			finally
			{
				connection.setAutoCommit(oldAutoCommitFlag);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst moving discussion.", e);
			return false;
		}
		finally
		{
			sakaiProxy.returnConnection(connection);
		}
	}

	public byte[] getProfile2Picture(String userId)
	{
		// TODO Auto-generated method stub
		String sql = sqlGenerator.getSelectProfile2Picture(userId);
		
		byte[] bytes = null;
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			if(rs.next())
			{
				String thumbResourceId = rs.getString("RESOURCE_THUMB");
				bytes = sakaiProxy.getResourceBytes(thumbResourceId);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting profile 2 picture. Reason:" + e.getMessage());
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return bytes;
	}

	public Map<String,Integer> getReadMessageCountForAllFora(String userId)
	{
		String sql = sqlGenerator.getSelectReadMessageCountForAllForaStatement(userId);
		
		Map<String,Integer> counts = new HashMap<String,Integer>();
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			while(rs.next())
			{
				String forumId = rs.getString("FORUM_ID");
				int read = rs.getInt("NUMBER_READ");
				counts.put(forumId, read);
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting read message ids.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return counts;
	}

	public Map<String, Integer> getReadMessageCountForForum(String userId, String forumId)
	{
		// TODO Auto-generated method stub
		
		String discussionIdsQuery = sqlGenerator.getSelectDiscussionIdsForForumStatement(forumId);
		
		Map<String,Integer> counts = new HashMap<String,Integer>();
		
		Connection connection = null;
		Statement discussionIdsST = null;
		PreparedStatement countST = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			countST = sqlGenerator.getSelectReadMessageCountForDiscussionStatement(userId,connection);
			discussionIdsST = connection.createStatement();
			
			ResultSet idsRS = discussionIdsST.executeQuery(discussionIdsQuery);
			while(idsRS.next())
			{
				String discussionId = idsRS.getString("DISCUSSION_ID");
				
				countST.setString(1, discussionId);
				
				ResultSet countRS = countST.executeQuery();
				if(countRS.next())
				{
					int count = countRS.getInt("NUMBER_READ");
					counts.put(discussionId, count);
				}
			}
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting read message ids.", e);
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(countST != null) countST.close();
				if(discussionIdsST != null) discussionIdsST.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
		
		return counts;
	}

	/**
	 * @param title The title of the forum that the caller is looking for
	 * @param state One of the ForumPopulatedState states, currently FULL or PART
	 * @return The first forum with that title, or null if no forum with the
	 * 			supplied title could be found
	 */
	public Forum getForumForTitle(String title,String state)
	{
		String sql = sqlGenerator.getSelectForumIdForTitleStatement(title);
		
		Connection connection = null;
		Statement statement = null;
		ResultSet rs = null;
		try
		{
			connection = sakaiProxy.borrowConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			String forumId = null;
			
			if(rs.next())
				forumId = rs.getString("FORUM_ID");
			else
				return null;
			
			return getForum(forumId,state);
		}
		catch (Exception e)
		{
			logger.error("Caught exception whilst getting forum id for title.", e);
			return null;
		}
		finally
		{
			try
			{
				if(rs != null) rs.close();
				if(statement != null) statement.close();
			}
			catch (SQLException e) {}
			
			sakaiProxy.returnConnection(connection);
		}
	}
}
