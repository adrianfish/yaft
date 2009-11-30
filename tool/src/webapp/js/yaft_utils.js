var YaftUtils;

(function()
{
	if(YaftUtils == null)
		YaftUtils = new Object();
		
	YaftUtils.showDeleted = function()
	{
		$(".yaft_deleted_message").show();
	  	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	  	yaftShowingDeleted = true;
		$('#yaft_show_deleted_link').hide();
		$('#yaft_hide_deleted_link').show();
	}

	YaftUtils.clearDates = function()
	{
		$('#yaft_start_date').val('');
		$('#yaft_end_date').val('');
		$('#yaft_start_hour_selector').get(0).selectedIndex = 0;
		$('#yaft_start_minute_selector').get(0).selectedIndex = 0;
		$('#yaft_end_hour_selector').get(0).selectedIndex = 0;
		$('#yaft_end_minute_selector').get(0).selectedIndex = 0;
	}
	
	YaftUtils.clearActiveDiscussionsForCurrentUser = function()
	{
		$.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/activeDiscussions/clear",
			dataType : "text",
			cache: false,
			async : false,
	  		success : function(text,status)
	  		{
				$('.yaft_active_discussion_row').hide();
			},
			error : function(xmlHttpRequest,status,error)
			{
			alert('failed');
			}
		});
	}

	YaftUtils.showAdvancedOptions = function()
	{
		$('#yaft_availability_fieldset').show();
		$('#yaft_show_advanced_options_link').hide();
		$('#yaft_hide_advanced_options_link').show();
	 	$(document).ready(function()
	 	{
	 		setMainFrameHeight(window.frameElement.id);
		});
	}

	YaftUtils.hideAdvancedOptions = function()
	{
		$('#yaft_availability_fieldset').hide();
		$('#yaft_show_advanced_options_link').show();
		$('#yaft_hide_advanced_options_link').hide();
	 	$(document).ready(function()
	 	{
	 		setMainFrameHeight(window.frameElement.id);
		});
	}

	YaftUtils.hideDeleted = function()
	{
		$(".yaft_deleted_message").hide();
	   	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	  	yaftShowingDeleted = false;
		$('#yaft_show_deleted_link').show();
		$('#yaft_hide_deleted_link').hide();
	}

	YaftUtils.renderCurrentForumContent = function()
	{
		YaftUtils.render('yaft_forum_content_template',yaftCurrentForum,'yaft_content');
			
		$(document).ready(function()
		{
			YaftUtils.applyBanding();
		
  			$('a.profile').cluetip({
									local: true
									,width: '320px'
									,cluetipClass: 'jtip'
  									,dropShadow: false
									});
									
			$("#yaft_discussion_table").tablesorter({
	 							cssHeader:'yaftSortableTableHeader',
	 							cssAsc:'yaftSortableTableHeaderSortUp',
	 							cssDesc:'yaftSortableTableHeaderSortDown',
	 							headers:
	 								{
	 									4:{sorter: "isoDate"},
	 									5:{sorter: false}
	 								}
	 						});

			setMainFrameHeight(window.frameElement.id);
		});
	}

	YaftUtils.applyBanding = function()
	{
		$("tr:even").attr('class', function(indexArray)
									{
										var clz = this.getAttribute('class');

										if(clz == null)
										{
											var clzNode = this.attributes['class'];
											if(clzNode)
												clz = clzNode.nodeValue;
										}

										if(clz == null)
											return 'yaftEvenRow';

										if(clz.indexOf('yaftInvisible') != -1)
											return clz;
										else
											return clz + ' yaftEvenRow';
									} );
	}
	
	YaftUtils.renderCurrentForums = function()
	{
		YaftUtils.render('yaft_forums_content_template',yaftCurrentForums,'yaft_content');
	 	$(document).ready(function()
	 	{
	 		setMainFrameHeight(window.frameElement.id);
	 			
	 		$("#yaft_forum_table").tablesorter({
	 			cssHeader:'yaftSortableTableHeader',
	 			cssAsc:'yaftSortableTableHeaderSortUp',
	 			cssDesc:'yaftSortableTableHeaderSortDown',
	 			headers:
	 				{
	 					4: {sorter: "isoDate"},
	 					5: {sorter: false}
	 				}
	 		});

			YaftUtils.applyBanding();
	 	});
	}
	
	YaftUtils.validateMessageSubmission = function(originalSubject)
	{
		originalSubject = unescape(originalSubject);

		var subject = $("#yaft_message_subject_field").val();

		if(subject)
			return true;
		else
		{
			if(confirm(yaft_empty_message_subject_message))
			{
				if(originalSubject.match(/^Re: /) == null)
					$("#yaft_message_subject_field").val('Re: ' + originalSubject);
				else
					$("#yaft_message_subject_field").val(originalSubject);

				return true;
			}
			else
				return false;
		}
	}
	
	YaftUtils.getForum = function(forumId,state)
	{
		var forumDataUrl = "/portal/tool/" + yaftPlacementId + "/data/forums/" + forumId;
		if(state != null) forumDataUrl += "/" + state;
	
		var currentForum = null;
		
		$.ajax(
		{
	   		url : forumDataUrl,
			dataType : "json",
			cache: false,
			async : false,
	  		success : function(forum)
	  		{
	  			currentForum = forum;
			},
			error : function(xmlHttpRequest,status)
			{
			}
		});
		
		return currentForum;
	}
	
	YaftUtils.getUnsubscriptions = function()
	{
		var data = null;
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/data/users/" + yaftCurrentUser.id + "/unsubscriptions",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
	   		success : function(unsubscriptions,status)
			{
				data = unsubscriptions;
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to get unsubscription data. Reason: " + errorThrown);
			}
	  	});
	  	
	  	return data;
	}
	
	YaftUtils.getForumUnsubscriptions = function()
	{
		var data = null;
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/data/users/" + yaftCurrentUser.id + "/forumUnsubscriptions",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
	   		success : function(unsubscriptions,status)
			{
				data = unsubscriptions;
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to get forum unsubscription data. Reason: " + errorThrown);
			}
	  	});
	  	
	  	return data;
	}

	YaftUtils.setupForumUnsubscriptions = function()
	{
		var forums = yaftCurrentForums.items;

		for(var i=0,j=forums.length;i<j;i++)
		{
			var forum = forums[i];
			forum["unsubscribed"] = false;
			for(var k=0,m=yaftForumUnsubscriptions.length;k<m;k++)
			{
				var f = yaftForumUnsubscriptions[k];
				if(f == forum.id)
				{
					forum["unsubscribed"] = true;
					break;
				}
			}
		}
	}
	
	YaftUtils.setupCurrentForumUnsubscriptions = function()
	{
		for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
		{
			var discussion = yaftCurrentForum.discussions[i];
			discussion["unsubscribed"] = false;
			for(var k=0,m=yaftUnsubscriptions.length;k<m;k++)
			{
				var d = yaftUnsubscriptions[k];
				if(d == discussion.id)
				{
					discussion["unsubscribed"] = true;
					break;
				}
			}
		}
	}
	
	YaftUtils.getDiscussion = function(discussionId)
	{
		var discussion = null;
		
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/data/discussions/" + discussionId,
			dataType : "json",
			cache: false,
			async : false,
	  		success : function(d)
	  		{
				discussion = d;
	  		},
			error : function(xmlHttpRequest,textStatus,errorThrown)
	  		{
				alert("Failed to get discussion. Reason: " + xmlHttpRequest.statusText);
	 		}
		});
		
		return discussion;
	}
	
	/* START MESSAGE OPERATIONS */
	
	YaftUtils.publishMessage = function(messageId)
	{
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/messages/" + messageId + "/publish",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				var message = YaftUtils.findMessage(messageId);
				
				message.status = 'READY';
				for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
				{
					var testDiscussion = yaftCurrentForum.discussions[i];
					if(testDiscussion.id == yaftCurrentDiscussion.id)
						testDiscussion['messageCount'] = testDiscussion['messageCount'] + 1;
				}
				
				YaftUtils.render('yaft_message_template',message,message.id);

				$('#' + message.id + '_draft_label').hide();
				
				$(document).ready(function()
				{
					$('a.profile').cluetip({local: true
				         					,width: '320px'
									        ,hoverIntent: {
									        	interval:50,
							                    timeout:0
				                              }
				                           	,cluetipClass: 'jtip'
										    ,dropShadow: false});
			    });
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to publish message. Reason: " + errorThrown);
			}
		});

		return false;
	}
	
	YaftUtils.deleteMessage = function(messageId)
	{
		if(!confirm(yaft_delete_message_message))
			return false;
		
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/messages/" + messageId + "/delete",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				var message = YaftUtils.findMessage(messageId);
				message.status = 'DELETED';
				if(message.id == yaftCurrentDiscussion.firstMessage.id)
					message['isFirstMessage'] = true;
				else
					message['isFirstMessage'] = false;
				YaftUtils.render('yaft_message_template',message,message.id);
				if(yaftViewMode === 'minimal') {
					$('#' + message.id + '_link').hide();
					$('#' + message.id).hide();
				}
				$(document).ready(function()
				{
					$('a.profile').cluetip({local: true
				         					,width: '320px'
									        ,hoverIntent: {
									        	interval:50,
							                    timeout:0
				                              }
				                           	,cluetipClass: 'jtip'
										    ,dropShadow: false});
			    });
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to delete message. Reason: " + status);
			}
		});
		
		return false;
	}

	YaftUtils.findForum = function(id)
	{
		var forums = yaftCurrentForums.items;

		for(var i=0,j=forums.length;i<j;i++)
		{
			if(forums[i].id == id)
				return forums[i];
		}

		return null;
	}

	YaftUtils.findDiscussion = function(id)
	{
		var discussions = yaftCurrentForum.discussions;

		for(var i=0,j=discussions.length;i<j;i++)
		{
			if(discussions[i].id == id)
				return discussions[i];
		}

		return null;
	}
	
	YaftUtils.undeleteMessage = function(messageId)
	{
		if(!confirm(yaft_undelete_message_message))
			return false;
		
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/messages/" + messageId + "/undelete",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				var message = YaftUtils.findMessage(messageId);
				message.status = 'READY';
				if(message.id == yaftCurrentDiscussion.firstMessage.id)
					message['isFirstMessage'] = true;
				else
					message['isFirstMessage'] = false;
				YaftUtils.render('yaft_message_template',message,message.id);
				$(document).ready(function()
				{
					$('a.profile').cluetip({local: true
				         					,width: '320px'
									        ,hoverIntent: {
									        	interval:50,
							                    timeout:0
				                              }
				                           	,cluetipClass: 'jtip'
										    ,dropShadow: false});
			    });
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to un-delete message. Reason: " + status);
			}
		});
		
		return false;
	}
    
    YaftUtils.toggleMessage = function(e,messageId)
    {
		var message = YaftUtils.findMessage(messageId);

    	var descendantIds = new Array();
    	YaftUtils.getDescendantIds(messageId,message,descendantIds);

		if(message.collapsed)
		{
    		for(var i=0,j=descendantIds.length;i<j;i++)
    			$("#" + descendantIds[i]).show();

			e.innerHTML = yaft_collapse_label;
			message.collapsed = false;
		}
		else
		{
    		for(var i=0,j=descendantIds.length;i<j;i++)
    			$("#" + descendantIds[i]).hide();

			e.innerHTML = yaft_expand_label + ' (' + descendantIds.length + ')';
			message.collapsed = true;
		}
    }

    YaftUtils.markCurrentDiscussionRead = function(read)
	{
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/discussions/" + yaftCurrentDiscussion.id + "/markRead",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
				{
					var discussion = yaftCurrentForum.discussions[i];
					if(discussion.id == yaftCurrentDiscussion.id)
						discussion['unread'] = 0;
				}
				
				switchState('forum');
			},
			error : function(xmlHttpRequest,status,error)
			{
			}
	  	});

		return false;
	}
    
    YaftUtils.markMessageRead = function(message,read)
	{
		var message;
		
		if(message["read"] && read == message["read"]) return;
		
		var func = 'markRead';
		if(!read) func = 'markUnRead';
		
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/messages/" + message.id + "/" + func,
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				message["read"] = read;

				if('minimal' == yaftViewMode)
				{
					if(read)
						$("#" + message.id + "_read").show();
					else
						$("#" + message.id + "_read").hide();
				}

				YaftUtils.render('yaft_message_template',message,message.id);
					
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to mark message as read. Reason: " + status);
			}
	  	});

		return false;
	}
	
	/* END MESSAGE OPERATIONS */
	
	YaftUtils.deleteForum = function(forumId,forumTitle)
	{
		if(!confirm(yaft_delete_forum_message_one + "'" + forumTitle + "'" + yaft_delete_forum_message_two))
			return false;
		
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/forums/" + forumId + "/delete",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				switchState('forums');
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to delete forum. Reason: " + status);
			}
		});
		
		return false;
	}
	
	YaftUtils.deleteDiscussion = function(discussionId,discussionTitle)
	{
		if(!confirm(yaft_delete_discussion_message_one + "'" + discussionTitle + "'" + yaft_delete_discussion_message_two))
			return false;
		
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/discussions/" + discussionId + "/delete",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				switchState('forum',{'forumId':yaftCurrentForum.id});
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to delete discussion. Reason: " + status);
			}
		});
		
		return false;
	}
	
	YaftUtils.subscribeToDiscussion = function(discussionId,forumId)
	{
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/discussions/" + discussionId + "/subscribe",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
				{
					if(yaftCurrentForum.discussions[i].id == discussionId)
					{
						yaftCurrentForum.discussions[i].unsubscribed = false;
						
						// Remove this discussion id from the unsubscriptions list
						for(var k=0,m=yaftUnsubscriptions.length;k<m;k++)
						{
							if(yaftUnsubscriptions[k] == discussionId)
								yaftUnsubscriptions.splice(k,1);
						}
					}
				}

				YaftUtils.renderCurrentForumContent();

				//switchState('forum');
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to subscribe to discussion. Reason: " + status);
			}
		});
		
		return false;
	}
	
	YaftUtils.unsubscribeFromDiscussion = function(discussionId,forumId)
	{
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/discussions/" + discussionId + "/unsubscribe",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
				{
					if(yaftCurrentForum.discussions[i].id == discussionId)
					{
						yaftCurrentForum.discussions[i].unsubscribed = true;
						
						// Add this discussion id to the unsubscriptions list
						yaftUnsubscriptions.push(discussionId);
					}
				}

				YaftUtils.renderCurrentForumContent();

				//switchState('forum');
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to unsubscribe from discussion. Reason: " + error);
			}
		});
		
		return false;
	}

	YaftUtils.subscribeToForum = function(forumId)
	{
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/forums/" + forumId + "/subscribe",
	   		dataType : "text",
   			async : false,
			cache: false,
		   	success : function(text,status)
			{
				for(var i=0,j=yaftCurrentForums.items.length;i<j;i++)
				{
					var forum = yaftCurrentForums.items[i];
					if(forum.id == forumId)
					{
						for(var k=0,m=forum.discussions.length;k<m;k++)
							forum.discussions[k]['unsubscribed'] = false;

						forum['unsubscribed'] = false;
					}
				}

				yaftForumUnsubscriptions = YaftUtils.getForumUnsubscriptions();
				yaftUnsubscriptions = YaftUtils.getUnsubscriptions();

				YaftUtils.renderCurrentForums();
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to subscribe to forum. Reason: " + status);
			}
		});

		return false;
	}

	YaftUtils.unsubscribeFromForum = function(forumId)
	{
		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/forums/" + forumId + "/unsubscribe",
	   		dataType : "text",
   			async : false,
			cache: false,
		   	success : function(text,status)
			{
				for(var i=0,j=yaftCurrentForums.items.length;i<j;i++)
				{
					var forum = yaftCurrentForums.items[i];
					if(forum.id == forumId)
					{
						for(var k=0,m=forum.discussions.length;k<m;k++)
							forum.discussions[k]['unsubscribed'] = true;

						forum['unsubscribed'] = true;
					}
				}

				yaftForumUnsubscriptions = YaftUtils.getForumUnsubscriptions();
				yaftUnsubscriptions = YaftUtils.getUnsubscriptions();
				
				YaftUtils.renderCurrentForums();
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to unsubscribe to forum. Reason: " + status);
			}
		});

		return false;
	}
    
    YaftUtils.getDescendantIds = function(messageId,testMessage,descendantIds)
    {
    	for(var i=0,j=testMessage.children.length;i<j;i++)
    	{
    		descendantIds.push(testMessage.children[i].id);
    		YaftUtils.getDescendantIds(messageId,testMessage.children[i],descendantIds);
    	}
    }
	
	YaftUtils.removeAttachment = function(attachmentId,messageId,elementId)
	{
		if(!confirm(yaft_delete_attachment_message))
			return;

		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/messages/" + messageId + "/attachments/" + attachmentId + "/delete",
	   		dataType : "text",
	   		async : false,
			cache: false,
		   	success : function(text,status)
			{
				//if("success".equals(text))
					jQuery("#" + elementId).remove();
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to delete attachment. Reason: " + error);
			}
	  	});
	}

	YaftUtils.showSearchResults = function(searchTerms)
	{
		$("#yaft_add_discussion_link").hide();
		$("#yaft_add_forum_link").hide();
		$("#yaft_hide_deleted_link").hide();
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").hide();
		$("#yaft_permissions_link").hide();
		
		YaftUtils.render('yaft_search_results_breadcrumb_template',{},'yaft_breadcrumb');
		
    	jQuery.ajax(
		{
			url : "/portal/tool/" + yaftPlacementId + "/data/search/" + searchTerms,
        	dataType : "json",
        	async : false,
			cache: false,
        	success : function(results)
       	 	{
        		var params = new Object();
        		params["results"] = results;
				params["searchTerms"] = searchTerms;
				YaftUtils.render('yaft_search_results_content_template',params,'yaft_content');
	 			$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
        	},
        	error : function(xmlHttpRequest,status,error)
			{
			}
		});
	}
	
	YaftUtils.contains = function(list,test)
	{
		for(var i=0,j=list.length;i<j;i++)
		{
			if(test == list[i]) return true;
		}
		
		return false;
	}
	
	YaftUtils.markReadMessagesInFora = function()
	{
		var readMessages = null;
			
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/data/forums/readMessages",
			async : false,
   			dataType: "json",
			cache: false,
			success : function(read,status)
			{
				readMessages = read;
			},
			error : function(xmlHttpRequest,textStatus,error)
			{
				alert("Failed to get read messages. Reason: " + error);
			}
		});
			
		if(readMessages != null)
		{
			var fora = yaftCurrentForums.items;

			for(var i=0,j=fora.length;i<j;i++)
			{
				var forum = fora[i];
				var count = readMessages[forum.id];
				if(count)
					forum['unread'] = forum.messageCount - count;
				else
					forum['unread'] = forum.messageCount;

				if(forum['unread'] < 0) forum['unread'] = 0;
			}
		}
	}
	
	YaftUtils.setUnreadMessageCountForCurrentForum = function()
	{
		var readMessages = null;
			
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/data/forums/" + yaftCurrentForum.id + "/readMessages",
			async : false,
   			dataType: "json",
			cache: false,
			success : function(read,status)
			{
				readMessages = read;
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to get read messages. Reason: " + errorThrown);
			}
		});
			
		if(readMessages != null)
		{
			for(var i=0,j=yaftCurrentForum.discussions.length;i<j;i++)
			{
				var discussion = yaftCurrentForum.discussions[i];
				var count = readMessages[discussion.id];
				if(count)
					discussion['unread'] = discussion.messageCount - count;
				else
					discussion['unread'] = discussion.messageCount;

				if(discussion['unread'] < 0) discussion['unread'] = 0;
			}
		}
	}
	
	YaftUtils.markReadMessagesInCurrentDiscussion = function()
	{
		var readMessages = null;
			
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + yaftPlacementId + "/data/discussions/" + yaftCurrentDiscussion.id + "/readMessages",
			async : false,
   			dataType: "json",
			cache: false,
			success : function(read,status)
			{
				readMessages = read;
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to get read messages. Reason: " + errorThrown);
			}
		});
			
		if(readMessages != null)
		{
			var firstMessage = yaftCurrentDiscussion.firstMessage;
		
			if(YaftUtils.contains(readMessages,firstMessage.id) || firstMessage.creatorId == yaftCurrentUser.id)
				firstMessage['read'] = true;
			else
				firstMessage['read'] = false;
		
			YaftUtils.markReadMessages(firstMessage.children,readMessages);
		}
	}

	YaftUtils.markReadMessages = function(messages,readMessages)
	{
		for(var i=0,j=messages.length;i<j;i++)
        {
            if(YaftUtils.contains(readMessages,messages[i].id) || messages[i].creatorId == yaftCurrentUser.id)
				messages[i]["read"] = true;
			else
				messages[i]["read"] = false;

			YaftUtils.markReadMessages(messages[i].children,readMessages);
        }
	}
	
	YaftUtils.findMessage = function(wantedId)
	{
		var firstMessage = yaftCurrentDiscussion.firstMessage;
		
		if(firstMessage.id == wantedId)
		{
			firstMessage["isFirstMessage"] = true;
			return firstMessage;
		}
		else
			return YaftUtils.recursiveFindMessage(firstMessage.children,wantedId);
	}
	
	YaftUtils.recursiveFindMessage = function(messages,wantedId)
	{
		for(var i=0,j=messages.length;i<j;i++)
        {
			var message = messages[i];
            if(message.id == wantedId)
            {
				message["isFirstMessage"] = false;
                return message;
           	}
          	else
          	{
          		if(message.children.length > 0)
          		{
          			var test = YaftUtils.recursiveFindMessage(message.children,wantedId);
          			if(test == null)
          				continue;
          			else
          			{
						test["isFirstMessage"] = false;
          				return test;
          			}
          		}
          		else
          		{
          		}
          	}
        }
        
        return null;
	}
	
	YaftUtils.getUserPermissions = function(placementId)
	{
		var permissions = null;
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + placementId + "/data/userPermissions",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(perms)
			{
				permissions = perms;
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to get the user permissions. Reason: " + error);
			}
	  	});
	  	
	  	return permissions;
	}

	YaftUtils.getUserPreferences = function(placementId)
	{
		var preferences = null;
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + placementId + "/data/userPreferences",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(prefs)
			{
				preferences = prefs;
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to get the user preferences. Reason: " + error);
			}
	  	});
	  	
	  	return preferences;
	}
	
	YaftUtils.getCurrentUser = function(placementId)
	{
		var user = null;
		
		if(!placementId)
			return null;
			
		jQuery.ajax(
		{
	 		url : "/portal/tool/" + placementId + "/data/currentUser",
	   		dataType : "json",
	   		async : false,
	   		cache : false,
		   	success : function(u)
			{
				user = u;
			},
			error : function(xmlHttpRequest,status,error)
			{
				alert("Failed to get the user. Reason: " + error);
			}
	  	});
	  	
	  	return user;
	}

	YaftUtils.render = function(templateName,contextObject,output)
	{
		var templateNode = document.getElementById(templateName);
		var firstNode = templateNode.firstChild;
		var template = null;
		if ( firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4))
		{
  			template = templateNode.firstChild.data.toString();
   	 	}
		else
		{
   			template = templateNode.innerHTML.toString();
		}

		var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

   		var render = trimpathTemplate.process(contextObject);

		if (output)
		{
			document.getElementById(output).innerHTML = render;
		}

		return render;
	}
	
	YaftUtils.getBaseUrl = function()
	{
		var protocol = jQuery.url.attr("protocol");
		var host = jQuery.url.attr("host");
		var port = jQuery.url.attr("port");
	
		var urlBase = protocol + "://" + host;
		if(port != null)
			urlBase += ":" + port;
		urlBase += "/sakai-yaft-tool/json/";
		
		return urlBase;
	}
	
	YaftUtils.getParameters = function()
	{
		var arg = new Object();
		var href = document.location.href;

		if ( href.indexOf( "?") != -1)
		{
			var paramString = href.split( "?")[1];
			
			if(paramString.indexOf("#") != -1)
				paramString = paramString.split("#")[0];
				
			var params = paramString.split("&");

			for (var i = 0; i < params.length; ++i)
			{
				var name = params[i].split( "=")[0];
				var value = params[i].split( "=")[1];
				arg[name] = unescape(value);
			}
		}
	
		return arg;
	}
	
	YaftUtils.yaftSetupEditor = function(textarea_id,siteId)
	{
		var oFCKeditor = new FCKeditor(textarea_id);

		oFCKeditor.BasePath = "/library/editor/FCKeditor/";
		oFCKeditor.Width  = "800";
		oFCKeditor.Height = "500" ;
		oFCKeditor.ToolbarSet = 'Default' ;
		
		var collectionId = "/group/" + siteId + "/";
		
		oFCKeditor.Config['ImageBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Link&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashBrowserURL'] = oFCKeditor.BasePath + "editor/filemanager/browser/default/browser.html?Connector=/sakai-fck-connector/filemanager/connector&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['ImageUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Image&Command=QuickUpload&Type=Image&CurrentFolder=" + collectionId;
		oFCKeditor.Config['FlashUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=Flash&Command=QuickUpload&Type=Flash&CurrentFolder=" + collectionId;
		oFCKeditor.Config['LinkUploadURL'] = oFCKeditor.BasePath + "/sakai-fck-connector/filemanager/connector?Type=File&Command=QuickUpload&Type=Link&CurrentFolder=" + collectionId;

		oFCKeditor.Config['CurrentFolder'] = collectionId;

		oFCKeditor.Config['CustomConfigurationsPath'] = "/library/editor/FCKeditor/config.js";
		//oFCKeditor.Config['CustomConfigurationsPath'] = "/sakai-yaft-tool/js/config.js";
		oFCKeditor.ReplaceTextarea();
	}

}) ();
