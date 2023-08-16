<html>
<head>
<jsp:useBean id="experimenter"  scope="request" class="net.commerce.zocalo.JspSupport.ExperimenterScreen" />
<jsp:setProperty name="experimenter" property="*" />
<title>Experiment Management subFrame</title>
<!--
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
<script type="text/javascript">var logger = console;</script>
<script type="text/javascript" src="jquery-2.1.3.min.js"></script>
<script type="text/javascript" src="WebSocketClient.js"></script>
<script type="text/javascript" src="experiment.js"></script>
<%
    // JJDM: Force reload of parent frame when new file is uploaded.
    boolean isNewUpload = session.getAttribute("NEW_PROPERTIES_UPLOADED") == null ? false : true;
    if(isNewUpload) {
        session.removeAttribute("NEW_PROPERTIES_UPLOADED");
        out.println("<script>parent.location.reload();</script>");
    }

    // JJDM: Check for errors
    boolean errorOnPage = session.getAttribute("ERROR_ON_EXPERIMENTER") == null ? false : true;
    String errorMessage = null;
    String fullError = null;
    if(errorOnPage) {
    	errorMessage = (String) session.getAttribute("ROOT_CAUSE_MESSAGE");
    	fullError = (String) session.getAttribute("FULL_ERROR");
        session.removeAttribute("ERROR_ON_EXPERIMENTER");
        session.removeAttribute("ROOT_CAUSE_MESSAGE");
        session.removeAttribute("FULL_ERROR");
    }
%>
<style>
	td.connected { font-weight: bold; color: white; background-color: green;}
	#terminate { font-weight: bold; color: red; display: none; }
	#terminateConfirmation { color: gray; display: block; margin-bottom: 5px; display: none;}
	#errorMessage { font-weight: bold; font-family: Courier; color: red;}
	#errorStack { font: 12px Courier; color: #555555;}
</style>
</head>
<body>

<% if(errorOnPage) { %>
<div id="error">
	<p>There was an error on the page.  The message returned was:</p>
	<p id="errorMessage"><%= errorMessage %></p>
	<p>The full error trace of the issues can be seen below.</p>
	<p id="errorStack"><%= fullError %></p>
	<p>Please select, copy and save (e.g. to a Word document) the error message and trace above if the error persists.</p>
</div>
<% } %>

<% experimenter.processRequest(request, response); %>

<% if (experimenter.currentRound().equals("")) { %>
    <table align="center" border=1 cellspacing=0 cellpadding=0>
      <tr><td colspan=2><span style="color: red; ">Session not initialized</span></td></tr>
    </table>
<% } else { %>
    <table align="center" border=1 cellspacing=0 cellpadding=0>
      <tr><td align="center"><strong><%= experimenter.getCommonMessageLabel() %></strong></td></tr>
      <tr><td><%= experimenter.getCommonMessages() %></td></tr>
    </table>
    <p>
    <table align="center" border=0 cellspacing=5 cellpadding=0>
      <tr><td> Current <%= experimenter.roundLabel() %>:</td><td><%= experimenter.currentRound() %></td></tr>
    </table>
<% } %>

<% if (!experimenter.getErrorMessage().equals("")) { %>
    <table align="center" border=0 cellspacing=5 cellpadding=0>
      <tr><td><span style="color: red; "><%= experimenter.getErrorMessage() %></span></td></tr>
    </table>
<% } %>

<br/>

<%= experimenter.displayButtons() %>

<% if (!experimenter.currentRound().equals("")) { %>
    <h2 align="center">Scores</h2>
    <%=experimenter.getScoresHtml() %>
<% } %>
<%=experimenter.stateSpecificDisplay() %>

<p><br><p>

<h2>Configure experiment from file:</h2>
<FORM ENCTYPE="multipart/form-data" method="POST" action="ExperimenterSubFrame.jsp">
<input type="hidden" name="action" value="loadFile">
<INPUT TYPE="file" NAME="filename">
<INPUT TYPE="submit" VALUE="upload">
</FORM>

<p>
<h3>Experiment Results</h3>
<%=experimenter.linkForLogFile() %>
<%= experimenter.scaleDiv() %>

<script>

    var EXPERIMENTER_SUB_FRAME = "EXPERIMENTER_SUB_FRAME";

    function callService(args) {
        var toReturn = null;
        args["u"] = EXPERIMENTER_SUB_FRAME;
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

	function generateButton(name, enabled, width) {
		var buttonInForm = '<td width="' + width + '%" align="center"><form autocomplete="off" action="ExperimenterSubFrame.jsp" method="POST">';
		buttonInForm += '<input type="submit" value="';
		buttonInForm += name;
		buttonInForm += '" name="action" class="smallFontButton"';
		if(!enabled) {
			buttonInForm += ' disabled="true"';
		}
		buttonInForm += '>';
		buttonInForm += '</form></td>';
		return buttonInForm;
	}

	/**
	 * Generates one to three buttons: Start Round, Start Participation, Stop Round.
	 */
	function drawExperimenterButtons() {

		var buttonBuffer = "";

		var width = "33";

		// participation button
		if(g_chatState.participationEnabled) {
			var enabled = !g_chatState.participationRunning && !g_chatState.roundRunning && !g_chatState.experimentEnded;
			buttonBuffer += generateButton("Start Participation", enabled, width);
		} else {
			width = "50";
		}

		// TODO JJDM Hard-coded these "Year" values for rounds.  Need to leverage property file.  Also see ExperimenterScreen (~ Line 56, 74).

		// start round
		buttonBuffer += generateButton("Start Next Year", (!g_chatState.participationEnabled || g_chatState.participationRunning) && !g_chatState.roundRunning && !g_chatState.experimentEnded, width);

		// stop round
		buttonBuffer += generateButton("Stop Current Year", g_chatState.roundRunning && !g_chatState.experimentEnded, width);

		$("#experimenterButtons tr").html(buttonBuffer);

	}

	function getDisplayForParticipation(choice) {
		var display = "";
		if (g_chatState.roundParticipationCost == undefined) {
			display += "N/A"
		} else {
			display += choice.participate ? "Yes" : "No";
		}
		display += " - ";
		if (g_chatState.roundClueCost == undefined) {
			display += "N/A"
		} else if (g_chatState.clueDistributionMultiClue) {
			if(g_chatState.multiClueMaximumCount > g_chatState.multiClueInitialCount) {
				display += choice.additionalClues;
			} else {
				display += "N/A"
			}
		} else {
			return choice.receiveClue ? "Yes" : "No";
		}
		return display;
	}

	function updateParticipationChoices() {

		$(".participationColumn").remove();

		var clueHeader = g_chatState.clueDistributionMultiClue ? "Extra Clues" : "Clue?";

		$("#experimenterScores tr th:first").after('<th class="participationColumn">Participate? - ' + clueHeader + '</th>');
		$("#experimenterScores tr").each(function(index) {
			var userColumn = $(this).find("td:first");
			var contents = $(userColumn).text();
			var found = false;
			$(g_participationChoices).each(function(index){
				if(this.participantId == contents) {
					$(userColumn).after('<td class="participationColumn">' + getDisplayForParticipation(this) + '</td>');
					found = true;
				}
			});
			if(!found) {
				$(userColumn).after('<td class="participationColumn"> --- </td>');
			}
		});

	}

	function updateConnectedDesktops() {
		logger.debug("updateConnectedDesktops: " + g_connectedDesktops);
		var atLeastOneConnected = false;
		$("#experimenterScores tr").each(function() {
			var firstColumn = $(this).find("td:first");
			if($(firstColumn).html()) {
				var userId = $(firstColumn).html();
				var connected = g_connectedDesktops.indexOf(userId) != -1;
				if(connected) {
					$(firstColumn).addClass("connected");
					atLeastOneConnected = true;
				} else {
					$(firstColumn).removeClass("connected");
				}
			}
		});
		if(atLeastOneConnected) {
			$("#terminate").show();
		} else {
			$("#terminate").hide();
			$("#terminateConfirmation").hide();
		}
	}

	function updateParticipationCashTotals() {
		logger.debug("updateParticipationCashTotals: " + g_participationCashTotals);
		for(var userId in g_participationCashTotals) {
			var cashPerRound = g_participationCashTotals[userId];
			var firstColumn = $('#experimenterScores tr td').filter(function () { return $(this).text() == userId; });
			var columnToModify = firstColumn.next('td'); // participation column
			var totalCash = 0;
			for(c in cashPerRound) {
				var participationCash = cashPerRound[c];
				columnToModify = columnToModify.next('td');
				columnToModify.append(" (" + participationCash + ")");
				totalCash += Number(participationCash);
				columnToModify.attr('title', 'Participation cash contributed ' + participationCash + ' for the year.');

			}
			var lastColumn = $(firstColumn).parent().find("td:last");
			lastColumn.append(" (" + totalCash + ")");
			lastColumn.attr('title', 'Participation cash contributed ' + totalCash + ' overall.');
		}
	}

	function terminate() {
		logger.debug("Terminating");
		$("#terminateConfirmation").show();
		callService({ a: "terminateAllConnectedDesktops" })
	}

    $(document).ready(function() {

        var websocketClient = createWebSocketForUser(EXPERIMENTER_SUB_FRAME, function(message) {
            var data = JSON.parse(message.data);
            if(data.topic == "/global") {
				g_chatState = callService({ a: "chatState" });
                logger.debug(message);
                if(data.id == "TRADING_STATE_CHANGE") {
                    logger.debug("ExperimenterSubFrame received TRADING_STATE_CHANGE");
					parent.location.reload();
                } else if(data.id == "NEW_PARTICIPATION_CHOICE") {
					var data = JSON.parse(message.data);
					g_participationChoices[g_participationChoices.length] = data;
					updateParticipationChoices();
                } else if(data.id == "CONNECTED_DESKTOPS") {
					var data = JSON.parse(message.data);
					g_connectedDesktops = data.connectedDesktops;
					updateConnectedDesktops();
				}
            }
        });

		g_chatState = callService({ a: "chatState" });

		if(g_chatState) {

			logger.debug(g_chatState);

			drawExperimenterButtons();

			if(g_chatState.participationEnabled) {
				g_participationChoices = callService({ a: "participationChoices" });
				updateParticipationChoices();
				g_connectedDesktops = callService({ a: "connectedDesktops" });
				updateConnectedDesktops();
				g_participationCashTotals = callService({ a: "participationCashTotals" });
				updateParticipationCashTotals();
			}

		}

    });

	var g_chatState;
	var g_participationChoices;
	var g_connectedDesktops;
	var g_participationCashTotals;

</script>

<p>
<div id="terminateConfirmation">Termination sent. Please wait for a few seconds...</div>
<input type="button" id="terminate" onclick="terminate()" value="Terminate All Desktop Clients"/>
</p>

</body>

</html>
