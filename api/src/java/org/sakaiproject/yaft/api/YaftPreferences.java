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
