package org.sakaiproject.yaft.api;

import org.sakaiproject.service.gradebook.shared.GradeDefinition;

import lombok.Getter;
import lombok.Setter ;

@Getter
public class Author {

	private int numberOfPosts = 0;
	
	private String id;
	
	private String displayName;
	
    @Setter
	private GradeDefinition grade = null;
	
	public Author(String id,String displayName,int numberOfPosts) {

		this.id = id;
		this.displayName = displayName;
		this.numberOfPosts = numberOfPosts;
	}

	public boolean isGraded() {
		return grade != null;
	}
}
