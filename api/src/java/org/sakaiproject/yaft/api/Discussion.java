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
import java.util.List;
import java.util.Stack;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import lombok.Getter;
import lombok.Setter;

public class Discussion implements Entity {
     
    @Getter @Setter
	private int messageCount = 0;
	
    @Getter @Setter
	private long lastMessageDate;

    @Getter @Setter
	private Message firstMessage;
	
    @Getter @Setter
	private String forumId;
	
    @Getter @Setter
	private String status = "READY";
	
	// We need this to build direct urls in the rendered pages. Bogus, but necessary.
    @Getter @Setter
	private String pageId;
	
    @Getter @Setter
	private long start = -1L;

    @Getter @Setter
	private long end = -1L;

    @Getter @Setter
	private boolean allowAnonymousPosting = false;
	
    @Getter @Setter
	private boolean lockedForWriting = false;

    @Getter @Setter
	private boolean lockedForReading = false;
	
    @Getter @Setter
	private boolean groupsInherited = false;
	
    @Getter @Setter
	private List<Group> groups = new ArrayList<Group>();
	
    @Getter @Setter
	private YaftGBAssignment assignment = null;
	
	public String getId() {
		return firstMessage.getId();
	}

	public String getContent() {
		return firstMessage.getContent();
	}

	public String getSubject() {
		return firstMessage.getSubject();
	}
	
	public void setSubject(String subject) {
		firstMessage.setSubject(subject);
	}

	public long getCreatedDate() {
		return firstMessage.getCreatedDate();
	}

	public String getCreatorId() {
		return firstMessage.getCreatorId();
	}

	public String getCreatorDisplayName() {
		return firstMessage.getCreatorDisplayName();
	}
	
	public String getUrl() {
		return "/portal/tool/" + getPlacementId() + "/discussions/" + getId() + ".html";
	}
	
	public String getFullUrl() {
		return "/portal/directtool/" + getPlacementId() + "/discussions/" + getId() + ".html";
	}

	public String getPlacementId() {
		return firstMessage.getPlacementId();
	}
	
	public Element toXml(Document doc,Stack stack) {

		Element discussionElement = doc.createElement(XmlDefs.DISCUSSION);

		if (stack.isEmpty()) {
			doc.appendChild(discussionElement);
		}
		else {
			((Element) stack.peek()).appendChild(discussionElement);
		}

		//stack.push(discussionElement);

		discussionElement.setAttribute(XmlDefs.ID, getId());
		discussionElement.setAttribute(XmlDefs.CREATED_DATE, Long.toString(getCreatedDate()));
		discussionElement.setAttribute(XmlDefs.CREATOR_ID, getCreatorId());
		discussionElement.setAttribute(XmlDefs.SUBJECT, getSubject());
		discussionElement.setAttribute(XmlDefs.LAST_MESSAGE_DATE, Long.toString(lastMessageDate));
		discussionElement.setAttribute(XmlDefs.MESSAGE_COUNT, Integer.toString(messageCount));
		
		Element messagesElement = doc.createElement(XmlDefs.MESSAGES);
		discussionElement.appendChild(messagesElement);
		stack.push(messagesElement);
		
		firstMessage.toXml(doc,stack);
		
		stack.pop();
		
		return discussionElement;
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

	public boolean isGraded() {
		return assignment != null;
	}
	
	public ResourceProperties getProperties() {
		return null;
	}

	public String getReference() {
		return YaftForumService.REFERENCE_ROOT + Entity.SEPARATOR + getSiteId() + Entity.SEPARATOR + "discussions" + Entity.SEPARATOR + getId();
	}

	public String getReference(String rootProperty) {
		return getReference();
	}

	public String getUrl(String rootProperty) {
		return getUrl();
	}

	public String getSiteId() {
		return firstMessage.getSiteId();
	}
}
