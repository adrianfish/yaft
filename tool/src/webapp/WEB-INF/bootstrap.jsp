<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html  
      xmlns="http://www.w3.org/1999/xhtml"  
      xml:lang="${isoLanguage}"
      lang="${isoLanguage}">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <script type="text/javascript">

            var yaft = {
                userId: '${userId}',
                siteId: '${siteId}',
                placementId: '${placementId}',
                state: '${state}',
                editor: '${editor}',
                forumId: '${forumId}',
                viewMode: '${viewMode}',
                discussionId: '${discussionId}',
                messageId: '${messageId}',
                onPDAPortal: ${onPDAPortal},
                i18n: {
                    <c:forEach items="${i18n}" var="i">${i.key}: "${i.value}",</c:forEach>
                    months: [<c:forEach items="${months}" var="m" varStatus="ms">'${m}'<c:if test="${not ms.last}">,</c:if></c:forEach>]
                }
            };
    
        </script>
        ${sakaiHtmlHead}
        <link rel="stylesheet" type="text/css" href="/yaft-tool/js/jquery-ui/css/smoothness/jquery-ui-1.10.4.custom.min.css" />
        <link rel="stylesheet" type="text/css" href="/profile2-tool/css/profile2-profile-entity.css" media="all" />
        <link rel="stylesheet" type="text/css" href="/yaft-tool/css/jquery.datetimepicker.css" />
        <link rel="stylesheet" type="text/css" href="/yaft-tool/css/yaft.css" />
        <script type="text/javascript" src="/yaft-tool/js/es5-shim.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery-1.9.1.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery-ui-1.10.4.custom.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.cluetip.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.hoverIntent.minified.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.MultiFile.pack.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.tablesorter.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.form.min.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/jquery.datetimepicker.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/handlebars.runtime-v1.3.0.js"></script>
        <script type="text/javascript" src="/yaft-tool/templates/partials.handlebars"></script>
        <script type="text/javascript" src="/yaft-tool/templates/templates.handlebars"></script>
        <script type="text/javascript" src="/yaft-tool/js/sakaiutils.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/yaftpermissions.js"></script>
        <script type="text/javascript" src="/yaft-tool/js/yaftutils.js"></script>
        <script type="text/javascript" src="/profile2-tool/javascript/profile2-eb.js"></script>
    </head>
    <body>

    <div class="portletBody">

        <ul id="yaft_toolbar" class="navIntraTool actionToolBar" role="menu"></ul>
        <br />
        <div id="yaft_container">
            <div id="yaft_breadcrumb"></div>
            <br />
            <div id="yaft_feedback_message"></div>
            <br />
            <div id="yaft_content"></div>
        </div>

    </div> <!-- /portletBody-->

    <script type="text/javascript" src="/yaft-tool/js/yaft.js"></script>
    <link rel="stylesheet" type="text/css" href="/yaft-tool/css/jquery.cluetip.css" />

</body>
</html>
