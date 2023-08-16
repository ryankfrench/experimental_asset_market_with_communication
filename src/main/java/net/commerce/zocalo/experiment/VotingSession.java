package net.commerce.zocalo.experiment;

import net.commerce.zocalo.JspSupport.ExperimenterScreen;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.experiment.states.*;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.user.User;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.service.PropertyHelper;
import org.apache.log4j.Logger;
import org.mortbay.cometd.AbstractBayeux;

import java.util.*;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** VotingSession supports an experiment in which the traders vote on a message to be
 displayed to all traders in a subsequent round. */
public class VotingSession extends Session {
    static public final String END_VOTING_TRANSITION = "votingDone";

    /** Which rounds will votes be displayed in? */
    private List<Integer> voteBefore;
    /** available messages*/
    private HashMap<Integer, String> voteChoices;
    /** how did users vote?  round->map(user->choice)*/
    private Map<Integer, Map<User, Integer>> votes;
    /** How many votes did each message get?  round->map(outcome->total) */
    private HashMap<Integer, Map<Integer, Integer>> allVoteTotals = new HashMap<Integer, Map<Integer, Integer>>();
    /** which outcome will be displayed for each vote? */
    private Map<Integer, String> outcomes = new HashMap<Integer, String>();

    public VotingSession(Properties props, AbstractBayeux bayeux) {
        super(props, bayeux);
        initializeVotingState(props);
    }

    static boolean describesVotingSession(Properties props) {
        String alternatives = props.getProperty(VOTING_ALTERNATIVES);
        if (alternatives == null || alternatives.equals("")) {
            return false;
        }
        Integer alt = null;
        try {
            alt = Integer.parseInt(alternatives);
        } catch (NumberFormatException e) {
            return false;
        }
        return alt != null && alt.intValue() > 0;
    }

    private void ifVoting(VotingStatusAdaptor ad) {
        getVotingPhase().informVoting(ad);
    }

    private VotingStateHolder getVotingPhase() {
        return (VotingStateHolder)getPhase();
    }

    //// INITIALIZATION //////////////////////////////////////////////////////////
    private void initializeVotingState(Properties props) {
        votes = new HashMap<Integer, Map<User, Integer>>();
        initializeVotingMessages(props);
        initializeVotingRounds(props);
        for (Iterator<Integer> integerIterator = voteBefore.iterator(); integerIterator.hasNext();) {
            Integer round = integerIterator.next();
            votes.put(round, new HashMap<User, Integer>());
        }

        logVotingMessages(PropertyHelper.configLogger());
    }

    private void initializeVotingRounds(Properties props) {
        voteBefore = new LinkedList<Integer>();
        String roundString = props.getProperty(VOTING_ROUNDS);
        String[] rounds = roundString.split(COMMA_SPLIT_RE);
        for (int i = 0; i < rounds.length; i++) {
            int round;
            try {
                round = Integer.parseInt(rounds[i]);
            } catch (NumberFormatException e) {
                appendToErrorMessage(VOTING_ROUNDS + " should contain a comma-separated list of " + getRoundLabel() + " numbers.");
                continue;
            }
            voteBefore.add(round);
        }
    }

    private void initializeVotingMessages(Properties props) {
        voteChoices = new HashMap<Integer, String>();
        String alternatives = props.getProperty(VOTING_ALTERNATIVES);
        if (alternatives == null || alternatives.equals("")) {
            return;
        }
        int alt = 0;
        try {
            alt = Integer.parseInt(alternatives);
        } catch (NumberFormatException e) {
            return;
        }
        for (int i = 1; i <= alt ; i++) {
            voteChoices.put(i, props.getProperty(VOTE_TEXT + DOT_SEPARATOR + i));
        }
    }

    void initializeState() {
        final Session s = this;
        SessionStatusAdaptor ad = sessionEndTradingAdaptor(s);
        setPhase(new VotingStateHolder(ad));
    }

//// ACCESSORS ////////////////////////////////////////////////////////////
    public String voteText(int index) {
        return voteChoices.get(index);
    }

    public boolean voteBefore(int round) {
        return voteBefore.contains(new Integer(round));
    }

    public String voteResultMessage() {
        return "";
    }

    public Integer getVote(TradingSubject trader, int round) {
        User voter = getUserOrNull(trader.getName());
        if (voter != null) {
            return getVote(voter, round);
        }
        return null;
    }

    public Integer getVote(User trader, int round) {
        if (! voteBefore(round)) { return null; }
        Map<User, Integer> roundMap = votes.get(round);
        if (roundMap == null) {
            return null;
        }
        return roundMap.get(trader);
    }

    public boolean votingComplete(final int round) {
        final boolean[] complete = new boolean[]{false};
        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(round) {
            public void voting() {
                if (allVotesCast(round)) {
                    complete[0] = true;
                }
            }
        };
        ifVoting(ad);
        return complete[0];
    }

    private boolean allVotesCast(int round) {
        final boolean[] votingNow = new boolean[]{false};
        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(round) {
            public void voting() { votingNow[0] = true; }
        };
        ifVoting(ad);

        if (! votingNow[0] || ! voteBefore(round)) {
            return true;
        }
        for (Iterator<User> subjectIter = getTraders().iterator(); subjectIter.hasNext();) {
            User trader = subjectIter.next();
            Integer vote = getVote(trader, round);
            if (vote == null || "".equals(vote)) {
                return false;
            }
        }
        return true;
    }

    public String message(final int round) {
        final String[] msg = new String[]{""};

        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(round) {
            public void trading() { lookUpOrChooseOutcome(msg, round); }
            public void showingScores() { msg[0] = showVoteResults(round + 1); }
        };
        ifTrading(ad);
        ifScoring(ad);

        return msg[0];
    }

//// VOTING /////////////////////////////////////////////////
    public void setVote(TradingSubject trader, final int proposal) {
        User voter = getUserOrNull(trader.getName());
        if (voter != null) {
            setVote(voter, proposal);
        }
    }

    public void setVote(final User trader, final int proposal) {
        final int nextRound = getCurrentRound() + 1;
        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(getCurrentRound()) {
            public void voting() {
                Map<User, Integer> roundMap = votes.get(nextRound);
                roundMap.put(trader, proposal);
            }
        };
        ifVoting(ad);
    }

//// STATE TRANSITIONS //////////////////////////////////////////////////////
    public void endTrading(boolean manual) {
        super.endTrading(manual);
        if (!voteBefore(getCurrentRound() + 1)) {
            VotingTransitionAdaptor eVA = new NoActionVotingTransitionAdaptor() {
                public void endVoting() { outcomes.put(getCurrentRound() + 1, ""); }
            };
            getVotingPhase().endVoting(eVA);
        }
    }

    public void endVoting() {
        VotingTransitionAdaptor ad = new NoActionVotingTransitionAdaptor() {
            public void endVoting() {
                int round = getCurrentRound();
                if (votingComplete(round)) {
                    chooseOutcome(round + 1);
                    logEvent(END_VOTING_TEXT);
                    logTransitionEvent(END_VOTING_TRANSITION, END_VOTING_TEXT);
                }
            }
            public void warn(String warning) { appendToErrorMessage(warning); }
        };
        getVotingPhase().endVoting(ad);
    }

    private void lookUpOrChooseOutcome(String[] msg, int round) {
        msg[0] = outcomes.get(round);
        if (msg[0] == null) {
            msg[0] = chooseOutcome(round);
        }
    }

    private String showVoteResults(int round) {
        if (! voteBefore(round)) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        Map<Integer, Integer> voteTotals = allVoteTotals.get(round);
        for (int i = 1; i <= voteChoices.size(); i++) {
            Integer countI = voteTotals.get(i);
            int count;
            if (countI == null) {
                count = 0;
            } else {
                count = countI;
            }

            String choice = voteChoices.get(i);
            buf.append("Participants choosing '" + choice + "': " + count + ".<br>");
        }
        return buf.toString();
    }

    protected void logCommonMessages(Logger log) {
        // Do nothing;
    }

    protected void logVotingMessages(Logger log) {
        Set<Integer> indices = voteChoices.keySet();
        for (Iterator<Integer> it = indices.iterator(); it.hasNext();) {
            Integer index = it.next();
            log.info(GID.log() + VOTE_TEXT + "." + index + ": " + voteChoices.get(index));
        }
    }

    private String chooseOutcome(int round) {
        if (! voteBefore(round)) {
            outcomes.put(round, "");
            return "";
        }
        Set<Integer> allBest = highestVoteTotals(round);
        if (allBest.size() == 1) {
            Integer bestIndex = allBest.iterator().next();
            return chosen(round, bestIndex);
        }
        double choice = Math.random() * allBest.size();
        int fallBackIndex = 0;
        int i = 0;
        for (Iterator<Integer> it = allBest.iterator(); it.hasNext(); i++) {
            Integer candidate = it.next();
            if (Math.floor(choice) <= i && i <= Math.ceil(choice)) {
                return chosen(round, candidate);
            } else {
                fallBackIndex = candidate;
            }
        }
        Logger log = Logger.getLogger(VotingSession.class);
        log.warn(GID.log() + "Didn't choose a vote winner randomly; using last candidate.");
        log.info(GID.log() + CHOSEN + fallBackIndex);
        return chosen(round, fallBackIndex);
    }

    private String chosen(int round, Integer candidate) {
        Logger logger = Logger.getLogger(VotingSession.class);

        String chosen = voteChoices.get(candidate);
        outcomes.put(round, chosen);
        logger.info(GID.log() + CHOSEN + chosen + "(" + candidate + ")");
        return chosen;
    }

    private Set<Integer> highestVoteTotals(int round) {
        countVotes(round);
        Map<Integer, Integer> voteTotals = allVoteTotals.get(round);
        Set<Integer> counts = voteTotals.keySet();

        Set<Integer> currentBest = new HashSet<Integer>();
        if (counts.size() == 0) {
            return voteChoices.keySet();
        }
        int bestTotal = 0;
        for (Iterator<Integer> it = counts.iterator(); it.hasNext();) {
            Integer proposal = it.next();
            Integer count = voteTotals.get(proposal);
            if (count == bestTotal) {
                currentBest.add(proposal);
            } else if (count > bestTotal) {
                currentBest = new HashSet<Integer>();
                bestTotal = count;
                currentBest.add(proposal);
            }
        }
        return currentBest;
    }

    private void countVotes(int round) {
        Map<Integer, Integer> voteTotals = new HashMap<Integer, Integer>();
        Logger l = Logger.getLogger(VotingSession.class);

        Map<User, Integer> currentVotes = votes.get(round);
        Set<User> subjects = currentVotes.keySet();
        for (Iterator<User> it = subjects.iterator(); it.hasNext();) {
            User subject = it.next();
            Integer proposal = currentVotes.get(subject);
            l.info(GID.log() + subject.getName() + VOTED_FOR + proposal);
            Integer votesOrNull = voteTotals.get(proposal);
            int votes;
            if (votesOrNull == null) {
                votes = 0;
            } else {
                votes = votesOrNull;
            }
            voteTotals.put(proposal, votes + 1);
        }
        allVoteTotals.put(round, voteTotals);
    }

/// WEB DISPLAY ///////////////////////////////////////////////////////////

    public String[] experimenterButtons() {
        String[] buttons;
        if (getCurrentRound() == 0) {
            buttons = new String[] { startRoundActionLabel() };
        } else {
            buttons = new String[] {
                    startRoundActionLabel(),
                    stopRoundActionLabel() + " " + getCurrentRound(),
                    ExperimenterScreen.STOP_VOTING_ACTION,
            };
        }
        return buttons;
    }

    public String stateSpecificDisplay() {
        Iterator iterator = playerNameSortedIterator();
        StringBuffer buff = new StringBuffer();
        printVoteSummaryTableHeader(buff);
        while (iterator.hasNext()) {
            String playerName = (String) iterator.next();
            User player = getUserOrNull(playerName);
            if (null == player) {
                continue;
            }
            HtmlRow.startTag(buff);
            buff.append(HtmlSimpleElement.printTableCell(playerName));
            for (Iterator<Integer> it = voteBefore.iterator(); it.hasNext();) {
                int round = it.next();
                Integer vote = getVote(player, round);
                String voteString;
                if (vote == null) {
                    voteString = "&nbsp;";
                } else {
                    voteString = vote.toString();
                }
                buff.append(HtmlSimpleElement.printTableCell(voteString));
            }

            HtmlRow.endTag(buff);
        }
        HtmlTable.endTagWithP(buff);
        return "<p><p>\n\t\t\t<center><h2>Votes</h2></center>\n\t\t\t"
                + buff.toString();
    }

    private void printVoteSummaryTableHeader(StringBuffer buff) {
        String[] columnLabels = new String[voteBefore.size() + 1];
        HtmlRow.labelFirstColumn("trader", columnLabels);
        HtmlRow.labelColumns(voteBefore, columnLabels, getRoundLabel());
        HtmlTable.start(buff, columnLabels);
    }

    String stateSpecificButtons(String[] buttons, String targetPage, String userName, String claimName, StringBuffer buff) {
        HtmlTable.startWideBorderLess(buff, "lightgray", "6");
        buff.append("\n\t<tr><td><center>");
        buff.append(getVotePrompt());
        buff.append("<br><center>");
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                inputForm(buff, buttons[i], targetPage, claimName, userName);
            }
        }
        buff.append("   </td></tr>");
        buff.append("\n   </table>\n");
        User user = getUserOrNull(userName);
        if (user != null) {
            Integer vote = getVote(user, getCurrentRound() + 1);
            if (vote != null) {
                buff.append("<center>Voted for '").append(voteChoices.get(vote)).append("'</center>");
            }
        }
        return buff.toString();
    }

    void inputForm(StringBuffer buff, String roundAction, String targetPage, String claimName, String userName) {
        buff.append(HtmlSimpleElement.formHeaderWithPost(targetPage).toString());
        buff.append(HtmlSimpleElement.submitInputField(roundAction));
        buff.append(HtmlSimpleElement.hiddenInputField("claimName", claimName));
        buff.append(HtmlSimpleElement.hiddenInputField("userName", userName));
        buff.append("\n\t\t</form>\n");
    }

    private String getVotePrompt() {
        String prompt = props.getProperty(VOTE_PROMPT);
        if (prompt == null || prompt.equals("")) {
            return DEFAULT_VOTE_PROMPT;
        } else {
            return prompt.trim();
        }
    }

    public String[] stateSpecificTraderButtons() {
        final String[] buttons = new String[voteChoices.size()];
        for (int i = 0; i < voteChoices.size(); i++) {
            buttons[i] = voteChoices.get(i + 1);
        }
        return buttons;
    }

    public String stateSpecificTraderHtml(final String claimName, final String userName) {
        final Session session = SessionSingleton.getSession();
        if (session == null) {
            return "";
        }

        final StringBuffer buff = new StringBuffer();
        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(getCurrentRound()) {
            public void voting() {
                String[] buttons = session.stateSpecificTraderButtons();
                String targetPage = "TraderSubSubFrame.jsp";
                stateSpecificButtons(buttons, targetPage, userName, claimName, buff);
            }
        };
        getVotingPhase().informVoting(ad);

        return buff.toString();
    }

    public String showEarningsSummary(final String userName) {
        final StringBuffer buff = new StringBuffer();

        VotingStatusAdaptor ad = new NoActionVotingStatusAdaptor(getCurrentRound()) {
            public void trading() { renderScore(userName, buff); }
            public void voting() { renderScoreAndExplanation(userName, buff); }
            public void showingScores() { renderScoreAndExplanation(userName, buff); }
        };
        ifScoring(ad);
        ifVoting(ad);
        ifTrading(ad);
        return buff.toString();
    }

    public void webAction(String userName, final String parameter) {
        Session session = SessionSingleton.getSession();
        final User user = session.getUserOrNull(userName);
        if (user != null) {
            int choice = voteChoice(parameter);
            if (choice > -1) {
                setVote(getUserOrNull(userName), choice);
            }
        }
    }

    private int voteChoice(String parameter) {
        for (int i = 1; i <= voteChoices.size(); i++) {
            if (voteChoices.get(i).equals(parameter)) {
                return i;
            }
        }
        return -1;
    }
}
