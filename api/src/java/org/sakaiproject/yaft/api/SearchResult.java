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

import java.util.ArrayList;
import java.util.List;

public class SearchResult
{
	private String url = "";
	private List<String> contextFragments = new ArrayList<String>();
	private String title = "";
	private String messageId = "";
	private String discussionId = "";
	private String forumId = "";
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setTitle(String title)
	{
		this.title = title;
	}
	public String getTitle()
	{
		return title;
	}
	public void setContextFragments(List<String> contextFragments)
	{
		this.contextFragments = contextFragments;
	}
	public List<String> getContextFragments()
	{
		return contextFragments;
	}
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	public String getMessageId()
	{
		return messageId;
	}
	public void setDiscussionId(String discussionId)
	{
		this.discussionId = discussionId;
	}
	public String getDiscussionId()
	{
		return discussionId;
	}
	public void setForumId(String forumId)
	{
		this.forumId = forumId;
	}
	public String getForumId()
	{
		return forumId;
	}
}
