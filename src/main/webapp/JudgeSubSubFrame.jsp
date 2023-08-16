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
</head>
<body>

<p align="center"><h2><%=judge.getCommonMessageLabel() %></h2>
<p align="center"><%=judge.getCommonMessages() %>
<p align="center"><h2>Your Earnings</h2>
<%= judge.showEarningsSummary() %>

<p>
    <%=judge.phaseDependent() %>

</body>
</html>
