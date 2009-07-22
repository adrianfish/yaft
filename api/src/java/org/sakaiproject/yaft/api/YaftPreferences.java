package org.sakaiproject.yaft.api;

public class YaftPreferences
{
	public static final String EACH = "each";
	public static final String NEVER = "never";
	public static final String DIGEST = "digest";
	public static final String FULL = "full";
	public static final String MINIMAL = "minimal";
	
	private String email = EACH;
	private String view = FULL;
	
	public YaftPreferences(String email,String view)
	{
		super();
		
		this.email = email;
		this.view = view;
	}
	
	public YaftPreferences() {}

	public void setEmail(String email)
	{
		this.email = email;
	}
	
	public String getEmail()
	{
		return email;
	}
	
	public void setView(String view)
	{
		this.view = view;
	}
	
	public String getView()
	{
		return view;
	}
}
