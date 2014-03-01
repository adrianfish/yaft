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

    yaft.utils = {};

    var addFormattedDatesToFora = function () {

        $.each(yaft.currentForums, function (index, forum) {

            if (forum.start > -1) {
                forum.formattedStartDate = yaft.utils.formatDate(forum.start);
            }
            if (forum.end > -1) {
                forum.formattedEndDate = yaft.utils.formatDate(forum.end);
            }
            if (forum.lastMessageDate > -1) {
                forum.formattedLastMessageDate = yaft.utils.formatDate(forum.lastMessageDate);
            } else {
                forum.formattedLastMessageDate = 'n/a';
            }
        });
    };
        
    yaft.utils.setCurrentForums = function (render) {

        $.ajax({
            url: yaft.baseDataUrl + "forums.json",
            dataType: "json",
            async: false,
            cache: false,
            success: function (forums, textStatus) {

                yaft.currentForums = forums;

                markReadMessagesInFora();

                $.each(forums, function (index, forum) {

                    if ( (yaft.currentUserPermissions.viewInvisible || forum.creatorId === yaft.startupArgs.userId)
                            && (forum.lockedForReadingAndUnavailable || forum.lockedForWritingAndUnavailable) ) {
                        forum.invisible = true;
                    }

                    if (yaft.currentUserPermissions.forumDeleteAny || (yaft.currentUserPermissions.forumDeleteOwn && forum.creatorId === yaft.startupArgs.userId)) {
                        forum.canDelete = true;
                    }

                    if (forum.groups && forum.groups.length > 0) {
                        forum.groups[forum.groups.length - 1].last = true;
                    }
                });

                addFormattedDatesToFora();

                if (render) {
                    yaft.utils.renderCurrentForums();
                }
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to set current forums. Reason: " + errorThrown);
            }
        });
    };
        
    yaft.utils.saveForum = function () {
    
        var title = $('#yaft_title_field').val();
        
        if (title === '') {
            alert(yaft_missing_title_message);
            return;
        }
        
        var description = $('#yaft_description_field').val();
        
        if (description.length > 255) {
            alert(yaft_description_too_long_message);
            return;
        }

        var startDate = (+$('#yaft_start_date_millis').val());
        var startHours = (+$('#yaft_start_hours').val());
        var startMinutes = (+$('#yaft_start_minutes').val());
        startDate += (startHours * 3600000) + (startMinutes * 60000);

        var endDate = (+$('#yaft_end_date_millis').val());
        var endHours = (+$('#yaft_end_hours').val());
        var endMinutes = (+$('#yaft_end_minutes').val());
        endDate += (endHours * 3600000) + (endMinutes * 60000);

        // Get any selected groups
        var groupBoxes = $('.yaft_group_checkbox:checked');

        var groups = '';

        $.each(groupBoxes, function (index, groupBox) {

            groups += groupBox.id;
            if (i < j) groups += ',';
        });

        var forum = {
                'siteId': yaft.startupArgs.siteId,
                'id': $('#yaft_id_field').val(),
                'startDate': startDate,
                'endDate': endDate,
                'title': title,
                'description': description,
                'sendEmail': $('#yaft_send_email_checkbox').prop('checked') ? 'true' : 'false',
                'lockWriting': $('#yaft_lock_writing_checkbox').prop('checked') ? 'true' : 'false',
                'lockReading': $('#yaft_lock_reading_checkbox').prop('checked') ? 'true' : 'false',
                'discussions': [],
                'groups': groups
            };
           
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/forums",
            dataType: "text",
            type: 'POST',
            'data': forum,
            async: false,
            cache: false,
            success: function (id, textStatus) {

                if ('' === forum.id) {
                    // New forum. Enter it.
                    yaft.currentForum = forum;
                    yaft.currentForum.id = id;
                    yaft.switchState('forum');
                } else {
                    yaft.switchState('forums');
                }
            },
            error: function (xhr, textStatus, errorThrown) {
                var message = $('#yaft_feedback_message');
                message.html('Failed to save forum');
                message.show();
            }
        });
    };
    
    yaft.utils.showDeleted = function () {

        $(".yaft_deleted_message").show();

        $(document).ready(function () {
            yaft.fitFrame();
        });
        yaft.showingDeleted = true;
        $('#yaft_show_deleted_link').hide();
        $('#yaft_hide_deleted_link').show();
    };

    yaft.utils.clearDates = function () {

        $('#yaft_start_date').val('');
        $('#yaft_end_date').val('');
        $('#yaft_start_hours').get(0).selectedIndex = 0;
        $('#yaft_start_minutes').get(0).selectedIndex = 0;
        $('#yaft_end_hours').get(0).selectedIndex = 0;
        $('#yaft_end_minutes').get(0).selectedIndex = 0;
        $('#yaft_start_date_millis').val('');
        $('#yaft_end_date_millis').val('');
    };
    
    yaft.utils.getGroupsForCurrentSite = function () {

        var groups = null;

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/siteGroups",
            dataType: "json",
            cache: false,
            async: false,
            success: function (data, textStatus) {
                groups = data;
            },
            error: function (xhr, textStatus, errorThrown) {
            }
        });

        return groups;
    };
    
    yaft.utils.clearActiveDiscussionsForCurrentUser = function () {

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/activeDiscussions/clear",
            dataType: "text",
            cache: false,
            async: false,
            success: function (text, status) {
                $('.yaft_active_discussion_row').hide();
            },
            error: function (xhr, textStatus, errorThrown) {
                alert('failed');
            }
        });
    };

    yaft.utils.hideDeleted = function () {

        $(".yaft_deleted_message").hide();
           $(document).ready(function () {
            yaft.fitFrame();
        });
        yaft.showingDeleted = false;
        $('#yaft_show_deleted_link').show();
        $('#yaft_hide_deleted_link').hide();
    };

    yaft.utils.renderCurrentForumContent = function () {

        var discussions = yaft.currentForum.discussions.filter(function (discussion) {

            if (!discussion.lockedForReadingAndUnavailable
                    || yaft.currentUserPermissions.viewInvisible
                    || discussion.creatorId === yaft.startupArgs.userId) {
                return true;
            }
        });

        this.renderHandlebarsTemplate('forum', {discussions: discussions, viewMode: yaft.startupArgs.viewMode, discussionDeleteAny: yaft.currentUserPermissions.discussionDeleteAny}, 'yaft_content');
            
        $(document).ready(function () {

            if (discussions.length <= 0) {
                $('#yaft-get-started-instruction').show();
            } else {
                $("#yaft_discussion_table").tablesorter({
                    cssHeader: 'yaftSortableTableHeader',
                    cssAsc: 'yaftSortableTableHeaderSortUp',
                    cssDesc: 'yaftSortableTableHeaderSortDown',
                    headers:
                        {
                            4: {sorter: "isoDate"},
                            5: {sorter: false},
                            6: {sorter: false}
                        },
                        sortList: [[0,0]],
                        widgets: ['zebra']
                });

                $('#yaft-delete-discussions-button').click(function () {
                    yaft.utils.deleteSelectedDiscussions();
                });

                $('#yaft-all-discussions-checkbox').show().click(function (e) {

                    if ($(this).prop('checked')) {
                        $('.yaft_bulk_option_candidate').prop('checked', true);
                        $('#yaft-delete-discussions-button').prop('disabled', false);
                    } else {
                        $('.yaft_bulk_option_candidate').prop('checked', false);
                        $('#yaft-delete-discussions-button').prop('disabled', true);
                    }
                });

                $('.yaft_bulk_option_candidate').click(function () {

                    if ($(this).prop('checked')) {
                        $('#yaft-delete-discussions-button').prop('disabled', false).addClass('enabled');
                    } else {
                        // Only enable the delete button if no other discussion is checked
                        if ($('.yaft_bulk_option_candidate:checked').length <= 0) {
                            $('#yaft-delete-discussions-button').prop('disabled', true).removeClass('enabled');
                        }
                    }
                });

                yaft.utils.attachProfilePopup();
            }

            yaft.fitFrame();
        });
    };
    
    yaft.utils.attachProfilePopup = function () {

        $('a.profile').cluetip({
            width: '640px',
            cluetipClass: 'yaft',
            sticky: true,
            dropShadow: false,
            arrows: true,
            mouseOutClose: true,
            closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
            closePosition: 'top',
            showTitle: false,
            hoverIntent: true
        });
    };

    yaft.utils.deleteSelectedFora  = function () {
    
        if (!confirm(yaft.translations.delete_selected_fora_message)) {
            return;
        }
        
        var candidates = $('.yaft_bulk_option_candidate:checked');

        var forumIds = [];

        $.each(candidates, function (index, candidate) {
            forumIds.push(candidate.id);
        });

        $.ajax( {
            url: yaft.baseDataUrl + "forums/delete",
            type: 'POST',
            dataType: "text",
            async: false,
            cache: false,
            data: { 'forumIds[]': forumIds },
            success: function (results, textStatus) {
                yaft.utils.setCurrentForums(/*render=*/true);
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete forums. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });
    };
    
    yaft.utils.deleteSelectedDiscussions  = function () {
    
        if (!confirm(yaft.translations.delete_selected_discussions_message)) {
            return;
        }
        
        var candidates = $('.yaft_bulk_option_candidate:checked');

        var discussionIds = [];

        $.each(candidates, function (index, candidate) {
            discussionIds.push(candidate.id);
        });

        $.ajax({
            url: yaft.baseDataUrl + "discussions/delete",
            type: 'POST',
            dataType: "text",
            async: false,
            cache: false,
            data: {'discussionIds[]': discussionIds},
            success: function (results, textStatus) {
                yaft.switchState('forum', {'forumId': yaft.currentForum.id});
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete discussions. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });
    };
    
    yaft.utils.renderCurrentForums = function () {

        var fora = yaft.currentForums.filter(function (forum, index, beingFiltered) {

            if (!forum.lockedForReadingAndUnavailable
                    || yaft.currentUserPermissions.viewInvisible
                    || forum.creatorId === yaft.startupArgs.userId) {
                return true;
            }
            
        });

        this.renderHandlebarsTemplate('forums', {'fora': fora, forumDeleteAny: yaft.currentUserPermissions.forumDeleteAny}, 'yaft_content');

        $(document).ready(function () {

            if (yaft.currentForums.length <= 0) {
                $('#yaft-get-started-instruction').show();
            } else {
                $("#yaft_forum_table").tablesorter({
                    cssHeader: 'yaftSortableTableHeader',
                    cssAsc: 'yaftSortableTableHeaderSortUp',
                    cssDesc: 'yaftSortableTableHeaderSortDown',
                    headers:
                        {
                            4: {sorter: 'isoDate'},
                            5: {sorter: false},
                            6: {sorter: false}
                        },
                    sortList: [[0, 0]],
                    widgets: ['zebra']
                });

                if (yaft.currentUserPermissions.forumDeleteAny) {
                    $('#yaft-all-fora-checkbox').show().click(function (e) {

                        if ($(this).prop('checked')) {
                            $('.yaft_bulk_option_candidate').prop('checked', true);
                            $('#yaft-delete-forums-button').prop('disabled', false);
                        } else {
                            $('.yaft_bulk_option_candidate').prop('checked', false);
                            $('#yaft-delete-forums-button').prop('disabled', true);
                        }
                    });

                    $('#yaft-delete-forums-button').show().click(function () {
                        yaft.utils.deleteSelectedFora();
                    });
                }

                $('.yaft_bulk_option_candidate').click(function () {

                    if ($(this).prop('checked')) {
                        $('#yaft-delete-forums-button').prop('disabled', false);
                    } else {
                        // Only enable the delete button if no other forum is checked
                        if ($('.yaft_bulk_option_candidate:checked').length <= 0) {
                            $('#yaft-delete-forums-button').prop('disabled', true);
                        }
                    }
                });
            }
            
            yaft.fitFrame();
        });
    };
    
    yaft.utils.validateReplySubject = function (originalSubject) {

        //originalSubject = unescape(originalSubject);

        var subject = $("#yaft_message_subject_field").val();

        if (subject) {
            return true;
        } else {
            if (confirm(yaft.translations.empty_message_subject_message)) {
                if (originalSubject.match(/^Re: /) == null) {
                    $("#yaft_message_subject_field").val('Re: ' + originalSubject);
                } else {
                    $("#yaft_message_subject_field").val(originalSubject);
                }

                return true;
            } else {
                return false;
            }
        }
    };
    
    yaft.utils.getForum = function (forumId, state) {

        var forumDataUrl = yaft.baseDataUrl + "forums/" + forumId + ".json";
        if (state != null) forumDataUrl += "?state=" + state;
    
        var currentForum = null;
        
        $.ajax({
            url: forumDataUrl,
            dataType: "json",
            cache: false,
            async: false,
            success: function (data) {

                currentForum = data.forum;

                $.each(currentForum.discussions, function (index, discussion) {

                    var count = data.counts[discussion.id];
                    if (count) {
                        discussion['unread'] = discussion.messageCount - count;
                    } else {
                        discussion['unread'] = discussion.messageCount;
                    }

                    if (discussion['unread'] < 0) {
                        discussion['unread'] = 0;
                    }

                    // Now set up the last group flag.
                    if(discussion.groups && discussion.groups.length > 0) {
                        discussion.groups[discussion.groups.length - 1].last = true;
                    }
                    if (yaft.currentUserPermissions.discussionDeleteAny
                            || (yaft.currentUserPermissions.discussionDeleteOwn && discussion.creatorId === yaft.startupArgs.userId)) {
                        discussion.canDelete = true;
                    }
                    if (yaft.currentForums && yaft.currentForums.length > 1 && yaft.currentUserPermissions.discussionCreate
                            && ((yaft.currentUserPermissions.discussionDeleteOwn && discussion.creatorId === yaft.startupArgs.userId)
                                    || yaft.currentUserPermissions.discussionDeleteAny)) {
                        discussion.canMove = true;
                    }
                });
            },
            error: function (xhr, textStatus, errorThrown) {}
        });
        
        return currentForum;
    };
    
    yaft.utils.getDiscussion = function (discussionId) {

        var discussion = null;
        
        $.ajax({
            url: yaft.baseDataUrl + "discussions/" + discussionId + ".json",
            dataType: "json",
            cache: false,
            async: false,
            success: function (d) {
                discussion = d;
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get discussion. Reason: " + xhr.statusText);
            }
        });
        
        return discussion;
    };

    yaft.utils.getDiscussionContainingMessage = function (messageId) {

        var discussion = null;
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/discussionContainingMessage?messageId=" + messageId,
            dataType: "json",
            cache: false,
            async: false,
            success: function (d) {
                discussion = d;
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get discussion. Reason: " + xhr.statusText);
            }
        });
        
        return discussion;
    };

    yaft.utils.getForumContainingMessage = function (messageId) {

        var forum = null;
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/forumContainingMessage?messageId=" + messageId,
            dataType: "json",
            cache: false,
            async: false,
            success: function (d) {
                forum = d;
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get forum. Reason: " + xhr.statusText);
            }
        });
        
        return forum;
    };

    yaft.utils.likeAuthor = function (authorId) {

        $.ajax({
            url: "/direct/likeservice/" + authorId + "/addLike",
            dateType: "text",
            cache: false,
            success: function (response) {
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to like author. Reason: " + xhr.statusText);
             }
        });
    };
    
    /* START MESSAGE OPERATIONS */
    
    yaft.utils.deleteMessage = function (messageId) {

        if (!confirm(yaft.translations.delete_message_message)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, textStatus) {

                var message = yaft.utils.findMessageInCurrentDiscussion(messageId);
                message.status = 'DELETED';
                message.deleted = true;
                if (message.id == yaft.currentDiscussion.firstMessage.id) {
                    message['isFirstMessage'] = true;
                } else {
                    message['isFirstMessage'] = false;
                }

                yaft.renderMessage(message);

                if (yaftViewMode === 'minimal') {
                    $('#' + message.id + '_link').hide();
                    $('#' + message.id).hide();
                }

                $(document).ready(function () {
                    yaft.utils.attachProfilePopup();
                });
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete message. Reason: " + textStatus);
            }
        });
        
        return false;
    };

    yaft.utils.findForum = function (id) {

        var forum = null;

        $.each(yaft.currentForums, function (index, testForum) {
            if (testForum.id == id) forum = testForum;
        });

        return forum;
    };

    yaft.utils.findDiscussion = function (id) {

        var discussion = null;

        $.each(yaft.currentForum.discussions, function (index, testDiscussion) {
            if (testDiscussion.id == id) discussion = testDiscussion;
        });

        return discussion;
    };
    
    yaft.utils.undeleteMessage = function (messageId) {

        if (!confirm(yaft.translations.undelete_message_message)) {
            return false;
        }
        
        $.ajax( {
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/undelete",
            dateType: "text",
            async : false,
            cache: false,
            success: function (text, textStatus) {
                var message = yaft.utils.findMessageInCurrentDiscussion(messageId);
                message.status = 'READY';
                message.ready = true;
                message.deleted = false;
                if (message.id === yaft.currentDiscussion.firstMessage.id) {
                    message.isFirstMessage = true;
                } else {
                    message.isFirstMessage = false;
                }

                yaft.renderMessage(message);

                $(document).ready(function () {
                    yaft.utils.attachProfilePopup();
                });
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to un-delete message. Reason: " + textStatus);
            }
        });
        
        return false;
    };

    var getFlattenedDescendantMessages = function (ancestor) {

        var descendants = [];

        var recurse = function (parent) {

            $.each(parent.children, function (index, child) {

                descendants.push(child);
                recurse(child);
            });
        };

        recurse(ancestor);

        return descendants;
    };
    
    yaft.utils.toggleMessage = function (e, messageId) {

        var message = this.findMessageInCurrentDiscussion(messageId);

        var descendants = getFlattenedDescendantMessages(message);

        if (message.collapsed) {
            $.each(descendants, function (index, descendant) {

                $("#" + descendant.id + ' .yaft_collapse_expand_link').html(yaft.translations.collapse_label);
                $("#" + descendant.id).show();
                descendant.collapsed = false;
            });

            e.innerHTML = yaft.translations.collapse_label;
            message.collapsed = false;
        } else {
            $.each(descendants, function (index, descendant) {
                $("#" + descendant.id).hide();
            });

            e.innerHTML = yaft.translations.expand_label + ' (' + descendants.length + ')';
            message.collapsed = true;
        }
    };

    yaft.utils.markCurrentDiscussionRead = function (read) {

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + yaft.currentDiscussion.id + "/markRead",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, status) {

                $.each(yaft.currentForum.discussions, function (index, discussion) {

                    if (discussion.id == yaft.currentDiscussion.id) {
                        discussion['unread'] = 0;
                    }
                });
                
                yaft.switchState('forum');
            },
            error: function (xhr, status, error) {}
        });

        return false;
    };
    
    yaft.utils.markMessageRead = function (message,read) {

        var message;
        
        if (message["read"] && read == message["read"]) {
            return;
        }
        
        var func = read ? 'markRead' : 'markUnRead';
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + message.id + "/" + func,
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, status) {

                message["read"] = read;

                if ('minimal' === yaftViewMode) {
                    if (read) {
                        $("#" + message.id + "_read").show();
                    } else {
                        $("#" + message.id + "_read").hide();
                    }
                }

                yaft.renderMessage(message);
                    
            },
            error: function (xhr, status, error) {
                alert("Failed to mark message as read. Reason: " + status);
            }
        });

        return false;
    };
    
    /* END MESSAGE OPERATIONS */
    
    yaft.utils.deleteForum = function (forumId, forumTitle) {

        if (!confirm(yaft.translations.delete_forum_message_one + "'" + forumTitle + "'" + yaft.translations.delete_forum_message_two)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/forums/" + forumId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text,textStatus) {
                yaft.switchState('forums');
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete forum. Reason: " + textStatus);
            }
        });
        
        return false;
    };
    
    yaft.utils.deleteDiscussion = function (discussionId, discussionTitle) {

        if (!confirm(yaft.translations.delete_discussion_message_one + "'" + discussionTitle + "'" + yaft.translations.delete_discussion_message_two)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + discussionId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, textStatus) {
                yaft.switchState('forum', {'forumId': yaft.currentForum.id});
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete discussion. Reason: " + textStatus);
            }
        });
        
        return false;
    };

    yaft.utils.clearDiscussion = function (discussionId,discussionTitle) {

        if (!confirm(yaft.translations.clear_discussion_message_one + "'" + discussionTitle + "'" + yaft.translations.clear_discussion_message_two)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + discussionId + "/clear",
            dateType: "json",
            async: false,
            cache: false,
            success: function (data, textStatus) {

                // Switch the updated discussion into the current forum
                $.each(yaft.currentForum.discussions, function (index, discussion) {

                    if (discussion.id === data.id) {
                        discussion = data;
                    }
                });
                yaft.switchState('forum', {'forumId': yaft.currentForum.id});
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to clear discussion. Reason: " + textStatus);
            }
        });
        
        return false;
    };
    
    yaft.utils.removeAttachment = function (attachmentId, messageId, elementId) {

        if (!confirm(yaft.translations.delete_attachment_message)) {
            return;
        }

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/attachments/" + attachmentId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, textStatus) {
                var element = $("#" + elementId);
                var siblingCount = element.siblings().length;
                element.remove();
                if (siblingCount  == 1) {
                    $('#yaft-current-attachments-fieldset').remove();
                }
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete attachment. Reason: " + errorThrown);
            }
        });
    };

    var markReadMessagesInFora = function () {

        var readMessages = null;
            
        $.ajax( {
            url: yaft.baseDataUrl + "forums/allReadMessages.json",
            async: false,
            dataType: "json",
            cache: false,
            success: function (read, textStatus) {
                readMessages = read;
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get read messages. Reason: " + errorThrown);
            }
        });
            
        if (readMessages != null) {
            $.each(yaft.currentForums, function (index, forum) {

                var count = readMessages[forum.id];
                if (count) {
                    forum['unread'] = forum.messageCount - count;
                } else {
                    forum['unread'] = forum.messageCount;
                }

                if (forum['unread'] < 0) {
                    forum['unread'] = 0;
                }
            });
        }
    };

    yaft.utils.addFormattedDatesToDiscussionsInCurrentForum = function () {

        var self = this;

        $.each(yaft.currentForum.discussions, function (index, discussion) {

            if (discussion.start > -1) {
                discussion.formattedStartDate = self.formatDate(discussion.start);
            }
            if (discussion.end > -1) {
                discussion.formattedEndDate = self.formatDate(discussion.end);
            }
            if (discussion.lastMessageDate > -1) {
                discussion.formattedLastMessageDate = self.formatDate(discussion.lastMessageDate);
            } else {
                discussion.formattedLastMessageDate = 'n/a';
            }
        });
    };

    var markReadMessages = function (messages, readMessages) {

        $.each(messages, function (index, message) {

            if ($.inArray(message.id, readMessages) || message.creatorId == yaft.startupArgs.userId) {
                message.read = true;
            } else {
                message.read = false;
            }

            markReadMessages(message.children, readMessages);
        });
    };
    
    yaft.utils.markReadMessagesInCurrentDiscussion = function () {

        var readMessages = [];
            
        $.ajax( {
            url: yaft.baseDataUrl + "discussions/" + yaft.currentDiscussion.id + "/readMessages.json",
            async: false,
            dataType: "json",
            cache: false,
            success: function (ids, textStatus) {

                $.each(ids, function (index, id) {
                    readMessages.push(id);
                });
            },
            error: function (xhr, textStatus, errorThrown) {

                // 404 can be thrown when there are no read messages
                if (404 != xhr.status) {
                    alert("Failed to get read messages. Reason: " + errorThrown);
                }
            }
        });
            
        if (readMessages != null) {
            var firstMessage = yaft.currentDiscussion.firstMessage;
        
            if ($.inArray(firstMessage.id, readMessages) || firstMessage.creatorId == yaft.startupArgs.userId) {
                firstMessage['read'] = true;
            } else {
                firstMessage['read'] = false;
            }
        
            markReadMessages(firstMessage.children,readMessages);
        }
    };

    var recursiveFindMessage = function (messages, wantedId) {

        var found = undefined;

        $.each(messages, function (index, message) {

            if (message.id === wantedId) {
                message.isFirstMessage = false;
                found = message;
            } else {
                if (message.children.length > 0) {
                    var test = recursiveFindMessage(message.children,wantedId);
                    if (test != null) {
                        test.isFirstMessage = false;
                        found = test;
                    }
                }
            }
        });
        
        return found;
    };

    yaft.utils.findMessageInCurrentDiscussion = function (wantedId) {

        if (yaft.currentDiscussion) {
            var firstMessage = yaft.currentDiscussion.firstMessage;
        
            if (firstMessage.id == wantedId) {
                firstMessage["isFirstMessage"] = true;
                return firstMessage;
            } else {
                return recursiveFindMessage(firstMessage.children,wantedId);
            }
        } else {
            alert("No current discussion");
        }
    };

    yaft.utils.getCurrentUserData = function () {

        var userData = null;
        $.ajax( {
            url: yaft.baseDataUrl + "userData.json",
            dateType: "json",
            async: false,
            cache: false,
            success: function (data, textStatus) {
                userData = data;
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get the current user data. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });
          
        return userData;
    };
    
    yaft.utils.getSitePermissionMatrix = function () {

        var perms = [];

        $.ajax( {
            url: yaft.baseDataUrl + "perms.json",
            dateType: "json",
            async: false,
            cache: false,
            success: function (p, textStatus) {

                for (role in p) {
                    var permSet = {'role': role};

                    $.each(p[role], function (index, perm) {

                        perm = perm.replace(/\./g,"_");
                        eval("permSet." + perm + " = true");
                    });

                    perms.push(permSet);
                }
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get permissions. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });

        return perms;
    };
    
    yaft.utils.savePermissions = function () {

        var boxes = $('.yaft_permission_checkbox');
        var myData = {};

        $.each(boxes, function (index, box) {

            if (box.checked) myData[box.id] = 'true';
            else myData[box.id] = 'false';
        });

        $.ajax({
            url: yaft.baseDataUrl + "setPerms.json",
            type: 'POST',
            data: myData,
            timeout: 30000,
            async: false,
            dataType: 'text',
            success: function (result, textStatus) {
                location.reload();
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to save permissions. Status: " + textStatus + '. Error: ' + errorThrown);
            }
        });

        return false;
    };

    yaft.utils.getAuthors = function () {

        this.setCurrentAuthors();
        this.renderHandlebarsTemplate('authors_breadcrumb', {placementId: yaft.startupArgs.placementId, currentForum: yaft.currentForum, currentDiscussion: yaft.currentDiscussion}, 'yaft_breadcrumb');
        var canGrade = yaft.currentUserPermissions.gradeAll && yaft.currentDiscussion.graded;
        this.renderHandlebarsTemplate('authors', {authors: yaft.currentAuthors, currentDiscussion: yaft.currentDiscussion, 'canGrade': canGrade}, 'yaft_content');

        $(document).ready(function () {

            yaft.utils.attachProfilePopup();
            $("#yaft_author_table").tablesorter({
                cssHeader:'yaftSortableTableHeader',
                cssAsc:'yaftSortableTableHeaderSortUp',
                cssDesc:'yaftSortableTableHeaderSortDown',
                widgets: ['zebra']
            });

            yaft.fitFrame();
        });
    };

    yaft.utils.setCurrentAuthors = function () {

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + yaft.currentDiscussion.id + "/authors",
            dataType: "json",
            cache: false,
            async: false,
            success: function (data, textStatus) {

                yaft.currentAuthors = data;
                yaft.currentAuthors.sort(function (a,b) {

                    if (a.displayName < b.displayName) return -1;
                    else if (a.displayName > b.displayName) return 1;
                    else return 0;
                });
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get the current set of authors. Status: " + textStatus + ". Error: " + errorThrown);
            }
          });
    };

    yaft.utils.getIconUrlForMimeType = function (mimeType, name) {

        if (mimeType === 'application/pdf') {
            return "/library/image/silk/page_white_acrobat.png";
        } else if (mimeType.match(/excel$/)) {
            return "/library/image/silk/page_white_excel.png";
        } else if (mimeType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet') {
            return "/library/image/silk/page_white_excel.png";
        } else if (mimeType === 'application/msword' || name.match(/\.docx?$/)) {
            return "/library/image/silk/page_white_word.png";
        } else if (mimeType.match(/powerpoint/)) {
            return "/library/image/silk/page_white_powerpoint.png";
        } else if (mimeType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation') {
            return "/library/image/silk/page_white_powerpoint.png";
        } else if (mimeType === 'text/plain') {
            return "/library/image/silk/page_white_text.png";
        } else if (mimeType === 'text/html') {
            return "/library/image/silk/html.png";
        } else if (mimeType.match(/^image/)) {
            return "/library/image/silk/picture.png";
        } else if (mimeType.match(/^audio/)) {
            return "/library/image/silk/sound.png";
        } else {
            return "/library/image/silk/error.png";
        }
    };

    yaft.utils.showAuthorPosts = function (authorId) {

        var author = null;

        if (yaft.currentAuthors == null) {
            this.setCurrentAuthors();
        }

        $.each(yaft.currentAuthors, function (index, testAuthor) {
            if (testAuthor.id === authorId) author = testAuthor;
        });

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + yaft.currentDiscussion.id + "/authors/" + authorId + "/messages",
            dateType: "json",
            cache: false,
            success: function (data, textStatus) {

                $.each(data, function (index, message) {

                    yaft.utils.recursivelyAddFormattedDatesToMessage(message);

                    $.each(message.attachments, function (index, attachment) {
                        attachment.iconUrl = yaft.utils.getIconUrlForMimeType(attachment.mimeType, attachment.name);
                    });
                });

                var stuff = {
                                messages: data,
                                assignment: yaft.currentDiscussion.assignment,
                                displayName: author.displayName,
                                grade: (author.grade) ? author.grade.grade : undefined,
                                gradeable: yaft.currentDiscussion.assignment && yaft.currentUserPermissions.gradeAll };

                yaft.utils.renderHandlebarsTemplate('author_messages', stuff, 'yaft_content');

                $(document).ready(function () {

                    yaft.utils.attachProfilePopup();

                    $('#yaft_grade_button').click(function () {

                        var gradePoints = $('#yaft_grade_field').val();

                        $.ajax({
                            url: "/portal/tool/" + yaft.startupArgs.placementId + "/assignments/" + yaft.currentDiscussion.assignment.id + "/authors/" + authorId + "/grade/" + gradePoints,
                            dateType: "text",
                            cache: false,
                            success: function (data, xtextStatus) {

                                $('.yaft_grade_field').val(gradePoints);
                                $('.yaft_grade_field2').val(gradePoints);

                                $('.yaft_grade_success_message').show();

                                window.setTimeout(function () { $('.yaft_grade_success_message').fadeOut(); },2000);
                            },
                            error: function (xhr, textStatus, errorThrown) {}
                        });
                    });
                    
                    $('#yaft_grade_button2').click(function () {

                        var gradePoints = $('#yaft_grade_field2').val();

                        $.ajax({
                            url: "/portal/tool/" + yaft.startupArgs.placementId + "/assignments/" + yaft.currentDiscussion.assignment.id + "/authors/" + authorId + "/grade/" + gradePoints,
                            dateType: "text",
                            cache: false,
                            success: function (data, textStatus) {

                                $('.yaft_grade_field').val(gradePoints);
                                $('.yaft_grade_field2').val(gradePoints);

                                $('.yaft_grade_success_message').show();

                                window.setTimeout(function () { $('.yaft_grade_success_message').fadeOut(); },2000);
                            },
                            error: function (xhr, textStatus, errorThrown) {}
                        });
                    });
                    
                    $('#yaft_previous_author_button').click(function () {

                        $.each(yaft.currentAuthors, function (index, testAuthor) {

                            if (testAuthor.id === authorId) {
                                if (index > 0) {
                                    yaft.utils.showAuthorPosts(yaft.currentAuthors[index - 1].id);
                                }
                            }
                        });
                    });

                    $('#yaft_next_author_button').click(function () {

                        for (var i=0,j=yaft.currentAuthors.length;i<j;i++) {
                            if (yaft.currentAuthors[i].id === authorId) {
                                if (i < (j - 1)) {
                                    yaft.utils.showAuthorPosts(yaft.currentAuthors[i + 1].id);
                                }
                            }
                        }
                    });

                    yaft.fitFrame();
                });
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to get posts for author. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });
    };

    yaft.utils.formatDate = function (time) {

        var d = new Date(time);
        var hours = d.getHours();
        if (hours < 10)  hours = '0' + hours;
        var minutes = d.getMinutes();
        if (minutes < 10) minutes = '0' + minutes;
        return d.getDate() + " " + yaft.translations.month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
    };
    
    yaft.utils.addFormattedDatesToCurrentDiscussion = function () {

        var formattedCreatedDate = this.formatDate(yaft.currentDiscussion.firstMessage.createdDate);
        yaft.currentDiscussion.firstMessage.formattedDate = formattedCreatedDate;
        
        this.recursivelyAddFormattedDatesToMessage(yaft.currentDiscussion.firstMessage);
    };
    
    yaft.utils.recursivelyAddFormattedDatesToMessage = function (message) {

        message.formattedDate = this.formatDate(message.createdDate);

        message.canViewAuthor = message.creatorId === yaft.startupArgs.userId || yaft.currentUserPermissions.discussionViewAnonymous;
        message.canDelete = message.parent && 'DELETED' !== message.status
                                && ((yaft.currentUserPermissions.messageDeleteOwn && yaft.startupArgs.userId === message.creatorId)
                                        || yaft.currentUserPermissions.messageDeleteAny);

        $.each(message.children, function (index, child) {
            yaft.utils.recursivelyAddFormattedDatesToMessage(child);
        });
    };

    yaft.utils.renderHandlebarsTemplate = function (name, context, output) {

        var template = Handlebars.templates[name];
        document.getElementById(output).innerHTML = template(context);
    };
}) (jQuery);
