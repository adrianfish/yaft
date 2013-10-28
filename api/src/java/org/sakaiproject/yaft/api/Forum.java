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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import lombok.Getter;
import lombok.Setter;

public class Forum implements Entity {

    @Getter @Setter
	private String id = "";

    @Getter @Setter
	public String title = "";

    @Getter @Setter
	private String description = "";

    @Getter @Setter
	private int discussionCount = 0;

    @Getter @Setter
	private int messageCount = 0;

    @Getter @Setter
	private long lastMessageDate;

    @Getter @Setter
	private long start = -1L;

    @Getter @Setter
	private long end = -1L;

    @Getter @Setter
	private boolean lockedForWriting = false;

    @Getter @Setter
	private boolean lockedForReading = false;

    @Getter
	private String siteId = "";

    @Getter @Setter
	private String status = "READY";

    @Getter @Setter
	private String creatorId = "";

    @Getter @Setter
	private List<Discussion> discussions= new ArrayList<Discussion>();

	private String url = "";

    @Getter
	private String fullUrl = "";

    @Getter @Setter
	private List<Group> groups = new ArrayList<Group>();
	
	public Forum() {
		id = UUID.randomUUID().toString();
	}
	
	public void setSiteId(String siteId) {

		this.siteId = siteId;
		
		try {
			Site site = SiteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			url = "/portal/tool/" + tc.getId() + "/forums/" + id + ".html";
			fullUrl = "/portal/directtool/" + tc.getId() + "/forums/" + id + ".html";
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public Element toXml(Document doc,Stack stack) {

		Element forumElement = doc.createElement(XmlDefs.FORUM);

		if (stack.isEmpty()) {
			doc.appendChild(forumElement);
		} else {
			((Element) stack.peek()).appendChild(forumElement);
		}

		//stack.push(forumElement);

		forumElement.setAttribute(XmlDefs.ID, id);
		forumElement.setAttribute(XmlDefs.LAST_MESSAGE_DATE, Long.toString(lastMessageDate));
		forumElement.setAttribute(XmlDefs.MESSAGE_COUNT, Integer.toString(messageCount));
		forumElement.setAttribute(XmlDefs.DISCUSSION_COUNT, Integer.toString(discussionCount));
		
		Element titleElement = doc.createElement(XmlDefs.TITLE);
		titleElement.setTextContent(title);
		forumElement.appendChild(titleElement);
		
		Element descriptionElement = doc.createElement(XmlDefs.DESCRIPTION);
		descriptionElement.setTextContent(description);
		forumElement.appendChild(descriptionElement);
		
		Element discussionsElement = doc.createElement(XmlDefs.DISCUSSIONS);
		forumElement.appendChild(discussionsElement);
		
		stack.push(discussionsElement);
		
		for(Discussion discussion : discussions) {
			discussion.toXml(doc,stack);
        }
		
		stack.pop();
		
		return forumElement;
	}

	public void fromXml(Element forumElement) {

		if(!forumElement.getTagName().equals(XmlDefs.FORUM)) {
			return;
		}
		
		NodeList children = forumElement.getElementsByTagName(XmlDefs.TITLE);
		title = children.item(0).getFirstChild().getTextContent();
		
		children = forumElement.getElementsByTagName(XmlDefs.DESCRIPTION);
		setDescription(children.item(0).getFirstChild().getTextContent());
	}

	public boolean isCurrent() {

		if(start == -1 || end == -1) {
			return false;
        } else {

			long currentDate = new Date().getTime();

			if(start <= currentDate && currentDate <= end) {
				return true;
            } else {
				return false;
            }
		}
	}

	public boolean isLockedForWritingAndUnavailable() {
		return lockedForWriting && !isCurrent();
	}

	public boolean isLockedForReadingAndUnavailable() {
		return lockedForReading && !isCurrent();
	}
	
	// START ENTITY IMPL
	
	public ResourceProperties getProperties() {
		return null;
	}

	public String getReference() {
		return YaftForumService.REFERENCE_ROOT + Entity.SEPARATOR + siteId  + Entity.SEPARATOR + "forums" + Entity.SEPARATOR + id;
	}

	public String getReference(String rootProperty) {
		return getReference();
	}

	public String getUrl() {
		return url;
	}

	public String getUrl(String rootProperty) {
		return getUrl();
	}
	
	// END ENTITY IMPL
}
