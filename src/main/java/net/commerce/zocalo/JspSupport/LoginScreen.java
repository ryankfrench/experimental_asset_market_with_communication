package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.experiment.ExperimentSubject;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.user.NonUser;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.Warnable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Support for the JSP login page for the GMU Experiment.  */
public class LoginScreen extends NamedUserPage {
    public static final String LOGIN_JSP = "Login.jsp";
    final private Warnable loginUser = new NonUser();
    final private String NoSession = "<center><h2>Session hasn't been created.  Please wait for the experimenter's signal.</h2></center>";

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            loginUser.warn("Please wait.  The session hasn't started yet.");
            return;
        } else if (null == getUserName() || "".equals(getUserName())) {
            return;
        }
        ExperimentSubject player = session.getPlayer(getUserName());
        if (null == player) {
            loginUser.warn("No such user available: " + getUserName());
        } else {
            redirectResult(request, response);
        }
    }

    public String getUserNameSelector() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return NoSession;
        }
        Iterator iterator = session.playerNameIterator();
        StringBuffer buff = new StringBuffer();
        buff.append("<h2>Please choose a user name to login.</h2>\n");
        buff.append("<table border=0><tr><td><ul>");
        while (iterator.hasNext()) {
            String userName = (String) iterator.next();
            ExperimentSubject player = session.getPlayer(userName);
            player.linkHtml(buff);
        }
        buff.append("</ul></td></tr></table>");
        return buff.toString();
    }

    public User getUser() {
        return null;
    }

    public Warnable getUserAsWarnable() {
        return loginUser;
    }

    public void warn(String s) {
        getUserAsWarnable().warn(s);
    }

    public String getRequestURL(HttpServletRequest request) {
        if ("".equals(getUserName())) {
            return LOGIN_JSP;
        }

        Session session = SessionSingleton.getSession();
        ExperimentSubject loggedInUser = session.getPlayer(getUserName());
        if (null != loggedInUser) {
            return loggedInUser.pageLink();
        }

        return LOGIN_JSP;
    }

    public String getWarning() {
        return loginUser.getWarningsHTML();
    }
}
