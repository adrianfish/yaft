package org.sakaiproject.yaft.api;

import java.util.UUID;

public class Attachment
{
	private String url = "";
	private String mimeType = "";
	private byte[] data;
	private String name = "";
	private String resourceId = "";
	
	public Attachment(String name,String contentType, byte[] bs)
	{
		this.name = name;
		mimeType = contentType;
		
		if(name.endsWith(".doc")) mimeType = "application/msword";
		data = bs;
	}
	
	public Attachment() {} 
	
	public void setUrl(String url)
	{
		this.url = url;
	}
	public String getUrl()
	{
		return url;
	}
	public void setMimeType(String mimeType)
	{
		this.mimeType = mimeType;
	}
	public String getMimeType()
	{
		return mimeType;
	}
	public void setData(byte[] data)
	{
		this.data = data;
	}
	public byte[] getData()
	{
		return data;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getName()
	{
		return name;
	}
	public void setResourceId(String resourceId)
	{
		this.resourceId = resourceId;
	}
	public String getResourceId()
	{
		return resourceId;
	}
}
