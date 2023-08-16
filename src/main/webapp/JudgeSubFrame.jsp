<html>
<head>
<jsp:useBean id="judge"  scope="request" class="net.commerce.zocalo.JspSupport.JudgeScreen" />
<jsp:setProperty name="judge" property="*" />
<title>Experiment Management subFrame</title>
<!--
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
    <script type="text/javascript" src="experiment.js"></script>
    <script type="text/javascript" src="judgeUI.js"></script>

    <script type="text/javascript" src="../dojo/dojo/dojo.js.uncompressed.js"
				djConfig="parseOnLoad: true"></script>
    <link id="themeStyles" rel="stylesheet" href="../dojo/dijit/themes/tundra/tundra.css">

    <script type="text/javascript">
        dojo.require("dijit.form.Slider");
        dojo.require("dojo.parser"); // scan page for widgets
        dojo.require("dojox.cometd");
    </script>

</head>
<body onLoad="onload_actions()" class="tundra">
<script type="text/javascript">
    function onload_actions() {
        var reloadableSubframe = getNamedIFrame('subFrame');

        top.reloadMySubframe = function() {
            reloadableSubframe.location.replace(reloadableSubframe.location.href);
        };

        top.transitionAction = function(transition) {
            if (transition === "endScoring") {
                top.reloadMySubframe();
                var span = document.getElementById("disabledMessage");
                span.innerHTML = "";
            } else {
                var enableInput = (transition === "<%= judge.openJudgingStatePattern() %>");
                setJudgeDisabled(! enableInput);
            }
        };

        top.startRoundActions = function() {
            top.reloadMySubframe();
        };

        top.handlePrivateMessage = function(event) {
            var disableJudging = event.disableJudging;
            if (disableJudging !== "" && disableJudging !== undefined && disableJudging.length > 0) {
                setJudgeDisabled(disableJudging);
                var span = document.getElementById("disabledMessage");
                span.innerHTML = "Entry of estimates has been disabled.";
            }
        };

        dojox.cometd.init('/cometd');

        function onPrivateMessage(msg) {
             callHandlePrivateMessage(msg.data);
        }
        dojox.cometd.subscribe(privateTopic,onPrivateMessage);

        var userName = "<%= judge.getUserName() %>";
        if (userName !== "") {
            dojox.cometd.publish(privateTopic, { user: userName, join: true });
        }
    }
</script>

<% judge.processRequest(request, response); %>

<p><br>

<p align="center"><h2><%=judge.getCommonMessageLabel() %></h2>
<p align="center"><%=judge.getCommonMessages() %><br>
<span style="background-color:red;" id="disabledMessage"></span>

<p align="center"><h2>Your Earnings</h2>
<%= judge.showEarningsSummary() %>

<p>
<%=judge.phaseDependent() %>

<p>
<%=judge.estimationForm() %>
<%= judge.scaleDiv() %>

</body>
</html>
