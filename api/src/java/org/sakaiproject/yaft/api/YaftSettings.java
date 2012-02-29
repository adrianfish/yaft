package org.sakaiproject.yaft.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class YaftSettings {
	public List<String> alertRoles = new ArrayList<String>();
	public String siteId = "";
	
	public List<String> getAlertRoles() {
		return alertRoles;
	}
	
	public YaftSettings() {
	}
	
	public YaftSettings(String siteId,String[] alertRoles) {
		this.siteId = siteId;
		this.alertRoles = Arrays.asList(alertRoles);
	}

	public String getSiteId() {
		return siteId;
	}
}
