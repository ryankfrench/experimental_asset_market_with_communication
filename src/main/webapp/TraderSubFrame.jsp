<html>
<head>
<jsp:useBean id="trader"  scope="request" class="net.commerce.zocalo.JspSupport.TraderScreen" />
<jsp:setProperty name="trader" property="*" />
<title> <%= trader.getClaimName() %> </title>
<script type="text/javascript">var logger = console;</script>
<!--
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
</head>
<body onLoad="onload_actions();">

<% trader.processRequest(request, response); %>

<h3 align="center">Offers</h3>
<div id="bigRedX" style="position: absolute; top: 5px; text-align: center; width: 100%; font-size: 155px; color: red; display: none;">
	X
</div>
<table align='center' bgcolor="DADBEC" border=0  cellpadding=8 cellspacing="3" id="makeOrderTable">
<%=trader.claimPurchaseFormRow() %>

<%=trader.marketOrderFormRow() %>
</table>

<br>
<iframe width="390" height="580" style="border: 0" name="traderDynamicFrame" id="traderDynamicFrame"
    src="TraderSubSubFrame.jsp?userName=<%= trader.getUserName() %>">
</iframe>
<%= trader.scaleDiv() %>

<script type="text/javascript" src="experiment.js"></script>
<script type="text/javascript" src="traderUI.js"></script>
<script type="text/javascript" src="//code.jquery.com/jquery-2.1.3.min.js"></script>
<script type="text/javascript">
    function onload_actions() {

        var reloadableSubSubframe = getNamedIFrame('traderDynamicFrame');
        var reloadableSubframe = getNamedIFrame('subFrame');

        top.reloadMySubSubframe = function() {
            reloadableSubSubframe.location.replace(reloadableSubSubframe.location.href);
        };

        top.reloadMySubframe = function() {
            /*
             * 20191121 JJDM: Fixing race condition.  The href attribute was truncating the path:
             * href: http://52.26.115.206:9080/TraderSubFrame.jsp?userName=a6
             * src:  http://52.26.115.206:9080/TraderSubFrame.jsp?userName=a6&action=trade&disabled=true
             * 
             * OLD: reloadableSubframe.location.replace(reloadableSubframe.location.href);
             */ 
            logger.debug("TraderSubFrame#top.reloadMySubframe: " + reloadableSubframe.frameElement.src);
            reloadableSubframe.location.replace(reloadableSubframe.frameElement.src);
        };

        top.startRoundActions = function() {
            logger.debug("TraderSubFrame#top.startRoundActions");
            top.reloadMySubframe();
        };

        top.transitionAction = function() {
            logger.debug("TraderSubFrame#top.transitionAction");
            top.reloadMySubframe();
        };

        top.updateBestBuyPrice = function(newValue) {
            updateInputValue('sellMarketOrderForm', 'price', newValue);
        };

        top.updateBestSellPrice = function(newValue) {
            updateInputValue('buyMarketOrderForm', 'price', newValue);
        };

    }
	
    $(document).ready(function() {
		$("#bigRedX").hide();
		logger.debug("getQueryString: <%= request.getQueryString() %>");
		var disable = "<%= request.getParameter("disabled") %>";
		if(disable == "true") {
			$("input").prop("disabled", true);
			$("#bigRedX").show();
		}
    });
	
</script>

</body>
</html>
