package net.commerce.zocalo.JspSupport;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.experiment.*;
import net.commerce.zocalo.experiment.states.NoActionStatusAdaptor;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Price;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import java.io.File;
import java.io.StringBufferInputStream;
import java.io.IOException;
import java.util.Properties;

public class ExperimenterScreenTest extends SessionTestCase {
    public void testScoreDisplay() {
        ExperimenterScreen screen = new ExperimenterScreen();
        String scoresHtml = screen.getScoresHtml();
        int traderAIndex = scoresHtml.indexOf(traderA.getName());
        int traderBIndex = scoresHtml.indexOf(traderB.getName());
        int manipulatorCIndex = scoresHtml.indexOf(manipulatorC.getName());
        int judgeDIndex = scoresHtml.indexOf(judgeD.getName());
        assertTrue(traderAIndex > 1);
        assertTrue(traderBIndex > 1);
        assertTrue(manipulatorCIndex > 1);
        assertTrue(judgeDIndex > 1);
        assertTrue(judgeDIndex < manipulatorCIndex);
        assertTrue(manipulatorCIndex < traderAIndex);
        assertTrue(traderAIndex < traderBIndex);
        String[] splitOnEmptyCells = scoresHtml.split("<td>&nbsp;</td>");

        int rounds = SessionSingleton.getSession().rounds();
        assertEquals(((rounds + 1) * 4) + 1, splitOnEmptyCells.length);
    }

    public void testGuessDisplay() throws ScoreException {
        ExperimenterScreen screen = new ExperimenterScreen();
        assertTrue(screen.stateSpecificDisplay().indexOf("50") > 1);
        session.startSession();
        judgeD.setEstimate(1, Price.dollarPrice(37));
        assertTrue(screen.stateSpecificDisplay().indexOf("50") > 1);
        assertEquals("", judgeD.getWarningsHtml());
        assertTrue(screen.stateSpecificDisplay().indexOf("50") > 1);
        assertTrue(screen.stateSpecificDisplay().indexOf("37") > 1);
    }

    public void testLogFileDisplay() throws ScoreException {
        ExperimenterScreen screen = new ExperimenterScreen();
        File logDir = new File("logs");
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        SessionSingleton.setSession(props, "ExperimentScreenTest");
        JudgingSession fileSession = (JudgingSession)SessionSingleton.getSession();
        judgeD = fileSession.getJudgeOrNull("judgeD");
        assertTrue(screen.linkForLogFile().matches("Logging to logs/.*"));
        fileSession.startSession();
        fileSession.endTrading(false);
        judgeD.setEstimate(1, Price.dollarPrice(37));
        assertEquals("", judgeD.getWarningsHtml());
        fileSession.endScoringPhase();
        fileSession.startNextRound();
        fileSession.endTrading(false);
        judgeD.setEstimate(2, Price.dollarPrice(67));
        assertEquals("", judgeD.getWarningsHtml());
        fileSession.endScoringPhase();
        fileSession.startNextRound();
        judgeD.setEstimate(3, Price.dollarPrice(55));
        assertEquals("", judgeD.getWarningsHtml());
        fileSession.endTrading(false);
        assertTrue(screen.linkForLogFile().matches("Logging to logs/.*"));
        fileSession.endScoringPhase();
        assertREMatches(".*Download.*<a href='../logs/.*", screen.linkForLogFile());
    }

    public void testVoting() throws IOException {
        String voteConfig = "sessionTitle: voting\n" +
                "rounds: 4\n" +
                "players: trader1, trader2, trader3\n" +
                "timeLimit: 3:50\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "\n" +
                "dividendValue:      10,    30,    40\n" +
                "\n" +
                "voteAlternatives: 3\n" +
                "voteText.1: The price is too high\n" +
                "voteText.2: The price is too low\n" +
                "voteText.3: The price is correct\n" +
                "voteBeforeRounds: 2, 4\n" +
                "";
        Properties props = new Properties();
        props.load(new StringBufferInputStream(voteConfig));
        SessionSingleton.setSession(props, "ExperimentScreenVoteTest");
        VotingSession session = (VotingSession)SessionSingleton.getSession();
        User t1 = session.getUserOrNull("trader1");
        User t2 = session.getUserOrNull("trader2");
        User t3 = session.getUserOrNull("trader3");
        ExperimenterScreen screen = new ExperimenterScreen();
        screen.setAction(session.startRoundActionLabel());
        HttpServletRequest request = wrappedMockRequest((Cookie)null);
        screen.processRequest(request, new MockHttpServletResponse());

        String noVotes = ".*<td>&nbsp;</td><td>&nbsp.*";
        String voteOne = ".*<td>1</td><td>&nbsp.*";
        String voteThree = ".*<td>3</td><td>&nbsp.*";
        String trader1None = ".*trader1" + noVotes;
        String trader1One = ".*trader1" + voteOne;
        String trader2One = ".*trader2" + voteOne;
        String trader3Three = ".*trader3" + voteThree;

        assertREMatches(trader1None, screen.stateSpecificDisplay());
        session.endTrading(true);
        session.setVote(t1, 1);
        assertREMatches(trader1One, screen.stateSpecificDisplay());
        session.setVote(t2, 1);
        assertREMatches(trader2One, screen.stateSpecificDisplay());
        session.setVote(t3, 3);
        assertREMatches(trader3Three, screen.stateSpecificDisplay());

        screen.setAction(ExperimenterScreen.STOP_VOTING_ACTION);
        screen.processRequest(request, new MockHttpServletResponse());
        session.setVote(t2, 3);
        assertREMatches(trader2One, screen.stateSpecificDisplay());
        final boolean[] scoring = new boolean[]{false};
        session.ifScoring(new NoActionStatusAdaptor() {
            public void showingScores() {
                scoring[0] = true;
            }
        });
        assertTrue(scoring[0]);

        screen.setAction(session.startRoundActionLabel());
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(2, session.getCurrentRound());
    }
}
