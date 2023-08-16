package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.experiment.ExperimentSubject;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlSimpleElement;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Support for JSP pages that support experiments.  */
public abstract class ExperimentPage extends NamedUserPage {

    abstract public Market getMarket(String claimName);

    public String getCommonMessages() {
        Session session = SessionSingleton.getSession();
        if (null == session) {
            return "";
        }
        return session.message();
    }

    public String getCommonMessageLabel() {
        Session session = SessionSingleton.getSession();
        if (null == session) {
            return "";
        }
        return session.getCommonMessageLabel();
    }

    public String currentRound() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return Integer.toString(session.getCurrentRound());
    }

    public String roundLabel() {
        return Session.getRoundLabelOrDefault();
    }

    public String getScoresHtml() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }

        StringBuffer buff = new StringBuffer();
        HtmlTable.start(buff, scoreTableColumnLabels());
        HtmlRow.startTag(buff);
        buff.append(scoresAsHtml(session.getPlayer(getUserName()), session.rounds()));
        HtmlRow.endTag(buff);
        HtmlTable.endTagWithP(buff);

        return buff.toString();
    }

    String[] scoreTableColumnLabels() {
        Session session = SessionSingleton.getSession();
        int rounds = session.rounds();
        String[] labels = new String[rounds + 1];
        HtmlRow.labelColumns(0, rounds, labels, session.getRoundLabel());
        labels[rounds] = "Total";
        return labels;
    }

    String[] scoreTableColumnLabels(String firstColumnLabel) {
        Session session = SessionSingleton.getSession();
        int rounds = session.rounds();
        String[] labels = new String[rounds + 2];
        HtmlRow.labelFirstColumn(firstColumnLabel, labels);
        HtmlRow.labelColumns(1, rounds, labels, session.getRoundLabel());
        labels[rounds + 1] = "Total";
        return labels;
    }

    public String scaleDiv() {
        StringBuffer divs = new StringBuffer();
        Session session = SessionSingleton.getSession();
        if (session != null) {
            Quantity rawScale = session.getChartScale();
            Price scale = null;
            if (rawScale == null || rawScale.isZero()) {
                scale = session.maxPrice();
            } else {
                scale = new Price(rawScale, rawScale);
            }

            appendDiv(divs, "scale", scale.toString());

            Quantity rawMajor = session.getMajorUnit();
            if (rawMajor != null && ! rawMajor.isZero()) {
                appendDiv(divs, "majorUnit", rawMajor.printAsQuantity());
            }

            Quantity rawMinor = session.getMinorUnit();
            if (rawMinor != null && ! rawMinor.isZero()) {
                appendDiv(divs, "minorUnit", rawMinor.printAsQuantity());
            }

            appendDiv(divs, "roundLabel", session.getRoundLabel());
            return divs.toString();
        }
        return "<div id=scale class=\"100\"></div>";

    }

    private void appendDiv(StringBuffer divs, String idLabel, String classLabel) {
        divs.append("<div id=" + idLabel + " class=\"" + classLabel + "\"></div>");
    }

    public String showEarningsSummary() {
        Session session = SessionSingleton.getSession();
        if (session == null || ! session.getShowEarnings()) {
            return "";
        }

        return session.showEarningsSummary(getUserName());
    }

    public String logoHTML() {
        String logoPath;
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "<img src='images/logo.zocalo.jpg' height='81' width='250' align='top'>";
        } else {
            logoPath = session.getLogoPath();
            if (logoPath == "") {
                return "";
            }
        }

        return "<img src='" + logoPath + "' align='top'>";
    }

    static public String scoresAsHtml(ExperimentSubject player, final int rounds) {
        StringBuffer buff = new StringBuffer();
        Quantity total = Quantity.ZERO;
        for (int round = 1; round <= rounds; round++) {
            Quantity score = player.getScore(round);
            HtmlSimpleElement.printScoreCell(buff, score);
            total = total.plus(score);
        }
        HtmlSimpleElement.printScoreCell(buff, total);
        return buff.toString();
    }
}
