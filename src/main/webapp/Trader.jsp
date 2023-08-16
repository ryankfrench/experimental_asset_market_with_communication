<html>
<head>
<jsp:useBean id="trader"  scope="request" class="net.commerce.zocalo.JspSupport.TraderScreen" />
<jsp:setProperty name="trader" property="*" />
<title>Trader <%= trader.getUserName() %></title>
<!--
Copyright 2007-2009 Chris Hibbert.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->

<script type="text/javascript">var logger = console;</script>
<% trader.defaultJavascriptSettings(); %>
</head>
<body>

<table cellspacing=0 cellpadding=0 border=0> <tbody>
  <tr>
    <td width="590" bgcolor="FFFFCC">
        <table cellspacing=0 cellpadding=0><tbody>
            <tr><td>
                <iframe width="590" height="550" style="border: 0" name="stripchartframe" id="stripchartframe"
                         src="stripChart/stripchartframe.html?userName=<%= trader.getUserName() %>&action=trade"></iframe>
            </td></tr><tr><td height=100 align="center">
            <%= trader.logoHTML() %>
            </td></tr><tr><td height=150>
            &nbsp;
            </td></tr>
        </tbody></table>
    </td>

    <td width="420" bgcolor="CCFFCC">
      <iframe width="420" height="800" style="border: 0" name="subFrame" id="subFrame"
            src="TraderSubFrame.jsp?userName=<%= trader.getUserName() %>&action=trade">

      </iframe>
    </td>

    <td width="815" bgcolor="EEEEEE" id="chatColumn">
      <iframe width="815" height="800" style="border: 0" name="chatFrame" id="chatFrame"
            src="ChatSubFrame.html?userName=<%= trader.getUserName() %>&action=trade">

      </iframe>
    </td>

  </tr>

</tbody></table>

<script type="text/javascript" src="jquery-2.1.3.min.js"></script>
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

	function findCurrentChoice() {
		var currentChoice = callService({ a: "participationChoiceForUser" });
		return currentChoice;
	}

    function toggleBasedOnChatState(chatState) {
        $("#chatColumn").css("display", chatState.chatEnabled ? "block" : "none");
    }

	function redirectToParticipation() {
		document.location = "Participation.html?userName=<%= trader.getUserName() %>";
	}

    function toggleBasedOnTradingState(chatState) {

		var isParticipating = true;
		if(chatState.participationEnabled) {
			var currentChoice = findCurrentChoice();
			isParticipating = currentChoice && currentChoice.participate;
		}

		var goToParticipation = chatState.participationRunning || (!isParticipating && !chatState.betweenRounds && chatState.experimentStarted);
		if (goToParticipation) {
			redirectToParticipation();
        } else {
			var traderSubFrameUrl = "TraderSubFrame.jsp?userName=<%= trader.getUserName() %>&action=trade";
			var allowTrading = !chatState.experimentStarted || chatState.betweenRounds || (!chatState.tradingPaused && isParticipating);
			if(!allowTrading) {
				traderSubFrameUrl += "&disabled=true";
			}
			logger.debug("traderSubFrameUrl: " + traderSubFrameUrl);
			$("#subFrame").prop("src", traderSubFrameUrl);
		}
		
	}

    $(document).ready(function() {

        var websocketClient = createWebSocket(function(message) {
            var data = JSON.parse(message.data);
            if(data.topic == "/global") {
				var chatState = callService({ a: "chatState" });
                logger.debug(message);
                toggleBasedOnChatState(chatState);
                if(data.id == "TRADING_STATE_CHANGE" || data.id == "PARTICIPATION_STATE_CHANGE") {
                    toggleBasedOnTradingState(chatState);
                } else if(data.id == "CHAT_STATE_CHANGE") {
                    document.getElementById("chatFrame").contentWindow.location.reload();
                }
            }
        });

		var chatState = callService({ a: "chatState" });
        toggleBasedOnChatState(chatState);
        toggleBasedOnTradingState(chatState);

    });

</script>

</body>
</html>
