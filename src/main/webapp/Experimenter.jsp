<html>
<head>
<jsp:useBean id="experimenter" scope="request" class="net.commerce.zocalo.JspSupport.ExperimenterScreen" />
<jsp:setProperty name="experimenter" property="*" />
<title>Experiment Manager's Screen</title>
<!--
Copyright 2007-2009 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
<script type="text/javascript">var logger = console;</script>
</head>
<body>

<table border=0 cellspacing=0 cellpadding=0> <tbody>
  <tr>
    <td width="590" bgcolor="FFFFCC">
        <table cellspacing=0 cellpadding=0><tbody>
            <tr><td>
                <iframe width="590" height="550" style="border: 0;" name="stripchartframe" id="stripchartframe"
                          src="stripChart/stripchartframe.html?userName=STRIP_CHART&action=experiment"></iframe>
            </td></tr><tr><td height=100 align="center" valign="bottom">
            <%= experimenter.logoHTML() %>
            </td></tr><tr><td height=150>
            &nbsp;
            </td></tr>
        </tbody></table>

    </td>

    <td width="420" bgcolor="CCFFCC">
      <iframe width="420" height="800" style="border: 0;" name="subFrame" id="subFrame"
            src="ExperimenterSubFrame.jsp?action=experiment">
      </iframe>
    </td>
  </tr>

</tbody></table>

</body>
</html>
