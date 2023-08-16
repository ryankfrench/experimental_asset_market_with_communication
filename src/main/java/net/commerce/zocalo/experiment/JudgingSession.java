package net.commerce.zocalo.experiment;

import org.apache.log4j.Logger;
import org.mortbay.cometd.AbstractBayeux;

import java.util.*;

import net.commerce.zocalo.experiment.role.*;
import net.commerce.zocalo.experiment.states.*;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.JspSupport.ExperimenterScreen;
import net.commerce.zocalo.service.PropertyHelper;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** supports experiments in which one set of subjects doesn't trade,
 and is responsible for guessing the value of the traded commodity
 on the basis of the trading history. */
public class JudgingSession extends Session {
    static public TimerTask DEAD_TASK = null;
    private int earliestCutoff;
    private int latestCutoff;
    private boolean simultaneousCutoff;
    private Timer timer;
    private int sliderLabelStepSize;
    private int sliderInputStepSize;

    public JudgingSession(Properties props, AbstractBayeux bayeux) {
        super(props, bayeux);
    }

///// INITIALIZATION /////////////////////////////////////////////////////////
    static boolean describesJudgingSession(Properties props) {
        String rolesProperty = props.getProperty(ROLES_PROPNAME);
        if (rolesProperty != null && rolesProperty.indexOf(JUDGE_PROPERTY_WORD) > -1) {
            return true;
        } else {
            return nonTradersPresent(props);
        }
    }

    protected HashMap<String, Role> initializeRoles(Properties props) {
        String rolesProperty = props.getProperty(ROLES_PROPNAME);
        if (rolesProperty == null || rolesProperty.length() == 0) {
            return initializeRoles(new String[] { "trader", "manipulator", "judge" }, props);
        } else {
            return initializeRoles(rolesProperty.split(COMMA_SPLIT_RE), props);
        }
    }

    private HashMap<String, Role> initializeRoles(String[] rolesProperty, Properties props) {
        Map<String, Role> availableRoles = new HashMap<String, Role>();
        availableRoles.put("trader", new TraderRole("trader"));
        availableRoles.put("manipulator", new ManipulatorRole());
        availableRoles.put("judge", new JudgeRole());

        HashMap<String, Role> rolesMap = new HashMap<String, Role>();
        for (int i = 0; i < rolesProperty.length; i++) {
            String role = rolesProperty[i];

            if (rolesMap.get(role) == null) {
                Role subjectType = availableRoles.get(role);
                if (subjectType == null) {
                    subjectType = new TraderRole(role);
                    subjectType.initializeFromProps(props);
                    rolesMap.put(role, subjectType);
                } else {
                    subjectType.initializeFromProps(props);
                    rolesMap.put(role, subjectType);
                }
            }
        }

        if (rolesMap.get("judge") == null) {
            String judgeWarning = "Judge role required if manipulators are used.";
            appendToErrorMessage(judgeWarning);
        }

        return rolesMap;
    }

    void initializeState() {
        final Session s = this;
        SessionStatusAdaptor ad = sessionEndTradingAdaptor(s);
        JudgingStateHolder newPhase = new JudgingStateHolder(ad);
        setPhase(newPhase);
    }

    protected void logSessionInitialization() {
        super.logSessionInitialization();
        logCutoffParameters();
        logScoringParameters(PropertyHelper.configLogger());
    }

    public Quantity initialManipulatorTickets() {
        String manipulatorTicketsProp = PropertyHelper.dottedWords(TICKETS_PROPERTY_WORD, MANIPULATOR_PROPERTY_WORD);
        return new Quantity(PropertyHelper.parseDouble(manipulatorTicketsProp, props));
    }

    private void logScoringParameters(Logger log) {
        String rewards = (String) props.get(JUDGE_REWARDS);
        if (null == rewards || rewards.length() == 0) {
            String[] scoreInputs = new String[] { SCORING_FACTOR_PROPERTY_WORD, SCORING_CONSTANT_PROPERTY_WORD };
            String[] scoreRoles = new String[] { JUDGE_PROPERTY_WORD };
            logParameterCombinations(scoreInputs, scoreRoles, log, false);
        } else {
            StringBuffer buff = new StringBuffer();
            buff.append("Judge scoring based on table: ");
            String[] rewardArray = rewards.split(COMMA_SPLIT_RE);
            for (int i = 0; i < rewardArray.length; i++) {
                String reward = rewardArray[i];
                buff.append(reward);
                if (i + 1 < rewardArray.length) {
                    buff.append(", ");
                }
            }
            log.info(buff.toString());
        }
        String manipRewards = props.getProperty(Manipulator.MANIPULATOR_REWARDS_TOKEN);

        if (null == manipRewards || manipRewards.length() == 0) {
            String[] scoreInputs = new String[] { SCORING_FACTOR_PROPERTY_WORD, SCORING_CONSTANT_PROPERTY_WORD };
            String[] scoreRoles = new String[] { MANIPULATOR_PROPERTY_WORD };
            logParameterCombinations(scoreInputs, scoreRoles, log, false);
        } else {
            log.info("Manipulator scoring based on " + manipRewards);
        }
    }

///// ACCESSORS ///////////////////////////////////////////////////////////
    protected String[] endowedRoles() {
        return super.endowedRoles();
    }

    private void logCutoffParameters() {
        String earliest = props.getProperty(EARLIEST_JUDGE_CUTOFF_WORD);
        String latest = props.getProperty(LATEST_JUDGE_CUTOFF_WORD);
        String simultaneity = props.getProperty(SIMULTANEOUS_JUDGE_CUTOFF_WORD);
        sliderLabelStepSize = PropertyHelper.getSliderLabelStepSize(props);
        sliderInputStepSize = PropertyHelper.getSliderInputStepSize(props);
        Logger logger = PropertyHelper.configLogger();
        if (earliest == null && latest == null && simultaneity == null) {
            earliestCutoff = 0;
            latestCutoff = 0;
            timer = null;

            logger.info("Not cutting off judges.");
            return;
        }

        timer = new Timer();
        DEAD_TASK = new TimerTask() { public void run() { /* empty */ } };
        timer.schedule(DEAD_TASK, new Date().getTime());  // scheduledExecutionTime() is in the past

        int roundDuration = timeLimit();
        if (earliest == null || earliest.length() == 0) {
            earliestCutoff = (roundDuration - 60);
            logger.info("Defaulting Earliest Cutoff to one minute before round end: " + earliestCutoff);
        } else {
            earliestCutoff = PropertyHelper.parseTimeStringAsSeconds(earliest);
            logger.info("Earliest Judge Cutoff: " + earliestCutoff);
        }

        if (latest == null || latest.length() == 0) {
            latestCutoff = roundDuration + 10;
            logger.info("Defaulting Latest Cutoff to round duration: " + roundDuration + " plus 10 seconds");
        } else {
            latestCutoff = PropertyHelper.parseTimeStringAsSeconds(latest);
            logger.info("Latest Judge Cutoff: " + latestCutoff);
        }

        simultaneousCutoff = PropertyHelper.parseBoolean(SIMULTANEOUS_JUDGE_CUTOFF_WORD, props, false);
    }

    public boolean cuttingOffJudges() {
        return earliestCutoff != 0 || latestCutoff != 0;
    }

    void otherStartActions() {
        if (cuttingOffJudges()) {
            setupCutoffTimers();
        }
    }

    private void setupCutoffTimers() {
        Logger cutoffLogger = sessionLogger();
        Random random = new Random();
        int cutoffIfSimultaneous = (int) (random.nextDouble() * (latestCutoff - earliestCutoff));
        if (simultaneousCutoff) {
                cutoffLogger.info("using a simultaneous cutoff of " + cutoffIfSimultaneous + " seconds.");
            }
        Iterator<String> nameIterator = playerNameIterator();
        while (nameIterator.hasNext()) {
            Judge judge = getJudgeOrNull(nameIterator.next());
            if (judge == null) { continue; }

            int cutoff;
            if (simultaneousCutoff) {
                cutoff = cutoffIfSimultaneous;
            } else {
                cutoff = earliestCutoff + (int) (random.nextDouble() * (latestCutoff - earliestCutoff));
                cutoffLogger.info("using a cutoff for judge '" + judge.getName() + "' of " + cutoff + " seconds.");
                int roundDuration = timeLimit();
                if (cutoff > roundDuration + 10) {
                    cutoff = roundDuration + 10;
                    cutoffLogger.info("reducing timeout to " + cutoff + " (10 seconds after round finishes).");
                }
            }
            TimerTask task = judge.getCutoffTimer();
            timer.schedule(task, 1000 * cutoff);
        }
    }

    // cancel all the timers
    void otherEndTradingEvents() {
        Iterator<String> nameIterator = playerNameIterator();
        while (nameIterator.hasNext()) {
            Judge judge = getJudgeOrNull(nameIterator.next());
            if (judge == null) { continue; }

            judge.cancelCutOffTimer();
        }
    }

    static private boolean requiresJudges(String playerName, Properties props) {
        String roleName = props.getProperty(PropertyHelper.dottedWords(playerName, ROLE_PROPERTY_WORD));
        if (roleName == null) {
            return false;
        } else {
            roleName = roleName.trim();
        }
        return roleName.equals(JUDGE_PROPERTY_WORD) || roleName.equals(MANIPULATOR_PROPERTY_WORD);
    }

    static public boolean nonTradersPresent(Properties props) {
        String playersProperty = props.getProperty(PLAYERS_PROPNAME);
        String[] playerNames = playersProperty.split(COMMA_SPLIT_RE);
        for (int i = 0; i < playerNames.length; i++) {
            if (requiresJudges(playerNames[i], props)) {
                return true;
            }
        }
        return false;
    }

    protected Quantity bookFundingRequired(Properties props) {
        String playersProperty = props.getProperty(PLAYERS_PROPNAME);
        Quantity playerCount = new Quantity(playersProperty.split(COMMA_SPLIT_RE).length);
        Quantity maxTickets = initialTraderTickets().min(initialManipulatorTickets());
        return maxTickets.times(playerCount.times(new Quantity(rounds())));
    }

    public Judge getJudgeOrNull(String userName) {
        ExperimentSubject player = getPlayer(userName);

        if (player instanceof Judge) {
            return (Judge)player;
        }
        return null;
    }

///// STATE TRANSITIONS /////////////////////////////////////////////////////
    public void ifJudging(JudgingStatusAdaptor ifJudging) throws ScoreException {
        getJudgingPhase().informJudging(ifJudging);
    }

    public void endScoringPhase() throws ScoreException {
        final Session s = this;
        final ScoreException scoreException[] = new ScoreException[1];

        JudgingTransitionAdaptor ad = new NoActionJudgingTransitionAdaptor() {
            public boolean endJudging() {
                logEvent("Display Scores");
                try {
                    s.calculateScores();
                } catch (ScoreException e) {
                    scoreException[0] = e;
                    return false;
                }
                logTransitionEvent(END_SCORING_TRANSITION, END_SCORING_TEXT);
                if (lastRound()) {
                    endSession();
                }
                return true;
            }
        };

        scoreException[0] = null;
        getJudgingPhase().endJudging(ad);
        if (scoreException[0] != null) {
            throw scoreException[0];
        }
    }

    private JudgingStateHolder getJudgingPhase() {
        return (JudgingStateHolder)getPhase();
    }

///// SCORING /////////////////////////////////////////////////////////////

    protected void calculateScores(Price average) throws ScoreException {
        Quantity judged = judgedValue(getCurrentRound());
        Quantity target = getJudgingTarget(getCurrentRound());
        accrueDividendsAndBonuses(average, judged, target);
    }

    public Price judgedValue(int round) throws ScoreException {
        return getMedianJudgedValue(round);
    }

    public Price getMedianJudgedValue(int round) throws ScoreException {
        Quantity totalValue = Quantity.ZERO;
        int judgeCount = 0;
        for (Iterator iterator = playerNameIterator(); iterator.hasNext();) {
            Judge judge = getJudgeOrNull((String) iterator.next());
            if (judge == null || judge.isDormant(getCurrentRound())) {
                continue;
            }
            Price estimate = judge.getEstimate(round);
            if (estimate == null) {
                continue;
            }

            totalValue = totalValue.plus(estimate);
            judgeCount += 1;
        }
        if (judgeCount == 0) {
            if (cuttingOffJudges()) {
                return Price.dollarPrice(50);
            }
            String message = "Judges have not entered estimates for " + getRoundLabel() + " " + round + ".";
            appendToErrorMessage(message);
            throw new ScoreException(message);
        } else {
            return marketPrice(totalValue.div(judgeCount));
        }
    }

    public Quantity getJudgingTarget(int round) throws ScoreException {
        String targetValue = PropertyHelper.indirectPropertyForRound(TARGET_VALUE_PROPNAME, round, props);
        if ("".equals(targetValue)) {
            return getDividend(round);
        }
        return new Quantity(targetValue);
    }

    ///// WEB DISPLAY ///////////////////////////////////////////////////////////
    public String[] experimenterButtons() {
        String[] buttons;
        if (getCurrentRound() == 0) {
            buttons = new String[] { startRoundActionLabel()};
        } else {
            buttons = new String[] {
                    startRoundActionLabel(),
                    stopRoundActionLabel() + " " + getCurrentRound(),
                    ExperimenterScreen.DISPLAY_SCORES_ACTION
            };
        }
        return buttons;
    }

    // override to renderScore() when judging
    public String showEarningsSummary(final String userName) {
        final StringBuffer buff = new StringBuffer();
        JudgingStatusAdaptor ad = new NoActionJudgingStatusAdaptor() {
            public void judging() { renderScore(userName, buff); }
            public void trading() { renderScore(userName, buff); }
            public void showingScores() { renderScoreAndExplanation(userName, buff); }
        };
        try {
            ifJudging(ad);
        } catch (ScoreException e) {
            Logger logger = sessionLogger();
            logger.error(e);
            appendToErrorMessage("unable to compute scores.");
        }
        ifScoring(ad);
        ifTrading(ad);
        return buff.toString();
    }

    public String stateSpecificDisplay() {
        String result;
        Session session = SessionSingleton.getSession();
        if (session == null) {
            result = "";
        }
        Iterator iterator = session.playerNameSortedIterator();
        StringBuffer buff = new StringBuffer();
        printEstimatesTableHeader(session, buff);
        while (iterator.hasNext()) {
            String playerName = (String) iterator.next();
            Judge judge = getJudgeOrNull(playerName);
            if (null == judge) {
                continue;
            }
            HtmlRow.startTag(buff);
            buff.append(HtmlSimpleElement.printTableCell(playerName));
            judge.getGuessesRow(buff, session);
            HtmlRow.endTag(buff);
        }
        HtmlTable.endTagWithP(buff);
        result = buff.toString();
        return result;
    }

    public String getEstimatesHtml() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }
        Iterator iterator = session.playerNameSortedIterator();
        StringBuffer buff = new StringBuffer();
        printEstimatesTableHeader(session, buff);
        while (iterator.hasNext()) {
            String playerName = (String) iterator.next();
            Judge judge = getJudgeOrNull(playerName);
            if (null == judge) {
                continue;
            }
            HtmlRow.startTag(buff);
            buff.append(HtmlSimpleElement.printTableCell(playerName));
            judge.getGuessesRow(buff, session);
            HtmlRow.endTag(buff);
        }
        HtmlTable.endTagWithP(buff);
        return buff.toString();
    }

    private void printEstimatesTableHeader(Session session, StringBuffer buff) {
        int rounds = session.rounds();
        String[] columnLabels = new String[rounds + 1];
        HtmlRow.labelFirstColumn("Judge", columnLabels);
        HtmlRow.labelColumns(1, rounds, columnLabels, session.getRoundLabel());
        HtmlTable.start(buff, columnLabels);
    }

    public String judgeInputChoiceList() {
        return (String) props.get(JUDGE_INPUT_CHOICE_KEYWORD);
    }

    public String judgeRewardsTable() {
        return (String) props.get(JUDGE_REWARDS);
    }

    public int getSliderLabelStepSize() {
        return sliderLabelStepSize;
    }

    public int getSliderInputStepSize() {
        return sliderInputStepSize;
    }

    public String getEstimateSliderLabel() {
        String label = (String)props.get(ESTIMATE_LABEL_WORD);
        if (label != null) {
            return label;
        } else {
            return DEFAULT_ESTIMATE_SLIDER_LABEL;
        }
    }

    public String getSliderFeedbackLabel() {
        String label = (String)props.get(FEEDBACK_LABEL_WORD);
        if (label != null) {
            return label;
        } else {
            return DEFAULT_JUDGE_SLIDER_FEEDBACK_LABEL;
        }
    }
}
