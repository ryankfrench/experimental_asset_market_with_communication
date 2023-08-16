<html>
<head>
<jsp:useBean id="judge"  scope="request" class="net.commerce.zocalo.JspSupport.JudgeScreen" />
<jsp:setProperty name="judge" property="*" />
<title>Judge <%=judge.getUserName() %> </title>
<!--
Copyright 2007-2009 Chris Hibbert.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
</head>
<body>

<table border=0 cellspacing=0 cellpadding=0> <tbody>
  <tr>
    <td width="590" bgcolor="FFFFCC">
        <table cellspacing=0 cellpadding=0><tbody>
            <tr><td>
                <iframe width="590" height="550" style="border: 1px" name="stripchartframe" id="stripchartframe"
                        src="stripChart/stripchartframe.html"></iframe>
            </td></tr><tr><td height=100 align="center">
            <%= judge.logoHTML() %>
            </td></tr><tr><td height=150>
            &nbsp;
            </td></tr>
        </tbody></table>

    </td>

      <td width="420" bgcolor="CCFFCC">
          <iframe width="420" height="800" style="border: 0" name="subFrame" id="subFrame"
                  src="JudgeSubFrame.jsp?userName=<%= judge.getUserName() %>&action=judge">
          </iframe>
      </td>

  </tr>

</tbody></table>
<%= judge.scaleDiv() %>

</body>
</html>
