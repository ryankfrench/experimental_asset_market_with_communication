package net.commerce.zocalo.experiment;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.io.IOException;
import java.io.StringWriter;

import org.apache.log4j.*;
import net.commerce.zocalo.experiment.role.Trader;
import net.commerce.zocalo.JspSupport.AccountDisplay;
import net.commerce.zocalo.JspSupport.ClaimPurchase;
import net.commerce.zocalo.user.User;

public class UnJudgedSessionTest extends SessionTestCase {
    public void testJudgedExplicitlySession() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "roles: trader, judge, manipulator\n" +
                "players: trader1, manip1, judge1\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "manip1.target: 60\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.manipulator: 0\n" +
                "scoringConstant.manipulator: 0\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        assertNotNull(((JudgingSession)session).getJudgeOrNull("judge1"));
        assertNotNull(session.getPlayer("trader1"));
        assertNotNull(session.getPlayer("manip1"));

        setupSession(config + "players: trader1, judge1\n");
        assertNotNull(((JudgingSession)session).getJudgeOrNull("judge1"));
        assertNotNull(session.getPlayer("trader1"));
        assertNull(session.getPlayer("manip1"));
    }

    public void testJudgedByDefaultSession() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "players: trader1, manip1, judge1\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "manip1.target: 60\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.manipulator: 0\n" +
                "scoringConstant.manipulator: 0\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        assertNotNull(((JudgingSession)session).getJudgeOrNull("judge1"));
        assertNotNull(session.getPlayer("trader1"));
        assertNotNull(session.getPlayer("manip1"));

        setupSession(config + "players: trader1, judge1\n");
        assertNotNull(((JudgingSession)session).getJudgeOrNull("judge1"));
        assertNotNull(session.getPlayer("trader1"));
        assertNull(session.getPlayer("manip1"));
    }

    public void testUnjudgedSession() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "roles: traderA, traderB\n" +
                "players: trader1, trader2, trader3\n" +
                "trader1.role: traderA\n" +
                "trader2.role: traderB\n" +
                "trader3.role: traderA\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.traderA: 200\n" +
                "endowment.traderB: 300\n" +
                "tickets.traderA: 11\n" +
                "tickets.traderB: 20\n";

        StringWriter stringWriter = new StringWriter();
        WriterAppender appender = new WriterAppender(new PatternLayout("%d{yyyy/MM/dd hh:mm:ss} %6p - %12c{1} - %m\n"), stringWriter);
        BasicConfigurator.configure(appender);

        setupSession(config);
        assertRENoMatch(".*ERROR.*", stringWriter.toString());

        session.startNextRound();
        Trader trader1 = (Trader) session.getPlayer("trader1");
        assertNotNull(trader1);
        assertQEquals(200, trader1.getUser().cashOnHand());
        StringBuffer trader1Buf = new StringBuffer();
        String page1 = ClaimPurchase.claimPurchasePage(trader1.getUser());
        final User trader1User = trader1.getUser();
        trader1User.displayAccounts(trader1Buf, AccountDisplay.allHoldingsPrinter(trader1User));
        assertREMatches(".*11.*", trader1Buf.toString());
        Trader trader2 = (Trader) session.getPlayer("trader2");
        StringBuffer trader2Buf = new StringBuffer();
        String page2 = ClaimPurchase.claimPurchasePage(trader2.getUser());
        trader2.getUser().displayAccounts(trader2Buf, null, AccountDisplay.claimHoldingsBinaryMarketPrinter());
        assertNotNull(trader2);
        assertQEquals(300, trader2.getUser().cashOnHand());
        assertREMatches(".*20.*", trader2Buf.toString());
    }

    public void testIncompleteRoles() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "roles: traderA, traderB\n" +
                "players: trader1, trader2, trader3\n" +
                "trader1.role: traderA\n" +
                "trader2.role: traderB\n" +
                "trader3.role: traderA\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.traderA: 200\n" +
                "endowment.traderB: 300\n" +
//                "tickets.traderA: 10\n" +
                "tickets.traderB: 20\n";

        StringWriter stringWriter = new StringWriter();
        WriterAppender appender = new WriterAppender(new PatternLayout("%d{yyyy/MM/dd hh:mm:ss} %6p - %12c{1} - %m\n"), stringWriter);
        BasicConfigurator.configure(appender);

        setupSession(config);
        assertREMatches(".*WARN.*", stringWriter.toString());
        assertMatches("'tickets.traderA' is null", session.getErrorMessage());
    }

    public void testOmittedJudge() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "roles: trader, manipulator\n" +
                "players: trader1, manip1, judge1\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "manip1.target: 60\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.manipulator: 0\n" +
                "scoringConstant.manipulator: 0\n";

        StringWriter stringWriter = new StringWriter();
        WriterAppender appender = new WriterAppender(new PatternLayout("%d{yyyy/MM/dd hh:mm:ss} %6p - %12c{1} - %m\n"), stringWriter);
        BasicConfigurator.configure(appender);

        setupSession(config);
        assertREMatches(".*ERROR.*", stringWriter.toString());
        assertMatches("Judge role required if manipulators are used.", session.getErrorMessage());
    }

    public void testMissingPlayer() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "timeLimit: 5\n" +
                "roles: traderA, traderB\n" +
                "players: trader1, trader2, trader3\n" +
                "trader1.role: traderA\n" +
//                "trader2.role: traderB\n" +
                "trader3.role: traderA\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "endowment.traderA: 200\n" +
                "endowment.traderB: 300\n" +
                "tickets.traderA: 10\n" +
                "tickets.traderB: 20\n";

        StringWriter stringWriter = new StringWriter();
        WriterAppender appender = new WriterAppender(new PatternLayout("%d{yyyy/MM/dd hh:mm:ss} %6p - %12c{1} - %m\n"), stringWriter);
        BasicConfigurator.configure(appender);

        setupSession(config);
        assertREMatches(".*ERROR.*", stringWriter.toString());
        assertMatches("player 'trader2' has no assigned Role.", session.getErrorMessage());

        session.startNextRound();
        Trader trader3 = (Trader) session.getPlayer("trader3");
        assertNotNull(trader3);
    }
}
