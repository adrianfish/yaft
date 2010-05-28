/* Stuff that we always expect to be setup */
var yaftPlacementId = null;
var yaftSiteId = null;
var yaftCurrentUser = null;
var yaftCurrentUserPermissions = null;
var yaftCurrentUserPreferences = null;
var yaftUnsubscriptions = null;
var yaftForumUnsubscriptions = null;
var yaftCurrentForums = null;

/* State specific stuff */
var yaftCurrentForum = null;
var yaftCurrentDiscussion = null;
var yaftViewMode = "full";
var yaftShowingDeleted = false;

(function() {
	// We need the toolbar in a template so we can swap in the translations
	SakaiUtils.renderTrimpathTemplate('yaft_toolbar_template',{},'yaft_toolbar');

	$('#yaft_home_link').bind('click',function(e) {
		switchState('forums');
	});

	$('#yaft_add_forum_link').bind('click',function(e) {
		switchState('editForum');
	});

	$('#yaft_add_discussion_link').bind('click',function(e) {
		switchState('startDiscussion');
	});

	$('#yaft_permissions_link').bind('click',function(e) {
		switchState('permissions');
	});

	$('#yaft_minimal_link').bind('click',function(e) {
		switchState('minimal');
	});

	$('#yaft_full_link').bind('click',function(e) {
		switchState('full');
	});

	$('#yaft_show_deleted_link').bind('click',YaftUtils.showDeleted);
	$('#yaft_hide_deleted_link').bind('click',YaftUtils.hideDeleted);

	$('#yaft_preferences_link').bind('click',function(e) {
		switchState('preferences');
	});

	$('#yaft_search_field').change(function(e) {
		YaftUtils.showSearchResults(e.target.value);
	});
	
	// This is always showing in every state, so show it here.
	$('#yaft_home_link').show();
	
	var arg = SakaiUtils.getParameters();
	
	if(!arg || !arg.placementId || !arg.siteId) {
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}
	
	// Stuff that we always expect to be setup
	yaftPlacementId = arg.placementId;
	yaftSiteId = arg.siteId;
	yaftCurrentUser = SakaiUtils.getCurrentUser();

	yaftCurrentUserPermissions = new YaftPermissions(SakaiUtils.getCurrentUserPermissions(yaftSiteId,'yaft'));

	yaftCurrentUserPreferences = YaftUtils.getUserPreferences(arg.placementId);
	yaftUnsubscriptions = YaftUtils.getUnsubscriptions();
	yaftForumUnsubscriptions = YaftUtils.getForumUnsubscriptions();
	
	if(yaftCurrentUser != null && yaftCurrentUserPermissions != null) {
		// Now switch into the requested state
		switchState(arg.state,arg);
	} else {
		// TODO: Need to add error message to page
	}
})();

function switchState(state,arg) {

	$('#yaft_message').hide();

	// If a forum id has been specified we need to refresh the current forum
	// state. We need to do it here as the breadcrumb in various states uses
	// the information.
	if(arg && arg.forumId) {

		yaftCurrentForum = YaftUtils.getForum(arg.forumId,"part");
		YaftUtils.setUnreadMessageCountForCurrentForum();
		YaftUtils.setupCurrentForumUnsubscriptions();
	}

	if('forums' === state) {

		if(yaftCurrentUserPermissions.forumCreate)
			$("#yaft_add_forum_link").show();
		else
			$("#yaft_add_forum_link").hide();

		if(yaftCurrentUserPermissions.modifyPermissions)
			$("#yaft_permissions_link").show();
		else
			$("#yaft_permissions_link").hide();

		$("#yaft_add_discussion_link").hide();
		$("#yaft_show_deleted_link").hide();
		$("#yaft_hide_deleted_link").hide();
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").hide();

		jQuery.ajax( {
	   		//url : "/portal/tool/" + yaftPlacementId + "/data/forums",
	   		url : "/direct/yaft-forum.json?siteId=" + yaftSiteId,
	   		dataType : "json",
	   		async : false,
			cache: false,
	  		success : function(forums,status) {
				yaftCurrentForums = forums['yaft-forum_collection'];

				YaftUtils.markReadMessagesInFora();

				YaftUtils.setupForumUnsubscriptions();
				
				$("#yaft_breadcrumb").html(yaft_forums_label);
						
				YaftUtils.renderCurrentForums();
			},
			error : function(xmlHttpRequest,textStatus,errorThrown) {
				alert("Failed to get forums. Reason: " + errorThrown);
			}
		});
	}
	else if('forum' === state) {
		// If a forum id has been specified we need to refresh the current forum
		// state
		if(arg && arg.forumId) {
			yaftCurrentForum = YaftUtils.getForum(arg.forumId,"part");
			YaftUtils.setUnreadMessageCountForCurrentForum();
			YaftUtils.setupCurrentForumUnsubscriptions();
		}
		
		if(yaftCurrentUserPermissions.discussionCreate && (!yaftCurrentForum.lockedForWritingAndUnavailable || yaftCurrentUserPermissions.viewInvisible || yaftCurrentUser.id == yaftCurrentForum.creatorId))
			$("#yaft_add_discussion_link").show();
		else
			$("#yaft_add_discussion_link").hide();

		$("#yaft_add_forum_link").hide();
		$("#yaft_show_deleted_link").hide();
		$("#yaft_hide_deleted_link").hide();
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").hide();
			$("#yaft_permissions_link").hide();
	  		
		SakaiUtils.renderTrimpathTemplate('yaft_forum_breadcrumb_template',yaftCurrentForum,'yaft_breadcrumb');

		YaftUtils.renderCurrentForumContent();
	}
	else if('full' === state) {
		if(arg && arg.discussionId) {
			yaftCurrentDiscussion = YaftUtils.getDiscussion(arg.discussionId);
			YaftUtils.markReadMessagesInCurrentDiscussion();
		}
		
		// At this point yaftCurrentForum and yaftCurrentDiscussion must be set
		if(yaftCurrentForum == null) alert("yaftCurrentForum is null");
		if(yaftCurrentDiscussion == null) alert("yaftCurrentDiscussion is null");
			
		yaftViewMode = 'full';

		if(yaftCurrentUserPermissions.messageDeleteAny) {
			if(yaftShowingDeleted) {
				$("#yaft_show_deleted_link").hide();
				$("#yaft_hide_deleted_link").show();
			} else {
				$("#yaft_show_deleted_link").show();
				$("#yaft_hide_deleted_link").hide();
			}
		}
		else {
			$("#yaft_show_deleted_link").hide();
			$("#yaft_hide_deleted_link").hide();
		}

		$("#yaft_add_discussion_link").hide();
		$("#yaft_add_forum_link").hide();
		$("#yaft_minimal_link").show();
		$("#yaft_full_link").hide();
		$("#yaft_permissions_link").hide();
		
		if(yaftCurrentDiscussion != null) {
			SakaiUtils.renderTrimpathTemplate('yaft_discussion_breadcrumb_template',yaftCurrentDiscussion,'yaft_breadcrumb');
			SakaiUtils.renderTrimpathTemplate('yaft_discussion_content_template',yaftCurrentDiscussion,'yaft_content');

			if(!yaftCurrentDiscussion.lockedForReadingAndUnavailable || yaftCurrentUserPermissions.viewInvisible || yaftCurrentDiscussion.creatorId == yaftCurrentUser.id) {
				SakaiUtils.renderTrimpathTemplate('yaft_message_template',yaftCurrentDiscussion.firstMessage,yaftCurrentDiscussion.firstMessage.id);
				renderChildMessages(yaftCurrentDiscussion.firstMessage);
			}
			
			if(yaftCurrentUserPermissions.messageDeleteAny) {
				if(yaftShowingDeleted)
					$(".yaft_deleted_message").show();
				else
					$(".yaft_deleted_message").hide();
			}
		}
		
	   	$(document).ready(function() {
			YaftUtils.attachProfilePopup();

	   		if(arg && arg.messageId) {
				//alert('Scrolling to ' + arg.messageId + ' ...');
	   			window.location.hash = arg.messageId;
				$.scrollTo("#"+ arg.messageId);
	   		}

	   		setMainFrameHeight(window.frameElement.id);
	   	});
	}
	else if('minimal' === state) {
		if(arg && arg.discussionId) {
			yaftCurrentDiscussion = YaftUtils.getDiscussion(arg.discussionId);
			YaftUtils.markReadMessagesInCurrentDiscussion();
		}
			
		// At this point yaftCurrentForum and yaftCurrentDiscussion must be set
		if(yaftCurrentForum == null) alert("yaftCurrentForum is null");
		if(yaftCurrentDiscussion == null) alert("yaftCurrentDiscussion is null");
			
		yaftViewMode = 'minimal';

		$("#yaft_add_discussion_link").hide();
		$("#yaft_show_deleted_link").hide();
		$("#yaft_permissions_link").hide();
		
		var message = null;
		if(arg != null && arg.messageId != null) {
			alert(arg.messageId);
			message = YaftUtils.findMessage(arg.messageId);
			alert(message.subject);
		}
		else
			message = yaftCurrentDiscussion.firstMessage;

		SakaiUtils.renderTrimpathTemplate('yaft_message_view_breadcrumb_template',yaftCurrentDiscussion,'yaft_breadcrumb');

		SakaiUtils.renderTrimpathTemplate('yaft_message_view_content_template',yaftCurrentDiscussion,'yaft_content');
		SakaiUtils.renderTrimpathTemplate('yaft_message_template',yaftCurrentDiscussion.firstMessage,yaftCurrentDiscussion.firstMessage.id);
		renderChildMessages(yaftCurrentDiscussion.firstMessage,true);
		YaftUtils.attachProfilePopup();

		$('#' + message.id).show();
					
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").show();
  		$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	}
	else if('editForum' === state) {
		var forum = {'id':'','title':'','description':'',start: -1,end: -1};

		if(arg && arg.forumId)
			forum = YaftUtils.findForum(arg.forumId);

		SakaiUtils.renderTrimpathTemplate('yaft_edit_forum_breadcrumb_template',arg,'yaft_breadcrumb');
		SakaiUtils.renderTrimpathTemplate('yaft_edit_forum_content_template',forum,'yaft_content');

		$('#yaft_edit_forum_form').submit(function()
									{
										if($('#yaft_title_field').val() == '')
										{
											var message = $('#yaft_message');
											message.html(yaft_missing_title_message);
											message.show();
											return false;
										}
										else
											return true;
									});

		setupAvailability(forum);

		$("#yaft_permissions_link").hide();
	
	 	$(document).ready(function() {
	 			$('#yaft_title_field').focus();
	 			setMainFrameHeight(window.frameElement.id);
	 		});
	}
	else if('editMessage' === state) {
		var message = YaftUtils.findMessage(arg.messageId);
		message['editMode'] = 'EDIT';
		SakaiUtils.renderTrimpathTemplate('yaft_edit_message_breadcrumb_template',message,'yaft_breadcrumb');
		SakaiUtils.renderTrimpathTemplate('yaft_edit_message_content_template',message,'yaft_content');
		$("#yaft_permissions_link").hide();

		jQuery(document).ready(function() {
	    	$('#yaft_attachment').MultiFile(
		            {
		            	max: 5,
					    namePattern: '$name_$i'
					});
		    SakaiUtils.setupFCKEditor('fck',800,500,'Default',yaftSiteId);
		    setMainFrameHeight(window.frameElement.id);
	    });
	}
	else if('reply' === state) {
		$("#yaft_permissions_link").hide();
		
		// Look up the message that we are replying to in the current cache
		var messageBeingRepliedTo = YaftUtils.findMessage(arg.messageBeingRepliedTo);
						
		// We need to pass a few extra things to the template, so set them.
		messageBeingRepliedTo["mode"] = arg.mode;
		messageBeingRepliedTo["editMode"] = 'REPLY';
		SakaiUtils.renderTrimpathTemplate('yaft_edit_message_breadcrumb_template',messageBeingRepliedTo,'yaft_breadcrumb');
		SakaiUtils.renderTrimpathTemplate('yaft_reply_message_content_template',messageBeingRepliedTo,'yaft_content');
	  	jQuery(document).ready(function() {
			$('#yaft_attachment').MultiFile(
			{
				max: 5,
				namePattern: '$name_$i'
			});
				
			SakaiUtils.setupFCKEditor('fck',800,500,'Default',yaftSiteId);
	 		setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if('startDiscussion' === state) {
		$("#yaft_permissions_link").hide();

		var discussion = {'id':'','subject':'',lockedForWriting:yaftCurrentForum.lockedForWriting,lockedForReading:yaftCurrentForum.lockedForReading,start: yaftCurrentForum.start,end: yaftCurrentForum.end,'firstMessage':{'content':''}};

		if(arg && arg.discussionId)
			discussion = YaftUtils.findDiscussion(arg.discussionId);

		SakaiUtils.renderTrimpathTemplate('yaft_start_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
		SakaiUtils.renderTrimpathTemplate('yaft_start_discussion_content_template',discussion,'yaft_content');

		$('#yaft_start_discussion_form').submit(function()
									{
										if($('#yaft_subject_field').val() == '')
										{
											var message = $('#yaft_message');
											message.html(yaft_missing_subject_message);
											message.show();
											return false;
										}
										else
											return true;
									});

		setupAvailability(discussion);

   		$(document).ready(function() {
	    	$('#yaft_attachment').MultiFile(
		            {
		            	max: 5,
					    namePattern: '$name_$i'
					});
			SakaiUtils.setupFCKEditor('fck',800,500,'Default',yaftSiteId);
			$('#yaft_subject_field').focus();
   			setMainFrameHeight(window.frameElement.id);
   		});
	}
	else if('moveDiscussion' === state) {
		$("#yaft_permissions_link").hide();
		SakaiUtils.renderTrimpathTemplate('yaft_move_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
		
		jQuery.ajax( {
			url : "/portal/tool/" + yaftPlacementId + "/data/forums",
			dataType : "json",
			async : false,
			cache: false,
			success : function(forums) {
				jQuery.ajax( {
					url : "/portal/tool/" + yaftPlacementId + "/data/discussions/" + arg.discussionId,
					dataType : "json",
					async : false,
					cache: false,
					success : function(discussion) {
						forums["discussion"] = discussion;
					},
					error : function(xmlHttpRequest,textStatus,errorThrown) {
						alert("Failed to get discussion. Reason: " + errorThrown);
					}
				});
				SakaiUtils.renderTrimpathTemplate('yaft_move_discussion_content_template',forums,'yaft_content');
		
				$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
			},
			error : function(xmlHttpRequest,textStatus,errorThrown) {
				alert("Failed to get forums. Reason: " + errorThrown);
			}
		});
	}
	else if('permissions' === state) {
		var perms = SakaiUtils.getSitePermissionMatrix(yaftSiteId,'yaft');
		SakaiUtils.renderTrimpathTemplate('yaft_permissions_content_template',{'perms':perms},'yaft_content');

	 	$(document).ready(function() {
			$('#yaft_permissions_save_button').bind('click',function(e) {
				return SakaiUtils.savePermissions(yaftSiteId,'yaft_permission_checkbox',function() { switchState('forums'); });
			});

			if(window.frameElement)
				setMainFrameHeight(window.frameElement.id);
		});
	}
	else if('preferences' === state) {
		SakaiUtils.renderTrimpathTemplate('yaft_preferences_breadcrumb_template',arg,'yaft_breadcrumb');
		SakaiUtils.renderTrimpathTemplate('yaft_preferences_template',{},'yaft_content');
		$('#yaft_email_' + yaftCurrentUserPreferences.email + '_option').attr('checked',true);
		$('#yaft_view_' + yaftCurrentUserPreferences.view + '_option').attr('checked',true);
	 	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	}
	
	return false;
}

function renderChildMessages(parent,skipDeleted) {
	var children = parent.children;
	
	for(var i=0,j=children.length;i<j;i++) {
		var message = children[i];

		if(message.status !== 'DELETED' || !skipDeleted) {
			SakaiUtils.renderTrimpathTemplate('yaft_message_template',message,message.id);
		}

		renderChildMessages(message,skipDeleted);
	}
}

/**
 *	Used when in the minimal view
 */
function yaftShowMessage(messageId) {
	var message = YaftUtils.findMessage(messageId);
	$('.yaft_full_message').hide();
	$('#' + message.id).show();
   	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
}

function setupAvailability(element) {
	var startDate = $('#yaft_start_date');
	var endDate = $('#yaft_end_date');

	if(element.start != -1 && element.end != -1) {
		startDate.attr('disabled',false);
		startDate.css('background-color','white');
		$('#yaft_start_hour_selector').attr('disabled',false);
		$('#yaft_start_hour_selector').css('background-color','white');
		$('#yaft_start_minute_selector').attr('disabled',false);
		$('#yaft_start_minute_selector').css('background-color','white');
		endDate.attr('disabled',false);
		endDate.css('background-color','white');
		$('#yaft_end_hour_selector').attr('disabled',false);
		$('#yaft_end_hour_selector').css('background-color','white');
		$('#yaft_end_minute_selector').attr('disabled',false);
		$('#yaft_end_minute_selector').css('background-color','white');

		var start = new Date(element.start);
		startDate.val(start.getDate() + ' ' + (1 + start.getMonth()) + ' ' + start.getFullYear());

		var hours = start.getHours();
		if(hours < 10)  hours = '0' + hours;
		var minutes = start.getMinutes();
		if(minutes == 0) minutes += '0';

		$('#yaft_start_hour_selector option:contains(' + hours + ')').attr('selected','selected');
		$('#yaft_start_minute_selector option:contains(' + minutes + ')').attr('selected','selected');

		var end = new Date(element.end);
		endDate.val(end.getDate() + ' ' + (1 + end.getMonth()) + ' ' + end.getFullYear());

		hours = end.getHours();
		if(hours < 10)  hours = '0' + hours;
			minutes = end.getMinutes();
		if(minutes == 0) minutes += '0';

		$('#yaft_end_hour_selector option:contains(' + hours + ')').attr('selected','selected');
		$('#yaft_end_minute_selector option:contains(' + minutes + ')').attr('selected','selected');
	}

	var writingCheckbox = $('#yaft_lock_writing_checkbox');

	var readingCheckbox = $('#yaft_lock_reading_checkbox');

	if(element.lockedForWriting)
		$('#yaft_lock_writing_checkbox').attr('checked',true);

	if(element.lockedForReading)
		$('#yaft_lock_reading_checkbox').attr('checked',true);

	startDate.datepicker({
		dateFormat: 'dd mm yy',
		defaultDate: new Date(),
		minDate: new Date(),
		hideIfNoPrevNext: true
	});

	endDate.datepicker({
		dateFormat: 'dd mm yy',
		defaultDate: new Date(),
		minDate: new Date(),
		hideIfNoPrevNext: true
	});
}
