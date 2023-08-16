package net.commerce.zocalo.JspSupport;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.experiment.*;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Properties;

public class JudgeScreenTest extends SessionTestCase {
    final private String emptyCellString = "<td>&nbsp;</td>";
    final private String rowHeader = "<tr>";
    final private String emptyCell = HtmlSimpleElement.printTableCell("");
    final private String tableHeader = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}>\n" +
                "<tr><th>round 1</th><th>round 2</th><th>round 3</th><th>Total</th></tr>\n";
    final private String emptyCells = tableHeader + rowHeader +
                emptyCellString + emptyCellString + emptyCellString + emptyCellString + "</tr></table><p>";

    public void testSubmitScore() throws ScoreException {
        session.startSession();
        session.endTrading(false);
        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());
        screen.setPriceEstimate("35");
        screen.processRequest(null, null);
        JudgingSession jSession = (JudgingSession)SessionSingleton.getSession();
        assertQEquals(35, jSession.getJudgeOrNull(judgeD.getName()).getEstimate(1));
        assertQEquals(35, judgeD.getEstimate(1));
        assertEquals("35", screen.getPriceEstimate());
        assertEquals("35", screen.currentEstimate());
    }

    public void testCommonMessages() throws ScoreException {
        session.startSession();
        session.endTrading(false);
        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());
        assertEquals("Some players are trying to raise the apparent price", screen.getCommonMessages());
    }

    public void testJspRequiredMethods() throws ScoreException {
        session.startSession();
        JudgeScreen screen = new JudgeScreen();
        assertEquals(session.getClaim().getName(), screen.getClaimName());
    }

    public void testScoreDisplayHtml() throws ScoreException {
        Price fiftyCents = Price.dollarPrice("50");
        String cellTwoHundred = HtmlSimpleElement.printTableCell("200");
        String cellTotal = HtmlSimpleElement.printTableCell("400");

        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());
        assertREMatches(emptyCells, screen.getScoresHtml());
        session.startSession();
        session.endTrading(false);
        judgeD.setEstimate(1, fiftyCents);
        assertEquals("", judgeD.getWarningsHtml());
        assertREMatches(emptyCells, screen.getScoresHtml());
        JudgingSession jSession = (JudgingSession)session;

        jSession.endScoringPhase();

        StringBuffer expect1 = new StringBuffer();
        expect1.append(tableHeader + rowHeader);
        expect1.append(cellTwoHundred);
        expect1.append(emptyCell);
        expect1.append(emptyCell);
        expect1.append(cellTwoHundred);
        expect1.append("</tr></table><p>");

        assertREMatches(expect1.toString(), screen.getScoresHtml());

        session.startNextRound(0);
        session.endTrading(false);
        judgeD.setEstimate(2, fiftyCents);
        assertEquals("", judgeD.getWarningsHtml());
        assertEquals("", judgeD.getWarningsHtml());
        assertEquals("", judgeD.getWarningsHtml());
        jSession.endScoringPhase();

        StringBuffer expect2 = new StringBuffer();
        expect2.append(tableHeader + rowHeader);
        expect2.append(cellTwoHundred);
        expect2.append(cellTwoHundred);
        expect2.append(emptyCell);
        expect2.append(cellTotal);
        expect2.append("</tr></table><p>");

        assertREMatches(expect2.toString(), screen.getScoresHtml());
        session.startNextRound(0);
        judgeD.setEstimate(3, fiftyCents);
        assertEquals("", judgeD.getWarningsHtml());
        assertREMatches(expect2.toString(), screen.getScoresHtml());
    }

    public void testGuessDisplayHtml() {
        final String noGuessString = "<td>50</td>";

        String tableHeader = "<table (cellpadding='3' |align='center' |cellspacing='0' |border='1' ){4}>\n" +
                    "<tr><th>round 1</th><th>round 2</th><th>round 3</th></tr>\n";
        String emptyCells = tableHeader  + "<tr>" +
                noGuessString + noGuessString + noGuessString + "</tr></table><p>";
        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());

        assertREMatches(emptyCells, screen.getGuessesTableHtml());
    }

    public void testLockOutScoring() throws ScoreException {
        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());

        session.startSession();
        session.endTrading(false);
        assertEquals(1, session.getCurrentRound());
        assertEstimate(screen, 10, judgeD, 1);
        assertEstimate(screen, 20, judgeD, 1);
        assertQEquals(50, judgeD.getEstimate(2));

        session.endTrading(false);
        assertEstimate(screen, 30, judgeD, 1);
        assertEquals(1, session.getCurrentRound());

        ((JudgingSession) session).endScoringPhase();
        assertEquals(1, session.getCurrentRound());
        assertEstimate(screen, 40, 30, judgeD, 1);


        session.startNextRound();
        assertEquals(2, session.getCurrentRound());
        assertEstimate(screen, 55, judgeD, 2);

        session.endTrading(false);
        assertEquals(2, session.getCurrentRound());

        assertEstimate(screen, 35, judgeD, 2);

        ((JudgingSession) session).endScoringPhase();
        assertEquals(2, session.getCurrentRound());
        assertEstimate(screen, 45, 35, judgeD, 2);


        session.startNextRound();
        assertEquals(3, session.getCurrentRound());
        session.endTrading(false);
        ((JudgingSession) session).endScoringPhase();

        assertEstimate(screen, 50, judgeD, 3);

        ((JudgingSession) session).endScoringPhase();  // now Judges have entered scores
    }

    private void assertEstimate(JudgeScreen screen, double guess, Judge judge, int round) {
        setEstimateViaRequest(screen, guess);
        assertEstimateValue(guess, judge, round);
    }

    private void assertEstimate(JudgeScreen screen, double guess, double actual, Judge judge, int round) {
        setEstimateViaRequest(screen, guess);
        assertEstimateValue(actual, judge, round);
    }

    private void setEstimateViaRequest(JudgeScreen screen, double guess) {
        screen.setPriceEstimate(Double.toString(guess));
        screen.processRequest(null, null);
    }

    private void assertEstimateValue(double guess, Judge judge, int round) {
        JudgingSession jSession = (JudgingSession)SessionSingleton.getSession();
        assertQEquals(guess, jSession.getJudgeOrNull(judge.getName()).getEstimate(round));
        assertQEquals(guess, judge.getEstimate(round));
    }

    public void testDormantPlayers() throws IOException, ScoreException, DuplicateOrderException, IncompatibleOrderException {
        final String simpleConfig = "sessionTitle: DormantPlayersTest\n" +
                "rounds: 3\n" +
                "players: j1, j2, m3, t4, t5\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "j1.role: judge\n" +        // never dormant
                "j2.role: judge\n" +        // sometimes dormant
                "m3.role: manipulator\n" +  // player sometimes dormant
                "t4.role: trader\n" +       // never dormant
                "t5.role: trader\n" +       // never dormant
                "actualValue:    0,  100,  40\n" +
                "endowment.trader: 100\n" +
                "endowment.manipulator: 100\n" +
                "j2.dormantRounds: 2,3\n" +
                "m3.dormantRounds: 2\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "m3.target:  40,         40,          100\n" +
                "tickets.manipulator: 5\n" +
                "tickets.trader: 5";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        SessionSingleton.setSession(props, null);
        JudgingSession session = (JudgingSession) SessionSingleton.getSession();
        Judge j1 = (Judge) session.getPlayer("j1");
        Judge j2 = (Judge) session.getPlayer("j2");
        TradingSubject m3 = (TradingSubject) session.getPlayer("m3");
        TradingSubject t4 = (TradingSubject) session.getPlayer("t4");
        TradingSubject t5 = (TradingSubject) session.getPlayer("t5");
        BinaryMarket market = session.getMarket();

        session.startSession();   //////////////////////////// Round 1
        int currentRound = session.getCurrentRound();

        assertQEquals(100, m3.balance());
        assertQEquals(100, t4.balance());
        assertQEquals(100, t5.balance());
        assertFalse(j1.isDormant(currentRound));
        assertFalse(j2.isDormant(currentRound));
        assertTrue(m3.canBuy(currentRound));
        assertTrue(m3.canSell(currentRound));
        assertTrue(t4.canBuy(currentRound));
        assertTrue(t4.canSell(currentRound));

        BinaryClaim claim = market.getBinaryClaim();
        Position no = claim.getNoPosition();
        Position yes = claim.getYesPosition();
        assertQEquals(5, m3.currentCouponCount(claim));

        Price seventyFive = Price.dollarPrice(75);
        limitOrder(market, no, seventyFive.inverted(), 1, m3.getUser());
        marketOrder(market, yes, seventyFive, 1, t4.getUser());

        assertQEquals(4, m3.currentCouponCount(claim));

        session.endTrading(true);

        j1.setEstimate(1, seventyFive);
        j2.setEstimate(1, Price.dollarPrice(95));
        session.endScoringPhase();

        assertQEquals(137.5, j1.getScore(1));
        assertQEquals(69.5, j2.getScore(1));
        assertQEquals(110 + 175, m3.getScore(1));
        assertQEquals(100 - 75, t4.getScore(1));
        assertQEquals(100, t5.getScore(1));

        session.startNextRound();   /////////////////////////// Round 2
        currentRound = session.getCurrentRound();
        assertFalse(j1.isDormant(currentRound));
        assertTrue(j2.isDormant(currentRound));
        assertFalse(m3.canBuy(currentRound));
        assertFalse(m3.canSell(currentRound));
        assertTrue(t4.canBuy(currentRound));
        assertTrue(t4.canSell(currentRound));

        limitOrder(market, no, seventyFive.inverted(), 1, t4.getUser());
        assertQEquals(5, m3.currentCouponCount(claim));

        marketOrder(market, yes, seventyFive, 1, t5.getUser());
        assertQEquals(4, t4.currentCouponCount(claim));
        assertQEquals(6, t5.currentCouponCount(claim));

        session.endTrading(true);

        j1.setEstimate(2, seventyFive);
        session.endScoringPhase();

        assertQEquals(250 - 12.5, j1.getScore(2));
        assertQEquals(0.0, j2.getScore(2));
        assertQEquals(0.0, m3.getScore(2));
        assertQEquals(175 + (100 * 4), t4.getScore(2));
        assertQEquals(25 + (100 * 6), t5.getScore(2));

        session.startNextRound();   /////////////////////////// Round 3
        currentRound = session.getCurrentRound();
        assertFalse(j1.isDormant(currentRound));
        assertTrue(j2.isDormant(currentRound));
        assertTrue(m3.canBuy(currentRound));
        assertTrue(m3.canSell(currentRound));
        assertTrue(t4.canBuy(currentRound));
        assertTrue(t4.canSell(currentRound));

        limitOrder(market, no, seventyFive.inverted(), 1, t4.getUser());
        marketOrder(market, yes, seventyFive, 1, t5.getUser());
        assertQEquals(4, t4.currentCouponCount(claim));
        assertQEquals(6, t5.currentCouponCount(claim));

        session.endTrading(true);

        j1.setEstimate(3, seventyFive);
        session.endScoringPhase();

        assertQEquals(250 - ((75-40) * (75-40) * .02), j1.getScore(3));
        assertQEquals(0.0, j2.getScore(3));
        assertQEquals(100 + (40.0 * 5) + 200 - (2 * (100 - 75)), m3.getScore(3));
        assertQEquals(175 + (40.0 * 4), t4.getScore(3));
        assertQEquals(25 + (40.0 * 6), t5.getScore(3));
    }
}
