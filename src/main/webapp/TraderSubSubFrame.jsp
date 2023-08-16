<%@page import="jjdm.zocalo.data.ZocaloLabels" %>
<html>
<head>
<jsp:useBean id="trader"  scope="request" class="net.commerce.zocalo.JspSupport.TraderScreen" />
<jsp:setProperty name="trader" property="*" />
<title> <%= trader.getClaimName() %> </title>
<!--
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
<script type="text/javascript">var logger = console;</script>
</head>
<body>

<%-- This section is visually a continuation of the preceding data provided in the parent frame. --%>

<% trader.processRequest(request, response); %>

<%=trader.pricesTable() %>

<hr noshade color="black">

<h3 align="center">Your Holdings</h3>
<table align=center border=1 cellspacing=0 cellpadding=3 id="holdingsTable">
    <tr><td>Cash</td><td><%= trader.getBalanceMessage() %></td></tr>
    <tr><td><%= trader.getSharesLabel() %></td><td><%= trader.getHoldingsMessage() %></td></tr>
    <%= trader.getReservesRow() %>
</table>
<p>
<%= trader.showEarningsSummary() %>

<hr color="black">

<h3 align="center">Information</h3>

<table align=center border=0 cellspacing=0 cellpadding=5>
  <tr><td>
      <table align=center border=0 cellspacing=0 cellpadding=2>
          <tr><td align="center">Alert:</td></tr>
  <% if (trader.hasErrorMessages()) { %>
          <tr><td><span style="background-color:red;" id="traderRedMessage"><%= trader.getErrorMessages() %></span></td></tr>
  <% } else { %>
          <tr><td><span style="background-color:red;" id="traderRedMessage"></span></td></tr>
  <% } %>
      </table>
  </td></tr>
  <tr><td>
    <table align=center border=0 cellspacing=0 cellpadding=2>
        <tr><td align="center"><%= trader.getMessageLabel() %></td></tr>
            <tr><td align="center"><b><%= trader.getMessage() %></b></td></tr>
    </table>
  </td></tr>
  <tr><td>
    <table align=center border=0 cellspacing=0 cellpadding=2>
        <tr><td align="center"><%= trader.getCommonMessageLabel() %></td></tr>
            <tr><td><b><%= trader.getCommonMessages() %></b></td></tr>
    </table>
  </td></tr>
</table>

<script type="text/javascript" src="//code.jquery.com/jquery-2.1.3.min.js"></script>
<script type="text/javascript" src="WebSocketClient.js"></script>

<script>

    function callService(args) {
        var toReturn = null;
        args["u"] = getUsername();
        logger.debug(args);
        $.ajax("service.jsp", {
            accepts: "json",
            async: false,
            cache: false,
            data: args,
            success: function(data) { toReturn = data; },
            error: function(error) { logger.error(error); }
        });
        logger.debug(toReturn);
        return toReturn;
    }

    $(document).ready(function() {
		var chatState = callService({ a: "chatState" });
        if(chatState.participationEnabled) {
			var roundParticipationCash = callService({ a: "participationCashForUser" });
			var totalParticipationCash = callService({ a: "participationCashTotalForUser" });
			logger.debug("roundParticipationCash: " + roundParticipationCash + "; totalParticipationCash: " + totalParticipationCash);
			// Add the participation cash row
			$("#holdingsTable").prepend("<tr><td> <%= ZocaloLabels.getParticipationCash() %></td><td>" + roundParticipationCash + "</td></tr>");
			var showingTotal = chatState.betweenRounds;
			if(showingTotal) {
				var totalWithoutParticipation = Number($("table:eq(2) td:eq(1)").text().trim());
				var grandTotal = totalWithoutParticipation + Number(roundParticipationCash);
				logger.debug("grandTotal: " + grandTotal);
				$("table:eq(2) td:eq(1)").text(grandTotal);
			}
		}
    });

</script>

</body>
</html>
