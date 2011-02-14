package org.sakaiproject.yaft.api;

public class Author
{
	private int numberOfPosts = 0;
	
	private String id;
	
	private String displayName;
	
	public Author(String id,String displayName,int numberOfPosts) {
		this.id = id;
		this.displayName = displayName;
		this.numberOfPosts = numberOfPosts;
	}

	public String getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getNumberOfPosts() {
		return numberOfPosts;
	}
}