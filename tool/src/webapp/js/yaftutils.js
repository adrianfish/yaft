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

        for (var i=0,j=yaft.currentForums.length;i<j;i++) {
            var forum = yaft.currentForums[i];
            if (forum.start > -1) {
                var d = new Date(forum.start);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                forum.formattedStartDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
            }
            if (forum.end > -1) {
                var d = new Date(forum.end);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                forum.formattedEndDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
            }
            if (forum.lastMessageDate > -1) {
                var d = new Date(forum.lastMessageDate);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                forum.formattedLastMessageDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
            } else {
                forum.formattedLastMessageDate = 'n/a';
            }
        }
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

        for (var i=0,j=groupBoxes.length;i<j;i++) {
            groups += groupBoxes[i].id;
            if (i<j) groups += ',';
        }

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

        this.renderTrimpathTemplate('yaft_forum_content_template', yaft.currentForum, 'yaft_content');
            
        $(document).ready(function () {
        
            // If there are no discussions in the current forum, hide the check all checkbox
            if (!yaft.currentForum || yaft.currentForum.discussions.length <= 0) {
                $('#yaft_all_discussions_checkbox').hide();
            }

            $('#yaft_bulk_discussions_delete_button').click(function () {
                yaft.utils.deleteSelectedDiscussions();
            });
            $('.yaft_bulk_option_candidate').click(function () {

                if ($(this).prop('checked')) {
                    $('#yaft_bulk_discussions_delete_button').prop('disabled', false);
                    $('#yaft_bulk_discussions_delete_button').addClass('enabled');
                } else {
                    // Only enable the delete button if no other discussion is checked
                    if ($('.yaft_bulk_option_candidate:checked').length <= 0) {
                        $('#yaft_bulk_discussions_delete_button').prop('disabled', true);
                        $('#yaft_bulk_discussions_delete_button').removeClass('enabled');
                    }
                }
            });

            $('#yaft_all_discussions_checkbox').click(function () {

                if ($(this).prop('checked')) {
                    $('.yaft_bulk_option_candidate').prop('checked', true);
                    $('#yaft_bulk_discussions_delete_button').prop('disabled', false);
                    $('#yaft_bulk_discussions_delete_button').addClass('enabled');
                } else {
                    $('.yaft_bulk_option_candidate').prop('checked', false);
                    $('#yaft_bulk_discussions_delete_button').prop('disabled', true);
                    $('#yaft_bulk_discussions_delete_button').removeClass('enabled');
                }
            });
                                    
            // We need to check this as tablesorter complains at empty tbody
            if (yaft.currentForum.discussions.length > 0) {
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

                yaft.utils.attachProfilePopup();
            }

            yaft.fitFrame();
        });
    };
    
    yaft.utils.attachProfilePopup = function () {

        $('a.profile').cluetip({
            width: '620px',
            cluetipClass: 'yaft',
            sticky: true,
            dropShadow: false,
            arrows: true,
            mouseOutClose: true,
            closeText: '<img src="/library/image/silk/cross.png" alt="close" />',
            closePosition: 'top',
            showTitle: false,
            hoverIntent: true,
            ajaxSettings: {type: 'GET'}
        });
    };

    yaft.utils.deleteSelectedFora  = function () {
    
        if (!confirm(yaft_delete_selected_fora_message)) {
            return;
        }
        
        var candidates = $('.yaft_bulk_option_candidate:checked');

        var forumIds = [];

        for (var i=0,j=candidates.length;i<j;i++) {
            forumIds.push(candidates[i].id);
        }

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
    
        if (!confirm(yaft_delete_selected_discussions_message)) {
            return;
        }
        
        var candidates = $('.yaft_bulk_option_candidate:checked');

        var discussionIds = [];

        for (var i=0,j=candidates.length;i<j;i++) {
            discussionIds.push(candidates[i].id);
        }

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

        this.renderTrimpathTemplate('yaft_forums_content_template', {'items': yaft.currentForums}, 'yaft_content');
        $(document).ready(function () {

            // If there are no fora, hide the check all checkbox
            if (!yaft.currentForums || yaft.currentForums.length <= 0) {
                $('#yaft_all_fora_checkbox').hide();
            }

            $('#yaft_bulk_forum_delete_button').click(function () {
                yaft.utils.deleteSelectedFora();
            });
            $('.yaft_bulk_option_candidate').click(function () {

                if ($(this).prop('checked')) {
                    $('#yaft_bulk_forum_delete_button').prop('disabled', false);
                    $('#yaft_bulk_forum_delete_button').addClass('enabled');
                } else {
                    // Only enable the delete button if no other forum is checked
                    if ($('.yaft_bulk_option_candidate:checked').length <= 0) {
                        $('#yaft_bulk_forum_delete_button').prop('disabled', true);
                        $('#yaft_bulk_forum_delete_button').removeClass('enabled');
                    }
                }
            });

            $('#yaft_all_fora_checkbox').click(function () {

                if ($(this).prop('checked')) {
                    $('.yaft_bulk_option_candidate').prop('checked', true);
                    $('#yaft_bulk_forum_delete_button').prop('disabled', false);
                    $('#yaft_bulk_forum_delete_button').addClass('enabled');
                } else {
                    $('.yaft_bulk_option_candidate').prop('checked', false);
                    $('#yaft_bulk_forum_delete_button').prop('disabled', true);
                    $('#yaft_bulk_forum_delete_button').removeClass('enabled');
                }
            });
                 
            if (yaft.currentForums.length > 0) {
                $("#yaft_forum_table").tablesorter({
                    cssHeader:'yaftSortableTableHeader',
                    cssAsc:'yaftSortableTableHeaderSortUp',
                    cssDesc:'yaftSortableTableHeaderSortDown',
                    headers:
                        {
                            4: {sorter: "isoDate"},
                            5: {sorter: false},
                            6: {sorter: false}
                        },
                    sortList: [[0,0]],
                    widgets: ['zebra']
                });
            }
            
            yaft.fitFrame();
        });
    };
    
    yaft.utils.validateMessageSubmission = function (originalSubject) {

        originalSubject = unescape(originalSubject);

        var subject = $("#yaft_message_subject_field").val();

        if (subject) {
            return true;
        } else {
            if (confirm(yaft_empty_message_subject_message)) {
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

                for (var i=0,j=currentForum.discussions.length;i<j;i++) {
                    var discussion = currentForum.discussions[i];
                    var count = data.counts[discussion.id];
                    if (count) {
                        discussion['unread'] = discussion.messageCount - count;
                    } else {
                        discussion['unread'] = discussion.messageCount;
                    }

                    if (discussion['unread'] < 0) {
                        discussion['unread'] = 0;
                    }
                }
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

        if (!confirm(yaft_delete_message_message)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, textStatus) {

                var message = yaft.utils.findMessage(messageId);
                message.status = 'DELETED';
                if (message.id == yaft.currentDiscussion.firstMessage.id) {
                    message['isFirstMessage'] = true;
                } else {
                    message['isFirstMessage'] = false;
                }
                yaft.utils.renderTrimpathTemplate('yaft_message_template', message, message.id);
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

        for (var i=0,j=yaft.currentForums.length;i<j;i++) {
            if (yaft.currentForums[i].id == id) {
                return yaft.currentForums[i];
            }
        }

        return null;
    };

    yaft.utils.findDiscussion = function (id) {

        var discussions = yaft.currentForum.discussions;

        for (var i=0,j=discussions.length;i<j;i++) {
            if (discussions[i].id == id)
                return discussions[i];
        }

        return null;
    };
    
    yaft.utils.undeleteMessage = function (messageId) {

        if (!confirm(yaft_undelete_message_message)) {
            return false;
        }
        
        $.ajax( {
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/undelete",
            dateType: "text",
            async : false,
            cache: false,
            success: function (text, textStatus) {
                var message = yaft.utils.findMessage(messageId);
                message.status = 'READY';
                if (message.id == yaft.currentDiscussion.firstMessage.id) {
                    message['isFirstMessage'] = true;
                } else {
                    message['isFirstMessage'] = false;
                }
                yaft.utils.renderTrimpathTemplate('yaft_message_template', message, message.id);
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

    var getDescendantIds = function (messageId, testMessage, descendantIds) {

        for (var i=0,j=testMessage.children.length;i<j;i++) {
            descendantIds.push(testMessage.children[i].id);
            getDescendantIds(messageId,testMessage.children[i],descendantIds);
        }
    };
    
    yaft.utils.toggleMessage = function (e, messageId) {

        var message = this.findMessage(messageId);

        var descendantIds = new Array();
        getDescendantIds(messageId, message, descendantIds);

        if (message.collapsed) {
            for (var i=0,j=descendantIds.length;i<j;i++) {
                $("#" + descendantIds[i] + ' .yaft_collapse_expand_link').html(yaft_collapse_label);
                $("#" + descendantIds[i]).show();
                var descendant = this.findMessage(descendantIds[i]);
                descendant.collapsed = false;
            }

            e.innerHTML = yaft_collapse_label;
            message.collapsed = false;
        } else {
            for (var i=0,j=descendantIds.length;i<j;i++) {
                $("#" + descendantIds[i]).hide();
            }

            e.innerHTML = yaft_expand_label + ' (' + descendantIds.length + ')';
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

                for (var i=0,j=yaft.currentForum.discussions.length;i<j;i++) {
                    var discussion = yaft.currentForum.discussions[i];
                    if (discussion.id == yaft.currentDiscussion.id) {
                        discussion['unread'] = 0;
                    }
                }
                
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

                yaft.utils.renderTrimpathTemplate('yaft_message_template', message, message.id);
                    
            },
            error: function (xhr, status, error) {
                alert("Failed to mark message as read. Reason: " + status);
            }
        });

        return false;
    };
    
    /* END MESSAGE OPERATIONS */
    
    yaft.utils.deleteForum = function (forumId, forumTitle) {

        if (!confirm(yaft_delete_forum_message_one + "'" + forumTitle + "'" + yaft_delete_forum_message_two)) {
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

        if (!confirm(yaft_delete_discussion_message_one + "'" + discussionTitle + "'" + yaft_delete_discussion_message_two)) {
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

        if (!confirm(yaft_clear_discussion_message_one + "'" + discussionTitle + "'" + yaft_clear_discussion_message_two)) {
            return false;
        }
        
        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + discussionId + "/clear",
            dateType: "json",
            async: false,
            cache: false,
            success: function (data, textStatus) {

                // Switch the updated discussion into the current forum
                for (var i=0,j=yaft.currentForum.discussions.length;i<j;i++) {
                    if (yaft.currentForum.discussions[i].id === data.id) {
                        yaft.currentForum.discussions[i] = data;
                    }
                }
                yaft.switchState('forum', {'forumId': yaft.currentForum.id});
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to clear discussion. Reason: " + textStatus);
            }
        });
        
        return false;
    };
    
    yaft.utils.removeAttachment = function (attachmentId, messageId, elementId) {

        if (!confirm(yaft_delete_attachment_message)) {
            return;
        }

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/messages/" + messageId + "/attachments/" + attachmentId + "/delete",
            dateType: "text",
            async: false,
            cache: false,
            success: function (text, textStatus) {
                $("#" + elementId).remove();
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to delete attachment. Reason: " + errorThrown);
            }
        });
    };

    yaft.utils.showSearchResults = function (searchTerms) {

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/search",
            type: 'POST',
            dateType: "json",
            async: false,
            cache: false,
            data: {'searchTerms': searchTerms},
            success: function (results, textStatus) {

                var hits = results;
                $('#yaft_breadcrumb').html('');
                yaft.utils.renderTrimpathTemplate('yaft_search_results_content_template', {'results': hits}, 'yaft_content');

                $(document).ready(function () {
                    yaft.fitFrame();
                });
            },
            error: function (xhr, textStatus, errorThrown) {
                alert("Failed to search. Status: " + textStatus + ". Error: " + errorThrown);
            }
        });

        $("#yaft_add_discussion_link").hide();
        $("#yaft_add_forum_link").hide();
        $("#yaft_hide_deleted_link").hide();
        $("#yaft_minimal_link").hide();
        $("#yaft_full_link").hide();
        $("#yaft_permissions_link").hide();
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
            for (var i=0,j=yaft.currentForums.length;i<j;i++) {
                var forum = yaft.currentForums[i];
                var count = readMessages[forum.id];
                if (count) {
                    forum['unread'] = forum.messageCount - count;
                } else {
                    forum['unread'] = forum.messageCount;
                }

                if (forum['unread'] < 0) {
                    forum['unread'] = 0;
                }
            }
        }
    };

    yaft.utils.addFormattedDatesToDiscussionsInCurrentForum = function () {

        for (var i=0,j=yaft.currentForum.discussions.length;i<j;i++) {
            var discussion = yaft.currentForum.discussions[i];
            if (discussion.start > -1) {
                var d = new Date(discussion.start);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                var formattedStartDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
                discussion.formattedStartDate = formattedStartDate;
            }
            if (discussion.end > -1) {
                var d = new Date(discussion.end);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                var formattedEndDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
                discussion.formattedEndDate = formattedEndDate;
            }
            if (discussion.lastMessageDate > -1) {
                var d = new Date(discussion.lastMessageDate);
                var hours = d.getHours();
                if (hours < 10)  hours = '0' + hours;
                var minutes = d.getMinutes();
                if (minutes < 10)  minutes = '0' + minutes;
                discussion.formattedLastMessageDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + hours + ":" + minutes;
            } else {
                discussion.formattedLastMessageDate = 'n/a';
            }
        }
    };

    var contains = function (list, test) {

        for (var i=0,j=list.length;i<j;i++) {
            if (test == list[i]) {
                return true;
            }
        }
        
        return false;
    };

    var markReadMessages = function (messages, readMessages) {

        for (var i=0,j=messages.length;i<j;i++) {
            if (contains(readMessages,messages[i].id) || messages[i].creatorId == yaft.startupArgs.userId) {
                messages[i]["read"] = true;
            } else {
                messages[i]["read"] = false;
            }

            markReadMessages(messages[i].children,readMessages);
        }
    };
    
    yaft.utils.markReadMessagesInCurrentDiscussion = function () {

        var readMessages = [];
            
        $.ajax( {
            url: yaft.baseDataUrl + "discussions/" + yaft.currentDiscussion.id + "/readMessages.json",
            async: false,
            dataType: "json",
            cache: false,
            success: function (read, textStatus) {

                var ids = read;
                for (var i=0,j=ids.length;i<j;i++) {
                    readMessages.push(ids[i]);
                }
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
        
            if (contains(readMessages,firstMessage.id) || firstMessage.creatorId == yaft.startupArgs.userId) {
                firstMessage['read'] = true;
            } else {
                firstMessage['read'] = false;
            }
        
            markReadMessages(firstMessage.children,readMessages);
        }
    };

    var recursiveFindMessage = function (messages, wantedId) {

        for (var i=0,j=messages.length;i<j;i++) {
            var message = messages[i];
            if (message.id == wantedId) {
                message["isFirstMessage"] = false;
                return message;
            } else {
                if (message.children.length > 0) {
                    var test = recursiveFindMessage(message.children,wantedId);
                    if (test == null) {
                        continue;
                    } else {
                        test["isFirstMessage"] = false;
                          return test;
                    }
                }
            }
        }
        
        return null;
    };

    yaft.utils.findMessage = function (wantedId) {

        if (yaft.currentDiscussion) {
            var firstMessage = yaft.currentDiscussion.firstMessage;
        
            if (firstMessage.id == wantedId) {
                firstMessage["isFirstMessage"] = true;
                return firstMessage;
            } else {
                return recursiveFindMessage(firstMessage.children,wantedId);
            }
        } else {
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

                    for (var i=0,j=p[role].length;i<j;i++) {
                        var perm = p[role][i].replace(/\./g,"_");
                        eval("permSet." + perm + " = true");
                    }

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
        for (var i=0,j=boxes.length;i<j;i++) {
            var box = boxes[i];
            if (box.checked) {
                myData[box.id] = 'true';
            } else {
                myData[box.id] = 'false';
            }
        }

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
        this.renderTrimpathTemplate('yaft_discussion_authors_breadcrumb_template', yaft.currentDiscussion, 'yaft_breadcrumb');
        this.renderTrimpathTemplate('yaft_authors_template', {'authors': yaft.currentAuthors}, 'yaft_content');

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

    yaft.utils.showAuthorPosts = function (authorId) {

        var author = null;

        if (yaft.currentAuthors == null) {
            this.setCurrentAuthors();
        }

        for (var i=0,j=yaft.currentAuthors.length;i<j;i++) {
            if (yaft.currentAuthors[i].id === authorId) {
                author = yaft.currentAuthors[i];
            }
        }

        $.ajax({
            url: "/portal/tool/" + yaft.startupArgs.placementId + "/discussions/" + yaft.currentDiscussion.id + "/authors/" + authorId + "/messages",
            dateType: "json",
            cache: false,
            success: function (data, textStatus) {

                var stuff = {'messages': data,'assignment': yaft.currentDiscussion.assignment, 'displayName': author.displayName};

                if (author.grade) {
                    stuff.grade = author.grade.grade;
                }

                yaft.utils.renderTrimpathTemplate('yaft_author_messages_template', stuff, 'yaft_content');

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

                        for (var i=0,j=yaft.currentAuthors.length;i<j;i++) {
                            if (yaft.currentAuthors[i].id === authorId) {
                                if (i > 0) {
                                    yaft.utils.showAuthorPosts(yaft.currentAuthors[i - 1].id);
                                }
                            }
                        }
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
    
    yaft.utils.addFormattedDatesToCurrentDiscussion = function () {

        var d = new Date(yaft.currentDiscussion.firstMessage.createdDate);
        var minutes = d.getMinutes();
        if (minutes < 10) minutes = '0' + minutes;
        var formattedCreatedDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + minutes;
        yaft.currentDiscussion.firstMessage.formattedDate = formattedCreatedDate;
        
        this.recursivelyAddFormattedDatesToChildMessages(yaft.currentDiscussion.firstMessage);
    };
    
    yaft.utils.recursivelyAddFormattedDatesToChildMessages = function (parent) {

        for (var i=0,j=parent.children.length;i<j;i++) {
            var child = parent.children[i];
            var d = new Date(child.createdDate);
            var minutes = d.getMinutes();
            if (minutes < 10) minutes = '0' + minutes;
            var formattedCreatedDate = d.getDate() + " " + yaft_month_names[d.getMonth()] + " " + d.getFullYear() + " @ " + d.getHours() + ":" + minutes;
            child.formattedDate = formattedCreatedDate;
            
            this.recursivelyAddFormattedDatesToChildMessages(child);
        }
    };

    yaft.utils.renderTrimpathTemplate = function (templateName, contextObject, output) {

        var templateNode = document.getElementById(templateName);
        var firstNode = templateNode.firstChild;
        var template = null;

        if (firstNode && ( firstNode.nodeType === 8 || firstNode.nodeType === 4)) {
              template = templateNode.firstChild.data.toString();
        } else {
               template = templateNode.innerHTML.toString();
        }

        var trimpathTemplate = TrimPath.parseTemplate(template,templateName);

        var rendered = trimpathTemplate.process(contextObject);

        if (output) {
            document.getElementById(output).innerHTML = rendered;
        }

        return rendered;
    };
}) (jQuery);
