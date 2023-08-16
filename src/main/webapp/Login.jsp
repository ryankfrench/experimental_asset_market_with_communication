<html>
<head>
<jsp:useBean id="login"  scope="request" class="net.commerce.zocalo.JspSupport.LoginScreen" />
<jsp:setProperty name="login" property="*" />
<title>Please choose a user name</title>
<!--
Copyright 2007 Chris Hibbert.  All rights reserved.
Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

This software is published under the terms of the MIT license, a copy
of which has been included with this distribution in the LICENSE file.
-->
</head>
<body bgcolor="CCFFCC">
<% login.processRequest(request, response); %>

<table border=0 cellspacing=0 cellpadding=50 width="90%" > <tbody>
  <tr> <td align="center">
        <img src="images/logo.zocalo.jpg" height=81 width=250 align="top" >
       </td>
  </tr>
  <tr> <td align="center">
           <%= login.getWarning() %>
       </td>
  </tr>
  <tr> <td align="center">
            <p>
            Type an account name:
            <form method=GET action=Login.jsp>
            name: <input type=text name=userName >
            <input type=submit name=action value="Submit">
            </form>
       </td>
  </tr>
</tbody></table>



</body>
</html>
