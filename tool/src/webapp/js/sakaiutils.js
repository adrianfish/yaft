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

    yaft.sakai = {};

    yaft.sakai.getProfileMarkup = function (userId) {

        var profile = '';

        $.ajax({
            url: "/direct/profile/" + userId + "/formatted",
            dataType: "html",
            async: false,
            cache: false,
            success: function (p) {
                profile = p;
            },
            error : function (xmlHttpRequest, stat, error) {
                alert("Failed to get profile markup. Status: " + stat + ". Error: " + error);
            }
        });

        return profile;
    };

    yaft.sakai.setupFCKEditor = function (textarea_id, width, height) {
    
        sakai.editor.launch(textarea_id, {}, width,height);
        yaft.fitFrame();
    };
    
    yaft.sakai.setupCKEditor = function (textarea_id, width, height) {

        var instance = CKEDITOR.instances[textarea_id];
        if (typeof instance !== 'undefined') {
            delete CKEDITOR.instances[textarea_id];
        }
    
        sakai.editor.launch(textarea_id, {}, width, height);
        
        CKEDITOR.instances[textarea_id].on('instanceReady',function (e) { yaft.fitFrame(); });
    };
    
    yaft.sakai.setupWysiwygEditor = function (editorId,textarea_id,width,height) {

        if ('FCKeditor' === editorId) {
            this.setupFCKEditor(textarea_id,width,height);
        } else if ('ckeditor' === editorId) {
            this.setupCKEditor(textarea_id,width,height);
        }
    };
    
    yaft.sakai.getWysiwygEditor = function (editorId,textarea_id) {

        if ('FCKeditor' === editorId) {
            return FCKeditorAPI.GetInstance(textarea_id);
        } else if ('ckeditor' === editorId) {
            return CKEDITOR.instances[textarea_id];
        }
    };
    
    yaft.sakai.getEditorData = function (editorId,textarea_id) {

        if ('FCKeditor' === editorId) {
            return this.getWysiwygEditor(editorId,textarea_id).GetData();
        } else if ('ckeditor' === editorId) {
            return this.getWysiwygEditor(editorId,textarea_id).getData();
        } else {
            return $('#' + textarea_id).val();
        }
    };

    yaft.sakai.updateEditorElement = function (editorId,textarea_id) {

        if ('FCKeditor' === editorId) {
            return this.getWysiwygEditor(editorId,textarea_id).UpdateLinkedField();
        } else if ('ckeditor' === editorId) {
            return this.getWysiwygEditor(editorId,textarea_id).updateElement();
        }
    };
}) (jQuery);
