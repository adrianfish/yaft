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
import java.util.UUID;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.util.BaseResourceProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import lombok.Getter;
import lombok.Setter;

public class Message implements Entity {

    public static String READY = "READY";
    public static String DELETED = "DELETED";
    public static String DRAFT = "DRAFT";

	private static final String CDATA_SUFFIX = "]]>";
	private static final String CDATA_PREFIX = "<![CDATA[";
	
    @Getter @Setter
	private String id = "";
    @Getter @Setter
	private String subject = "";
    @Getter
	private List<Message> children = new ArrayList<Message>();
    @Getter @Setter
	private String creatorId = "";
    @Getter @Setter
	private long createdDate;
    @Getter @Setter
	private String content = "";
    @Getter @Setter
	private String parent = "";
    @Getter @Setter
	private String creatorDisplayName = "";
    @Setter
	private String discussionId = "";
    @Getter @Setter
    private boolean anonymous = false;
    @Getter @Setter
	private String status = DRAFT;
    @Getter @Setter
	private List<Attachment> attachments = new ArrayList<Attachment>();
	private String url = "";
    @Getter
	private String fullUrl = "";
    @Getter
	private String placementId = "";
    @Getter
	private String siteId = "";
	
    @Getter @Setter
	private List<Group> groups = new ArrayList<Group>();
	
	public Message() {

		id = UUID.randomUUID().toString();
		createdDate = new Date().getTime();
	}
	
	public boolean hasParent() {
		return (parent != null && parent.length() > 0);
	}

	public void addChild(Message child) {
		children.add(child);
	}
	
	public String getDiscussionId() {

		if("".equals(parent)) {
			return id;
        }
		
		return discussionId;
	}

	public void setSiteId(String siteId) {

		this.siteId = siteId;
		
		try {

			Site site = SiteService.getSite(siteId);
			ToolConfiguration tc = site.getToolForCommonId("sakai.yaft");
			placementId = tc.getId();
			url = "/portal/tool/" + tc.getId() + "/messages/" + id + ".html";
			fullUrl = "/portal/directtool/" + tc.getId() + "/messages/" + id + ".html";
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    public boolean isDeleted() {
        return status.equals(DELETED);
    }

    public boolean isReady() {
        return status.equals(READY);
    }

    public boolean isDraft() {
        return status.equals(DRAFT);
    }

	/** START Entity IMPLEMENTATION */

	/**
	 * From org.sakaiproject.entity.api.Entity
	 */
	public ResourceProperties getProperties() {

		ResourceProperties rp = new BaseResourceProperties();
		
		rp.addProperty("id", getId());
		return rp;
	}

	/**
	 * @see org.sakaiproject.entity.api.Entity#getReference()
	 * 
	 * @return
	 */
	public String getReference() {
		return YaftForumService.REFERENCE_ROOT + Entity.SEPARATOR + siteId  + Entity.SEPARATOR + "messages" + Entity.SEPARATOR + id;
	}

	/**
	 * @see org.sakaiproject.entity.api.Entity#getReference()
	 * 
	 * @return
	 */
	public String getReference(String rootProperty) {
		return getReference();
	}
	
	/**
	 * @see org.sakaiproject.entity.api.Entity#getUrl()
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * @see org.sakaiproject.entity.api.Entity#toXml()
	 * 
	 * @return
	 */
	public String getUrl(String rootProperty) {
		return url;
	}

	/**
	 * @see org.sakaiproject.entity.api.Entity#toXml()
	 * 
	 * @return
	 */
	public Element toXml(Document doc, Stack stack) {

		Element messageElement = doc.createElement(XmlDefs.MESSAGE);

		if (stack.isEmpty()) {
			doc.appendChild(messageElement);
		} else {
			((Element) stack.peek()).appendChild(messageElement);
		}

		//stack.push(messageElement);

		messageElement.setAttribute(XmlDefs.ID, id);
		messageElement.setAttribute(XmlDefs.CREATED_DATE, Long.toString(createdDate));
		messageElement.setAttribute(XmlDefs.CREATOR_ID, creatorId);
		messageElement.setAttribute(XmlDefs.STATUS, status);

		Element subjectElement = doc.createElement(XmlDefs.SUBJECT);
		subjectElement.setTextContent(subject);
		messageElement.appendChild(subjectElement);

		Element contentElement = doc.createElement(XmlDefs.CONTENT);
		contentElement.setTextContent(wrapWithCDATA(content));
		messageElement.appendChild(contentElement);
		
		Element attachmentsElement = doc.createElement(XmlDefs.ATTACHMENTS);
		
		for(Attachment attachment : attachments) {
			Element attachmentElement = doc.createElement(XmlDefs.ATTACHMENT);
			attachmentElement.setAttribute(XmlDefs.RESOURCE_ID, attachment.getResourceId());
			attachmentsElement.appendChild(attachmentElement);
		}
		
		messageElement.appendChild(attachmentsElement);
		
		Element repliesElement = doc.createElement(XmlDefs.REPLIES);
		messageElement.appendChild(repliesElement);
		stack.push(repliesElement);
		
		for(Message child : children) {
			child.toXml(doc, stack);
        }

		stack.pop();

		return messageElement;
	}
	
	/** END Entity IMPLEMENTATION */
	
	private String wrapWithCDATA(String s) {
		return CDATA_PREFIX + s + CDATA_SUFFIX;
	}
	
	private String stripCDATA(String s) {

		if(s.startsWith(CDATA_PREFIX) && s.endsWith(CDATA_SUFFIX)) {

			s = s.substring(CDATA_PREFIX.length());
			s = s.substring(0, s.length() - CDATA_SUFFIX.length());
		}
		
		return s;
	}

	public void fromXml(Element messageElement) {

		NodeList nodes = messageElement.getChildNodes();
		
		for(int i=0;i<nodes.getLength();i++) {

			Node node = nodes.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
            }
			
			Element element = (Element) node;
			
			if(XmlDefs.SUBJECT.equals(element.getTagName()))
				setSubject(element.getFirstChild().getTextContent());
			
			if(XmlDefs.CONTENT.equals(element.getTagName()))
				setContent(stripCDATA(element.getFirstChild().getTextContent()));
			
			if(XmlDefs.ATTACHMENTS.equals(element.getTagName())) {

				NodeList children = element.getChildNodes();
				for(int j=0;j<children.getLength();j++) {

					Node child = children.item(j);
					if(child.getNodeType() != Node.ELEMENT_NODE) {
						continue;
                    }
					
					Element childElement = (Element) child;
					
					if(XmlDefs.ATTACHMENT.equals(childElement.getTagName())) {
						String resourceId = childElement.getAttribute(XmlDefs.RESOURCE_ID);
						
						Attachment attachment = new Attachment();
						attachment.setResourceId(resourceId);
						attachments.add(attachment);
					}
				}
			}
		}
		
		setCreatorId(messageElement.getAttribute(XmlDefs.CREATOR_ID));
		setCreatedDate(Long.parseLong(messageElement.getAttribute(XmlDefs.CREATED_DATE)));
		setStatus(messageElement.getAttribute(XmlDefs.STATUS));
	}
}
