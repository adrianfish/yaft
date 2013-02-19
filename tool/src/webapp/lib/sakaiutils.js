/*
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
var SAKAIUTILS = (function ($) {
    var my = {};
		
	my.readCookie = function (name) {
    	var nameEQ = name + "=";
    	var ca = document.cookie.split(';');
    	for(var i=0;i < ca.length;i++) {
        	var c = ca[i];
        	while (c.charAt(0)==' ') c = c.substring(1,c.length);
        	if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
    	}
    	return null;
	};

	my.createCookie = function (name,value,days) {
    	if (days) {
        	var date = new Date();
        	date.setTime(date.getTime()+(days*24*60*60*1000));
        	var expires = "; expires="+date.toGMTString();
    	}
    	else var expires = "";
    	
    	document.cookie = name+"="+value+expires+"; path=/";
	};
		
	my.getCurrentUser = function () {
		var user = null;
		jQuery.ajax( {
	 		url : "/direct/user/current.json",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(u) {
				user = u;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get the current user. Status: " + stat + ". Error: " + error);
			}
	  	});

		return user;
	};

	my.getProfileMarkup = function (userId) {
		var profile = '';

		jQuery.ajax( {
	       	url : "/direct/profile/" + userId + "/formatted",
	       	dataType : "html",
	       	async : false,
			cache: false,
		   	success : function(p) {
				profile = p;
			},
			error : function(xmlHttpRequest,stat,error) {
				alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
			}
	   	});

		return profile;
	};
		
	my.getParameters = function () {
		var arg = new Object();
		var href = document.location.href;

		var paramString = '';
		
		if ( href.indexOf( "?") != -1) {
			var paramString = href.split( "?")[1];
		}
		else {
			// No url params. Let's try the cookie
			var paramString = unescape(this.readCookie('sakai-tool-params'));
		}
			
		if(paramString.indexOf("#") != -1)
			paramString = paramString.split("#")[0];
				
		var params = paramString.split("&");

		for (var i = 0; i < params.length; ++i) {
			var name = params[i].split( "=")[0];
			var value = params[i].split( "=")[1];
			arg[name] = unescape(value);
		}
	
		return arg;
	};

	my.renderTrimpathTemplate = function (templateName,contextObject,output) {
		var templateNode = document.getElementById(templateName);
		var firstNode = templateNode.firstChild;
		var template = null;

		if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4))
  			template = templateNode.firstChild.data.toString();
		else
   			template = templateNode.innerHTML.toString();

		var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

   		var render = trimpathTemplate.process(contextObject);

		if (output)
			document.getElementById(output).innerHTML = render;

		return render;
	};

	my.setupFCKEditor = function (textarea_id,width,height,toolbarSet,siteId) {
		var oFCKeditor = new FCKeditor(textarea_id);

		oFCKeditor.BasePath = "/library/editor/FCKeditor/";
		oFCKeditor.Width  = width;
		oFCKeditor.Height = height;
		oFCKeditor.ToolbarSet = toolbarSet;
		
		var collectionId = "/group/" + siteId + "/";
		
		oFCKeditor.Config['ImageBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['ImageUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId;

		oFCKeditor.Config['CurrentFolder'] = collectionId;

		oFCKeditor.Config['CustomConfigurationsPath'] = "/library/editor/FCKeditor/config.js";
		oFCKeditor.ReplaceTextarea();
		
        if(window.frameElement) {
            setMainFrameHeight(window.frameElement.id);
        }
	};
	
	my.setupCKEditor = function(textarea_id,width,height,toolbarSet,siteId) {
		// CKEDITOR.basePath already set
		
		if (CKEDITOR.instances[textarea_id]) {
			CKEDITOR.remove(CKEDITOR.instances[textarea_id]);
		}
		
		if ('Default' === toolbarSet) {
			toolbarSet = 'Full';
		}
		
		var collectionId = "/group/" + siteId + "/";
		
		// used to point to file manager/browser 
		fckBasePath = "/library/editor/FCKeditor/";
	
		CKEDITOR.replace(textarea_id, {
			width:width,
			height:height,
			toolbar:toolbarSet,
			skin: 'v2',
			customConfigurationsPath:"/library/editor/ckeditor/config.js",
			/* using FCK browser: see https://jira.sakaiproject.org/browse/SAK-17885 */
			filebrowserBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html",
			filebrowserImageBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId,
			filebrowserLinkBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId,
			filebrowserFlashBrowseUrl:fckBasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId,
			filebrowserImageUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId,
			filebrowserFlashUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId,
			filebrowserLinkUploadUrl:fckBasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId
		});
		
		CKEDITOR.instances[textarea_id].on('instanceReady',function (e) {
            if(window.frameElement) {
                setMainFrameHeight(window.frameElement.id);
            }
        });
	}
	
	my.setupWysiwygEditor = function(editorId,textarea_id,width,height,toolbarSet,siteId) {
		if ('FCKeditor' === editorId) {
			this.setupFCKEditor(textarea_id,width,height,toolbarSet,siteId);
		} else if ('ckeditor' === editorId) {
			this.setupCKEditor(textarea_id,width,height,toolbarSet,siteId);
		}
	}
	
	my.getWysiwygEditor = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return FCKeditorAPI.GetInstance(textarea_id);
		} else if ('ckeditor' === editorId) {
			return CKEDITOR.instances[textarea_id];
		}
	}
	
	my.getEditorData = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).GetData();
		} else if ('ckeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).getData();
		} else {
            return $('#' + textarea_id).val();
        }
	}

	my.updateEditorElement = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).UpdateLinkedField();
		} else if ('ckeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).updateElement();
		}
	}
	
	my.resetEditor = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			this.getWysiwygEditor(editorId,textarea_id).ResetIsDirty();
		} else if ('ckeditor' === editorId) {
			this.getWysiwygEditor(editorId,textarea_id).resetDirty();
		}
	}
		
	my.isEditorDirty = function(editorId,textarea_id) {
		if ('FCKeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).IsDirty();
		} else if ('ckeditor' === editorId) {
			return this.getWysiwygEditor(editorId,textarea_id).checkDirty();
		}
	}

    return my;

}(jQuery));
