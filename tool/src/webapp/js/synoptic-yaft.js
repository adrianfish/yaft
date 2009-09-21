/* Stuff that we always expect to be setup */
var yaftPlacementId = null;

(function()
{
	var arg = YaftUtils.getParameters();
	
	if(!arg || !arg.placementId)
	{
		alert('The placement id MUST be supplied as a page parameter');
		return;
	}
	
	// Stuff that we always expect to be setup
	yaftPlacementId = arg.placementId;
		
	$.ajax(
	{
		url : '/portal/tool/' + yaftPlacementId + '/data/activeDiscussions',
		dataType : "json",
		cache: false,
		async : false,
		success : function(ads)
		{
			activeDiscussions = ads;
		},
		error : function(xmlHttpRequest,status)
		{
		}
	});
			
	YaftUtils.render('synoptic_yaft_content_template',activeDiscussions,'synoptic_yaft_content');
	
	$(document).ready(function()
		{
			YaftUtils.applyBanding();
									
			$("#yaft_discussion_table").tablesorter({
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
