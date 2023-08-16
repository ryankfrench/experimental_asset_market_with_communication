package net.commerce.zocalo.JspSupport;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.experiment.*;
import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.NumberDisplay;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.ajax.dispatch.MockBayeuxChannel;
import net.commerce.zocalo.ajax.dispatch.BidUpdateDispatcher;
import net.commerce.zocalo.ajax.dispatch.TradeEventDispatcher;
import net.commerce.zocalo.service.PropertyHelper;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Properties;
import java.util.List;
import java.util.Map;

public class TraderScreenTest extends SessionTestCase {
    private final String lowBudgetConfig = "sessionTitle: TraderScreenTest\n" +
                    "rounds: 3\n" +
                    "players: trader1, trader2, manipulator3, judge4\n" +
                    "timeLimit: 5\n" +
                    "trader1.role: trader\n" +
                    "trader2.role: trader\n" +
                    "manipulator3.role: manipulator\n" +
                    "judge4.role: judge\n" +
                    "useUnaryAssets: true\n" +
                    "betterPriceRequired: true\n" +
                    "initialHint: Trading has not started yet.\n" +
                    "actualValue:          0,          100,         40\n" +
                    "manipulator3.target:  40,         40,          100\n" +
                    "endowment.trader: 1000\n" +
                    "endowment.manipulator: 100\n" +
                    "tickets.trader: 3\n" +
                    "tickets.manipulator: 2\n" +
                    "scoringFactor.judge: 0.02\n" +
                    "scoringConstant.judge: 250\n" +
                    "scoringFactor.manipulator: 200\n" +
                    "scoringConstant.manipulator: 2\n";

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testTraderMessage() throws ScoreException {
        TraderScreen screen = new TraderScreen();
        screen.setUserName(manipulatorC.getName());
        SessionSingleton.getSession().startSession();
        assertEquals("The ticket value is not 100.<br>" +
                "Your score will improve if the judge thinks the tickets are worth 40", screen.getMessage());

        String message = "Price target is 40.00";
        session.getPlayer(manipulatorC.getName()).setHint(message);
        assertTrue(screen.getMessage().indexOf(message) > -1);
    }

    public void testBalanceMessage() throws ScoreException {
        SessionSingleton.getSession().startSession();
        TraderScreen screen = new TraderScreen();
        screen.setUserName(manipulatorC.getName());
        assertTrue(screen.getBalanceMessage().indexOf("50") > -1);
        assertTrue(screen.getBalanceMessage().indexOf(".50") == -1);
    }

    public void testHoldingsMessage() throws ScoreException {
        SessionSingleton.getSession().startSession();
        processBuyTransaction(manipulatorC.getName(), "20", "2");

        Claim claim = session.getMarket().getClaim();
        Quantity traderACoupons = session.getUserOrNull(traderA.getName()).couponCount(claim);
        assertQEquals(30, traderACoupons);
        Quantity manipulatorCCoupons = session.getUserOrNull(manipulatorC.getName()).couponCount(claim);
        assertQEquals(20, manipulatorCCoupons);

        TraderScreen sellerScreen = processSellTransaction(traderA.getName(), "20", "1");
        assertEquals("29" /* 30 - 1 */ , sellerScreen.getHoldingsMessage());
        TraderScreen buyerScreen = processBuyTransaction(manipulatorC.getName(), "20", "0");
        assertEquals("21" /* 20 + 1 */ , buyerScreen.getHoldingsMessage());
    }

    private TraderScreen processBuyTransaction(String userName, String price, String quantity) {
        return processTransaction(userName, price, quantity, TradeSupport.BUY_ACTION);
    }

    private TraderScreen processSellTransaction(String userName, String price, String quantity) {
        return processTransaction(userName, price, quantity, TradeSupport.SELL_ACTION);
    }

    private TraderScreen processTransaction(String userName, String price, String quantity, String transactionType) {
        TraderScreen screen = new TraderScreen();
        screen.setUserName(userName);
        screen.setPrice(price);
        screen.setQuantity(quantity);
        screen.setAction(transactionType);
        screen.setOrderType(TraderScreen.NEW_ORDER);
        screen.processRequest(null, null);
        return screen;
    }

    public void testDeleteOrders() throws DuplicateOrderException, ScoreException, IncompatibleOrderException {
        String price20Input = HtmlSimpleElement.hiddenInputField("deleteOrderPrice", "20");
        String price70Input = HtmlSimpleElement.hiddenInputField("deleteOrderPrice", "70");
        TraderScreen screen = new TraderScreen();
        screen.setUserName(manipulatorC.getName());
        Session session = SessionSingleton.getSession();
        session.startSession();
        Market market = session.getMarket();

        Claim claim = market.getClaim();
        addOrder(market, claim.positions()[0], "20", 3, manipulatorC);
        addOrder(market, claim.positions()[1], "30", 5, manipulatorC);
        screen.processRequest(new MockHttpServletRequest(), null);

        assertMatches(price20Input, screen.pricesTable());
        processDeleteBuyOrder(manipulatorC, "20");
        assertFalse(screen.pricesTable().indexOf(price20Input) >= 0);

        assertTrue(screen.pricesTable().indexOf(price70Input) >= 0);
        processDeleteSellOrder(manipulatorC, "70");
        assertFalse(screen.pricesTable().indexOf(price70Input) >= 0);
    }

    private void processDeleteBuyOrder(TradingSubject user, String price) {
        processDeleteOrder(user, TradeSupport.BUY_ACTION, price);
    }

    private void processDeleteSellOrder(TradingSubject user, String price) {
        processDeleteOrder(user, TradeSupport.SELL_ACTION, price);
    }

    private void processDeleteOrder(TradingSubject user, String action, String price) {
        TraderScreen screen = new TraderScreen();
        screen.setUserName(user.getName());
        screen.setDeleteOrderPosition(action);
        screen.setDeleteOrderPrice(price);
        screen.processRequest(null, null);
    }

    public void testMarketOrders() throws DuplicateOrderException, ScoreException, IncompatibleOrderException {
        Session session = SessionSingleton.getSession();
        session.startSession();
        Market market = session.getMarket();

        Claim claim = market.getClaim();
        addOrder(market, claim.positions()[0], "20", 3, traderA);
        addOrder(market, claim.positions()[1], "30", 5, traderA);

        processMarketBuyOrder(traderB, "65");
        assertQEquals(Quantity.Q100, traderB.getUser().cashOnHand());

        processMarketBuyOrder(traderB, "70");
        assertQEquals((100 - 70), traderB.getUser().cashOnHand());
    }

    private void addOrder(Market market, Position pos, String price, int q, TradingSubject u) throws DuplicateOrderException, IncompatibleOrderException {
        market.getBook().addOrder(pos, new Price(price, market.currentPrice(pos)), q(q), u.getUser());
    }

    public void testDeleteOrdersWhenMarketIsClosed() throws DuplicateOrderException, ScoreException, IncompatibleOrderException {
        String price20Input = HtmlSimpleElement.hiddenInputField("deleteOrderPrice", "20");
        TraderScreen screen = new TraderScreen();
        screen.setUserName(manipulatorC.getName());
        Session session = SessionSingleton.getSession();
        session.startSession();
        Market market = session.getMarket();

        Claim claim = market.getClaim();
        assertEquals(0, manipulatorC.getUser().getOrders().size());
        addOrder(market, claim.positions()[0], "20", 3, manipulatorC);
        addOrder(market, claim.positions()[1], "30", 5, manipulatorC);
        assertEquals(2, manipulatorC.getUser().getOrders().size());
        screen.processRequest(new MockHttpServletRequest(), null);

        assertTrue(screen.pricesTable().indexOf(price20Input) >= 0);
        session.endTrading(false);
        processDeleteBuyOrder(manipulatorC, "20");
        assertEquals(2, manipulatorC.getUser().getOrders().size());

        judgeD.setEstimate(1, Price.dollarPrice("50"));
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound();
        assertFalse(screen.pricesTable().indexOf(price20Input) >= 0);
        assertEquals(0, manipulatorC.getUser().getOrders().size());
    }

    public void testSellingConstraints() throws IOException, ScoreException {
        props = new Properties();
        props.load(new StringBufferInputStream(lowBudgetConfig));
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI, false);
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject manipulator3 = (TradingSubject) session.getPlayer("manipulator3");
        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        Book book = session.getMarket().getBook();
        BinaryClaim claim = (BinaryClaim) book.getClaim();
        Position yes = claim.getYesPosition();
        Position no = claim.getNoPosition();
        session.startSession();
        assertTrue(PropertyHelper.getUnaryAssets(props));

        processNewSellOrder(manipulator3, "65");    // This order succeeds
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("You have already placed sell offers") < 0);
        assertQEquals(65, book.bestSellOfferFor(yes));

        processNewSellOrder(manipulator3, "62");    // This order succeeds
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("You have already placed sell offers") < 0);
        assertQEquals(62, book.bestSellOfferFor(yes));

        processNewSellOrder(manipulator3, "60");    // manipulator3 doesn't have enough coupons
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("You have already placed sell offers") >= 0);
        assertQEquals(62, book.bestSellOfferFor(yes));  // so the best offer stays the same

        processNewBuyOrder(trader1, "60");         // Add an opposing order to accept
        assertQEquals(40, book.bestSellOfferFor(no));

        manipulator3.getUser().getWarningsHTML();    // clear the warnings
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("You have already placed sell offers") < 0);

        processMarketSellOrder(manipulator3, "60");    // This order fails
        assertQEquals(40, book.bestSellOfferFor(no));
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("You have already placed sell offers") >= 0);
    }

    public void testParsingNumbers() throws ScoreException {
        Quantity three = new Quantity("3");
        Quantity zero = Quantity.ZERO;
        SessionSingleton.getSession().startSession();
        processBuyTransaction(manipulatorC.getName(), "20", "2");

        TraderScreen sellerScreen = processSellTransaction(traderA.getName(), "20", "1");
        assertQEquals(three, TradeSupport.parseInteger("3", sellerScreen.getUser()));
        assertEquals("", traderA.getUser().getWarningsHTML());
        assertQEquals(zero, TradeSupport.parseInteger("0", sellerScreen.getUser()));
        assertEquals("prices and quantities must be positive: 0<br>", traderA.getUser().getWarningsHTML());
        assertQEquals(zero, TradeSupport.parseInteger("a", sellerScreen.getUser()));
        assertEquals("Couldn't interpret as number: a<br>", traderA.getUser().getWarningsHTML());
        assertEquals("", traderA.getUser().getWarningsHTML());

        assertQEquals(zero, TradeSupport.parseInteger(".3", sellerScreen.getUser()));
        assertEquals("prices and quantities must be whole numbers: .3<br>", traderA.getUser().getWarningsHTML());
        assertEquals("", traderA.getUser().getWarningsHTML());

        assertQEquals(zero, TradeSupport.parseInteger("-3", sellerScreen.getUser()));
        assertEquals("prices and quantities must be positive: -3<br>", traderA.getUser().getWarningsHTML());
        assertEquals("", traderA.getUser().getWarningsHTML());

        assertQEquals(three, TradeSupport.parseDecimal("3", sellerScreen.getUser()));
        assertEquals("", traderA.getUser().getWarningsHTML());
        assertQEquals(zero, TradeSupport.parseDecimal("0", sellerScreen.getUser()));
        assertEquals("", traderA.getUser().getWarningsHTML());
        assertQEquals(zero, TradeSupport.parseDecimal("a", sellerScreen.getUser()));
        assertEquals("Couldn't interpret as number: a<br>", traderA.getUser().getWarningsHTML());
        assertEquals("", traderA.getUser().getWarningsHTML());

        Probability thirtyPercent = new Probability(".3");
        assertQEquals(TradeSupport.parseDecimal(".3", sellerScreen.getUser()), thirtyPercent);
        assertEquals("", traderA.getUser().getWarningsHTML());

        Quantity fivePointThree = new Quantity("5.3");
        assertQEquals(fivePointThree, TradeSupport.parseDecimal("5.3", sellerScreen.getUser()));
        assertEquals("", traderA.getUser().getWarningsHTML());

        assertQEquals(zero, TradeSupport.parseDecimal("-.3", sellerScreen.getUser()));
        assertEquals("prices and quantities must be positive: -.3<br>", traderA.getUser().getWarningsHTML());
        assertEquals("", traderA.getUser().getWarningsHTML());

        Quantity threeOhTwo = new Quantity(".302");
        assertQEquals(threeOhTwo, TradeSupport.parseDecimal("0.302", sellerScreen.getUser()));
        assertEquals("", traderA.getUser().getWarningsHTML());
    }

    public void testBuyingConstraints() throws IOException, ScoreException {
        String sixtyFive = "65";
        String seventy = "70";

        props = new Properties();
        props.load(new StringBufferInputStream(lowBudgetConfig));
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI, false);
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject manipulator3 = (TradingSubject) session.getPlayer("manipulator3");
        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        Book book = session.getMarket().getBook();
        BinaryClaim claim = (BinaryClaim) book.getClaim();
        Position yes = claim.getYesPosition();
        session.startSession();

        assertTrue(PropertyHelper.getUnaryAssets(props));
        processNewBuyOrder(manipulator3, sixtyFive);    // This order succeeds
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("can't afford") < 0);
        assertQEquals(new Probability(".65"), book.bestBuyOfferFor(yes).asProbability());

        processNewBuyOrder(manipulator3, seventy);    // This order is unafordable
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("can't afford") >= 0);
        assertQEquals(new Probability(".65"), book.bestBuyOfferFor(yes).asProbability());  // so the best offer stays the same

        processNewSellOrder(trader1, seventy);         // Add an opposing order to accept
        assertQEquals(new Probability(".7"), book.bestSellOfferFor(yes).asProbability());

        manipulator3.getUser().getWarningsHTML();
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("can't afford") < 0);

        assertQEquals(new Probability(".7"), book.bestSellOfferFor(yes).asProbability());
        processMarketBuyOrder(manipulator3, seventy);    // This order fails
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("can't afford") >= 0);

        session.endTrading(false);
        Judge judge4 = ((JudgingSession) session).getJudgeOrNull("judge4");
        judge4.setEstimate(1, Price.dollarPrice("40"));
        assertEquals("", judge4.getWarningsHtml());
        session.startNextRound();

        processNewBuyOrder(manipulator3, sixtyFive);    // order in new round succeeds
        assertTrue(manipulator3.getUser().getWarningsHTML().indexOf("can't afford") < 0);
        assertQEquals(new Probability(".65"), book.bestBuyOfferFor(yes).asProbability());
    }

    private void processNewBuyOrder(TradingSubject user, String price) {
        processOrder(TraderScreen.NEW_ORDER, TradeSupport.BUY_ACTION, price, user);
    }

    private void processMarketBuyOrder(TradingSubject user, String price) {
        processOrder(TraderScreen.MARKET_ORDER, TradeSupport.BUY_ACTION, price, user);
    }

    private void processMarketSellOrder(TradingSubject user, String price) {
        processOrder(TraderScreen.MARKET_ORDER, TradeSupport.SELL_ACTION, price, user);
    }

    private void processNewSellOrder(TradingSubject user, String price) {
        processOrder(TraderScreen.NEW_ORDER, TradeSupport.SELL_ACTION, price, user);
    }

    private void processOrder(String newOrder, String sellAction, String price, TradingSubject user) {
        TraderScreen screen = new TraderScreen();
        screen.setUserName(user.getName());
        screen.setQuantity("1");
        screen.setOrderType(newOrder);
        screen.setAction(sellAction);
        screen.setPrice(price);
        screen.processRequest(null, null);
    }

    public void testScoreTable() throws ScoreException {
        TraderScreen screen = new TraderScreen();
        screen.setUserName(manipulatorC.getName());
        Session session = SessionSingleton.getSession();
        session.startSession();
        session.endTrading(false);

        judgeD.setEstimate(1, new Price("50", Price.ONE_DOLLAR));
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertQEquals(230 /* (20 * 0) + 50 + 200 - (2 * (50-40)) */ , session.calculateEarnings(manipulatorC.getName()));

        session.startNextRound();
        judgeD.setEstimate(2, Price.dollarPrice("30"));
        assertEquals("", judgeD.getWarningsHtml());
        session.endTrading(false);
        ((JudgingSession)session).endScoringPhase();
        assertQEquals(2460 /* (20 * 100) + 50 + 200 - (2 * (40-30)) + 230*/ , session.calculateEarnings(manipulatorC.getName()));

        session.startNextRound();
        judgeD.setEstimate(3, Price.dollarPrice("80"));
        assertEquals("", judgeD.getWarningsHtml());
        session.endTrading(false);
        ((JudgingSession)session).endScoringPhase();
        assertQEquals(3470 /* (20 * 40) + 50 + 200 - (2 * (100-80)) + 2460 */ , session.calculateEarnings(manipulatorC.getName()));

        assertTrue(screen.showEarningsSummary().indexOf("Cumulative Earnings") > 0);
    }

    public void testNegativeZeroInDisplay() {
        assertEquals("0", NumberDisplay.printAsPrice(0));
        assertEquals("0", NumberDisplay.printAsPrice(-0));
        assertEquals("0", NumberDisplay.printAsPrice(0.1 - 0.1));
        assertEquals("0", NumberDisplay.printAsPrice(0.1 - 0.10000001));
    }

    public void testOrderAtZeroOr100() throws ScoreException {
        Session session = SessionSingleton.getSession();
        session.startSession();

        processMarketBuyOrder(traderB, "0");
        assertTrue(traderB.getUser().getWarningsHTML().indexOf("price must be") >= 0);

        processMarketSellOrder(traderB, "100");
        assertTrue(traderB.getUser().getWarningsHTML().indexOf("price must be") >= 0);
    }

    public void testSingleUnitTrades() throws IOException, ScoreException, DuplicateOrderException, IncompatibleOrderException {
        final String simpleConfig = "sessionTitle: SingleUnitTest\n" +
                "rounds: 3\n" +
                "players: a1, a2, b\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "a1.role: trader\n" +
                "a2.role: trader\n" +
                "b.role: manipulator\n" +
                "actualValue:    0,  100,  40\n" +
                "b.target:      40,   40,  100\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 2\n" +
                "tickets.manipulator: 2\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 200\n" +
                "scoringConstant.manipulator: 2\n";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX, false);
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject a1 = (TradingSubject) session.getPlayer("a1");
        TradingSubject a2 = (TradingSubject) session.getPlayer("a2");
        TradingSubject b = (TradingSubject) session.getPlayer("b");
        BinaryMarket market = session.getMarket();
        session.startSession();

        assertQEquals(200, b.getUser().cashOnHand());
        assertQEquals(200, a1.getUser().cashOnHand());
        assertQEquals(200, a2.getUser().cashOnHand());

        BinaryClaim claim = market.getBinaryClaim();
        addOrder(market, claim.getNoPosition(), "25", 1, a1);
        processMarketBuyOrder(b, "75");
        assertQEquals(125, b.getUser().cashOnHand());

        addOrder(market, claim.getYesPosition(), "70", 1, b);
        processMarketSellOrder(a2, "70");
        assertQEquals(55, b.getUser().cashOnHand());
        List events = historyChannel.getEvents(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        assertEquals(4, events.toArray().length);   // each trade creates 2 events, one each for limit and book trade
        Map e0 = (Map) events.toArray()[0];
        Map e1 = (Map) events.toArray()[2];
        assertTrue("traded should be 70: " + e1.get("traded") + " or " + e0.get("traded"), "70".equals(e1.get("traded").toString()) || "70".equals(e0.get("traded").toString()));

        addOrder(market, claim.getYesPosition(), "30", 1, a2);
        processMarketSellOrder(b, "30");
        assertQEquals(85, b.getUser().cashOnHand());
    }

    public void testInsufficientFunds_WholeShares() throws IOException, ScoreException, DuplicateOrderException, IncompatibleOrderException {
        final String simpleConfig = "sessionTitle: SingleUnitTest\n" +
                "rounds: 3\n" +
                "players: a1, a2, b\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "a1.role: trader\n" +
                "a2.role: trader\n" +
                "b.role: manipulator\n" +
                "actualValue:    0,  100,  40\n" +
                "b.target:      40,   40,  100\n" +
                "endowment.trader: 200\n" +
                "endowment.manipulator: 200\n" +
                "tickets.trader: 2\n" +
                "tickets.manipulator: 2\n" +
                "scoringFactor.judge: 0.02\n" +
                "scoringConstant.judge: 250\n" +
                "scoringFactor.manipulator: 200\n" +
                "scoringConstant.manipulator: 2\n";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX, false);
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject a1 = (TradingSubject) session.getPlayer("a1");
        TradingSubject a2 = (TradingSubject) session.getPlayer("a2");
        TradingSubject b = (TradingSubject) session.getPlayer("b");
        BinaryMarket market = session.getMarket();
        session.startSession();

        assertQEquals(200, b.getUser().cashOnHand());
        assertQEquals(200, a1.getUser().cashOnHand());
        assertQEquals(200, a2.getUser().cashOnHand());

        BinaryClaim claim = market.getBinaryClaim();
        addOrder(market, claim.getNoPosition(), "25", 1, a1);
        processMarketBuyOrder(b, "75");
        assertQEquals(125, b.getUser().cashOnHand());

        addOrder(market, claim.getYesPosition(), "70", 1, b);
        processMarketSellOrder(a2, "70");
        assertQEquals(55, b.getUser().cashOnHand());
        List events = historyChannel.getEvents(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        assertEquals(4, events.toArray().length);   // each trade creates 2 events, one each for limit and book trade
        Map e0 = (Map) events.toArray()[0];
        Map e1 = (Map) events.toArray()[2];
        assertTrue("traded should be 70: " + e1.get("traded"), "70".equals(e1.get("traded").toString()) || "70".equals(e0.get("traded").toString()));

        addOrder(market, claim.getYesPosition(), "30", 1, a2);
        processMarketSellOrder(b, "30");
        assertQEquals(85, b.getUser().cashOnHand());
    }

    public void testMarketOrderForm() throws ScoreException, DuplicateOrderException, IncompatibleOrderException {
        String disabledPattern = ".*disabled.*";
        Session session = SessionSingleton.getSession();
        session.startSession();
        Market market = session.getMarket();
        TraderScreen screen = new TraderScreen();
        screen.setUserName(traderB.getName());
        screen.setPrice("20");
        screen.setQuantity("40");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.setOrderType(TraderScreen.NEW_ORDER);

        assertREMatches(disabledPattern, screen.marketOrderFormRow());

        Claim claim = market.getClaim();
        addOrder(market, claim.positions()[0], "20", 3, traderA);
        addOrder(market, claim.positions()[1], "30", 5, traderA);
        String formAfterOrders = screen.marketOrderFormRow();
        assertRENoMatch(disabledPattern, formAfterOrders);
        assertREMatches(".*70.*20.*", formAfterOrders);
    }

    public void testScaleDiv() throws IOException, ScoreException, DuplicateOrderException {
        Session session = SessionSingleton.getSession();
        session.startSession();
        TraderScreen screen = new TraderScreen();
        screen.setUserName(traderB.getName());
        screen.setPrice("20");
        screen.setQuantity("40");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.setOrderType(TraderScreen.NEW_ORDER);

        String roundDiv = "<div id=roundLabel class=\"round\"></div>";
        assertREMatches("<div id=scale class=\"100\"></div>" + roundDiv, screen.scaleDiv());

        final String simpleConfig = "sessionTitle: SingleUnitTest\n" +
                        "rounds: 3\n" +
                        "players: a1, a2, b\n" +
                        "timeLimit: 5\n" +
                        "a1.role: trader\n" +
                        "a2.role: trader\n" +
                        "b.role: manipulator\n" +
                        "maxPrice: 1200\n" +
                        "actualValue:    0,  100,  40\n" +
                        "b.target:      40,   40,  100\n" +
                        "endowment.trader: 200\n" +
                        "endowment.manipulator: 200\n" +
                        "tickets.trader: 2\n" +
                        "tickets.manipulator: 2\n" +
                        "scoringFactor.judge: 0.02\n" +
                        "scoringConstant.judge: 250\n" +
                        "scoringFactor.manipulator: 200\n" +
                        "scoringConstant.manipulator: 2\n";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        historyChannel = (MockBayeuxChannel) mockBayeux.getChannel(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI, false);
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();

        session.startSession();

        screen.setUserName("a1");
        screen.setPrice("30");
        screen.setQuantity("5");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.setOrderType(TraderScreen.NEW_ORDER);

        assertREMatches("<div id=scale class=\"1200\"></div>" + roundDiv, screen.scaleDiv());
    }

    public void testSingleSidedTraders() throws IOException, ScoreException, DuplicateOrderException, IncompatibleOrderException {
        final String simpleConfig = "sessionTitle: SingleSidedTradersTest\n" +
                "rounds: 3\n" +
                "players: b1, s2, t3\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "roles: trader,buyer,seller\n" +
                "b1.role: buyer\n" +
                "s2.role: seller\n" +
                "t3.role: trader\n" +
                "actualValue:    0,  100,  40\n" +
                "endowment.buyer: 200\n" +
                "endowment.seller: 50\n" +
                "endowment.trader: 100\n" +
                "restriction.buyer: buyOnly\n" +
                "restriction.seller: sellOnly\n" +
                "tickets.seller: 5";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject b1 = (TradingSubject) session.getPlayer("b1");
        TradingSubject s2 = (TradingSubject) session.getPlayer("s2");
        TradingSubject t3 = (TradingSubject) session.getPlayer("t3");
        BinaryMarket market = session.getMarket();
        session.startSession();

        assertQEquals(200, b1.getUser().cashOnHand());
        assertQEquals(100, t3.getUser().cashOnHand());
        assertQEquals(50, s2.getUser().cashOnHand());

        ExperimentSubject p1 = session.getPlayer(b1.getName());
        ExperimentSubject p2 = session.getPlayer(s2.getName());
        ExperimentSubject p3 = session.getPlayer(t3.getName());
        int round = session.getCurrentRound();
        assertTrue(p1.canBuy(round));
        assertFalse(p1.canSell(round));
        assertTrue(p2.canSell(round));
        assertFalse(p2.canBuy(round));
        assertTrue(p3.canSell(round));
        assertTrue(p3.canBuy(round));

        BinaryClaim claim = market.getBinaryClaim();
        Position no = claim.getNoPosition();
        Position yes = claim.getYesPosition();
        double t3Cash = 100;
        double t3Tickets = 0;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));

        addOrder(market, no, "25", 1, t3);
        processMarketBuyOrder(s2, "75");
        assertQEquals("Seller can't buy.", q(50), s2.getUser().cashOnHand());
        processMarketBuyOrder(b1, "75");
        assertQEquals(125, b1.getUser().cashOnHand());
        t3Cash -= 25;
        assertQEquals(t3Cash, t3.getUser().cashOnHand());
        t3Tickets -= 1;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));

        addOrder(market, yes, "70", 1, t3);
        processMarketSellOrder(b1, "70");
        assertQEquals("buyer can't sell.", q(125), b1.getUser().cashOnHand());
        processMarketSellOrder(s2, "70");
        assertQEquals(120, s2.getUser().cashOnHand());
        t3Cash = t3Cash - 70 + 100;
        assertQEquals(t3Cash, t3.getUser().cashOnHand());
        t3Tickets += 1;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));

        addOrder(market, no, "30", 1, s2);
        processMarketBuyOrder(t3, "70");
        t3Cash -= 70;
        assertQEquals(t3Cash, t3.getUser().cashOnHand());
        t3Tickets += 1;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));
        addOrder(market, yes, "30", 1, b1);
        processMarketSellOrder(t3, "30");
        t3Cash = t3Cash - 70 + 100;
        assertQEquals(t3Cash, t3.getUser().cashOnHand());
        t3Tickets -= 1;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));
    }

    public void testParseScaledValues() throws IOException, ScoreException, DuplicateOrderException {
        final String simpleConfig = "sessionTitle: parseScaledValues\n" +
                "rounds: 3\n" +
                "players: b1, s2, t3\n" +
                "maxPrice: 400\n" +
                "timeLimit: 5\n" +
                "roles: trader,buyer,seller\n" +
                "b1.role: buyer\n" +
                "s2.role: seller\n" +
                "t3.role: trader\n" +
                "actualValue:    0,  100,  40\n" +
                "endowment.buyer: 300\n" +
                "endowment.seller: 50\n" +
                "endowment.trader: 100\n" +
                "tickets.seller: 5";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject b1 = (TradingSubject) session.getPlayer("b1");
        TradingSubject s2 = (TradingSubject) session.getPlayer("s2");
        TradingSubject t3 = (TradingSubject) session.getPlayer("t3");
        BinaryMarket market = session.getMarket();
        Book book = market.getBook();
        session.startSession();

        assertQEquals(300, b1.getUser().cashOnHand());
        assertQEquals(Quantity.Q100, t3.getUser().cashOnHand());
        assertQEquals(50, s2.getUser().cashOnHand());

        ExperimentSubject p1 = session.getPlayer(b1.getName());
        ExperimentSubject p2 = session.getPlayer(s2.getName());
        ExperimentSubject p3 = session.getPlayer(t3.getName());

        BinaryClaim claim = market.getBinaryClaim();
        Position no = claim.getNoPosition();
        Position yes = claim.getYesPosition();
        double t3Cash = 1;
        double t3Tickets = 0;
        assertQEquals(t3Tickets, t3.currentCouponCount(claim));

        processNewSellOrder(s2, "145");
        assertQEquals(145.0, book.bestSellOfferFor(yes));

        processDeleteSellOrder(b1, "145");
        assertQEquals(145.0, book.bestSellOfferFor(yes));
        processDeleteSellOrder(s2, "145");
        assertQEquals(400, book.bestSellOfferFor(yes));

        processNewSellOrder(s2, "145");
        assertQEquals(145.0, book.bestSellOfferFor(yes));
        processBuyTransaction("b1", "145", "1");
        assertQEquals(400, book.bestSellOfferFor(yes));
    }

    public void testDormantTraders() throws IOException, ScoreException, DuplicateOrderException {
        final String simpleConfig = "sessionTitle: DormantTradersTest\n" +
                "rounds: 3\n" +
                "players: b1, s2, t3, td4, d5, db6, ds7\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "roles: trader,buyer,seller,dormant, dormantBuyer, dormantSeller\n" +
                "b1.role: buyer\n" +    // buy only role, player sometimes dormant
                "s2.role: seller\n" +   // sell only role, player sometimes dormant
                "t3.role: trader\n" +   // never dormant
                "td4.role: trader\n" +  // player sometimes dormant
                "d5.role: dormant\n" +  // role sometimes dormant
                "db6.role: dormantBuyer\n" +    // buy only role, role sometimes dormant
                "ds7.role: dormantSeller\n" +   // sell only role, role sometimes dormant
                "actualValue:    0,  100,  40\n" +
                "endowment.buyer: 200\n" +
                "endowment.seller: 50\n" +
                "endowment.trader: 100\n" +
                "endowment.dormant: 100\n" +
                "endowment.dormantBuyer: 100\n" +
                "endowment.dormantSeller: 100\n" +
                "restriction.buyer: buyOnly\n" +
                "restriction.seller: sellOnly\n" +
                "restriction.dormantBuyer: buyOnly\n" +
                "restriction.dormantSeller: sellOnly\n" +
                "dormant.dormantRounds: 1,3\n" +
                "dormantSeller.dormantRounds: 2\n" +
                "dormantBuyer.dormantRounds: 3\n" +
                "td4.dormantRounds: 2,3\n" +
                "db6.dormantRounds: 2\n" +
                "ds7.dormantRounds: 1\n" +
                "tickets.dormantSeller: 5\n" +
                "tickets.dormant: 5\n" +
                "tickets.dormantBuyer: 5\n" +
                "tickets.buyer: 5\n" +
                "tickets.trader: 5\n" +
                "tickets.seller: 5";

        props = new Properties();
        props.load(new StringBufferInputStream(simpleConfig));
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        TradingSubject b1 = (TradingSubject) session.getPlayer("b1");
        TradingSubject s2 = (TradingSubject) session.getPlayer("s2");
        TradingSubject t3 = (TradingSubject) session.getPlayer("t3");
        TradingSubject td4 = (TradingSubject) session.getPlayer("td4");
        TradingSubject d5 = (TradingSubject) session.getPlayer("d5");
        TradingSubject db6 = (TradingSubject) session.getPlayer("db6");
        TradingSubject ds7 = (TradingSubject) session.getPlayer("ds7");
        BinaryMarket market = session.getMarket();

        ExperimentSubject p1 = session.getPlayer(b1.getName());
        ExperimentSubject p2 = session.getPlayer(s2.getName());
        ExperimentSubject p3 = session.getPlayer(t3.getName());
        ExperimentSubject p4 = session.getPlayer(td4.getName());
        ExperimentSubject p5 = session.getPlayer(d5.getName());
        ExperimentSubject p6 = session.getPlayer(db6.getName());
        ExperimentSubject p7 = session.getPlayer(ds7.getName());

        session.startSession();
        int currentRound = session.getCurrentRound();

        assertTrue(p1.canBuy(currentRound));
        assertFalse(p1.canSell(currentRound));
        assertFalse(p2.canBuy(currentRound));
        assertTrue(p2.canSell(currentRound));
        assertTrue(p3.canBuy(currentRound));
        assertTrue(p3.canSell(currentRound));
        assertTrue(p4.canBuy(currentRound));
        assertTrue(p4.canSell(currentRound));
        assertFalse(p5.canBuy(currentRound));
        assertFalse(p5.canSell(currentRound));
        assertTrue(p6.canBuy(currentRound));
        assertFalse(p6.canSell(currentRound));
        assertFalse(p7.canBuy(currentRound));
        assertFalse(p7.canSell(currentRound));

        session.endTrading(true);
        session.startNextRound();
        currentRound = session.getCurrentRound();
        assertTrue(p1.canBuy(currentRound));
        assertFalse(p1.canSell(currentRound));
        assertFalse(p2.canBuy(currentRound));
        assertTrue(p2.canSell(currentRound));
        assertTrue(p3.canBuy(currentRound));
        assertTrue(p3.canSell(currentRound));
        assertFalse(p4.canBuy(currentRound));
        assertFalse(p4.canSell(currentRound));
        assertTrue(p5.canBuy(currentRound));
        assertTrue(p5.canSell(currentRound));
        assertFalse(p6.canBuy(currentRound));
        assertFalse(p6.canSell(currentRound));
        assertFalse(p7.canBuy(currentRound));
        assertFalse(p7.canSell(currentRound));

        session.endTrading(true);
        session.startNextRound();
        currentRound = session.getCurrentRound();
        assertTrue(p1.canBuy(currentRound));
        assertFalse(p1.canSell(currentRound));
        assertFalse(p2.canBuy(currentRound));
        assertTrue(p2.canSell(currentRound));
        assertTrue(p3.canBuy(currentRound));
        assertTrue(p3.canSell(currentRound));
        assertFalse(p4.canBuy(currentRound));
        assertFalse(p4.canSell(currentRound));
        assertFalse(p5.canBuy(currentRound));
        assertFalse(p5.canSell(currentRound));
        assertFalse(p6.canBuy(currentRound));
        assertFalse(p6.canSell(currentRound));
        assertFalse(p7.canBuy(currentRound));
        assertTrue(p7.canSell(currentRound));
    }
}
