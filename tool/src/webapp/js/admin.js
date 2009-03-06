(function()
{
	var arg = YaftUtils.getParameters();
	
	YaftUtils.render('yaft_home_link_template',arg,'yaft_home_link');
	
	jQuery.ajax(
	{
	   	url : "/portal/tool/" + arg.placementId + "/sites",
	   	dataType : "json",
	   	async : false,
		cache: false,
	  	success : function(sites)
		{
			for(var i = 0;i<sites.length;i++)
			{
				var c = sites[i].title[0].toUpperCase();
				if(c in yaftSiteTable)
				{
					yaftSiteTable[c].push(sites[i]);
				}
				else
				{
					yaftSiteTable[c] = [sites[i]];
				}
			}
			var data = {"t":yaftSiteTable};
			data["placementId"] = arg.placementId;

			YaftUtils.render('yaft_admin_template',data,'yaft_admin');
			YaftUtils.render('yaft_sites_list_template',data,'yaft_sites_list');
	 		$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
		},
		error : function(reqObject,status)
		{
			alert("Failed to get sites. Reason: " + status);
		}
	});
})();

function yaftShowSites(letter)
{
	$(".yaftSites").hide();
	$("#" + letter + "_sites").show();
	$(document).ready(function() {setMainFrameHeight(window.frameElement.id);});
}
