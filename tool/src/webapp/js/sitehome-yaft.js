/* Stuff that we always expect to be setup */
var yaftSiteId = null;

(function()
{
	var arg = SakaiUtils.getParameters();
	
	if(!arg || !arg.siteId)
	{
		alert('The site id MUST be supplied as a page parameter');
		return;
	}
	
	// Stuff that we always expect to be setup
	yaftSiteId = arg.siteId;
	
	var activeDiscussions = null;
		
	$.ajax(
	{
		url : '/direct/yaft-forum/' + yaftSiteId + '/activeDiscussions.json',
		dataType : "json",
		cache: false,
		async : false,
		success : function(ads)
		{
			activeDiscussions = ads['yaft-forum_collection'];
		},
		error : function(xmlHttpRequest,status)
		{
		}
	});
			
	SakaiUtils.renderTrimpathTemplate('synoptic_yaft_content_template',{'discussions':activeDiscussions},'synoptic_yaft_content');
	
	$(document).ready(function()
		{
			YaftUtils.applyBanding();
									
			$("#yaft_active_discussion_table").tablesorter({
	 							cssHeader:'yaftSortableTableHeader',
	 							cssAsc:'yaftSortableTableHeaderSortUp',
	 							cssDesc:'yaftSortableTableHeaderSortDown',
	 							headers:
	 								{
	 									2:{sorter: "isoDate"}
	 								}
	 						});

			setMainFrameHeight(window.frameElement.id);
		});
})();
