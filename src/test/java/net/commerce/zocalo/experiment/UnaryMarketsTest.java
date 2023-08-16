package net.commerce.zocalo.experiment;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.JspSupport.JudgeScreen;
import net.commerce.zocalo.JspSupport.TraderScreen;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.logging.MockAppender;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.experiment.role.*;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.service.PropertyHelper;
import static net.commerce.zocalo.service.PropertyKeywords.*;

import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.ajax.events.BookTrade;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.Properties;
import java.util.Iterator;

import org.apache.log4j.Logger;

public class UnaryMarketsTest extends SessionTestCase {
    private String SESSION_NAME = "UnaryMarketsTest";
    private final String script =
            "sessionTitle: UnaryMarketsTest\n" +
            "rounds: 3\n" +
            "players: traderA, traderB, traderC\n" +
            "timeLimit: 5\n" +
            "traderA.role: trader\n" +
            "traderB.role: trader\n" +
            "traderC.role: trader\n" +
            "initialHint: Trading has not started yet.\n" +
            "\n" +
            "useUnaryAssets: true\n" +
            "requireReserves: true\n" +
            "endowment.trader: 100\n" +
            "tickets.trader: 30\n" +
            "\n" +
            "# These values are specified by round.\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          20,          100,         40\n" +
            "\n" +
            "# These values are specified by Player and Round.\n" +
            "\n" +
            "traderA.hint:         not100,     not40,       not100\n" +
            "traderB.hint:         not40,      notZero,     notZero\n" +
            "traderC.hint:    not100,\tnotZero,     notZero\n" +
            "\n" +
            "# text labels can be used in hints or commonMessage\n" +
            "\n" +
            "not100: The ticket value is not 100.\n" +
            "not40: The ticket value is not 40.\n" +
            "notZero: The ticket value is not 0.\n" +
            "\n" +
            "raisePrice: Some players are trying to raise the apparent price\n" +
            "changePrice: Some players are trying to change the apparent price\n" +
            "noMessage:\n" +
            "\n" +
            "worth40: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 40\n";
    protected TradingSubject traderC;

    protected void setUpTradingExperiment() throws Exception {
        Log4JHelper.getInstance();
        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        traderA = (TradingSubject) session.getPlayer("traderA");
        traderB = (TradingSubject) session.getPlayer("traderB");
        traderC = (TradingSubject) session.getPlayer("traderC");
        HibernateTestUtil.resetSessionFactory();
    }

    public void testScoring() throws Exception {
        setUpTradingExperiment();
        double actualOne = actualValue(1);
        double actualTwo = actualValue(2);
        double actualThree = actualValue(3);

        double traderEndowment = 100;
        double traderTickets = 30;

        // first round
        session.startSession();
        session.endTrading(false);

        assertQEquals(traderEndowment + (traderTickets * actualOne), traderA.getScore(1));

        // second round
        session.startNextRound(0);
        session.endTrading(false);

        assertQEquals(traderEndowment + (traderTickets * (actualTwo)), traderB.getScore(2));

        // third round
        session.startNextRound(0);
        session.endTrading(false);

        assertQEquals(traderEndowment + (traderTickets * (actualThree)), traderC.getScore(3));

        session.startNextRound();
        assertEquals("No more rounds.", session.getErrorMessage());
    }

    private double actualValue(int round) {
        String actualString = PropertyHelper.indirectPropertyForRound(ACTUAL_VALUE_PROPNAME, round, props);
        return Double.parseDouble(actualString);
    }

    public void testConfigVars() throws IOException {
        final String script =
                "sessionTitle: UnaryMarketsTest\n" +
                "rounds: 3\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "\n" +
                "betterPriceRequired: true\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "endowment.trader: 100\n" +
                "tickets.trader: 30\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice, noMessage\n" +
                "actualValue:          20,          100,         40\n";
        props = new Properties();
        props.load(new StringBufferInputStream(script));
        assertTrue(PropertyHelper.getBetterPriceRequired(props));
        assertTrue(PropertyHelper.getUnaryAssets(props));
    }

    public void testUnaryMarketScoring() throws IOException, ScoreException, DuplicateOrderException {
        int dividend1 = 5;
        int dividend2 = 30;
        final String UnaryAssetScript = "sessionTitle: binaryMarketScoringTest\n" +
            "rounds: 2\n" +
            "players: trader1, trader2\n" +
            "roles: trader\n" +
            "timeLimit: 5\n" +
            "useUnaryAssets: true\n" +
            "requireReserves: true\n" +
            "carryForward: true\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "\n" +
            "endowment.trader: 200\n" +
            "tickets.trader: 0\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice\n" +
            "dividendValue:  " + dividend1 + ",   " + dividend2 + "\n" +
            "maxDividend: 30\n" +
                "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n";
        setupSession(UnaryAssetScript);
        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = ((BinaryClaim) market.getClaim()).getNoPosition();

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");

        double trader1Balance = 200;
        double trader2Balance = 200;
        double reservesPerRound = 30;

        session.startSession(0);
        assertQEquals(dividend1, session.getDividend(1));
        assertQEquals(dividend2, session.getDividend(2));

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());

        limitOrder(market, yes, 38, 1, trader1.getUser());
        limitOrder(market, no,  Price.dollarPrice(38).inverted(), 1, trader2.getUser());
        assertQEquals(trader1Balance -= 38, trader1.balance());
        assertQEquals(trader2Balance += 38 - reservesPerRound * 2, trader2.balance());
        assertQEquals(0, reserves(trader1));
        assertQEquals(reservesPerRound * 2, reserves(trader2));
        assertQEquals(1, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-1, trader2.currentCouponCount(market.getBinaryClaim()));

        limitOrder(market, yes, 55, 1, trader2.getUser());
        limitOrder(market, yes, 77, 1, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(77).inverted(), 1, trader2.getUser());
        assertQEquals(trader1Balance -= 77, trader1.balance());
        assertQEquals(trader2Balance += 77 - reservesPerRound * 2, trader2.balance());
        assertQEquals(2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-2, trader2.currentCouponCount(market.getBinaryClaim()));

        session.endTrading(false);

        assertQEquals(trader1Balance += dividend1 * 2.0, trader1.balance());
        assertQEquals(trader2Balance += (2.0 * (dividend2 - dividend1)) , trader2.balance());
        assertQEquals(0, trader2.getScore(1));

        assertQEquals(2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-2, trader2.currentCouponCount(market.getBinaryClaim()));

        String trader1Explanation = trader1.getScoreExplanation();
        assertMatches(">5</td><td align=center>10<", trader1Explanation);
        String trader2Explanation = trader2.getScoreExplanation();
        assertMatches(">5</td><td align=center>-10<", trader2Explanation);
    }

    public void testBookOfferAtWorsePrice() throws IOException, ScoreException, DuplicateOrderException {
        int dividend1 = 5;
        int dividend2 = 30;
        final String UnaryAssetScript = "sessionTitle: binaryMarketScoringTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2\n" +
                "roles: trader\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "carryForward: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "dividendValue:  " + dividend1 + ",   " + dividend2 + "\n" +
                "maxDividend: 30\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n";
        setupSession(UnaryAssetScript);
        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = ((BinaryClaim) market.getClaim()).getNoPosition();
        MockAppender appender = new MockAppender();
        assertEquals(0, appender.messageCount());
        Logger.getLogger(PriceAction.class).addAppender(appender);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");

        double trader1Balance = 200;
        double trader2Balance = 200;
        double reservesPerRound = 30;

        session.startSession(0);
        assertQEquals(dividend1, session.getDividend(1));
        assertQEquals(dividend2, session.getDividend(2));

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());

        limitOrder(market, yes, 38, 1, trader1.getUser());
        limitOrder(market, no, 85, 1, trader2.getUser());
        assertQEquals(trader1Balance -= 38, trader1.balance());
        assertQEquals(trader2Balance += 38 - reservesPerRound * 2, trader2.balance());
        assertQEquals(0, reserves(trader1));
        assertQEquals(reservesPerRound * 2, reserves(trader2));
        assertQEquals(1, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-1, trader2.currentCouponCount(market.getBinaryClaim()));
        assertTrue(1 < appender.getEvents().size());
        Iterator allEvents = appender.getEvents().iterator();
        int found = 0;
        while (allEvents.hasNext()) {
            Object o =  allEvents.next();
            if (o instanceof BookTrade) {
                BookTrade trade = (BookTrade) o;
                assertQEquals(38, trade.getPrice());
                found++;
            }
        }
        assertEquals(1, found);
    }

    private Quantity reserves(TradingSubject t) {
        BinaryMarket market = session.getMarket();
        Position no = ((BinaryClaim) market.getClaim()).getNoPosition();
        return t.getUser().reserveBalance(no);
    }

    public void testPrivateDividends() throws IOException, ScoreException, DuplicateOrderException {
        final String PrivateDividendScript = "sessionTitle: PrivateDividendTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
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
        assertQEquals(40, session.getDividend(1));
        assertQEquals(100.0, session.getDividend(2));
        assertQEquals(300.0, session.getDividend(trader1, 1));
        assertQEquals(200.0, session.getDividend(trader1, 2));
        assertQEquals(50.0, session.getDividend(trader2, 1));
        assertQEquals(400.0, session.getDividend(trader2, 2));
        assertQEquals(120.0, session.getDividend(trader3, 1));
        assertQEquals(120.0, session.getDividend(trader3, 2));
        // trader4 has no private Dividends in config string; gets commonDiv only
        assertQEquals(40.0, session.getDividend(trader4, 1));
        assertQEquals(100.0, session.getDividend(trader4, 2));

        assertQEquals(3, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, trader4.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader1.balance());
        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader3.balance());
        assertQEquals(200, trader4.balance());

        limitOrder(market, onePosition, 38, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(38).inverted(), 1, trader2.getUser());   // trade @ 38

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
        limitOrder(market, onePosition, 77, 1, trader3.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(77).inverted(), 1, trader4.getUser());   // trade @ 77

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

    public void testPayDividendOnShorts() throws IOException, ScoreException, DuplicateOrderException {
        final String payDividendsOnShorts = "sessionTitle: PayDividendsOnShortsTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "carryForward: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          40, 70\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(payDividendsOnShorts);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();

        session.startSession(0);
        assertQEquals(40.0, session.getDividend(1));
        assertQEquals(70.0, session.getDividend(2));

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));

        double trader1Balance = 200;
        assertQEquals(trader1Balance, trader1.balance());
        double trader2Balance = 200;
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(200, trader3.balance());
        assertQEquals(200, trader4.balance());

        limitOrder(market, yes, 38, 3, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(38), 3, trader2.getUser());   // insufficient reserves, no trade
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());

        limitOrder(market, no, Price.dollarPrice(38).inverted(), 1, trader2.getUser());   // trade @ 62
        assertQEquals(trader1Balance -= 38, trader1.balance());
        assertQEquals(trader2Balance += 38 - (2 * 70), trader2.balance());

        assertQEquals(1, trader1.getUser().getAccounts().couponCount(yes));
        assertQEquals(1, trader2.getUser().getAccounts().couponCount(no));
        assertQEquals(140, reserves(trader2));
        session.endTrading(false);
/////////////// ROUND 2 //////////////////////////////////////////

        assertQEquals(trader1Balance += 40, trader1.balance());
        assertQEquals(trader2Balance += 30, trader2.balance());
        assertQEquals(70, reserves(trader2));
        double trader3Balance = 200;
        double trader4Balance = 200;
        assertQEquals(trader4Balance, trader4.balance());
        assertQEquals(trader3Balance, trader3.balance());

        session.startNextRound();

        limitOrder(market, yes, 43, 3, trader3.getUser());
        limitOrder(market, no, Price.dollarPrice(43).inverted(), 3, trader4.getUser());
        assertQEquals(trader3Balance -= (3 * 43), trader3.balance());
        assertQEquals(trader4Balance += (3 * 43) - (3 * 70), trader4.balance());

        assertQEquals(3, trader3.getUser().getAccounts().couponCount(yes));
        assertQEquals(3, trader4.getUser().getAccounts().couponCount(no));
        assertQEquals(0, reserves(trader3));
        assertQEquals(3 * 70, reserves(trader4));
        session.endTrading(false);

        assertQEquals(trader3Balance, trader3.balance());
        assertQEquals((trader3Balance + (3 * 70)), trader3.getScore(2));
        assertQEquals(trader4Balance, trader4.balance());
        assertQEquals(3 * 70, reserves(trader4));
        assertQEquals(trader4Balance, trader4.getScore(2));

        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals((trader1Balance + (1 * 70)), trader1.getScore(2));
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(70, reserves(trader2));
        assertQEquals(trader2Balance, trader2.getScore(2));

        session.endTrading(false);

        assertQEquals(trader1Balance + 70, trader1.getScore(2));
        assertQEquals(trader2Balance, trader2.getScore(2));
        assertQEquals(trader3Balance + 3 * 70, trader3.getScore(2));
        assertQEquals(trader4Balance, trader4.getScore(2));
    }

    public void testUnaryNoCarryForward() throws IOException, ScoreException, DuplicateOrderException {
        final String unaryNoCarryForward = "sessionTitle: unaryNoCarryForwardTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "carryForward: false\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "publicValueLabel: Dividend Value\n" +
                "totalAssetsLabel: Total Asset Value<br><font size='-2'>(Cash + reserves + shares x dividends)</font> \n" +
                "\n" +
                "commonDividendValue:          40, 70\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(unaryNoCarryForward);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();

        session.startSession(0);
        assertQEquals(40.0, session.getDividend(1));
        assertQEquals(70.0, session.getDividend(2));

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));

        double trader1Balance = 200;
        double trader2Balance = 200;
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(200, trader3.balance());
        assertQEquals(200, trader4.balance());

        limitOrder(market, yes, 38, 3, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(38).inverted(), 1, trader2.getUser());   // trade @ 62
        assertQEquals(trader1Balance -= 38, trader1.balance());
        assertQEquals(trader2Balance += 38 - 70, trader2.balance());

        assertQEquals(1, trader1.getUser().getAccounts().couponCount(yes));
        assertQEquals(1, trader2.getUser().getAccounts().couponCount(no));
        assertQEquals(70, reserves(trader2));
        session.endTrading(false);
/////////////// ROUND 2 //////////////////////////////////////////
        TraderScreen traderScreen = new TraderScreen();
        traderScreen.setUserName(trader1.getName());
        assertREMatches(".*Actual Ticket Value.*reserves \\+ shares x dividends.*", traderScreen.showEarningsSummary());

        session.startNextRound();
        trader1Balance = 200;
        trader2Balance = 200;
        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(0, reserves(trader1));
        assertQEquals(0, reserves(trader2));
        assertQEquals(0, reserves(trader3));
        assertQEquals(0, reserves(trader4));
        double trader3Balance = 200;
        double trader4Balance = 200;
        assertQEquals(trader4Balance, trader4.balance());
        assertQEquals(trader3Balance, trader3.balance());

        limitOrder(market, yes, 43, 3, trader3.getUser());
        limitOrder(market, no, Price.dollarPrice(43).inverted(), 3, trader4.getUser());
        assertQEquals(trader3Balance -= (3 * 43), trader3.balance());
        assertQEquals(trader4Balance += (3 * 43) - (3 * 70), trader4.balance());

        assertQEquals(3, trader3.getUser().getAccounts().couponCount(yes));
        assertQEquals(3, trader4.getUser().getAccounts().couponCount(no));
        assertQEquals(0, reserves(trader3));
        assertQEquals(3 * 70, reserves(trader4));
        session.endTrading(false);

        assertQEquals(trader3Balance, trader3.balance());
        assertQEquals((trader3Balance + (3 * 70)), trader3.getScore(2));
        assertQEquals(trader4Balance, trader4.balance());
        assertQEquals(3 * 70, reserves(trader4));
        assertQEquals(trader4Balance, trader4.getScore(2));

        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader1Balance, trader1.getScore(2));
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(0, reserves(trader2));
        assertQEquals(trader2Balance, trader2.getScore(2));

        session.endTrading(false);

        assertQEquals(trader1Balance, trader1.getScore(2));
        assertQEquals(trader2Balance, trader2.getScore(2));
        assertQEquals(trader3Balance + 3 * 70, trader3.getScore(2));
        assertQEquals(trader4Balance, trader4.getScore(2));
    }

    public void testUnaryNoCarryForwardNotStuck() throws IOException, ScoreException, DuplicateOrderException {
        final String unaryNoCarryForwardNotStuck = "sessionTitle: unaryNoCarryForwardNotStuckTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader6, trader4, manipulator1\n" +
                "roles: trader, manipulator\n" +
                "carryForward: false\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "betterPriceRequired: false\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader6.role: trader\n" +
                "trader4.role: trader\n" +
                "manipulator1.role: manipulator\n" +
                "\n" +
                "endowment.trader: 300\n" +
                "tickets.trader: 0\n" +
                "endowment.manipulator: 1200\n" +
                "tickets.manipulator: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "publicValueLabel: Dividend Value\n" +
                "totalAssetsLabel: Total Asset Value<br><font size='-2'>(Cash + reserves + shares x dividends)</font> \n" +
                "\n" +
                "commonDividendValue:          0, 100\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(unaryNoCarryForwardNotStuck);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        User t1 = trader1.getUser();
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        User t2 = trader2.getUser();
        TradingSubject trader6 = (TradingSubject) session.getPlayer("trader6");
        User t6 = trader6.getUser();
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");
        User t4 = trader4.getUser();
        TradingSubject manipulator1 = (TradingSubject) session.getPlayer("manipulator1");
        User m1 = manipulator1.getUser();

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();
        BinaryClaim claim = market.getBinaryClaim();

        session.startSession(0);

        assertQEquals(0, trader1.currentCouponCount(claim));
        assertQEquals(0, trader2.currentCouponCount(claim));
        assertQEquals(0, trader6.currentCouponCount(claim));
        assertQEquals(0, trader4.currentCouponCount(claim));
        assertQEquals(0, manipulator1.currentCouponCount(claim));

        double trader1Balance = 300;
        double trader2Balance = 300;
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(300, trader6.balance());
        assertQEquals(300, trader4.balance());

        limitOrder(market, yes, 5, 1, t4);
        limitOrder(market, yes, 33, 1, t1);
        limitOrder(market, yes, 10, 1, t2);
        limitOrder(market, yes, 15, 1, t2);
        limitOrder(market, yes, 44, 1, m1);
//        limitOrder(market, yes, 07, 1, t6);

        marketOrder(market, no, Price.dollarPrice(44).inverted(), 1, t2);
        limitOrder(market, no, Price.dollarPrice(85).inverted(), 1, t1);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t2);

        limitOrder(market, no, Price.dollarPrice(88).inverted(), 1, m1);

        marketOrder(market, no, Price.dollarPrice(33).inverted(), 1, t6);
        marketOrder(market, no, Price.dollarPrice(15).inverted(), 1, t6);
        marketOrder(market, no, Price.dollarPrice(10).inverted(), 1, t6);
//    cancel buy at 7

        marketOrder(market, yes, 75, 1, t1);
        marketOrder(market, yes, 85, 1, m1);
        marketOrder(market, no, Price.dollarPrice(5).inverted(), 1, t2);

        limitOrder(market, yes, 6, 1, t4);
        limitOrder(market, yes, 44, 1, m1);
        limitOrder(market, yes, 7, 1, t4);

        assertQEquals(44, market.getBook().bestBuyOfferFor(yes));
        marketOrder(market, no, Price.dollarPrice(70).inverted(), 1, t6);
        marketOrder(market, no, Price.dollarPrice(44).inverted(), 1, t6);
        assertQEquals(44, market.getBook().bestBuyOfferFor(yes));
    }

    public void testUnaryNoCarryForwardStuckOrders() throws IOException, ScoreException, DuplicateOrderException {
        final String unaryNoCarryForwardStuck = "sessionTitle: unaryNoCarryForwardStuckTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader6, trader4, trader5, manipulator1\n" +
                "roles: trader, manipulator\n" +
                "carryForward: false\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "betterPriceRequired: false\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader6.role: trader\n" +
                "trader5.role: trader\n" +
                "trader4.role: trader\n" +
                "manipulator1.role: manipulator\n" +
                "\n" +
                "endowment.trader: 300\n" +
                "tickets.trader: 0\n" +
                "endowment.manipulator: 1200\n" +
                "tickets.manipulator: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "publicValueLabel: Dividend Value\n" +
                "totalAssetsLabel: Total Asset Value<br><font size='-2'>(Cash + reserves + shares x dividends)</font> \n" +
                "\n" +
                "commonDividendValue:          0, 100\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(unaryNoCarryForwardStuck);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        User t1 = trader1.getUser();
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        User t2 = trader2.getUser();
        TradingSubject trader6 = (TradingSubject) session.getPlayer("trader6");
        User t6 = trader6.getUser();
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");
        User t4 = trader4.getUser();
        TradingSubject trader5 = (TradingSubject) session.getPlayer("trader5");
        User t5 = trader5.getUser();
        TradingSubject manipulator1 = (TradingSubject) session.getPlayer("manipulator1");
        User m1 = manipulator1.getUser();

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();
        BinaryClaim claim = market.getBinaryClaim();

        session.startSession(0);

        assertQEquals(0, trader1.currentCouponCount(claim));
        assertQEquals(0, trader2.currentCouponCount(claim));
        assertQEquals(0, trader6.currentCouponCount(claim));
        assertQEquals(0, trader4.currentCouponCount(claim));
        assertQEquals(0, manipulator1.currentCouponCount(claim));

        double trader1Balance = 300;
        double trader2Balance = 300;
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(300, trader6.balance());
        assertQEquals(300, trader4.balance());

        limitOrder(market, yes, 25, 1, t1);
        marketOrder(market, no, Price.dollarPrice(25).inverted(), 1, m1);
        for (int i = 0 ; i < 3 ; i++ ) {
            limitOrder(market, yes, 40, 1, t2);
            marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
            limitOrder(market, yes, 40, 1, t4);
            marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
            limitOrder(market, yes, 40, 1, t6);
            marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
            limitOrder(market, yes, 40, 1, t1);
            marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
            limitOrder(market, yes, 40, 1, t5);
            marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
        }

        limitOrder(market, yes, 40, 1, t6);
        marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);
        limitOrder(market, yes, 40, 1, t2);
        marketOrder(market, no, Price.dollarPrice(40).inverted(), 1, m1);

        assertQEquals(0, market.getBook().bestBuyOfferFor(no));
        assertQEquals(0, market.getBook().bestBuyOfferFor(yes));

        limitOrder(market, no, Price.dollarPrice(44).inverted(), 1, m1);
        assertQEquals(100 - 44, market.getBook().bestBuyOfferFor(no));
        assertQEquals(105, m1.cashOnHand());
        assertQEquals(1800, m1.reserveBalance(no));
        assertQEquals(-18, m1.couponCount(claim));
        assertQEquals(4, t1.couponCount(claim));
        assertQEquals(155, t1.cashOnHand());
        marketOrder(market, yes, 44, 1, t1);
        assertQEquals(0, market.getBook().bestBuyOfferFor(no));
        assertQEquals(105, m1.cashOnHand());
        assertQEquals(1800, m1.reserveBalance(no));
        assertQEquals(-18, m1.couponCount(claim));
        assertQEquals(4, t1.couponCount(claim));
        assertQEquals(155, t1.cashOnHand());
    }

    public void testDividendReserves() throws IOException, ScoreException, DuplicateOrderException {
        final String dividendReserves = "sessionTitle: dividendReservesTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "carryForward: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          40, 50\n" +
                "\n" +
                "trader1.hint:         not100,     not40\n" +
                "trader2.hint:         not40,      notZero\n" +
                "trader3.hint:         not40,      notZero\n" +
                "worth100: Your score will improve if the judge thinks the \\\n" +
                "tickets are worth 100";
        setupSession(dividendReserves);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();
        assertTrue(session.reservesAreRequired());

        session.startSession(0);
        assertQEquals(40.0, session.getDividend(1));
        assertQEquals(50, session.getDividend(2));
        assertQEquals(100, session.getRemainingDividend(trader2, 1));
        assertQEquals(50, session.getRemainingDividend(trader2, 2));

        double t1 = 200;
        double t2 = 200;
        double t3 = 200;
        double t4 = 200;

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(t1, trader1.balance());
        assertQEquals(t2, trader2.balance());
        assertQEquals(t3, trader3.balance());
        assertQEquals(t4, trader4.balance());

        limitOrder(market, yes, 38, 5, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(38).inverted(), 1, trader2.getUser());   // trade @ 38
        assertQEquals(100, trader2.getUser().reserveBalance(no));
        assertQEquals(t1 -= 38, trader1.balance());
        assertQEquals(t2 += 38 - 100, trader2.balance());

        limitOrder(market, yes, 42, 2, trader4.getUser());
        limitOrder(market, no, Price.dollarPrice(42).inverted(), 2, trader3.getUser());
        assertQEquals(t4 -= (2 * 42), trader4.balance());
        assertQEquals(t3 += (2 * 42) - (2 * 100), trader3.balance());
        assertQEquals(t1, trader1.balance());
        assertQEquals(t2, trader2.balance());

        assertQEquals(1, trader1.getUser().getAccounts().couponCount(yes));
        assertQEquals(0, trader1.getUser().reserveBalance(no));
        assertQEquals(1, trader2.getUser().getAccounts().couponCount(no));
        assertQEquals(0, trader1.getUser().reserveBalance(no));
        assertQEquals(-2, trader3.getUser().getAccounts().couponCount(market.getClaim()));
        assertQEquals(2, trader4.getUser().getAccounts().couponCount(market.getClaim()));
        session.endTrading(false);

        assertQEquals(t1 += 40, trader1.balance());
        assertQEquals(t2 += 10, trader2.balance());
        assertQEquals(t3 += 20, trader3.balance());
        assertQEquals(t4 += 80, trader4.balance());

        assertQEquals(1, trader1.getScoreComponent(TradingSubject.AssetsComponent));
        session.startNextRound();

        limitOrder(market, no, 65, 3, trader1.getUser());                 // offered 3
        assertQEquals(65, market.getBook().bestBuyOfferFor(no));
        limitOrder(market, yes, Price.dollarPrice(65).inverted(), 2, trader3.getUser());          // accepted 2
        assertQEquals(-1, trader1.currentCouponCount((BinaryClaim) yes.getClaim()));
        assertQEquals(50, trader1.getUser().reserveBalance(no));
        assertQEquals(1, trader2.getUser().getAccounts().couponCount(no));
        assertQEquals(50, trader2.getUser().reserveBalance(no));
        assertQEquals(0, trader3.getUser().getAccounts().couponCount(market.getClaim()));
        assertQEquals(0, trader3.getUser().reserveBalance(no));
        assertQEquals(2, trader4.getUser().getAccounts().couponCount(market.getClaim()));
        assertQEquals(0, trader4.getUser().reserveBalance(no));
        assertEquals("", trader1.getScoreExplanation());
        assertQEquals(t1 += (2 * 35) - 50, trader1.balance());  // sell 1, short 1
        assertQEquals(t2, trader2.balance());
        assertQEquals(t3 -= 2 * (35 - 50), trader3.balance()); // cover 2
        session.endTrading(false);

        assertQEquals(-1, trader1.getScoreComponent(TradingSubject.AssetsComponent));
        assertQEquals(t1, trader1.getScore(2));
        assertQEquals(t2, trader2.getScore(2));
        assertQEquals(t3, trader3.getScore(2));
        assertQEquals(t4 + 2 * 50, trader4.getScore(2));
    }

    public void testOrderCantAffordTradeZeroBalance() throws DuplicateOrderException, IOException, IncompatibleOrderException {
        final String reservesScenario = "sessionTitle: OrderCantAffordTradeZeroBalance\n" +
                "rounds: 3\n" +
                "players: trader1, trader2\n" +
                "roles: trader, trader0\n" +
                "carryForward: false\n" +
                "timeLimit: 5\n" +
                "maxPrice: 400\n" +
                "useUnaryAssets: true\n" +
                "wholeShareTradingOnly: true\n" +
                "requireReserves: true\n" +
                "trader1.role: trader0\n" +
                "trader2.role: trader\n" +
                "\n" +
                "endowment.trader0: 0\n" +
                "tickets.trader0: 1\n" +
                "endowment.trader: 100\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          60, 40, 20\n" +
                "\n" +
                "trader1.hint:         moreThanDollar,     not100,       not100\n" +
                "trader2.hint:         not40,      notZero,     notZero\n" +
                "\n" +
                "moreThanDollar: The total dividend is more than 100\n" +
                "lessThanDollar: The total dividend is less than 100\n" +
                "not100: The current dividend is not 100.\n" +
                "not40: The current dividend is not 40.\n" +
                "notZero: The current dividend is not 0.\n" +
                "noMessage:";
        setupSession(reservesScenario);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");

        BinaryMarket market = session.getMarket();
        Book book = market.getBook();
        BinaryClaim claim = (BinaryClaim) market.getClaim();
        Position yes = claim.getYesPosition();
        Position no = yes.opposite();
        assertTrue(session.reservesAreRequired());
        session.startSession(0);

        addOrder(book, no, "70", 1, trader1.getUser());
        assertQEquals(1, trader1.getUser().couponCount(yes));
        assertQEquals(0, trader2.getUser().couponCount(yes));
        assertQEquals(1, book.bestQuantity(no));

        marketOrder(market, yes, "30", 1, trader2.getUser());
        assertQEquals(0, book.bestQuantity(yes));
        assertQEquals(0, trader1.getUser().couponCount(yes));
        assertQEquals(1, trader2.getUser().couponCount(yes));
    }

    public void testReservesScenario() throws IOException, ScoreException, DuplicateOrderException {
        final String reservesScenario = "sessionTitle: dividendReservesScenarioTest\n" +
                "rounds: 3\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "carryForward: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "requireReserves: true\n" +
                "trader1.role: trader\n" +
                "trader2.role: trader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 400\n" +
                "tickets.trader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          60, 40, 20\n" +
                "\n" +
                "trader1.hint:         moreThanDollar,     not100,       not100\n" +
                "trader2.hint:         not40,      notZero,     notZero\n" +
                "trader3.hint:          not100,     notZero,     notZero\n" +
                "trader4.hint:          notZero,     not100,     notZero\n" +
                "\n" +
                "moreThanDollar: The total dividend is more than 100\n" +
                "lessThanDollar: The total dividend is less than 100\n" +
                "not100: The current dividend is not 100.\n" +
                "not40: The current dividend is not 40.\n" +
                "notZero: The current dividend is not 0.\n" +
                "noMessage:";
        setupSession(reservesScenario);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        BinaryClaim claim = (BinaryClaim) market.getClaim();
        Position yes = claim.getYesPosition();
        Position no = yes.opposite();
        assertTrue(session.reservesAreRequired());
        double totalReserves = 180;

        session.startSession(0);

        assertQEquals(0, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader3.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));

        double t1 = 400;
        double t2 = 400;
        double t3 = 400;
        double t4 = 400;

        assertQEquals(t1, trader1.balance());
        assertQEquals(t2, trader2.balance());
        assertQEquals(t3, trader3.balance());
        assertQEquals(t4, trader4.balance());

        limitOrder(market, yes, 35, 1, trader1.getUser()); // Trader1 buy +1
        marketOrder(market, no, 65, 1, trader2.getUser()); // Trader 2 short  -1
        assertQEquals( 1, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader2.currentCouponCount(claim));
        assertQEquals(totalReserves, reserves(trader2));
        assertQEquals(t1 -= 35, trader1.balance());
        assertQEquals(t2 += 35 - totalReserves, trader2.balance());
        assertReservesDisplay(trader1, 0);
        assertReservesDisplay(trader2, 180);

        limitOrder(market,  no, 37, 1, trader3.getUser());  // Trader3 short  -1
        marketOrder(market, yes, 63, 1, trader1.getUser()); // Trader1 buy   +2
        assertQEquals( 2, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader3.currentCouponCount(claim));
        assertQEquals(0, reserves(trader1));
        assertReservesDisplay(trader1, 0);
        assertQEquals(totalReserves, reserves(trader3));
        assertQEquals(t1 -= 63, trader1.balance());
        assertQEquals(t3 += 63 - totalReserves, trader3.balance());

        limitOrder(market, yes, 60, 3, trader3.getUser());  // trader3 cover(1) and buy(2)  +2
        marketOrder(market, no, 40, 3, trader1.getUser());  // trader1 sell(2) and short(1) -1
        assertQEquals(-1, trader1.currentCouponCount(claim));
        assertQEquals( 2, trader3.currentCouponCount(claim));
        assertQEquals(totalReserves, reserves(trader1));
        assertReservesDisplay(trader1, q(totalReserves));
        assertQEquals(0  , reserves(trader3));
        assertQEquals(t1 += (3 * 60) - totalReserves, trader1.balance());
        assertQEquals(t3 -= (3 * 60) - (totalReserves), trader3.balance());

        limitOrder(market, yes, 38, 1, trader1.getUser());  // trader1 cover 0
        marketOrder(market, no, 62, 1, trader2.getUser());  // trader2 short -2
        assertQEquals( 0, trader1.currentCouponCount(claim));
        assertQEquals(-2, trader2.currentCouponCount(claim));
        assertReservesDisplay(trader2, q(2 * totalReserves));
        assertQEquals(0  , reserves(trader1));
        assertQEquals(totalReserves * 2, reserves(trader2));
        assertQEquals(t1 -= 38 - totalReserves, trader1.balance());
        assertQEquals(t2 += 38 - totalReserves, trader2.balance());

        limitOrder(market, yes, 38, 1, trader2.getUser());  // trader2 cover -1
        marketOrder(market, no, 62, 1, trader3.getUser());  // trader3 sell +1
        assertQEquals(-1, trader2.currentCouponCount(claim));
        assertQEquals( 1, trader3.currentCouponCount(claim));
        assertQEquals(totalReserves, reserves(trader2));
        assertQEquals(0  , reserves(trader3));
        assertReservesDisplay(trader2, q(totalReserves));
        assertReservesDisplay(trader3, 0);
        assertQEquals(t2 -= 38 - totalReserves, trader2.balance());
        assertQEquals(t3 += 38, trader3.balance());

        limitOrder(market, no, 42, 1, trader3.getUser());  // trader3 sell 0
        marketOrder(market, yes, 58, 1, trader2.getUser()); // trader2 cover 0
        assertQEquals(0, trader2.currentCouponCount(claim));
        assertQEquals(0, trader3.currentCouponCount(claim));
        assertQEquals(0, reserves(trader2));
        assertQEquals(0, reserves(trader3));
        assertReservesDisplay(trader2, 0);
        assertReservesDisplay(trader3, 0);
        assertQEquals(t2 -= 58 - totalReserves, trader2.balance());
        assertQEquals(t3 += 58, trader3.balance());
    }

    private void assertReservesDisplay(TradingSubject user, Quantity reserves) {
        if (reserves.isZero()) {
            assertReserveDisplayString(user, "");
        } else {
            assertReservesDisplay(user, reserves.printAsQuantity());
        }
    }

    private void assertReservesDisplay(TradingSubject user, int reserves) {
        if (reserves == 0) {
            assertReserveDisplayString(user, "");
        } else {
            assertReservesDisplay(user, Integer.toString(reserves));
        }
    }

    private void assertReservesDisplay(TradingSubject user, String reserves) {
        assertReserveDisplayString(user, "<tr>\n<td>Reserves</td>\n<td>" + reserves + "</td>\n</tr>\n");
    }

    private void assertReserveDisplayString(TradingSubject user, String expected) {
        TraderScreen traderScreen = new TraderScreen();
        traderScreen.setUserName(user.getName());
        assertMatches(expected,
                traderScreen.getReservesRow());
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
        final Judge judge4 = ((JudgingSession)session).getJudgeOrNull("judge4");

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];

        session.startSession(0);
        assertQEquals(30, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30, trader2.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, trader2.balance());
        assertQEquals(200, trader1.balance());

        limitOrder(market, onePosition, 38, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(38).inverted(), 1, trader2.getUser());
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        limitOrder(market, onePosition, 55, 1, trader1.getUser());
        limitOrder(market, onePosition, 77, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(77).inverted(), 1, trader1.getUser());
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        limitOrder(market, anotherPosition, Price.dollarPrice(78).inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, 78, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(55).inverted(), 1, trader2.getUser());
        limitOrder(market, onePosition, 26, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(63).inverted(), 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(26).inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, 63, 1, trader2.getUser());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        judge4.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judge4.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));

        String traderAExplanation = trader1.getScoreExplanation();
        assertMatches("<tr><th>Dividend Value</th><th>Total Dividend added to Cash</th></tr>", traderAExplanation);
        assertREMatches(".*>10</td><.*>280</td></tr>.*", traderAExplanation);
        String traderBExplanation = trader2.getScoreExplanation();
        assertMatches("<tr><th>Dividend Value</th><th>Total Dividend added to Cash</th></tr>", traderBExplanation);
        assertREMatches(".*>10</td><.*>320</td></tr>.*", traderBExplanation);
        String manipulatorCExplanation = manipulator3.getScoreExplanation();
        assertMatches("<th>Target</th><th>Judges' Estimate</th><th>Bonus", manipulatorCExplanation);
        assertREMatches(".*>40</td><.*>50</td><.*>180</td>.*", manipulatorCExplanation);

        assertQEquals((250 - (.02 * (50 - 20) * (50 - 20))), judge4.getScore(1));
        assertQEquals(0, trader1.getScore(1));
        assertQEquals(0, trader2.getScore(1));

        jSession.endScoringPhase();   // again!

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());

        String manipulatorCExplanationAgain = manipulator3.getScoreExplanation();
        assertMatches("<th>Target</th><th>Judges' Estimate</th><th>Bonus", manipulatorCExplanationAgain);
        assertREMatches(".*>40<.*>50<.*>180<.*", manipulatorCExplanationAgain);

        session.startNextRound(0);
        session.endTrading(false);
        judge4.setEstimate(2, Price.dollarPrice(40));
        assertEquals("", judge4.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(250 - (.02 * (100 - 40) * (100 - 40)), judge4.getScore(2));
        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertEquals(2, session.getCurrentRound());
        assertMatches("<tr><th>Dividend Value</th><th>Total Asset Value<br>", manipScreen.showEarningsSummary());
        assertMatches("Cumulative Earnings</td>\n<td>1780", manipScreen.showEarningsSummary());
        TraderScreen traderScreen = new TraderScreen();
        traderScreen.setUserName(trader1.getName());
        assertMatches("<tr><th>Dividend Value</th><th>Total Asset Value<br>", traderScreen.showEarningsSummary());
        assertMatches("Cumulative Earnings</td>\n<td>1471", traderScreen.showEarningsSummary());
        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judge4.getName());
        String judgeScore = judgeScreen.showEarningsSummary();
        assertMatches("<th>Actual Ticket Value</th><th>Your Estimate</th><th>", judgeScore);
        assertMatches("Cumulative Earnings</td>\n<td>410", judgeScore);

        ((JudgingSession)session).endScoringPhase();  // again

        manipScreen.setUserName(manipulator3.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1780", manipScreen.showEarningsSummary());
        traderScreen.setUserName(trader1.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1471", traderScreen.showEarningsSummary());
        judgeScreen.setUserName(judge4.getName());
        assertMatches("Cumulative Earnings</td>\n<td>410", judgeScreen.showEarningsSummary());
    }

    public void testDontDisplayCarryForward() throws DuplicateOrderException, ScoreException, IOException {
        final String CARRY_FORWARD_SCRIPT = "sessionTitle: carryForwardTest\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, manipulator3, judge4\n" +
            "timeLimit: 5\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "\n" +
            "carryForward: all\n" +
            "displayCarryForwardScores: false" +
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

        limitOrder(market, onePosition, 38, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(38).inverted(), 1, trader2.getUser());
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        limitOrder(market, onePosition, 55, 1, trader1.getUser());
        limitOrder(market, onePosition, 77, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(77).inverted(), 1, trader1.getUser());
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        limitOrder(market, anotherPosition, Price.dollarPrice(78).inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, 78, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(55).inverted(), 1, trader2.getUser());
        limitOrder(market, onePosition, 26, 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(63).inverted(), 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(26).inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, 63, 1, trader2.getUser());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        judge4.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judgeD.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));

        String traderAExplanation = trader1.getScoreExplanation();
        assertEquals("", traderAExplanation);
        assertRENoMatch(".*>10</td><.*>280</td></tr>.*", traderAExplanation);
        String traderBExplanation = trader2.getScoreExplanation();
        assertEquals("", traderBExplanation);
        assertRENoMatch(".*>10</td><.*>320</td></tr>.*", traderBExplanation);
        String manipulatorCExplanation = manipulator3.getScoreExplanation();
        assertEquals("", manipulatorCExplanation);
        assertRENoMatch(".*>40</td><.*>50</td><.*>180</td>.*", manipulatorCExplanation);

        assertQEquals((250 - (.02 * (50 - 20) * (50 - 20))), judge4.getScore(1));
        assertQEquals(0, trader1.getScore(1));
        assertQEquals(0, trader2.getScore(1));

        jSession.endScoringPhase();   // again!

        assertQEquals(200 - 151 + (10 * 32), trader2.balance());
        assertQEquals(200 + 151 + (10 * 28), trader1.balance());
        assertQEquals(200 + (10 * 30) + 200 - 2 * (50 - 40), manipulator3.balance());

        String manipulatorCExplanationAgain = manipulator3.getScoreExplanation();
        assertEquals("", manipulatorCExplanationAgain);
        assertRENoMatch(".*>40<.*>50<.*>180<.*", manipulatorCExplanationAgain);

        session.startNextRound(0);
        session.endTrading(false);
        judge4.setEstimate(2, Price.dollarPrice(40));
        assertEquals("", judgeD.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(250 - (.02 * (100 - 40) * (100 - 40)), judge4.getScore(2));
        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertEquals(2, session.getCurrentRound());
        assertMatches("Cumulative Earnings</td>\n<td>1780", manipScreen.showEarningsSummary());
        TraderScreen traderScreen = new TraderScreen();
        traderScreen.setUserName(trader1.getName());
        assertMatches("Cumulative Earnings</td>\n<td>1471", traderScreen.showEarningsSummary());
        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judge4.getName());
        String judgeScore = judgeScreen.showEarningsSummary();
        assertMatches("<th>Actual Ticket Value</th><th>Your Estimate</th><th>", judgeScore);
        assertMatches("Cumulative Earnings</td>\n<td>410", judgeScore);

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
        judge1.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        assertQEquals(50, ((JudgingSession)session).judgedValue(1));

        try {
            ((JudgingSession)session).endScoringPhase();
            fail("NumberFormatException should be thrown when no manipulator target is provided");
        } catch (NumberFormatException e) {
            assertQEquals(0, manip1.getScore(1));
        }

        session.startNextRound();

    }

    public void testThresholdDependantMessagePercent() throws IOException, ScoreException, DuplicateOrderException {
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

        limitOrder(market, onePosition, 20, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(20).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 20, manip1.balance());
        assertQEquals(200 - 20, trader1.balance());

        limitOrder(market, onePosition, 15, 1, manip1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(15).inverted(), 1, trader1.getUser());
        assertQEquals(200 + 20 - 15, manip1.balance());
        assertQEquals(200 - 20 + 15, trader1.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));

        session.endTrading(false);
        judge1.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is too high", session.aboveThresholdMessage());
        assertEquals("The price is too high", session.message(2));

        limitOrder(market, onePosition, 20, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(20).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 20, manip1.balance());
        assertQEquals(200 - 20, trader1.balance());

        limitOrder(market, onePosition, 15, 1, manip1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(15).inverted(), 1, trader1.getUser());
        assertQEquals(200 + 20 - 15, manip1.balance());
        assertQEquals(200 - 20 + 15, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(2, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 40% too low", session.message(3));
    }

    public void testThresholdDependantMessageDiff() throws IOException, ScoreException, DuplicateOrderException {
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
        limitOrder(market, onePosition, 10, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(10).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 10, manip1.balance());
        assertQEquals(200 - 10, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("", session.message(2));

        limitOrder(market, onePosition, 10, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(10).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 10, manip1.balance());
        assertQEquals(200 - 10, trader1.balance());

        limitOrder(market, onePosition, 15, 1, manip1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(15).inverted(), 1, trader1.getUser());
        assertQEquals(200 + 10 - 15, manip1.balance());
        assertQEquals(200 - 10 + 15, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(2, Price.dollarPrice(50));
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
                "aboveThresholdMessage: The price is $percent$ too high\n" +
                "belowThresholdMessage: The price is $difference$ cents too low";

        setupSession(config);
        assertEquals("", session.message());

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        Manipulator manip1 = (Manipulator) session.getPlayer("manip1");
        final Judge judge1 = ((JudgingSession)session).getJudgeOrNull("judge1");

        Price p300 = new Price(q(300));
        Price p150 = new Price(150, p300);
        Price p30 = new Price(30, p300);
        Price p27 = new Price(27, p300);
        Price p15 = new Price(15, p300);

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position onePosition = positions[0];
        Position anotherPosition = positions[1];
        assertQEquals(200, manip1.balance());
        assertQEquals(200, trader1.balance());

        session.startSession(0);
        limitOrder(market, onePosition, p30, 1, trader1.getUser());
        limitOrder(market, anotherPosition, p30.inverted(), 1, manip1.getUser());
        assertQEquals(200 + 30, manip1.balance());
        assertQEquals(200 - 30, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(1, p150);
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 200 too high", session.message(2));  //  200%

        limitOrder(market, onePosition, p27, 1, trader1.getUser());
        limitOrder(market, anotherPosition, p27.inverted(), 1, manip1.getUser());
        assertQEquals(200 + 27, manip1.balance());
        assertQEquals(200 - 27, trader1.balance());

        limitOrder(market, onePosition, p15, 1, manip1.getUser());
        limitOrder(market, anotherPosition, p15.inverted(), 1, trader1.getUser());
        assertQEquals(200 + 27 - 15, manip1.balance());
        assertQEquals(200 - 27 + 15, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(2, p150);
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 9 cents too low", session.message(3));
    }

    public void testRemainingDividendThreshold() throws IOException, ScoreException, DuplicateOrderException {
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
        limitOrder(market, onePosition, 75, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(75).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 75, manip1.balance());
        assertQEquals(200 - 75, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(1, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is 5 cents too low", session.message(2));

        limitOrder(market, onePosition, 75, 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice(75).inverted(), 1, manip1.getUser());
        assertQEquals(200 + 75, manip1.balance());
        assertQEquals(200 - 75, trader1.balance());

        session.endTrading(false);
        judge1.setEstimate(2, Price.dollarPrice(50));
        assertEquals("", judge1.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
        assertEquals("The price is %7 too high", session.message(3));
    }

    public void testInfiniteLoopScenario() throws IOException, ScoreException, DuplicateOrderException {
        String config = "\n" +
                "sessionTitle: Manipulation-9-10\n" +
                "rounds: 20\n" +
                "players: j1, j2, j3, j4, j5, j6, j7, j8, j9, j10, t1, t2, t3, t4, t5, t6, t7, t8\n" +
                "timeLimit: 3\n" +
                "initialHint: Trading has not started yet.\n" +
                "useUnaryAssets: true\n" +
                "betterPriceRequired: false\n" +
                "requireReserves: true\n" +
                "\n" +
                "manipulatorRewards: difference\n" +
                "judgeRewards:     0,5,10,15,20,25,30,35,40,45,50,54.5,58.5,62,65,67.5,69.5,71,72,72.5,72.7\n" +
                "judgeInputChoices: slider\n" +
                "earliestJudgeCutoff: 1\n" +
                "latestJudgeCutoff: 11\n" +
                "simultaneousJudgeCutoff: false\n" +
                "judgeSliderLabel: Amount to be invested in Black\n" +
                "judgeSliderFeedbackLabel: Black\n" +
                "judgeSliderStepsize: 5\n" +
                "\n" +
                "j1.role: judge\n" +
                "j2.role: judge\n" +
                "j3.role: judge\n" +
                "j4.role: judge\n" +
                "j5.role: judge\n" +
                "j6.role: judge\n" +
                "j7.role: judge\n" +
                "j8.role: judge\n" +
                "j9.role: judge\n" +
                "j10.role: judge\n" +
                "t1.role: trader\n" +
                "t2.role: trader\n" +
                "t3.role: trader\n" +
                "t4.role: trader\n" +
                "t5.role: trader\n" +
                "t6.role: trader\n" +
                "t7.role: trader\n" +
                "t8.role: trader\n" +
                "\n" +
                "actualValue: 100, 100, 0, 100, 0, 0, 100, 0, 100, 100, 0, 0, 0, 100, 100, 0, 0, 0, 0, 100\n" +
                "eventOutcome: Black, Black, White, Black, White, White, Black, White, Black, Black, White, White, White, Black, Black, White, White, White, White, Black\n" +
                "\n" +
                "endowment.trader: 300\n" +
                "endowment.manipulator: 1200\n" +
                "tickets.trader: 0\n" +
                "tickets.manipulator: 0\n" +
                "\n" +
                "#each round one trader is blank, and one or two get wrong hints\n" +
                "t1.hint: lg,dg,lg,dg,dg,lg,lg,dg,dg,dg,lg,lg,lg,lg,dg,lg,lg,lg,lg,dg\n" +
                "t2.hint: dg,lg,lg,dg,lg,lg,lg,lg,dg,dg,dg,lg,dg,dg,dg,lg,dg,dg,lg,lg\n" +
                "t3.hint: dg,dg,lg,dg,lg,lg,lg,dg,dg,dg,lg,dg,lg,dg,dg,lg,lg,lg,dg,dg\n" +
                "t4.hint: lg,lg,dg,dg,lg,lg,dg,lg,dg,lg,lg,dg,lg,dg,dg,lg,lg,lg,lg,dg\n" +
                "t5.hint: lg,dg,lg,lg,lg,lg,dg,lg,dg,lg,lg,lg,dg,dg,dg,lg,lg,dg,dg,dg\n" +
                "t6.hint: lg,dg,lg,dg,lg,lg,dg,lg,dg,lg,lg,dg,lg,dg,lg,dg,lg,lg,lg,lg\n" +
                "t7.hint: lg,dg,dg,lg,lg,dg,lg,dg,dg,dg,lg,dg,lg,lg,lg,lg,dg,dg,dg,lg\n" +
                "t8.hint: dg,dg,lg,dg,lg,lg,dg,lg,lg,dg,lg,dg,lg,lg,dg,dg,lg,lg,lg,dg\n" +
                "\n" +
                "commonMessage: a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a, a\n" +
                "\n" +
                "traderInvestmentLabel: Actual Value\n";

        setupSession(config);
        assertEquals("", session.message());

        User t1 = ((TradingSubject) session.getPlayer("t1")).getUser();
        User t2 = ((TradingSubject) session.getPlayer("t2")).getUser();
        User t3 = ((TradingSubject) session.getPlayer("t3")).getUser();
        User t4 = ((TradingSubject) session.getPlayer("t4")).getUser();
        User t5 = ((TradingSubject) session.getPlayer("t5")).getUser();
        User t6 = ((TradingSubject) session.getPlayer("t6")).getUser();
        User t7 = ((TradingSubject) session.getPlayer("t7")).getUser();
        User t8 = ((TradingSubject) session.getPlayer("t8")).getUser();

        BinaryMarket market = session.getMarket();
        Position[] positions = market.getClaim().positions();
        Position yes = positions[0];
        Position no = positions[1];

        session.startSession(0);
        limitOrder(market, yes, 45, 1, t1);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t4);
        marketOrder(market, yes, 75, 1, t2);
        marketOrder(market, no, Price.dollarPrice(45).inverted(), 1, t6);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t4);
        marketOrder(market, yes, 75, 1, t8);
        limitOrder(market, no, Price.dollarPrice(80).inverted(), 1, t8);
        marketOrder(market, yes, 80, 1, t6);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t5);
        marketOrder(market, yes, 75, 1, t2);
        limitOrder(market, no, Price.dollarPrice(56).inverted(), 1, t6);
        limitOrder(market, no, Price.dollarPrice(80).inverted(), 1, t4);
        marketOrder(market, yes, 56, 1, t3);
        marketOrder(market, yes, 80, 1, t2);
        limitOrder(market, yes, 45, 1, t7);
        limitOrder(market, yes, 55, 1, t7);
        limitOrder(market, yes, 56, 1, t1);
        limitOrder(market, no, Price.dollarPrice(80).inverted(), 1, t7);
        marketOrder(market, no, Price.dollarPrice(56).inverted(), 1, t6);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t7);
        marketOrder(market, yes, 75, 1, t8);
        marketOrder(market, no, Price.dollarPrice(55).inverted(), 1, t6);
        marketOrder(market, no, Price.dollarPrice(45).inverted(), 1, t6);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t7);
        marketOrder(market, yes, 75, 1, t5);
        limitOrder(market, yes, 50, 1, t7);
        limitOrder(market, yes, 45, 1, t6);
        limitOrder(market, no, Price.dollarPrice(75).inverted(), 1, t7);
        marketOrder(market, yes, 75, 1, t6);
        marketOrder(market, no, Price.dollarPrice(50).inverted(), 1, t2);
        limitOrder(market, yes, 51, 1, t1);
        marketOrder(market, no, Price.dollarPrice(51).inverted(), 1, t2);
        marketOrder(market, yes, 80, 1, t6);
        limitOrder(market, yes, 47, 1, t1);
        marketOrder(market, no, Price.dollarPrice(47).inverted(), 1, t6);
        limitOrder(market, yes, 50, 1, t7);
        limitOrder(market, yes, 65, 1, t4);
        marketOrder(market, no, Price.dollarPrice(65).inverted(), 1, t7);
        limitOrder(market, no, Price.dollarPrice(70).inverted(), 1, t4);
        marketOrder(market, yes, 70, 1, t2);
        limitOrder(market, yes, 65, 1, t4);
        limitOrder(market, yes, 47, 1, t6);
        limitOrder(market, yes, 48, 1, t6);

        marketOrder(market, no, Price.dollarPrice(65).inverted(), 1, t6);

        session.endTrading(false);
        ((JudgingSession)session).endScoringPhase();
        session.startNextRound(0);
    }
}
