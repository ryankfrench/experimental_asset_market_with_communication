package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.experiment.*;
import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.experiment.states.JudgingStatusAdaptor;
import net.commerce.zocalo.experiment.states.NoActionJudgingStatusAdaptor;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.service.PropertyKeywords;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Jsp support for Judge's screen in the GMU experiment.  */
public class JudgeScreen extends ExperimentPage {
    private String priceEstimate;
    private String phase;
    private final String DISABLED = "disabled";

    public String getPriceEstimate() {
        return priceEstimate;
    }

    public void setPriceEstimate(String priceEstimate) {
        this.priceEstimate = priceEstimate;
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        JudgingSession session = judgingSession();
        if (session != null) {
            Judge judge = getJudgeOrNull(session);
            if (null != priceEstimate && ! "".equals(priceEstimate)) {
                Quantity judgedProb = Quantity.ZERO;
                try {
                    judgedProb = new Quantity(priceEstimate);
                } catch (NumberFormatException e) {
                    warn("estimate must be a number.");
                }
                judge.setEstimate(session.getCurrentRound(), new Price(judgedProb, session.maxPrice()));
            }
        }
        if (request != null && "POST".equals(request.getMethod())) {
            redirectResult(request, response);
        }
    }

    private Judge getJudgeOrNull(JudgingSession session) {
        return session.getJudgeOrNull(getUserName());
    }

    public void warn(String s) {
        JudgingSession session = judgingSession();
        Judge judge = getJudgeOrNull(session);
        judge.warn(s);
    }

    public Market getMarket(String claimName) {
        Session session = SessionSingleton.getSession();
        return(session.getMarket());
     }

    public String getRequestURL(HttpServletRequest request) {
        return request.getRequestURL() + "?userName=" + getUserName();
    }

    public User getUser() {
        return null;
    }

    public String getClaimName(){
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        return session.getClaim().getName();
    }

    public String getGuessesTableHtml() {
        StringBuffer buf = new StringBuffer();
        JudgingSession session = judgingSession();
        if (session == null) {
            return "";
        }
        int rounds = session.rounds();

        String[] labels = new String[rounds];
        HtmlRow.labelColumns(0, rounds, labels, session.getRoundLabel());
        HtmlTable.start(buf, labels);
        Judge judge = getJudgeOrNull(session);
        if (judge == null) {
            return "Judge not found: " + getUserName();
        }

        HtmlRow.startTag(buf);
        judge.getGuessesRow(buf, session);
        HtmlRow.endTag(buf);
        HtmlTable.endTagWithP(buf);

        return buf.toString();
    }

    public String currentEstimate() {
        JudgingSession session = judgingSession();

        Judge judge = getJudgeOrNull(session);
        if (judge == null) {
            return "-";
        }
        return judge.estimateAsString(Math.max(1, session.getCurrentRound()));
    }

    private JudgingSession judgingSession() {
        return (JudgingSession)SessionSingleton.getSession();
    }

    public String disabledFlag(int currentRound) {
        final String[] flag = new String[]{ DISABLED };
        JudgingSession session = judgingSession();

        Judge judge = getJudgeOrNull(session);
        if  (judge.isDormant(currentRound)) {
            return DISABLED;
        }

        if (session.cuttingOffJudges()) {
            return disabledFlagForCutoff();
        }

        JudgingStatusAdaptor ifJudging = new NoActionJudgingStatusAdaptor() {
            public void judging() { flag[0] = ""; }
        };
        try {
            session.ifJudging(ifJudging);
        } catch (ScoreException e) {
            // not a problem here
        }
        return flag[0];
    }

    private String disabledFlagForCutoff() {
        JudgingSession session = judgingSession();
        if (timingCutoff(session)) {
            return DISABLED;
        }

        final String[] flag = new String[]{ DISABLED };

        JudgingStatusAdaptor ifScoringOrTrading = new NoActionJudgingStatusAdaptor() {
            public void showingScores() { flag[0] = ""; }
            public void trading()       { flag[0] = ""; }
        };
        session.ifScoring(ifScoringOrTrading);
        session.ifTrading(ifScoringOrTrading);
        return flag[0];
    }

    private boolean timingCutoff(JudgingSession session) {
        Judge judge = getJudgeOrNull(session);
        if (judge == null) { return false; }

        if (judge.judgingCutoff()) {
            warn("Judging cutoff by timer.");
            return true;
        }
        return false;
    }

    public String openJudgingStatePattern() {
        if (judgingSession().cuttingOffJudges()) {
            return "startRound";
        } else {
            return "endTrading";
        }
    }

    public String getCommonMessageLabel() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "Shared Message";
        } else {
            return session.getCommonMessageLabel();
        }
    }

    public String phaseDependent() {
        phase = "";
        JudgingSession session = (JudgingSession) SessionSingleton.getSession();
        final String round = session.getRoundLabel();
        NoActionJudgingStatusAdaptor ifScoring = new NoActionJudgingStatusAdaptor() {
            public void showingScores() { showPhaseDependentScore(round); }
            public void judging() { showPhaseDependentScore(round); }
        };
        session.ifScoring(ifScoring);
        try {
            session.ifJudging(ifScoring);
        } catch (ScoreException e) {
            // do nothing
        }

        Judge judge = getJudgeOrNull(session);
        if (judge != null && judge.hasWarnings()) {
            phase += judge.getWarningsHtml();
        }
        return phase;
    }

    private void showPhaseDependentScore(String round) {
        phase =
        "    <h3><center>Your Estimate for " + round + " " + currentRound() + "</center></h3>\n" +
        "    <center>" + currentEstimate() + "</center>\n    <p>";
    }

    public String estimationForm() {
        StringBuffer buff = new StringBuffer();
        buff.append(HtmlSimpleElement.postFormHeaderWithClass("JudgeSubFrame.jsp", "judgeScoringForm",
                "userName", getUserName()));
        if (judgingSession().cuttingOffJudges()) {
            buff.append("\n" + judgingSession().getEstimateSliderLabel() + ":\n        ");
        }
        estimateInputField(buff);
        buff.append("\n        ");
        if (disabledFlag(judgingSession().getCurrentRound()) == DISABLED) {
            buff.append(HtmlSimpleElement.disabledSubmitInputField("Submit"));
        } else {
            buff.append(HtmlSimpleElement.submitInputField("Submit"));
        }
        buff.append("\n</form>");
        return buff.toString();
    }

    private void estimateInputField(StringBuffer buff) {
        String name = "priceEstimate";
        String estimate = currentEstimate();
        int curValue;
        if (estimate.equals("-")) {
            curValue = 50;  // Also set in JudgeSubFrame.jsp:startRoundActions()
        } else {
            curValue = Integer.parseInt(estimate);
        }
        JudgingSession session = judgingSession();
        String choices = session.judgeInputChoiceList();
        if (null == choices || choices.length() == 0) {
            buff.append(HtmlSimpleElement.flaggedInputField(name, 5, curValue, disabledFlag(session.getCurrentRound())));
        } else if ("slider".equalsIgnoreCase(choices.trim())) {
            String feedbackLabel = judgingSession().getSliderFeedbackLabel();
            int scaleAsInt = session.getChartScale().asValue().intValue();
            if (scaleAsInt == 0) {
                scaleAsInt = session.getMarket().getMaxPrice().asValue().intValue();
            }
            HtmlSimpleElement.slider(buff, name, curValue, 0, scaleAsInt, getSliderInputStepSize(), getSliderLabelStepSize(), disabledFlag(session.getCurrentRound()), feedbackLabel);
        } else {
            String[] choiceArray = choices.split(PropertyKeywords.COMMA_SPLIT_RE);
            buff.append(HtmlSimpleElement.selectList(name, choiceArray, estimate));
        }
    }

    private int getSliderInputStepSize() {
        return judgingSession().getSliderInputStepSize();
    }

    private int getSliderLabelStepSize() {
        return judgingSession().getSliderLabelStepSize();
    }
}
