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

(function()
{
	// We need the toolbar in a template so we can swap in the translations
	YaftUtils.render('yaft_toolbar_template',{},'yaft_toolbar');
	
	// This is always showing in every state, so show it here.
	$('#yaft_home_link').show();
	
	var arg = YaftUtils.getParameters();
	
	if(!arg || !arg.placementId || !arg.siteId)
	{
		alert('The placement id and site id MUST be supplied as page parameters');
		return;
	}
	
	// Stuff that we always expect to be setup
	yaftPlacementId = arg.placementId;
	yaftSiteId = arg.siteId;
	yaftCurrentUser = YaftUtils.getCurrentUser(arg.placementId);
	yaftCurrentUserPermissions = YaftUtils.getUserPermissions(arg.placementId);
	yaftCurrentUserPreferences = YaftUtils.getUserPreferences(arg.placementId);
	yaftUnsubscriptions = YaftUtils.getUnsubscriptions();
	yaftForumUnsubscriptions = YaftUtils.getForumUnsubscriptions();
	
	if(yaftCurrentUser != null && yaftCurrentUserPermissions != null)
	{
		// Now switch into the requested state
		switchState(arg.state,arg);
	}
	else
	{
		// TODO: Need to add error message to page
	}
})();

function switchState(state,arg)
{
	$('#yaft_message').hide();

	// If a forum id has been specified we need to refresh the current forum
	// state. We need to do it here as the breadcrumb in various states uses
	// the information.
	if(arg && arg.forumId)
	{
		yaftCurrentForum = YaftUtils.getForum(arg.forumId,"part");
		YaftUtils.setUnreadMessageCountForCurrentForum();
		YaftUtils.setupCurrentForumUnsubscriptions();
	}

	if(state == 'forums')
	{
		if(yaftCurrentUserPermissions.forumCreate)
			$("#yaft_add_forum_link").show();
		else
			$("#yaft_add_forum_link").hide();

		// Site maintainers are the only ones who can change permissions
		if(yaftCurrentUserPermissions.role == 'maintain')
			$("#yaft_permissions_link").show();
		else
			$("#yaft_permissions_link").hide();

		$("#yaft_add_discussion_link").hide();
		$("#yaft_show_deleted_link").hide();
		$("#yaft_hide_deleted_link").hide();
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").hide();

		jQuery.ajax(
		{
	   		url : "/portal/tool/" + yaftPlacementId + "/data/forums",
	   		dataType : "json",
	   		async : false,
			cache: false,
	  		success : function(forums,status)
			{
				yaftCurrentForums = forums;

				YaftUtils.markReadMessagesInFora();

				YaftUtils.setupForumUnsubscriptions();
				
				$("#yaft_breadcrumb").html(yaft_forums_label);
						
				YaftUtils.renderCurrentForums();
			},
			error : function(xmlHttpRequest,textStatus,errorThrown)
			{
				alert("Failed to get forums. Reason: " + errorThrown);
			}
		});
	}
	else if(state == 'forum') 
	{
		// If a forum id has been specified we need to refresh the current forum
		// state
		if(arg && arg.forumId)
		{
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
	  		
		YaftUtils.render('yaft_forum_breadcrumb_template',yaftCurrentForum,'yaft_breadcrumb');

		YaftUtils.renderCurrentForumContent();
	}
	else if(state == 'full') 
	{
		if(arg && arg.discussionId)
		{
			yaftCurrentDiscussion = YaftUtils.getDiscussion(arg.discussionId);
			YaftUtils.markReadMessagesInCurrentDiscussion();
		}
		
		// At this point yaftCurrentForum and yaftCurrentDiscussion must be set
		if(yaftCurrentForum == null) alert("yaftCurrentForum is null");
		if(yaftCurrentDiscussion == null) alert("yaftCurrentDiscussion is null");
			
		yaftViewMode = 'full';

		if(yaftCurrentUserPermissions.messageDeleteAny)
		{
			if(yaftShowingDeleted)
			{
				$("#yaft_show_deleted_link").hide();
				$("#yaft_hide_deleted_link").show();
			}
			else
			{
				$("#yaft_show_deleted_link").show();
				$("#yaft_hide_deleted_link").hide();
			}
		}
		else
		{
			$("#yaft_show_deleted_link").hide();
			$("#yaft_hide_deleted_link").hide();
		}

		$("#yaft_add_discussion_link").hide();
		$("#yaft_add_forum_link").hide();
		$("#yaft_minimal_link").show();
		$("#yaft_full_link").hide();
		$("#yaft_permissions_link").hide();
		
		if(yaftCurrentDiscussion != null)		
		{
			YaftUtils.render('yaft_discussion_breadcrumb_template',yaftCurrentDiscussion,'yaft_breadcrumb');
			YaftUtils.render('yaft_discussion_content_template',yaftCurrentDiscussion,'yaft_content');

			if(!yaftCurrentDiscussion.lockedForReadingAndUnavailable || yaftCurrentUserPermissions.viewInvisible || yaftCurrentDiscussion.creatorId == yaftCurrentUser.id)
			{
				YaftUtils.render('yaft_message_template',yaftCurrentDiscussion.firstMessage,yaftCurrentDiscussion.firstMessage.id);
				renderChildMessages(yaftCurrentDiscussion.firstMessage);
			}
			
			if(yaftCurrentUserPermissions.messageDeleteAny)
			{
				if(yaftShowingDeleted)
					$(".yaft_deleted_message").show();
				else
					$(".yaft_deleted_message").hide();
			}
		}
		
	   	$(document).ready(function()
	   	{
  			$('a.profile').cluetip({local: true
									,width: '320px'
									,hoverIntent: {    
									    interval:     50,
									    timeout:      0
								     } 
									,cluetipClass: 'jtip'
  									,dropShadow: false});

	   		if(arg && arg.messageId)
	   		{
				//alert('Scrolling to ' + arg.messageId + ' ...');
	   			window.location.hash = arg.messageId;
				$.scrollTo("#"+ arg.messageId);
	   		}

	   		setMainFrameHeight(window.frameElement.id);
	   	});
	}
	else if(state == 'minimal') 
	{
		if(arg && arg.discussionId)
		{
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

		YaftUtils.render('yaft_message_view_breadcrumb_template',yaftCurrentDiscussion,'yaft_breadcrumb');
		
		var message = null;
		if(arg != null && arg.messageId != null)
			message = YaftUtils.findMessage(arg.messageId);
		else
			message = yaftCurrentDiscussion.firstMessage;

		YaftUtils.render('yaft_message_view_content_template',yaftCurrentDiscussion,'yaft_content');
		YaftUtils.render('yaft_message_template',yaftCurrentDiscussion.firstMessage,yaftCurrentDiscussion.firstMessage.id);
		renderChildMessages(yaftCurrentDiscussion.firstMessage,true);

		$('#' + message.id).show();
					
		$("#yaft_minimal_link").hide();
		$("#yaft_full_link").show();
  		$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	}
	else if(state == 'editForum')
	{
		var forum = {'id':'','title':'','description':'',start: -1,end: -1};

		if(arg && arg.forumId)
			forum = YaftUtils.findForum(arg.forumId);

		YaftUtils.render('yaft_edit_forum_breadcrumb_template',arg,'yaft_breadcrumb');
		YaftUtils.render('yaft_edit_forum_content_template',forum,'yaft_content');

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
	
	 	$(document).ready(function()
	 		{
	 			setMainFrameHeight(window.frameElement.id);
	 		});
	}
	else if(state == 'editMessage')
	{
		var message = YaftUtils.findMessage(arg.messageId);
		message['editMode'] = 'EDIT';
		YaftUtils.render('yaft_edit_message_breadcrumb_template',message,'yaft_breadcrumb');
		YaftUtils.render('yaft_edit_message_content_template',message,'yaft_content');
		$("#yaft_permissions_link").hide();

		jQuery(document).ready(function()
	    {
	    	$('#yaft_attachment').MultiFile(
		            {
		            	max: 5,
					    namePattern: '$name_$i'
					});
		    YaftUtils.yaftSetupEditor('fck',yaftSiteId);
		    setMainFrameHeight(window.frameElement.id);
	    });
	}
	else if(state == 'reply')
	{
		if(!arg || !arg.messageBeingRepliedTo)
		{
		}
		$("#yaft_permissions_link").hide();
		
		// Look up the message that we are replying to in the current cache
		var messageBeingRepliedTo = YaftUtils.findMessage(arg.messageBeingRepliedTo);
						
		// We need to pass a few extra things to the template, so set them.
		messageBeingRepliedTo["mode"] = arg.mode;
		messageBeingRepliedTo["editMode"] = 'REPLY';
		YaftUtils.render('yaft_edit_message_breadcrumb_template',messageBeingRepliedTo,'yaft_breadcrumb');
		YaftUtils.render('yaft_reply_message_content_template',messageBeingRepliedTo,'yaft_content');
	  	jQuery(document).ready(function()
	  	{
			$('#yaft_attachment').MultiFile(
			{
				max: 5,
				namePattern: '$name_$i'
			});
				
			YaftUtils.yaftSetupEditor('fck',yaftSiteId);
	 		setMainFrameHeight(window.frameElement.id);
	 	});
	}
	else if(state == 'startDiscussion')
	{
		$("#yaft_permissions_link").hide();

		var discussion = {'id':'','subject':'',lockedForWriting:yaftCurrentForum.lockedForWriting,lockedForReading:yaftCurrentForum.lockedForReading,start: yaftCurrentForum.start,end: yaftCurrentForum.end,'firstMessage':{'content':''}};

		if(arg && arg.discussionId)
			discussion = YaftUtils.findDiscussion(arg.discussionId);

		YaftUtils.render('yaft_start_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
		YaftUtils.render('yaft_start_discussion_content_template',discussion,'yaft_content');

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

   		$(document).ready(function()
   		{
	    	$('#yaft_attachment').MultiFile(
		            {
		            	max: 5,
					    namePattern: '$name_$i'
					});
			YaftUtils.yaftSetupEditor('fck',yaftSiteId);
   			setMainFrameHeight(window.frameElement.id);
   		});
	}
	else if(state == 'moveDiscussion')
	{
		$("#yaft_permissions_link").hide();
		YaftUtils.render('yaft_move_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
		
		jQuery.ajax(
		{
	   	url : "/portal/tool/" + yaftPlacementId + "/data/forums",
	   	dataType : "json",
	   	async : false,
		cache: false,
	  	success : function(forums)
		{
			jQuery.ajax(
			{
	   			url : "/portal/tool/" + yaftPlacementId + "/data/discussions/" + arg.discussionId,
	   			dataType : "json",
	   			async : false,
				cache: false,
	  			success : function(discussion)
				{
					forums["discussion"] = discussion;
				},
				error : function(xmlHttpRequest,textStatus,errorThrown)
				{
					alert("Failed to get discussion. Reason: " + errorThrown);
				}
			});
			YaftUtils.render('yaft_move_discussion_content_template',forums,'yaft_content');
		
	 		$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
		},
		error : function(xmlHttpRequest,textStatus,errorThrown)
		{
			alert("Failed to get forums. Reason: " + errorThrown);
		}
	});
	}
	else if(state == 'permissions')
	{
		$("#yaft_permissions_link").hide();
		YaftUtils.render('yaft_permissions_breadcrumb_template',arg,'yaft_breadcrumb');
	
		jQuery.ajax(
		{
	       	url : "/portal/tool/" + yaftPlacementId + "/data/permissions",
	       	dataType : "json",
	       	async : false,
			cache: false,
		   	success : function(permissions)
			{
				YaftUtils.render('yaft_permissions_content_template',{'permissions':permissions},'yaft_content');
	 			$(document).ready(function()
					{
						$("tbody tr:odd").attr('class', 'yaftEvenRow');
						setMainFrameHeight(window.frameElement.id);
					});
			},
			error : function(xmlHttpRequest,status,errorThrown)
			{
				alert("Failed to get permissions. Reason: " + errorThrown);
			}
	   	});
	}
	else if(state == 'preferences')
	{
		YaftUtils.render('yaft_preferences_breadcrumb_template',arg,'yaft_breadcrumb');
		YaftUtils.render('yaft_preferences_template',{},'yaft_content');
		$('#yaft_email_' + yaftCurrentUserPreferences.email + '_option').attr('checked',true);
		$('#yaft_view_' + yaftCurrentUserPreferences.view + '_option').attr('checked',true);
	 	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
	}
	
	return false;
}

function renderChildMessages(parent,skipDeleted)
{
	var children = parent.children;
	
	for(var i=0,j=children.length;i<j;i++)
	{
		var message = children[i];

		if(message.status !== 'DELETED' || !skipDeleted) {
			YaftUtils.render('yaft_message_template',message,message.id);
		}

		renderChildMessages(message,skipDeleted);
	}
}

/**
 *	Used when in the minimal view
 */
function yaftShowMessage(messageId)
{
	var message = YaftUtils.findMessage(messageId);
	$('.yaft_full_message').hide();
	$('#' + message.id).show();
   	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
}

function setupAvailability(element)
{
	var startDate = $('#yaft_start_date');
	var endDate = $('#yaft_end_date');

	if(element.start != -1 && element.end != -1)
	{
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
