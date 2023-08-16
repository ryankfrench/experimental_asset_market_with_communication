package net.commerce.zocalo.experiment;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.JspSupport.JudgeScreen;
import net.commerce.zocalo.JspSupport.TraderScreen;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.experiment.role.*;
import net.commerce.zocalo.experiment.states.StatusAdaptor;
import net.commerce.zocalo.experiment.states.NoActionStatusAdaptor;
import net.commerce.zocalo.ajax.dispatch.BidUpdateDispatcher;
import net.commerce.zocalo.ajax.dispatch.MockBayeux;
import net.commerce.zocalo.service.PropertyHelper;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.currency.Currency;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

public class SessionTest extends SessionTestCase {
    private String SESSION_NAME = "SessionTestCase";

    public void testCreateSession() {
        assertEquals(3, session.rounds());
        assertEquals("The ticket value is not 100.", session.getPriceHint("traderA", 1));
        assertEquals("Your score will improve if the judge thinks the tickets are worth 100",
                     session.getEarningsHint("manipulatorC", 3));
        assertEquals("The ticket value is not 0.",
                     session.getPriceHint("manipulatorC", 3));
        assertEquals("Some players are trying to change the apparent price", session.message(2));
        assertEquals(5 * 60, session.timeLimit());
        assertQEquals(20.0, ((JudgingSession)session).initialManipulatorTickets());
        assertQEquals(30.0, session.initialTraderTickets());
    }

    public void testInitializeSession() {
        assertTrue("ManipulatorC should be a Manipulator", manipulatorC instanceof Manipulator);
        assertTrue("traderA should be a trader", traderA instanceof Trader);
        assertEquals(0, session.getCurrentRound());
        assertTrue("JudgeD should be a judge", judgeD instanceof Judge);
        assertEquals("Trading has not started yet.", judgeD.getHint());
        assertEquals("Trading has not started yet.", traderA.getHint());
        assertEquals("Trading has not started yet.", manipulatorC.getHint());
        assertTrue("Market should be inactive to start", ! session.marketIsActive());
    }

    public void testBrokenProps() throws IOException {
        String broken = "sessionTitle: broken\nplayers: unknown\nrounds: 3\ntimeLimit:3";
        Properties brokenProps = new Properties();
        brokenProps.load(new StringBufferInputStream(broken));
        Session brokenSession = new Session(brokenProps, "foo", new MockBayeux());
        assertNull("unknown player should be a trader", brokenSession.getPlayer("unknown"));
    }

    public void testStartFirstRound() throws ScoreException {
        session.startSession();
        assertQEquals(Quantity.Q100, traderA.balance());
        assertQEquals(20.0, manipulatorC.currentCouponCount(session.getClaim()));
        assertQEquals(30.0, traderB.currentCouponCount(session.getClaim()));
        assertEquals("The ticket value is not 100.", traderA.getHint());
        assertTrue(session.marketIsActive());
        assertQEquals(0.0, traderA.totalScore());
        assertQEquals(0.0, traderB.totalScore());
        assertQEquals(0.0, manipulatorC.totalScore());
        assertQEquals(0.0, judgeD.totalScore());
        assertTrue(session.marketIsActive());
        assertEquals("The ticket value is not 100.", traderA.getHint());
        assertEquals("The ticket value is not 100.<br>" +
                "Your score will improve if the judge thinks the tickets are worth 40", manipulatorC.getHint());
        assertEquals("", judgeD.getHint());
    }

    public void testEndFirstRound() throws ScoreException {
        session.startSession();
        session.endTrading(false);
        assertQEquals(0.0, traderA.totalScore());
        assertQEquals(0.0, traderB.totalScore());
        assertQEquals(0.0, manipulatorC.totalScore());
        assertFalse(session.marketIsActive());
    }

    public void testJudgingFirstRound() throws ScoreException {
        JudgeScreen screen = new JudgeScreen();
        screen.setUserName(judgeD.getName());
        session.startSession();
        assertEquals("disabled", screen.disabledFlag(session.getCurrentRound()));
        session.endTrading(false);
        assertEquals("", screen.disabledFlag(session.getCurrentRound()));
        screen.setPriceEstimate("40");
        screen.processRequest(null, null);
        Session session = SessionSingleton.getSession();
        assertQEquals(40, ((JudgingSession)session).getJudgeOrNull(judgeD.getName()).getEstimate(1));
        assertQEquals(40, ((JudgingSession)session).judgedValue(1));
    }

    public void testGettingMarket() throws ScoreException {
        session.startSession();
        assertEquals(SESSION_NAME, session.getMarket().getName());
    }

    public void testGettingUsers() throws ScoreException {
        session.startSession();
        String traderAName = "traderA";
        assertEquals(traderAName, session.getUserOrNull(traderAName).getName());
    }

    public void testScoring() throws ScoreException {
        double actualOne = actualValue(1);
        double actualTwo = actualValue(2);
        double actualThree = actualValue(3);

        double targetOne = target(manipulatorC, 1);
        double targetTwo = target(manipulatorC, 2);
        double targetThree = target(manipulatorC, 3);

        double estimateOne = 45;
        double estimateTwo = 25;
        double estimateThree = 35;

        double traderEndowment = 1.00;
        double manipulatorEndowment = 0.50;
        double manipulatorFactor = 2;
        double manipulatorConstant = 200;
        double judgeFactor = 0.02;
        double judgeConstant = 250;
        double traderTickets = 30;
        double manipulatorTickets = 20;

        // first round
        session.startSession();
        session.endTrading(false);
        setEstimate(judgeD, 1, "" + estimateOne);
        ((JudgingSession)session).endScoringPhase();

        assertQEquals((Currency.CURRENCY_SCALE * traderEndowment) + (traderTickets * actualOne), traderA.getScore(1));
        assertQEquals(judgeConstant - judgeFactor * (estimateOne - actualOne) * (estimateOne - actualOne), judgeD.getScore(1));
        assertQEquals((Currency.CURRENCY_SCALE * manipulatorEndowment) + (manipulatorTickets * actualOne) + manipulatorConstant
                - (manipulatorFactor * (estimateOne - targetOne)), manipulatorC.getScore(1));

        // second round
        session.startNextRound(0);
        session.endTrading(false);
        setEstimate(judgeD, 2, "" + estimateTwo);
        ((JudgingSession)session).endScoringPhase();

        assertQEquals((Currency.CURRENCY_SCALE * traderEndowment) + (traderTickets * actualTwo), traderA.getScore(2));
        assertQEquals((Currency.CURRENCY_SCALE * manipulatorEndowment) + (manipulatorTickets * actualTwo) + manipulatorConstant
                   - (manipulatorFactor * (targetTwo - estimateTwo)), manipulatorC.getScore(2));
        assertQEquals(judgeConstant - judgeFactor * (actualTwo - estimateTwo) * (actualTwo - estimateTwo), judgeD.getScore(2));

        // third round
        session.startNextRound(0);
        session.endTrading(false);
        setEstimate(judgeD, 3, "" + estimateThree);
        ((JudgingSession)session).endScoringPhase();

        assertQEquals((Currency.CURRENCY_SCALE * traderEndowment) + (traderTickets * actualThree), traderA.getScore(3));
        assertQEquals((Currency.CURRENCY_SCALE * manipulatorEndowment) + (manipulatorTickets * actualThree) + manipulatorConstant
                        - (manipulatorFactor * (targetThree - estimateThree)),
                manipulatorC.getScore(3));
        assertQEquals(judgeConstant - judgeFactor * (actualThree - estimateThree) * (actualThree - estimateThree), judgeD.getScore(3));

        session.startNextRound();
        assertEquals("No more rounds.", session.getErrorMessage());
    }

    private double target(TradingSubject manipulator, int round) {
        String propName = manipulator.getName() + DOT_SEPARATOR + TARGET_PROPERTY_WORD;
        String targetString = PropertyHelper.indirectPropertyForRound(propName, round, props);
        return Double.parseDouble(targetString);
    }

    private double actualValue(int round) {
        String actualString = PropertyHelper.indirectPropertyForRound(ACTUAL_VALUE_PROPNAME, round, props);
        return Double.parseDouble(actualString);
    }

    public void testStartSecondRound() throws ScoreException {
        session.startSession();
        session.endTrading(false);
        setEstimate(judgeD, 1, "25");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        Session session = SessionSingleton.getSession();
        assertFalse(session.marketIsActive());
        session.startNextRound(0);

        assertQEquals(Quantity.Q100, traderA.balance());
        assertQEquals(20.0, manipulatorC.currentCouponCount(session.getClaim()));
        assertQEquals(30.0, traderB.currentCouponCount(session.getClaim()));

        assertTrue(session.marketIsActive());
        assertEquals("The ticket value is not 40.", traderA.getHint());
        assertEquals("The ticket value is not 0.", traderB.getHint());
        assertQEquals(Quantity.Q100, traderA.totalScore());
        assertQEquals(220.0, manipulatorC.totalScore());
        assertQEquals(237.5, judgeD.totalScore());
        assertEquals("The ticket value is not 40.", traderA.getHint());
        assertEquals("The ticket value is not 0.<br>" +
                "Your score will improve if the judge thinks the tickets are worth 40", manipulatorC.getHint());
        assertEquals("", judgeD.getHint());
        assertEquals("Some players are trying to change the apparent price", session.message());
    }

    public void testDisableShowEarnings() throws ScoreException, IOException {
        final String config = "sessionTitle: disableShowEarnings\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 5\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "manip1.target:  40,   40, 40\n" +
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
        session.startSession();
        session.endTrading(false);
        Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");
        setEstimate(judge1, 1, "25");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName("manip1");
        assertMatches("Cumulative Earnings</td>\n<td>", manipScreen.showEarningsSummary());
        manipScreen.setUserName("trader1");
        assertMatches("Cumulative Earnings</td>\n<td>", manipScreen.showEarningsSummary());

        setupSession(config + "showEarnings: false");
        session.startSession();
        session.endTrading(false);
        setEstimate(judge1, 1, "25");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        manipScreen.setUserName("manip1");
        assertTrue(manipScreen.showEarningsSummary().indexOf("Cumulative Earnings</td><td>") == -1);
        manipScreen.setUserName("trader1");
        assertTrue(manipScreen.showEarningsSummary().indexOf("Cumulative Earnings</td><td>") == -1);

    }

    public void testMessagesByRound() throws ScoreException {
        session.startSession();
        assertEquals("Some players are trying to change the apparent price", session.message(2));
        assertEquals("Some players are trying to raise the apparent price", session.message());

        session.endTrading(false);
        assertEquals("", session.message(3));
        setEstimate(judgeD, 1, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();

        session.startNextRound(0);
        assertEquals("", session.message(3));
        assertEquals("Some players are trying to change the apparent price", session.message());

        session.endTrading(false);
        assertEquals("Some players are trying to change the apparent price", session.message(2));
        assertEquals("Some players are trying to change the apparent price", session.message());

        setEstimate(judgeD, 2, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("Some players are trying to raise the apparent price", session.message(1));
        assertEquals("", session.message());
    }

    public void testClearBookAtStartOfRound() throws DuplicateOrderException, ScoreException {
        Market market = session.getMarket();
        Position onePosition = market.getClaim().positions()[0];

        session.startSession();
        makeLimitOrder(market, onePosition, 20, 1, traderA);
        assertQEquals(1, market.getBook().bestQuantity(onePosition));
        session.endTrading(false);
        assertFalse(session.marketIsActive());

        setEstimate(judgeD, 1, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);

        assertTrue(session.marketIsActive());
        assertQEquals(0, market.getBook().bestQuantity(onePosition));

        String liveTopic = BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI;
        int oldSize = livingChannel.getEvents(liveTopic).size();
        makeLimitOrder(market, onePosition, 20, 1, traderA);
        assertTrue("should have increased in size.", oldSize < livingChannel.getEvents(liveTopic).size());
    }

    private void makeLimitOrder(Market m, Position pos, Price price, int quantity, TradingSubject subject) throws DuplicateOrderException {
        m.limitOrder(pos, price, new Quantity(quantity), subject.getUser());
    }

    private void makeLimitOrder(Market m, Position pos, double price, int quantity, TradingSubject subject) throws DuplicateOrderException {
        m.limitOrder(pos, Price.dollarPrice(price), new Quantity(quantity), subject.getUser());
    }

    public void testClearAccountsAtStartOfRound() throws DuplicateOrderException, ScoreException {
        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(30, traderA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, traderB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(Quantity.Q100, traderA.balance());

        makeLimitOrder(market, onePosition, 20, 1, traderA);
        makeLimitOrder(market, onePosition, 30, 1, traderA);
        makeLimitOrder(market, anotherPosition, 20, 1, traderA);
        makeLimitOrder(market, anotherPosition, 80, 1, traderB);
        assertQEquals(31, traderA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(29, traderB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(100 + 30, traderB.balance());
        assertQEquals(100 - 30, traderA.balance());

        session.endTrading(false);
        setEstimate(judgeD, 1, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession) session).endScoringPhase();
        session.startNextRound(0);

        assertQEquals(30, traderA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, traderB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(Quantity.Q100, traderB.balance());
        assertQEquals(Quantity.Q100, traderA.balance());
    }

    public void testClearAccountsAtStartOfRoundDespiteFloatBug() throws DuplicateOrderException, ScoreException, IOException {
         final String script =
                "sessionTitle: SessionTest\n" +
                "rounds: 3\n" +
                "players: trader1, trader2, judge3, manipulator4\n" +
                "timeLimit: 5\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "judge3.role: judge\n" +
                "manipulator4.role: manipulator\n" +
                "\n" +
                "useUnaryAssets: false\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice, noMessage\n" +
                "actualValue:          0,          100,         40\n" +
                "\n" +
                "trader1.hint:         not100,     not40,       not100\n" +
                "trader2.hint:         not40,      notZero,     notZero\n" +
                "manipulator4.hint:    not100,\tnotZero,     notZero\n" +
                "manipulator4.earningsHint:    worth40,\tworth40,     worth100\n" +
                "manipulator4.target:  40,         40,          100\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";

        setupSession(script);

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject)session.getPlayer("trader2");
        final Judge judge3 = ((JudgingSession)session).getJudgeOrNull("judge3");
        TradingSubject manipulator4 = (TradingSubject) session.getPlayer("manipulator4");
        assertEquals("manipulator" , manipulator4.propertyWordForRole());

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(30, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader1.balance());

        makeLimitOrder(market, onePosition, 38, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 38, 1, trader2);
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        makeLimitOrder(market, onePosition, 55, 1, trader1);
        makeLimitOrder(market, onePosition, 77, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 77, 1, trader1);
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        makeLimitOrder(market, anotherPosition, 100 - 78, 1, trader1);
        makeLimitOrder(market, onePosition, 78, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 55, 1, trader2);
        makeLimitOrder(market, onePosition, 26, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 63, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 26, 1, trader1);
        makeLimitOrder(market, onePosition, 63, 1, trader2);
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());

        session.endTrading(false);
        setEstimate(judge3, 1, "66");
        assertEquals("", judge3.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);

        assertQEquals(30, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader1.balance());
    }

    public void testBasicTimer() throws InterruptedException, ScoreException {
        assertFalse(session.marketIsActive());

        session.startNextRound(500);
        long originalTimeRemaining = session.rawTimeRemaining();
        assertTrue(session.marketIsActive());
        assertTrue(500 >= originalTimeRemaining);
        assertTrue(300 < originalTimeRemaining);
        Thread.sleep(200);
        assertTrue(session.marketIsActive());
        long laterTimeRemaining = session.rawTimeRemaining();
        assertTrue(300 >= laterTimeRemaining);
        assertTrue(100 < laterTimeRemaining);

        Thread.sleep(400);
        assertFalse("timer problem", session.marketIsActive());
    }

    public void testMultiSessionTimerInteraction() throws InterruptedException, ScoreException, IOException {
        MockAppender appender = new MockAppender();
        BasicConfigurator.configure(appender);

        SessionSingleton.setSession(props, null);
        Session firstSession = SessionSingleton.getSession();
        assertFalse(firstSession.marketIsActive());
        firstSession.startNextRound(200);
        assertEquals(1, changeChannel.getEvents("/transition").size());
        assertTrue(firstSession.marketIsActive());

        final String fourJudgesConfig = "sessionTitle: FourJudges\n" +
                        "rounds: 1\n" +
                        "players: judgeA, judgeB, judgeC, judgeD\n" +
                        "timeLimit: 3\n" +
                        "judgeA.role: judge\n" +
                        "judgeB.role: judge\n" +
                        "judgeC.role: judge\n" +
                        "judgeD.role: judge\n" +
                        "initialHint: Trading has not started yet.\n" +
                        "actualValue: 50, 50, 50\n" +
                        "scoringFactor.judge: 0.02\n" +
                        "scoringConstant.judge: 250\n";

        Properties properties2 = new Properties();
        properties2.load(new StringBufferInputStream(fourJudgesConfig));
        SessionSingleton.setSession(properties2, null);
        Session secondSession = SessionSingleton.getSession();
        assertFalse(secondSession.marketIsActive());
        secondSession.startNextRound(100);
        assertEquals(3, changeChannel.getEvents("/transition").size());
        assertFalse(firstSession.marketIsActive());
        assertTrue(secondSession.marketIsActive());
        Thread.sleep(150);
        assertEquals(4, changeChannel.getEvents("/transition").size());
        assertFalse(firstSession.marketIsActive());
        assertFalse(secondSession.marketIsActive());
        Thread.sleep(100);
        assertEquals(4, changeChannel.getEvents("/transition").size());
    }

    public void testTimerOnlyIncrementsIfNotDone() throws InterruptedException, ScoreException {
        assertFalse(session.marketIsActive());

        session.startNextRound(500);
        long originalTimeRemaining = session.rawTimeRemaining();
        assertTrue(session.marketIsActive());
        assertTrue(500 >= originalTimeRemaining);
        assertTrue(300 < originalTimeRemaining);
        Thread.sleep(200);
        assertTrue(session.marketIsActive());
        long laterTimeRemaining = session.rawTimeRemaining();
        assertTrue(300 >= laterTimeRemaining);
        assertTrue(100 < laterTimeRemaining);
        session.endTrading(false);
        setEstimate(judgeD, 1, "45");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(500);

        Thread.sleep(400);   //  give time for previous timers to ping
        assertEquals(2, session.getCurrentRound());
        final boolean isTrading[] = new boolean[] { false };
        StatusAdaptor ad = new NoActionStatusAdaptor() {
            public void trading() { isTrading[0] = true; }
        };
        session.ifTrading(ad);
        assertTrue("timer problem", isTrading[0]);
        assertTrue(session.marketIsActive());
    }

    public void testMedianJudgeScore() throws IOException, ScoreException {
        final String threeJudgesConfig = "sessionTitle: ThreeJudges\n" +
                        "rounds: 3\n" +
                        "players: judge1, judge2, judge3\n" +
                        "timeLimit: 5\n" +
                        "judge1.role: judge\n" +
                        "judge2.role: judge\n" +
                        "judge3.role: judge\n" +
                        "initialHint: Trading has not started yet.\n" +
                        "actualValue: 50, 50, 50\n" +
                        "scoringFactor.judge: 0.02\n" +
                        "scoringConstant.judge: 250\n";

        final String fourJudgesConfig = "sessionTitle: FourJudges\n" +
                        "rounds: 3\n" +
                        "players: judgeA, judgeB, judgeC, judgeD\n" +
                        "timeLimit: 5\n" +
                        "judgeA.role: judge\n" +
                        "judgeB.role: judge\n" +
                        "judgeC.role: judge\n" +
                        "judgeD.role: judge\n" +
                        "initialHint: Trading has not started yet.\n" +
                        "actualValue: 50, 50, 50\n" +
                        "scoringFactor.judge: 0.02\n" +
                        "scoringConstant.judge: 250\n";

        setupSession(threeJudgesConfig);
        JudgingSession jSession = (JudgingSession)session;
        Judge judge1 = jSession.getJudgeOrNull("judge1");
        Judge judge2 = jSession.getJudgeOrNull("judge2");
        Judge judge3 = jSession.getJudgeOrNull("judge3");
        session.startSession();

        assertQEquals(50, jSession.judgedValue(1));

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertQEquals(50, jSession.getMedianJudgedValue(1));
        assertEquals("", judge1.getWarningsHtml());

        setEstimate(judge1, 1, "40");
        setEstimate(judge2, 1, "40");
        setEstimate(judge3, 1, "40");
        assertQEquals(40, jSession.getMedianJudgedValue(1));
        assertEquals("", judge1.getWarningsHtml());
        assertEquals("", judge2.getWarningsHtml());
        assertEquals("", judge3.getWarningsHtml());

        session.startNextRound();
        session.endTrading(false);
        setEstimate(judge1, 2, "0");
        setEstimate(judge2, 2, "50");
        setEstimate(judge3, 2, "100");
        assertQEquals(50, jSession.getMedianJudgedValue(2));
        assertEquals("", judge1.getWarningsHtml());
        assertEquals("", judge2.getWarningsHtml());
        assertEquals("", judge3.getWarningsHtml());

        session.startNextRound();
        session.endTrading(false);
        setEstimate(judge1, 3, "0");
        setEstimate(judge2, 3, "10");
        setEstimate(judge3, 3, "20");
        assertQEquals(10, jSession.getMedianJudgedValue(3));
        assertQEquals(40, jSession.getMedianJudgedValue(1));
        assertQEquals(50, jSession.getMedianJudgedValue(2));
        assertEquals("", judge1.getWarningsHtml());
        assertEquals("", judge2.getWarningsHtml());
        assertEquals("", judge3.getWarningsHtml());

        props.load(new StringBufferInputStream(fourJudgesConfig));
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        jSession = (JudgingSession)session;
        Judge judgeA = jSession.getJudgeOrNull("judgeA");
        Judge judgeB = jSession.getJudgeOrNull("judgeB");
        Judge judgeC = jSession.getJudgeOrNull("judgeC");
        Judge judgeD = jSession.getJudgeOrNull("judgeD");

        session.startSession();
        session.endTrading(false);
        setEstimate(judgeA, 2, "0");
        setEstimate(judgeB, 2, "50");
        setEstimate(judgeC, 2, "50");
        setEstimate(judgeD, 2, "100");
        assertQEquals(50, jSession.getMedianJudgedValue(2));
        assertEquals("", judgeA.getWarningsHtml());
        assertEquals("", judgeB.getWarningsHtml());
        assertEquals("", judgeC.getWarningsHtml());
        assertEquals("", judgeD.getWarningsHtml());
    }

    public void testReportSessionInitialization() {
        MockAppender appender = new MockAppender();
        BasicConfigurator.configure(appender);

        SessionSingleton.setSession(props, null);

        Set events = appender.getEvents();
        assertMatch(SESSION_TITLE_PROPNAME, "SessionTestCase", events);
        assertMatch(ROUNDS_PROPNAME, "3", events);
        assertMatch(TIME_LIMIT_PROPNAME, "300 seconds.", events);
        assertMatch(COMMON_MESSAGE_PROPNAME + ".1", "", events);
        assertMatch(COMMON_MESSAGE_PROPNAME + ".2", "Some players are trying to raise the apparent price", events);
        assertMatch(COMMON_MESSAGE_PROPNAME + ".3", "Some players are trying to change the apparent price", events);
        assertMatch(ACTUAL_VALUE_PROPNAME, "0, 100, 40", events);

        assertMatch("endowment.trader", "100", events);
        assertMatch("endowment.manipulator", "50", events);
        assertMatch("tickets.trader", "30", events);
        assertMatch("tickets.manipulator", "20", events);

        assertMatch("scoringFactor.manipulator", "2", events);
        assertMatch("scoringFactor.judge", "0.02", events);
        assertMatch("scoringConstant.judge", "250", events);
        assertMatch("scoringConstant.manipulator", "200", events);

        assertMatch("traderA.role", TRADER_PROPERTY_WORD, events);
        assertRegExMatch("traderA.hint", "not100, +not40, +not100", events);

        assertMatch("traderB.role", TRADER_PROPERTY_WORD, events);
        assertRegExMatch("traderB.hint", "not40,\\s+notZero,\\s+notZero", events);

        assertMatch("manipulatorC.role", MANIPULATOR_PROPERTY_WORD, events);
        assertRegExMatch("manipulatorC.hint", "not100,\\s+notZero,\\s+notZero", events);
        assertRegExMatch("manipulatorC.earningsHint", "worth40,\\s+worth40,\\s+worth100", events);
        assertRegExMatch("manipulatorC.target", "40,\\s+40,\\s+100", events);

        assertMatch("judgeD.role", JUDGE_PROPERTY_WORD, events);
    }

    public void testConfigVars() {
        SessionSingleton.setSession(props, null);

        props.setProperty(BETTER_PRICE_REQUIRED, "true");
        props.setProperty(UNARY_ASSETS, "true");
        SessionSingleton.setSession(props, null);
        assertTrue(PropertyHelper.getBetterPriceRequired(props));
        assertTrue(PropertyHelper.getUnaryAssets(props));
    }

    static private void assertRegExMatch(String propName, String propValue, Set events) {
        boolean matchedOnce = false;
        for (Iterator iterator = events.iterator(); iterator.hasNext();) {
            LoggingEvent event = (LoggingEvent) iterator.next();
            String message = (String) event.getMessage();
            if (message.matches("\\d+# " + propName + ":.*")) {
                assertTrue(message.split("\\d+# " + propName + ":")[1].trim().matches(propValue));
                matchedOnce = true;
            }
        }
        assertTrue("Expecting '" + propName + "' property with value: '" + propValue + "'", matchedOnce);
    }

    static private void assertMatch(String propName, String propValue, Set events) {
        boolean matchedOnce = false;
        Set copy = new HashSet();  // shouldn't be necessary, but I'm seeing ConcurrentModificationExceptions below. 
        copy.addAll(events);
        for (Iterator iterator = copy.iterator(); iterator.hasNext();) {
            LoggingEvent event = (LoggingEvent) iterator.next();
            String message = (String) event.getMessage();
            if (message.matches("\\d*# " + propName + ":.*")) {
                assertEquals(propValue, message.split("\\d*# " + propName + ":")[1].trim());
                matchedOnce = true;
            }
        }
        assertTrue("Expecting '" + propName + "' property with value: '" + propValue + "'", matchedOnce);
    }

    public void testBinaryMarketScoring() throws IOException, ScoreException, DuplicateOrderException {
        int dividend1 = 5;
        int dividend2 = 30;
        final String UnaryAssetScript = "sessionTitle: binaryMarketScoringTest\n" +
            "rounds: 2\n" +
            "players: trader1, trader2\n" +
            "roles: trader\n" +
            "timeLimit: 5\n" +
            "useUnaryAssets: false\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "\n" +
            "endowment.trader: 200\n" +
            "tickets.trader: 0\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice\n" +
            "dividendValue:          " + dividend1 + ",          " + dividend2 + "\n" +
                "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(UnaryAssetScript);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(dividend1, session.getDividend(1));
        assertQEquals(dividend2, session.getDividend(2));

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader1.balance());

        makeLimitOrder(market, onePosition, 38, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 38, 1, trader2);
        assertQEquals(200 - (100 - 38), trader2.balance());
        assertQEquals(200 - 38, trader1.balance());
        assertQEquals(1, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-1, trader2.currentCouponCount(market.getBinaryClaim()));

        makeLimitOrder(market, onePosition, 55, 1, trader2);
        makeLimitOrder(market, onePosition, 77, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 77, 1, trader2);
        assertQEquals(200 - 38 - 77, trader1.balance());
        assertQEquals(200 - (100 - 38) - (100 - 77), trader2.balance());
        assertQEquals(2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-2, trader2.currentCouponCount(market.getBinaryClaim()));

        session.endTrading(false);

        assertQEquals(200 - 38 - 77 + (dividend1 * 2), trader1.getScore(1));
        assertQEquals(200 - (100 - 38) - (100 - 77) + (dividend1 * -2), trader2.getScore(1));

        assertQEquals(2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-2, trader2.currentCouponCount(market.getBinaryClaim()));

        String traderAExplanation = trader1.getScoreExplanation();
        assertMatches(">95<", traderAExplanation);
        String traderBExplanation = trader2.getScoreExplanation();
        assertMatches(">105<", traderBExplanation);
    }

    public void testPrivateDividends() throws IOException, ScoreException, DuplicateOrderException {
        final String PrivateDividendScript = "sessionTitle: PrivateDividendTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          40, 100\n" +
                "trader1.payCommonDividend: no\n" +
                "trader1.privateDividends: 300, 200\n" +
                "trader2.payCommonDividend: yes, no\n" +
                "trader2.privateDividends: 10, 400\n" +
                "trader3.payCommonDividend: yes\n" +
                "trader3.privateDividends: 80, 20" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(PrivateDividendScript);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(40.0, session.getDividend(1));
        assertQEquals(Quantity.Q100, session.getDividend(2));
        assertQEquals(300.0, session.getDividend(trader1, 1));
        assertQEquals(200.0, session.getDividend(trader1, 2));
        assertQEquals(50.0, session.getDividend(trader2, 1));
        assertQEquals(400.0, session.getDividend(trader2, 2));
        assertQEquals(120.0, session.getDividend(trader3, 1));
        assertQEquals(120.0, session.getDividend(trader3, 2));
        // trader4 has no private Dividends in config string; gets commonDiv only
        assertQEquals(40.0, session.getDividend(trader4, 1));
        assertQEquals(Quantity.Q100, session.getDividend(trader4, 2));

        assertQEquals(3, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader4.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader1.balance());
        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader3.balance());
        assertQEquals(200, trader4.balance());

        makeLimitOrder(market, onePosition, 38, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 38, 1, trader2);

        assertQEquals(200 - 38, trader1.balance());
        assertQEquals(4, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200, trader3.balance());
        assertQEquals(3, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200, trader4.balance());
        assertQEquals(3, trader4.currentCouponCount(market.getBinaryClaim()));

        session.endTrading(false);

        assertQEquals(200 - 38 + (4 * 300), trader1.getScore(1));
        assertQEquals(200 + 38 + (2 * (40 + 10)), trader2.getScore(1));
        assertQEquals(200 + (3 * (40 + 80)), trader3.getScore(1));
        assertQEquals(200 + (3 * 40), trader4.getScore(1));

        session.startNextRound();
        makeLimitOrder(market, onePosition, 77, 1, trader3);
        makeLimitOrder(market, anotherPosition, 100 - 77, 1, trader4);

        assertQEquals(200, trader1.balance());
        assertQEquals(3, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200, trader2.balance());
        assertQEquals(3, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 77, trader3.balance());
        assertQEquals(4, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 + 77, trader4.balance());
        assertQEquals(2, trader4.currentCouponCount(market.getBinaryClaim()));

        session.endTrading(false);

        assertQEquals(200 + 3 * 200, trader1.getScore(2));
        assertQEquals(200 + 3 * 400, trader2.getScore(2));
        assertQEquals(200 - 77 + (4 * (100 + 20)), trader3.getScore(2));
        assertQEquals(200 + 77 + (2 * 100), trader4.getScore(2));
    }

    public void testCarryForward() throws DuplicateOrderException, ScoreException, IOException {
        final String CARRY_FORWARD_SCRIPT = "sessionTitle: carryForwardTest\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, manipulator3, judge4\n" +
            "timeLimit: 5\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "\n" +
            "carryForward: all" +
            "\n" +
            "endowment.trader: 200\n" +
            "endowment.manipulator: 200\n" +
            "tickets.trader: 30\n" +
            "tickets.manipulator: 30\n" +
            "scoringFactor.judge: 0.02\n" +
            "scoringConstant.judge: 250\n" +
            "scoringFactor.manipulator: 2\n" +
            "scoringConstant.manipulator: 200\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "dividendValue:          10,          30\n" +
            "judge.target:          20,         100\n" +
            "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "manipulator3.hint:    not100,\tnotZero\n" +
            "manipulator3.earningsHint:    worth40,\tworth40\n" +
            "manipulator3.target:  40,         40\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(CARRY_FORWARD_SCRIPT);

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject)session.getPlayer("trader2");
        TradingSubject manipulator3 = (Manipulator)session.getPlayer("manipulator3");
        Judge judge4 = ((JudgingSession)session).getJudgeOrNull("judge4");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(30, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader1.balance());

        makeLimitOrder(market, onePosition, 38, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 38, 1, trader2);
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        makeLimitOrder(market, onePosition, 55, 1, trader1);
        makeLimitOrder(market, onePosition, 77, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 77, 1, trader1);
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        makeLimitOrder(market, anotherPosition, 100 - 78, 1, trader1);
        makeLimitOrder(market, onePosition, 78, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 55, 1, trader2);
        makeLimitOrder(market, onePosition, 26, 1, trader2);
        makeLimitOrder(market, anotherPosition, 100 - 63, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 26, 1, trader1);
        makeLimitOrder(market, onePosition, 63, 1, trader2);
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        setEstimate(judge4, 1, "50");
        jSession.endScoringPhase();

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));

        String traderAExplanation = trader1.getScoreExplanation();
        assertMatches(">10<", traderAExplanation);
        assertMatches(">280<", traderAExplanation);
        String traderBExplanation = trader2.getScoreExplanation();
        assertMatches(">10<", traderBExplanation);
        assertMatches(">320<", traderBExplanation);
        String manipulatorCExplanation = manipulator3.getScoreExplanation();
        assertMatches(">40<", manipulatorCExplanation);
        assertMatches(">50<", manipulatorCExplanation);
        assertMatches(">180<", manipulatorCExplanation);

        assertQEquals((250 - (.02 * (50 - 20) * (50 - 20))), judge4.getScore(1));
        assertQEquals(0, trader1.getScore(1));
        assertQEquals(0, trader2.getScore(1));

        jSession.endScoringPhase();   // again!

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());

        String manipulatorCExplanationAgain = manipulator3.getScoreExplanation();
        assertMatches(">40<", manipulatorCExplanationAgain);
        assertMatches(">50<", manipulatorCExplanationAgain);
        assertMatches(">180<", manipulatorCExplanationAgain);
        assertMatches("<th>Target</th><th>Judges' Estimate</th><th>Bonus", manipulatorCExplanationAgain);

        session.startNextRound(0);
        session.endTrading(false);
        setEstimate(judge4, 2, "40");
        jSession.endScoringPhase();

        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertEquals(2, session.getCurrentRound());
        assertMatches("Cumulative Earnings</td>\n<td>1780", manipScreen.showEarningsSummary());
        TraderScreen traderScreen = new TraderScreen();
        traderScreen.setUserName(trader1.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1471", traderScreen.showEarningsSummary());
        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judge4.getName());
        assertQEquals((250 - (.02 * (40 - 100) * (40 - 100))), judge4.getScore(2));

        ((JudgingSession)session).endScoringPhase();  // again

        manipScreen.setUserName(manipulator3.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1780", manipScreen.showEarningsSummary());
        traderScreen.setUserName(trader1.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1471", traderScreen.showEarningsSummary());
        judgeScreen.setUserName(judge4.getName());
        assertMatches("Cumulative Earnings</td>\n<td>410", judgeScreen.showEarningsSummary());
    }

    public void testNoManipTarget() throws IOException, ScoreException {
        final String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 5\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
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
        final Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");
        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject manip1 = (Manipulator)session.getPlayer("manip1");
        assertTrue(session.getErrorMessage().indexOf("is null") > 0);
        session.startSession();

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertQEquals(50, ((JudgingSession)session).getMedianJudgedValue(1));
        assertEquals("", judge1.getWarningsHtml());

        try {
            ((JudgingSession)session).endScoringPhase();
            fail("NumberFormatException should be thrown when no manipulator target is provided");
        } catch (NumberFormatException e) {
            assertQEquals(0, manip1.getScore(1));
        }

        session.startNextRound();
    }

    public void testOverlappingOrdersBug() throws IOException, DuplicateOrderException {
        final String config = "sessionTitle: overLappingOrders\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, trader2, manip1\n" +
                "timeLimit: 5\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "manip1.role: manipulator\n" +
                "initialHint: Trading has not started yet.\n" +
                "actualValue: 50, 50, 50\n" +
                "maxPrice: 200\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.manipulator: 0\n" +
                "scoringConstant.manipulator: 0\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject)session.getPlayer("trader2");
        TradingSubject manip1 = (Manipulator)session.getPlayer("manip1");
        assertTrue(session.getErrorMessage().indexOf("is null") > 0);
        session.startSession();

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        makeLimitOrder(market, onePosition, 23, 1, trader1);
        Book book = market.getBook();
        assertQEquals(23, book.bestBuyOfferFor(onePosition));
        market.limitOrder(anotherPosition, market.asPrice(new Quantity(127)), new Quantity(1), manip1.getUser());
        assertQEquals(73, book.bestSellOfferFor(onePosition));
        makeLimitOrder(market, anotherPosition, 11, 1, trader2);
    }

    public void testTimeParsing() throws IOException, ScoreException {
        String config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 3:50\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        assertEquals(((3 * 60) + 50), session.timeLimit());

        config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: :50\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        assertEquals(50, session.timeLimit());

        config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 3.5\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "scoringConstant.judge: 250\n";

        boolean caughtException = false;
        try {
            setupSession(config);
        } catch (NumberFormatException e) {
            caughtException = true;
        }
        assertTrue(caughtException);

        config = "sessionTitle: noManipTarget\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 5\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "scoringConstant.judge: 250\n";

        setupSession(config);
        assertEquals(5 * 60, session.timeLimit());
    }

    public void testDetectHtmlSpecialChars() {
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo<Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo>Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo&Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo'Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo\"Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlLinkSpecial("foo\\?Bar"));
        assertFalse(HtmlSimpleElement.detectHtmlLinkSpecial("fooBar"));
        assertFalse(HtmlSimpleElement.detectHtmlLinkSpecial("foo#Bar"));
        assertFalse(HtmlSimpleElement.detectHtmlLinkSpecial("foo=()Bar"));

        assertTrue(HtmlSimpleElement.detectHtmlTextSpecial("foo<Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlTextSpecial("foo>Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlTextSpecial("foo&Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlTextSpecial("foo'Bar"));
        assertTrue(HtmlSimpleElement.detectHtmlTextSpecial("foo\"Bar"));
        assertFalse(HtmlSimpleElement.detectHtmlTextSpecial("foo\\?Bar"));
        assertFalse(HtmlSimpleElement.detectHtmlTextSpecial("fooBar"));
        assertFalse(HtmlSimpleElement.detectHtmlTextSpecial("foo=()Bar"));
    }

    public void testThresholdDependantMessagePercent() throws IOException, ScoreException, DuplicateOrderException {
        String config = "sessionTitle: thresholds\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 3:50\n" +
                "useUnaryAssets: false\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "\n" +
                "scoringConstant.judge: 250\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "\n" +
                "dividendValue:          10,          30\n" +
                "judge.target:          20,         100\n" +
                "manip1.target:  40,         40\n" +
                "\n" +
                "thresholdValue: dividend\n" +
                "aboveThresholdMessage: The price is too high\n" +
                "belowThresholdMessage: The price is $percent$% too low";

        setupSession(config);
        assertEquals("The price is too high", session.aboveThresholdMessage());

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        Manipulator manip1 = (Manipulator) session.getPlayer("manip1");
        final Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(30, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, manip1.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, manip1.balance());
        assertQEquals(200, trader1.balance());

        makeLimitOrder(market, onePosition, 20, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 20, 1, manip1);
        assertQEquals(200 + 20, manip1.balance());
        assertQEquals(200 - 20, trader1.balance());

        makeLimitOrder(market, onePosition, 15, 1, manip1);
        makeLimitOrder(market, anotherPosition, 100 - 15, 1, trader1);
        assertQEquals(200 + 20 - 15, manip1.balance());
        assertQEquals(200 - 20 + 15, trader1.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is too high", session.aboveThresholdMessage());
        assertEquals("The price is too high", session.message(2));

        makeLimitOrder(market, onePosition, 20, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 20, 1, manip1);
        assertQEquals(200 + 20, manip1.balance());
        assertQEquals(200 - 20, trader1.balance());

        makeLimitOrder(market, onePosition, 15, 1, manip1);
        makeLimitOrder(market, anotherPosition, 100 - 15, 1, trader1);
        assertQEquals(200 + 20 - 15, manip1.balance());
        assertQEquals(200 - 20 + 15, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 2, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 40% too low", session.message(3));
    }

    public void testThresholdDependantMessageDiff() throws IOException, ScoreException, DuplicateOrderException {
        String config = "sessionTitle: thresholds\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "useUnaryAssets: false\n" +
                "timeLimit: 3:50\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "\n" +
                "scoringConstant.judge: 250\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "\n" +
                "dividendValue:      10,    30,    40\n" +
                "judge.target:          20,         100\n" +
                "manip1.target:  40,         40\n" +
                "\n" +
                "thresholdValue: dividend\n" +
                "aboveThresholdMessage: The price is $percent$ too high\n" +
                "belowThresholdMessage: The price is $difference$ cents too low";

        setupSession(config);
        assertEquals("", session.message());

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        Manipulator manip1 = (Manipulator)session.getPlayer("manip1");
        final Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        makeLimitOrder(market, onePosition, 10, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 10, 1, manip1);
        assertQEquals(200 + 10, manip1.balance());
        assertQEquals(200 - 10, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("", session.message(2));

        makeLimitOrder(market, onePosition, 10, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 10, 1, manip1);
        assertQEquals(200 + 10, manip1.balance());
        assertQEquals(200 - 10, trader1.balance());

        makeLimitOrder(market, onePosition, 15, 1, manip1);
        makeLimitOrder(market, anotherPosition, 100 - 15, 1, trader1);
        assertQEquals(200 + 10 - 15, manip1.balance());
        assertQEquals(200 - 10 + 15, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 2, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 17 cents too low", session.message(3));
    }

    public void testThresholdScaled() throws IOException, ScoreException, DuplicateOrderException {
        String config = "sessionTitle: thresholds\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 3:50\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "\n" +
                "scoringConstant.judge: 250\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "\n" +
                "dividendValue:      10,    30,    40\n" +
                "judge.target:          20,         100\n" +
                "manip1.target:  40,         40\n" +
                "maxPrice:  300\n" +
                "\n" +
                "thresholdValue: dividend\n" +
                "aboveThresholdMessage: The price is $percent$% too high\n" +
                "belowThresholdMessage: The price is $difference$ cents too low";

        setupSession(config);
        assertEquals("", session.message());

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        Manipulator manip1 = (Manipulator) session.getPlayer("manip1");
        Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];
        assertQEquals(200, manip1.balance());
        assertQEquals(200, trader1.balance());

        session.startSession(0);
        makeLimitOrder(market, onePosition, new Price(30, market.maxPrice()), 1, trader1);
        makeLimitOrder(market, anotherPosition, new Price(300 - 30, market.maxPrice()), 1, manip1);
        assertQEquals(200 + 30, manip1.balance());
        assertQEquals(200 - 30, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 200% too high", session.message(2));  //  200%

        makeLimitOrder(market, onePosition, new Price(27, market.maxPrice()), 1, trader1);
        makeLimitOrder(market, anotherPosition, new Price(300 - 27, market.maxPrice()), 1, manip1);
        assertQEquals(200 + 27, manip1.balance());
        assertQEquals(200 - 27, trader1.balance());

        makeLimitOrder(market, onePosition, new Price(15, market.maxPrice()), 1, manip1);
        makeLimitOrder(market, anotherPosition, new Price(300 - 15, market.maxPrice()), 1, trader1);
        assertQEquals(200 + 27 - 15, manip1.balance());
        assertQEquals(200 - 27 + 15, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 2, "50");
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 9 cents too low", session.message(3));
    }

    public void testRemainingDividendThreshold() throws IOException, ScoreException, DuplicateOrderException {
        String config = "sessionTitle: thresholds\n" +
                "rounds: 3\n" +
                "players: judge1, trader1, manip1\n" +
                "timeLimit: 3:50\n" +
                "useUnaryAssets: false\n" +
                "judge1.role: judge\n" +
                "trader1.role: trader\n" +
                "manip1.role: manipulator\n" +
                "\n" +
                "scoringConstant.judge: 250\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 30\n" +
                "tickets.manipulator: 30\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 2\n" +
                "scoringConstant.manipulator: 200\n" +
                "\n" +
                "dividendValue:      10,    30,    40\n" +
                "judge.target:          20,         100\n" +
                "manip1.target:  40,         40\n" +
                "\n" +
                "thresholdValue: remainingDividend\n" +
                "aboveThresholdMessage: The price is %$percent$ too high\n" +
                "belowThresholdMessage: The price is $difference$ cents too low";

        setupSession(config);
        assertEquals("", session.message());

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        Manipulator manip1 = (Manipulator)session.getPlayer("manip1");
        final Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 75, 1, manip1);
        assertQEquals(200 + 75, manip1.balance());
        assertQEquals(200 - 75, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 1, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 5 cents too low", session.message(2));

        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeLimitOrder(market, anotherPosition, 100 - 75, 1, manip1);
        assertQEquals(200 + 75, manip1.balance());
        assertQEquals(200 - 75, trader1.balance());

        session.endTrading(false);
        setEstimate(judge1, 2, "50");
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is %7 too high", session.message(3));
    }

    public void testVotingSession() throws IOException, DuplicateOrderException {
        String config = "sessionTitle: voting\n" +
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

        Properties nonVote = new Properties();
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "foo");
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "0");
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "3");
        assertTrue(VotingSession.describesVotingSession(nonVote));

        VotingSession voting = setupVotingSession(config);
        assertEquals("The price is too high", voting.voteText(1));
        assertEquals("The price is too low", voting.voteText(2));
        assertEquals("The price is correct", voting.voteText(3));
        assertNull(voting.voteText(4));
        assertFalse(voting.voteBefore(0));
        assertFalse(voting.voteBefore(1));
        assertTrue(voting.voteBefore(2));
        assertFalse(voting.voteBefore(3));
        assertTrue(voting.voteBefore(4));
        Integer one = new Integer(1);
        Integer two = new Integer(2);
        Integer three = new Integer(3);

        voting.startNextRound();     //   ROUND 1
        int round = 1;
        assertEquals("", voting.message(round));
        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader1, 2);
        assertEquals("", voting.message(round));

        voting.endTrading(true);           // VOTING
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader1, 1));

        voting.setVote(trader1, 2);
        assertEquals("", voting.message(round));
        assertEquals(two, voting.getVote(trader1, 2));
        assertFalse("waiting for votes", voting.votingComplete(round + 1));
        voting.setVote(trader2, 2);
        voting.setVote(trader3, 1);
        assertEquals(two, voting.getVote(trader2, 2));
        assertTrue("everyone voted, n'est ce pas?", voting.votingComplete(round + 1));
        voting.endVoting();
        String expected =
                ".*" + voting.voteText(1) + ".*1.*"
                        + voting.voteText(2) + ".*2.*"
                        + voting.voteText(3) + ".*0.*";
        assertREMatches(expected, voting.message(round));

        voting.startNextRound();      // ROUND 2
        round = 2;
        assertEquals(voting.voteText(2), voting.message(round));
        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader2, 3);
        assertEquals("Shouldn't allow voting during trading.", two, voting.getVote(trader2, 2));
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader2, 3));

        voting.endTrading(true);        // not voting
        assertEquals("", voting.message(round));
        voting.setVote(trader1, 2);
        assertNull("Vote not scheduled", voting.getVote(trader1, 3));
        assertFalse("Not waiting for votes", voting.votingComplete(round + 1));
        voting.endVoting();

        voting.startNextRound();     //   ROUND 3
        round = 3;
        assertEquals("", voting.message(round));
        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader1, 2);
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader1, 3));
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader1, 4));

        voting.endTrading(true);           // VOTING
        voting.setVote(trader1, 3);
        assertEquals("", voting.message(round));
        assertEquals(three, voting.getVote(trader1, 4));
        assertFalse("waiting for votes", voting.votingComplete(round + 1));
        voting.setVote(trader2, 1);
        assertEquals(one, voting.getVote(trader2, 4));
        voting.endVoting();

        voting.startNextRound();     //   ROUND 4
        round = 4;
        assertTrue("Should choose one of the top vote getters, not '" + voting.message(round) + "'",
                voting.voteText(1).equals(voting.message(round))
                || voting.voteText(3).equals(voting.message(round)));
        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader1, 2);
        assertEquals("Shouldn't change vote after voting closed.", three, voting.getVote(trader1, 4));
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader1, 5));

        voting.endTrading(true);           // not voting
        assertEquals("", voting.message(round));
        voting.setVote(trader1, 2);
        assertNull("Not voting", voting.getVote(trader1, 5));
        voting.setVote(trader2, 1);
        assertFalse("Not Voting", voting.votingComplete(round + 1));
    }

    private void makeMarketOrder(BinaryMarket m, Position pos, double price, int quantity, TradingSubject subject) throws DuplicateOrderException {
        m.marketOrder(pos, Price.dollarPrice(price), new Quantity(quantity), subject.getUser());
    }

    public void testNoVotes() throws IOException, DuplicateOrderException {
        String config = "sessionTitle: voting\n" +
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

        Properties nonVote = new Properties();
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "foo");
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "0");
        assertFalse(VotingSession.describesVotingSession(nonVote));
        nonVote.setProperty("voteAlternatives", "3");
        assertTrue(VotingSession.describesVotingSession(nonVote));

        VotingSession voting = setupVotingSession(config);
        assertEquals("The price is too high", voting.voteText(1));
        assertEquals("The price is too low", voting.voteText(2));
        assertEquals("The price is correct", voting.voteText(3));
        assertNull(voting.voteText(4));
        assertFalse(voting.voteBefore(0));
        assertFalse(voting.voteBefore(1));
        assertTrue(voting.voteBefore(2));
        assertFalse(voting.voteBefore(3));
        assertTrue(voting.voteBefore(4));

        voting.startNextRound();     //   ROUND 1
        int round = 1;
        assertEquals("", voting.message(round));
        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader1, 2);
        assertEquals("", voting.message(round));

        voting.endTrading(true);           // VOTING
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader1, 1));

        assertEquals("", voting.message(round));
        assertEquals(null, voting.getVote(trader1, 2));
        assertFalse("waiting for votes", voting.votingComplete(round + 1));
        assertEquals(null, voting.getVote(trader2, 2));
        assertFalse("no one voted, n'est ce pas?", voting.votingComplete(round + 1));
        voting.endVoting();
        String expected =
                ".*" + voting.voteText(1) + ".*0.*"
                        + voting.voteText(2) + ".*0.*"
                        + voting.voteText(3) + ".*0.*";
        assertREMatches(expected, voting.message(round));

        voting.startNextRound();      // ROUND 2
        round = 2;
        assertTrue("no one voted,but we should choose a msg anyway.",
                voting.message(2) != null && ! voting.message(2).equals(""));
        makeLimitOrder(market, onePosition, 75, 1, trader1);
        makeMarketOrder(market, anotherPosition, 55, 1, trader2);
        voting.setVote(trader2, 3);
        assertEquals("Shouldn't allow voting during trading.", null, voting.getVote(trader2, 2));
        assertNull("Shouldn't allow voting during trading.", voting.getVote(trader2, 3));
    }

    private VotingSession setupVotingSession(String config) throws IOException {
        setupSession(config);
        assertTrue(session instanceof VotingSession);
        return (VotingSession)session;
    }
}
