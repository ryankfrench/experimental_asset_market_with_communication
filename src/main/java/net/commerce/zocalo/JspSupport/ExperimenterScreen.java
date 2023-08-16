package net.commerce.zocalo.JspSupport;

import jjdm.zocalo.DownloadHelper;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.experiment.*;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.market.Market;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
//  Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Jsp support for Experimenter's screen in experiments.  */
public class ExperimenterScreen extends ExperimentPage {
    final static public String FILENAME_FIELD = "filename";
    final static public String DISPLAY_SCORES_ACTION = "Display scores";
    final static public String STOP_VOTING_ACTION = "Stop Voting";

    private String action;
    private String filename;
    private String message = "";
    private Logger logger = Logger.getLogger("jjdm.ExperimenterScreen");

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        Session session = SessionSingleton.getSession();
        try {
            if (null == action) {
                boolean isMultipart = ServletFileUpload.isMultipartContent(request);
                if (isMultipart) {
                    ZocaloService.resetService(); // clear out the service immediately
                    FileItemFactory factory = new DiskFileItemFactory();
                    ServletFileUpload upload = new ServletFileUpload(factory);
                    List items = upload.parseRequest(request);
                    for (Iterator iterator = items.iterator(); iterator.hasNext();) {
                        FileItem fileItem = (FileItem) iterator.next();
                        if (fileItem.getFieldName().equals(FILENAME_FIELD)) {
                            SessionSingleton.setSession(processStream(fileItem.getInputStream()));
                        }
                    }
                    request.getSession().setAttribute("NEW_PROPERTIES_UPLOADED", true);
                }
            } else if (session == null) {
                return;
            } else if ("Start Next Year".equalsIgnoreCase(action)) {
                session.startNextTimedRound();
            } else if (DISPLAY_SCORES_ACTION.equals(action)) {
                if (session instanceof JudgingSession) {
                    JudgingSession jSession = (JudgingSession)session;
                    jSession.endScoringPhase();
                    message = "";
                } else {
                    message = "judging not enabled.";
                }
            } else if (STOP_VOTING_ACTION.equals(action)) {
                if (session instanceof VotingSession) {
                    VotingSession vSession = (VotingSession)session;
                    vSession.endVoting();
                    message = "";
                } else {
                    message = "judging not enabled.";
                }
            } else if ("Stop Current Year".equalsIgnoreCase(action)) {
                session.endTrading(true);
            } else if ("Start Participation".equalsIgnoreCase(action)) {
                ZocaloService.getInstance().startParticipation();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            StringBuffer b = new StringBuffer();
            int count = 0;
            for(StackTraceElement trace :  e.getStackTrace()) {
                if(count++ >= 10) {
                    break;
                }
                b.append(trace.toString());
                b.append("<br/>");
            }
            request.getSession().setAttribute("ERROR_ON_EXPERIMENTER", true);
            request.getSession().setAttribute("ROOT_CAUSE_MESSAGE", e.getMessage());
	        request.getSession().setAttribute("FULL_ERROR", b.toString());
        }
        if (request != null && "POST".equals(request.getMethod())) {
            redirectResult(request, response);
        }
    }

    private Properties processStream(InputStream inputStream) throws IOException {
	    Properties props = new Properties();
        props.load(inputStream);
        return props;
    }

    public Market getMarket(String claimName) {
        Session session = SessionSingleton.getSession();
        return(session.getMarket());
     }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getScoresHtml() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        Iterator iterator = session.playerNameSortedIterator();
        StringBuffer buff = new StringBuffer();
        HtmlTable.start(buff, scoreTableColumnLabels("Player"), "experimenterScores");
        while (iterator.hasNext()) {
            String playerName = (String) iterator.next();
            HtmlRow.startTag(buff);
            buff.append(HtmlSimpleElement.printTableCell(playerName));
            buff.append(scoresAsHtml(session.getPlayer(playerName), session.rounds()));
            HtmlRow.endTag(buff);
        }
        HtmlTable.endTagWithP(buff);
        return buff.toString();
    }

    public String getRequestURL(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }

    public String displayButtons() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return stateSpecificButtons(session.experimenterButtons(), "ExperimenterSubFrame.jsp");
    }

    // called for experimenter's state transition buttons
    static private String stateSpecificButtons(String[] buttons, String targetPage) {
        StringBuffer buff = new StringBuffer();
        buff.append("   <table id=\"experimenterButtons\" align=\"center\" width=\"100%\" border=0 cellspacing=6>\n");
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                inputForm(buff, buttons[i], targetPage);
            }
        }
	    buff.append("   </tr>\n   </table>\n");
        return buff.toString();
    }

    static private void inputForm(StringBuffer buff, String roundAction, String targetPage) {
        buff.append("\n\t<td>");
        buff.append(HtmlSimpleElement.formHeaderWithPost(targetPage).toString());
	    buff.append(HtmlSimpleElement.submitInputField(roundAction));
        buff.append("\n\t\t</form>\n\t</td>\n");
    }


    public String getErrorMessage() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return message;
        } else {
            return message + "<br>" + session.getErrorMessage();
        }
    }

    public void warn(String s) {
        message = message + "<br>" + s;
    }

    public String linkForLogFile() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "no active log.";
        }
        StringBuffer buff = new StringBuffer();
        PrintStringAdaptor printer = new PrintStringAdaptor() {
            public void printString(StringBuffer buff, String s) {
                printLogFileLinks(buff, s);
            }
        };
        session.logFileLinks(buff, printer);
        return buff.toString();
    }

    public String stateSpecificDisplay() {
        Session session = SessionSingleton.getSession();
        if (session != null) {
            return session.stateSpecificDisplay();
        }
        return "";
    }

    public static void printLogFileLinks(StringBuffer b, String linkableName) {
        String urlName = linkableName.replace('\\', '/');
        String jspPath = "./Download.jsp?file=" + urlName + "&type=";
        b.append("Download ").append(HtmlSimpleElement.link("Raw Logfile", jspPath + DownloadHelper.TYPE_RAW));
	    b.append("<br/>");
	    b.append("Download ").append(HtmlSimpleElement.link("CSV Data", jspPath + DownloadHelper.TYPE_CSV));
    }
}
