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

(function ($) {

    yaft.fitFrame = function () {

        try {
            if (window.frameElement) {
                setMainFrameHeight(window.frameElement.id);
            }
        } catch (err) {
            // This is likely under an LTI provision scenario.
            // XSS protection will block this call.
        }
    };

    yaft.switchState = function (state, arg) {
    
        $('#yaft_toolbar > li > span').removeClass('current');
    
        $("#yaft_minimal_link").hide();
        $("#yaft_full_link").hide();
        $("#yaft_show_deleted_link").hide();
        $("#yaft_hide_deleted_link").hide();
        $("#yaft_add_discussion_link").hide();
        $("#yaft_authors_view_link").hide();
    
        $('#yaft_feedback_message').hide();
    
        $('#cluetip').hide();
    
        // If a forum id has been specified we need to refresh the current forum
        // state. We need to do it here as the breadcrumb in various states uses
        // the information.
        if (arg && arg.forumId) {
            yaft.currentForum = yaft.utils.getForum(arg.forumId, "part");
            yaft.utils.addFormattedDatesToDiscussionsInCurrentForum();
        }
    
        if ('forums' === state) {
            $('#yaft_home_link > span').addClass('current');
    
            if (yaft.currentUserPermissions.forumCreate) {
                $("#yaft_add_forum_link").show();
            } else {
                $("#yaft_add_forum_link").hide();
            }
    
            $("#yaft_breadcrumb").html(yaft_forums_label);
            
            yaft.utils.setCurrentForums(/* render = */ true);
        } else if ('authors' === state) {
            $('#yaft_authors_view_link').show();
            $('#yaft_authors_view_link > span').addClass('current');
    
            $("#yaft_breadcrumb").html(yaft_authors_label);
            yaft.utils.getAuthors();
        } else if ('author' === state) {
            if (!arg.id) {
                return false;
            }
    
            yaft.util.renderTrimpathTemplate('yaft_author_messages_breadcrumb_template', {'id': arg.id, 'displayName': arg.displayName}, 'yaft_breadcrumb');
    
            yaft.utils.showAuthorPosts(arg.id);
        } else if ('forum' === state) {
            
            if (!yaft.currentForums) {
                yaft.utils.setCurrentForums();
            }
            
            if (yaft.currentUserPermissions.discussionCreate && (!yaft.currentForum.lockedForWritingAndUnavailable || yaft.currentUserPermissions.viewInvisible || yaft.startupArgs.userId === yaft.currentForum.creatorId)) {
                $("#yaft_add_discussion_link").show();
            } else {
                $("#yaft_add_discussion_link").hide();
            }
    
            $("#yaft_add_forum_link").hide();
            $("#yaft_show_deleted_link").hide();
            $("#yaft_hide_deleted_link").hide();
            $("#yaft_minimal_link").hide();
            $("#yaft_full_link").hide();
                 
            yaft.utils.renderTrimpathTemplate('yaft_forum_breadcrumb_template', yaft.currentForum, 'yaft_breadcrumb');
    
            yaft.utils.renderCurrentForumContent();
        } else if ('full' === state) {
            if (arg && arg.discussionId) {
                yaft.currentDiscussion = yaft.utils.getDiscussion(arg.discussionId);
                yaft.utils.markReadMessagesInCurrentDiscussion();
                yaft.utils.addFormattedDatesToCurrentDiscussion();
            }
            
            // At this point yaft.currentForum and yaft.currentDiscussion must be set
            if (!yaft.currentForum) alert("yaft.currentForum is null");
            if (!yaft.currentDiscussion) alert("yaft.currentDiscussion is null");
                
            yaftViewMode = 'full';
    
            if (yaft.currentUserPermissions.messageDeleteAny) {
                if (yaft.showingDeleted) {
                    $("#yaft_show_deleted_link").hide();
                    $("#yaft_hide_deleted_link").show();
                } else {
                    $("#yaft_show_deleted_link").show();
                    $("#yaft_hide_deleted_link").hide();
                }
            } else {
                $("#yaft_show_deleted_link").hide();
                $("#yaft_hide_deleted_link").hide();
            }
    
            $("#yaft_add_discussion_link").hide();
            $("#yaft_add_forum_link").hide();
            $("#yaft_minimal_link").show();
            $("#yaft_full_link").hide();
            $("#yaft_authors_view_link").show();
            
            if (yaft.currentDiscussion) {
                yaft.utils.renderTrimpathTemplate('yaft_discussion_breadcrumb_template',yaft.currentDiscussion,'yaft_breadcrumb');
                yaft.utils.renderTrimpathTemplate('yaft_discussion_content_template',yaft.currentDiscussion,'yaft_content');
    
                if (!yaft.currentDiscussion.lockedForReadingAndUnavailable || yaft.currentUserPermissions.viewInvisible || yaft.currentDiscussion.creatorId === yaft.startupArgs.userId) {
                    yaft.utils.renderTrimpathTemplate('yaft_message_template',yaft.currentDiscussion.firstMessage,yaft.currentDiscussion.firstMessage.id);
                    yaft.renderChildMessages(yaft.currentDiscussion.firstMessage);
                }
                
                if (yaft.currentUserPermissions.messageDeleteAny) {
                    if (yaft.showingDeleted) {
                        $(".yaft_deleted_message").show();
                    } else {
                        $(".yaft_deleted_message").hide();
                    }
                }
            }
            
            $(document).ready(function () {
    
                yaft.utils.attachProfilePopup();
    
                if (arg && arg.messageId) {
                    window.location.hash = "message-" + arg.messageId;
                }
    
                yaft.fitFrame();
            });
        } else if ('minimal' === state) {
            if (arg && arg.discussionId) {
                yaft.currentDiscussion = yaft.utils.getDiscussion(arg.discussionId);
                yaft.utils.markReadMessagesInCurrentDiscussion();
                yaft.utils.addFormattedDatesToCurrentDiscussion();
            }
    
            if (!yaft.currentDiscussion) {
                if (arg && arg.messageId) {
                    yaft.currentDiscussion = yaft.utils.getDiscussionContainingMessage(arg.messageId);
                    yaft.utils.markReadMessagesInCurrentDiscussion();
                }
            }
    
            if (!yaft.currentForum) {
                if (arg && arg.messageId) {
                    yaft.currentForum = yaft.utils.getForumContainingMessage(arg.messageId);
                }
            }
                
            // At this point yaft.currentForum and yaft.currentDiscussion must be set
            if (!yaft.currentForum) alert("yaft.currentForum is null");
            if (!yaft.currentDiscussion) alert("yaft.currentDiscussion is null");
                
            yaftViewMode = 'minimal';
    
            $("#yaft_add_discussion_link").hide();
            $("#yaft_show_deleted_link").hide();
            $("#yaft_authors_view_link").show();
            
            var message = null;
            if (arg != null && arg.messageId != null) {
                message = yaft.utils.findMessage(arg.messageId);
            } else {
                message = yaft.currentDiscussion.firstMessage;
            }
    
            yaft.utils.renderTrimpathTemplate('yaft_message_view_breadcrumb_template', yaft.currentDiscussion, 'yaft_breadcrumb');
    
            yaft.utils.renderTrimpathTemplate('yaft_message_view_content_template', yaft.currentDiscussion, 'yaft_content');
            yaft.utils.renderTrimpathTemplate('yaft_message_template', yaft.currentDiscussion.firstMessage, yaft.currentDiscussion.firstMessage.id);
            $('#' + yaft.currentDiscussion.firstMessage.id + '_link').hide();
            yaft.renderChildMessages(yaft.currentDiscussion.firstMessage, true);
            yaft.utils.attachProfilePopup();
    
            $('#' + message.id).show();
                        
            $("#yaft_minimal_link").hide();
            $("#yaft_full_link").show();
            $(document).ready(function () {
                yaft.fitFrame();
            });
        } else if ('editForum' === state) {
            $('#yaft_add_forum_link > span').addClass('current');
    
            var forum = {'id': '','title': '','description': '',start: -1,end: -1,'groups': []};
    
            if (arg && arg.forumId) {
                // This is an edit of a current forum.
                forum = yaft.utils.findForum(arg.forumId);
            }
    
            var siteGroups = yaft.utils.getGroupsForCurrentSite();
    
            yaft.utils.renderTrimpathTemplate('yaft_edit_forum_breadcrumb_template', arg, 'yaft_breadcrumb');
            yaft.utils.renderTrimpathTemplate('yaft_edit_forum_content_template',{'forum': forum, 'siteGroups': siteGroups}, 'yaft_content');
    
            this.setupAvailability(forum);
        
            $(document).ready(function () {

                // If this forum is already group limited, show the groups options.
                if (forum.groups.length > 0) {
                    $('#yaft_group_options').show();
                }
                for (var i=0,j=forum.groups.length;i<j;i++) {
                    $('#' + forum.groups[i].id).prop('checked',true);
                }
    
                $('#yaft_title_field').focus();
                $('#yaft_forum_save_button').click(yaft.utils.saveForum);
                     
                $('#yaft_toggle_group_options_link').click(function () {
    
                    $('#yaft_group_options').toggle();
                    yaft.fitFrame();
                });
    
                $('#yaft_toggle_availability_options_link').click(function () {

                    $('#yaft_availability_options').toggle();
                    yaft.fitFrame();
                });
    
                if (arg && arg.forumId) {
                    // This is an edit of a current forum.
                    $('#yaft_send_email_checkbox').prop('checked', false);
                }
                    
                $('#yaft_title_field').keypress(function (e) {

                    if (e.keyCode == '13') { // Enter key
                        yaft.utils.saveForum();
                    }
                });

                $('#yaft_description_field').keypress(function (e) {

                    if (e.keyCode == '13') { // Enter key
                        yaft.utils.saveForum();
                    }
                });
    
                yaft.fitFrame();
            });
        } else if ('editMessage' === state) {
    
            var message = yaft.utils.findMessage(arg.messageId);
            message['editMode'] = 'EDIT';
            yaft.utils.renderTrimpathTemplate('yaft_edit_message_breadcrumb_template',message,'yaft_breadcrumb');
            yaft.utils.renderTrimpathTemplate('yaft_edit_message_content_template',message,'yaft_content');
            
            var saveMessageOptions = { 
                    dataType: 'text',
                    timeout: 30000,
                    iframe: true,
                    success: function (responseText, statusText, xhr) {

                        yaft.currentDiscussion = yaft.utils.getDiscussion(yaft.currentDiscussion.id);
                        yaft.utils.markReadMessagesInCurrentDiscussion();
                        yaft.utils.addFormattedDatesToCurrentDiscussion();
                        yaft.switchState('full');
                    },
                    beforeSubmit: function (arr, $form, options) {

                        if ($('#yaft_message_editor').val() === '') {
                            alert(yaft_missing_message_message);
                            return false;
                        }
                               
                        return true;
                    },
                    error : function (xmlHttpRequest, textStatus, errorThrown) {}
                }; 
         
            $('#yaft_message_form').ajaxForm(saveMessageOptions);
            $('#yaft_message_form').bind('form-pre-serialize', function (event, $form, formOptions, veto) {
                
                var data = yaft.sakai.getEditorData(yaft.startupArgs.editor, 'yaft_message_editor');
        
                if (data == '') {
                    $('#yaft_message_editor').val('');
                } else {
                    yaft.sakai.updateEditorElement(yaft.startupArgs.editor, 'yaft_message_editor');
                }
            });
        
            $(document).ready(function () {
        
                $('#yaft_attachment').MultiFile({
                    max: 5,
                    namePattern: '$name_$i'
                });
                yaft.sakai.setupWysiwygEditor(yaft.startupArgs.editor, 'yaft_message_editor', 800, 500);
            });
        } else if ('reply' === state) {
            
            // Look up the message that we are replying to in the current cache
            var messageBeingRepliedTo = yaft.utils.findMessage(arg.messageBeingRepliedTo);
                            
            // We need to pass a few extra things to the template, so set them.
            messageBeingRepliedTo["mode"] = arg.mode;
            messageBeingRepliedTo["editMode"] = 'REPLY';
            yaft.utils.renderTrimpathTemplate('yaft_edit_message_breadcrumb_template', messageBeingRepliedTo, 'yaft_breadcrumb');
            yaft.utils.renderTrimpathTemplate('yaft_reply_message_content_template', messageBeingRepliedTo, 'yaft_content');
    
            $('#yaft_publish_anonymously_button').click(function (e) {
    
                $('#yaft_anonymous_flag').val('true');
                $('#yaft_message_form').submit();
                return false;
            });
    
            var saveMessageOptions = { 
                    dataType: 'text',
                    timeout: 30000,
                    iframe: true,
                    success: function (responseText, statusText, xhr) {
    
                        yaft.currentDiscussion = yaft.utils.getDiscussion(yaft.currentDiscussion.id);
                        yaft.utils.markReadMessagesInCurrentDiscussion();
                        yaft.utils.addFormattedDatesToCurrentDiscussion();
                        yaft.switchState('full');
                    },
                    beforeSubmit: function (arr, $form, options) {
    
                        if ($('#yaft_message_editor').val() === '') {
                            alert(yaft_missing_message_message);
                            return false;
                        }
                           
                        return true;
                    },
                    error : function (xmlHttpRequest, textStatus, errorThrown) {}
                };
     
            $('#yaft_message_form')
                .ajaxForm(saveMessageOptions)
                .bind('form-pre-serialize', function (event, $form, formOptions, veto) {
    
                    var data = yaft.sakai.getEditorData(yaft.startupArgs.editor,'yaft_message_editor');
            
                    if (data == '') {
                        $('#yaft_message_editor').val('');
                    } else {
                        yaft.sakai.updateEditorElement(yaft.startupArgs.editor, 'yaft_message_editor');
                    }
            });
    
            $(document).ready(function () {
    
                var replySubject = messageBeingRepliedTo.subject;
                if ( ! replySubject.match(/^Re:.*/) ) {
                    replySubject = 'Re: ' + messageBeingRepliedTo.subject;
                }
                  
                $("#yaft_message_subject_field").val(replySubject);
                
                $('#yaft_attachment').MultiFile({
                    max: 5,
                    namePattern: '$name_$i'
                });
                    
                yaft.sakai.setupWysiwygEditor(yaft.startupArgs.editor, 'yaft_message_editor', 800, 500);
             });
        } else if ('startDiscussion' === state) {
            $('#yaft_add_discussion_link').show();
            $('#yaft_add_discussion_link > span').addClass('current');
    
            var discussion =
                            {
                                'id': '',
                                'subject': '',
                                'allowAnonymousPosting': false,
                                'lockedForWriting': yaft.currentForum.lockedForWriting,
                                'lockedForReading': yaft.currentForum.lockedForReading,
                                'start': yaft.currentForum.startDate,
                                'end': yaft.currentForum.endDate,
                                'firstMessage': {'content': ''},
                                'grade': false,
                                'groups': [],
                                'groupsInherited': (yaft.currentForum.groups.length > 0)
                            };
    
            if (arg && arg.discussionId) {
                discussion = yaft.utils.findDiscussion(arg.discussionId);
            }
            
            var groups = yaft.utils.getGroupsForCurrentSite();
    
            yaft.utils.renderTrimpathTemplate('yaft_start_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
            yaft.utils.renderTrimpathTemplate('yaft_start_discussion_content_template',{'discussion':discussion,'groups':groups},'yaft_content');
    
            if (discussion.allowAnonymousPosting) {
                $('#yaft_allow_anonymous_posting_checkbox').prop('checked', true);
            }
            
            var saveDiscussionOptions = { 
                    dataType: 'html',
                    iframe: true,
                    timeout: 30000,
                    beforeSerialize: function ($form, options) {
    
                        var startDate = (+$('#yaft_start_date_millis').val());
                        var startHours = (+$('#yaft_start_hours').val());
                        var startMinutes = (+$('#yaft_start_minutes').val());
                        startDate += (startHours * 3600000) + (startMinutes * 60000);
                        $('#yaft_start_date_millis').val(startDate);
    
                        var endDate = (+$('#yaft_end_date_millis').val());
                        var endHours = (+$('#yaft_end_hours').val());
                        var endMinutes = (+$('#yaft_end_minutes').val());
                        endDate += (endHours * 3600000) + (endMinutes * 60000);
                        $('#yaft_end_date_millis').val(endDate);
                    },
                    beforeSubmit: function (arr, $form, options) {
    
                        for (var i=0,j=arr.length;i<j;i++) {
                            if ('subject' === arr[i].name) {
                                if (!arr[i].value || arr[i].value.length < 4) {
                                    alert(yaft_subject_too_short);
                                    return false;
                                }
                             }
                        }
                        if ($('#yaft_discussion_editor').val() === '') {
                            alert(yaft_missing_message_message);
                            return false;
                        }
                           
                        return true;
                    },
                    success: function (responseText, statusText, xhr) {
    
                        if (responseText.match(/^ERROR.*/)) {
                            alert(yaft_failed_to_create_edit_discussion + ". Reason: " + responseText);
                        } else {
                            var discussion = yaft.utils.getDiscussion(responseText);
                            if (!arg || !arg.discussionId) {
                                yaft.currentForum.discussions.push(discussion);
                            } else {
                                for (var i=0,j=yaft.currentForum.discussions.length;i<j;i++) {
                                    if (responseText === yaft.currentForum.discussions[i].id) {
                                        yaft.currentForum.discussions.splice(i,1,discussion);
                                        break;
                                    }
                                }
                            }
                            yaft.switchState('forum');
                        }
                    },
                    error: function (xmlHttpRequest, textStatus, errorThrown) {
                        alert(yaft_failed_to_create_edit_discussion);
                    }
                }; 
     
            $('#yaft_discussion_form').ajaxForm(saveDiscussionOptions);
            $('#yaft_discussion_form').bind('form-pre-serialize', function (event, $form, formOptions, veto) {

                var data = yaft.sakai.getEditorData(yaft.startupArgs.editor,'yaft_discussion_editor');

                if (data == '') {
                    $('#yaft_discussion_editor').val('');
                } else {
                    yaft.sakai.updateEditorElement(yaft.startupArgs.editor,'yaft_discussion_editor');
                }
            });

            this.setupAvailability(discussion);

            $(document).ready(function () {

                // If this discussion is already group limited, show the groups options.
                if (discussion.groups.length > 0) {
                    $('#yaft_group_options').show();
                }
                for (var i=0,j=discussion.groups.length;i<j;i++) {
                    $('#' + discussion.groups[i].id).prop('checked', true);
                }
            
                $('#yaft_toggle_group_options_link').click(function () {

                    $('#yaft_group_options').toggle();
                    yaft.fitFrame();
                });

                $('#yaft_toggle_availability_options_link').click(function () {

                    $('#yaft_availability_options').toggle();
                    yaft.fitFrame();
                });

                $('#yaft_toggle_gradebook_options_link').click(function () {

                    $('#yaft_gradebook_options').toggle();
                    yaft.fitFrame();
                });
                if (arg && arg.discussionId) {
                    // This is an edit of a current discussion.
                    $('#yaft_send_email_checkbox').prop('checked', false);
                }
                $('#yaft_attachment').MultiFile({
                        max: 5,
                        namePattern: '$name_$i'
                });
                $('#yaft_subject_field').focus();

                if (yaft.gradebookAssignments && yaft.gradebookAssignments.length > 0) {
                    $('#yaft_grading_fieldset').show();
                }

                $('#yaft_grade_checkbox').click(function () {

                    if (this.checked === true) {
                        $('#yaft_assignment_selector').show();
                    } else {
                        $('#yaft_assignment_selector').hide();
                    }
                });

                if (discussion.assignment) {
                    $('#yaft_grade_checkbox').prop('checked', true);
                    $('#yaft_assignment_selector').show();
                    $("#yaft_assignment_selector option[value='" + discussion.assignment.id + "']").attr('selected', 'selected');
                }
            
                yaft.sakai.setupWysiwygEditor(yaft.startupArgs.editor, 'yaft_discussion_editor', 800, 500);
            });
        } else if ('moveDiscussion' === state) {
            yaft.utils.renderTrimpathTemplate('yaft_move_discussion_breadcrumb_template',arg,'yaft_breadcrumb');
            
            yaft.utils.setCurrentForums();
            var discussion = null;
            $.ajax( {
                url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + arg.discussionId + ".json",
                dataType: "json",
                async: false,
                cache: false,
                success: function (d, textStatus) {
                    discussion = d;
                },
                error: function (xhr, textStatus, errorThrown) {
                    alert(yaft_failed_to_get_discussion + errorThrown);
                }
            });
                    
            yaft.utils.renderTrimpathTemplate('yaft_move_discussion_content_template',{'forums':yaft.currentForums,'discussion':discussion},'yaft_content');
            
            $(document).ready(function () {

                var moveDiscussionOptions = { 
                        dataType: 'text',
                        timeout: 30000,
                        async: false,
                        success: function (responseText, statusText, xhr) {
                            yaft.switchState('forums');
                        },
                        error : function (xmlHttpRequest, textStatus, errorThrown) {}
                    }; 
     
                $('#yaft_move_discussion_form').ajaxForm(moveDiscussionOptions);
    
                yaft.fitFrame();
            });
        } else if ('permissions' === state) {
    
            $('#yaft_permissions_link > span').addClass('current');
            var perms = yaft.utils.getSitePermissionMatrix();
            yaft.utils.renderTrimpathTemplate('yaft_permissions_breadcrumb_template',arg,'yaft_breadcrumb');
            yaft.utils.renderTrimpathTemplate('yaft_permissions_content_template',{'perms':perms},'yaft_content');
    
             $(document).ready(function () {
    
                $('#yaft_permissions_save_button').click(function (e) {
                    return yaft.utils.savePermissions();
                });
    
                $('#yaft_permissions_table tbody tr:odd').addClass('yaft_odd_row');
    
                yaft.fitFrame();
            });
        }
    
        return false;
    };
    
    yaft.renderChildMessages = function (parent, skipDeleted) {
    
        var children = parent.children;
        
        for (var i=0,j=children.length;i<j;i++) {
            var message = children[i];
    
            if (message.status !== 'DELETED' || !skipDeleted) {
                var element = document.getElementById(message.id);
                if (element) {
                    yaft.utils.renderTrimpathTemplate('yaft_message_template',message,message.id);
                }
            }
    
            this.renderChildMessages(message,skipDeleted);
        }
    };
    
    /**
     *    Used when in the minimal view
     */
    yaft.showMessage = function (messageId) {
    
        var message = yaft.utils.findMessage(messageId);
        $('.yaft_full_message').hide();
        $('.yaft_message_minimised').show();
        $('#' + message.id).show();
        $('#' + message.id + '_link').hide();

        $(document).ready(function () {
            yaft.fitFrame();
        });
    };
    
    yaft.setupAvailability = function (element) {
    
        var startDate = $('#yaft_start_date');
    
        startDate.datepicker({
            altField: '#yaft_start_date_millis',
            altFormat: '@',
            defaultDate: new Date(),
            minDate: new Date(),
            hideIfNoPrevNext: true
        });
    
        var endDate = $('#yaft_end_date');
    
        endDate.datepicker({
            altField: '#yaft_end_date_millis',
            altFormat: '@',
            defaultDate: new Date(),
            minDate: new Date(),
            hideIfNoPrevNext: true
        });
    
        if (element.start > 0 && element.end > 0) {
            startDate.prop('disabled', false);
            startDate.css('background-color','white');
            $('#yaft_start_hour_selector').prop('disabled', false);
            $('#yaft_start_hour_selector').css('background-color','white');
            $('#yaft_start_minute_selector').prop('disabled', false);
            $('#yaft_start_minute_selector').css('background-color','white');
            endDate.prop('disabled', false);
            endDate.css('background-color','white');
            $('#yaft_end_hour_selector').prop('disabled', false);
            $('#yaft_end_hour_selector').css('background-color','white');
            $('#yaft_end_minute_selector').prop('disabled', false);
            $('#yaft_end_minute_selector').css('background-color','white');
    
            var test = new Date(element.start);
            var localOffset = test.getTimezoneOffset() * 60000;
    
            var start = new Date(element.start + localOffset);
            startDate.datepicker("setDate",start);
            startDate.val(start.toString(Date.CultureInfo.formatPatterns.shortDate));
    
            var hours = start.getHours();
            if (hours < 10)  hours = '0' + hours;
            var minutes = start.getMinutes();
            if (minutes == 0) minutes += '0';
    
            $('#yaft_start_hours option:contains(' + hours + ')').attr('selected','selected');
            $('#yaft_start_minutes option:contains(' + minutes + ')').attr('selected','selected');
    
            var end = new Date(element.end + localOffset);
            endDate.datepicker("setDate",end);
            endDate.val(end.toString(Date.CultureInfo.formatPatterns.shortDate));
    
            hours = end.getHours();
            if (hours < 10) {
                hours = '0' + hours;
            }
    
            minutes = end.getMinutes();
            if (minutes == 0) {
                minutes += '0';
            }
    
            $('#yaft_end_hours option:contains(' + hours + ')').attr('selected','selected');
            $('#yaft_end_minutes option:contains(' + minutes + ')').attr('selected','selected');
    
        }
    
        var writingCheckbox = $('#yaft_lock_writing_checkbox');
    
        var readingCheckbox = $('#yaft_lock_reading_checkbox');
        
        if (element.lockedForWriting) {
            $('#yaft_lock_writing_checkbox').prop('checked', true);
            $('#yaft_availability_options').show();
        }
    
        if (element.lockedForReading) {
            $('#yaft_lock_reading_checkbox').prop('checked', true);
            $('#yaft_availability_options').show();
        }
    };

    if (yaft.startupArgs.onPDAPortal) {
        yaft.baseDataUrl = "/portal/pda/" + yaft.startupArgs.siteId + "/tool/" + yaft.startupArgs.placementId + "/";
    } else {
        yaft.baseDataUrl = "/portal/tool/" + yaft.startupArgs.placementId + "/";
    }

    var locale = sakai.locale.userLanguage;

    if (typeof sakai.locale.userCountry !== 'undefined' && sakai.locale.userCountry !== '') {
        locale += '-' + sakai.locale.userCountry;
    }
    
    //$.localise('yaft-translations',{language: locale,loadBase: true,path: '/yaft-tool/'});

    var dpUrl = '/yaft-tool/lib/jquery-ui/i18n/jquery.ui.datepicker-' + locale + ".js";
    
    $.ajax({
        url: dpUrl,
        dataType: "script",
        success: function () {},
        error: function () {

            // The full code failed. Try just the language code.
            var dpUrl = '/yaft-tool/lib/jquery-ui/i18n/jquery.ui.datepicker-' + sakai.locale.userLanguage + ".js";
            $.getScript(dpUrl);
        }
    });

    var djsUrl = "/yaft-tool/lib/datejs/date-" + locale + ".js";
    
    $.ajax({
        url: djsUrl,
        async: false,
        dataType: "script",
        error: function () {

            var djsUrl = "/yaft-tool/lib/datejs/date-" + sakai.locale.userLanguage + ".js";
            $.getScript(djsUrl);
        }
    });
    
    // We need the toolbar in a template so we can swap in the translations
    yaft.utils.renderTrimpathTemplate('yaft_toolbar_template', {}, 'yaft_toolbar');

    $('#yaft_home_link > span > a').click(function (e) {
        yaft.switchState('forums');
    });

    $('#yaft_add_forum_link > span > a').click(function (e) {
        yaft.switchState('editForum');
    });

    $('#yaft_add_discussion_link > span > a').click(function (e) {
        yaft.switchState('startDiscussion');
    });

    $('#yaft_permissions_link > span > a').click(function (e) {
        yaft.switchState('permissions');
    });

    $('#yaft_minimal_link > span > a').click(function (e) {
        yaft.switchState('minimal');
    });

    $('#yaft_full_link > span > a').click(function (e) {
        yaft.switchState('full');
    });

    $('#yaft_show_deleted_link > span > a').click(yaft.utils.showDeleted);
    $('#yaft_hide_deleted_link > span > a').click(yaft.utils.hideDeleted);

    $('#yaft_authors_view_link > span > a').click(function (e) {
        yaft.switchState('authors');
    });
    
    // This is always showing in every state, so show it here.
    $('#yaft_home_link').show();

    $('#yaft_search_field').change(function (e) {
        yaft.utils.showSearchResults(e.target.value);
    });
    
    var data = yaft.utils.getCurrentUserData();

    yaft.gradebookAssignments = data.assignments ? data.assignments : [];

    yaft.currentUserPermissions = new YaftPermissions(data.permissions);

    if (yaft.currentUserPermissions.modifyPermissions) {
        $("#yaft_permissions_link").show();
        $("#yaft_settings_link").show();
    } else {
        $("#yaft_permissions_link").hide();
        $("#yaft_settings_link").hide();
    }

    if (yaft.startupArgs.userId !== '' && yaft.currentUserPermissions != null) {
        // Now switch into the requested state
        yaft.switchState(yaft.startupArgs.state, yaft.startupArgs);
    } else {
        // TODO: Need to add error message to page
    }
}) (jQuery);
