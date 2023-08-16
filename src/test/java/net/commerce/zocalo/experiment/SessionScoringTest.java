package net.commerce.zocalo.experiment;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.io.IOException;

import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.UnaryMarket;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.experiment.role.Manipulator;
import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.Currency;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.JspSupport.TraderScreen;
import net.commerce.zocalo.JspSupport.JudgeScreen;
import static net.commerce.zocalo.service.PropertyKeywords.DOT_SEPARATOR;
import static net.commerce.zocalo.service.PropertyKeywords.TARGET_PROPERTY_WORD;
import static net.commerce.zocalo.service.PropertyKeywords.ACTUAL_VALUE_PROPNAME;
import net.commerce.zocalo.service.PropertyHelper;

public class SessionScoringTest extends SessionTestCase {
    public void testScoring() throws ScoreException {
        double actualOne = actualValue(1);
        double actualTwo = actualValue(2);
        double actualThree = actualValue(3);

        double targetOne = target(manipulatorC, 1);
        double targetTwo = target(manipulatorC, 2);
        double targetThree = target(manipulatorC, 3);

        int estimateOne = 45;
        int estimateTwo = 25;
        int estimateThree = 35;

        double traderEndowment = 100;
        double manipulatorEndowment = 50;
        double manipulatorFactor = 2;
        double manipulatorConstant = 200;
        double judgeFactor = 0.02;
        double judgeConstant = 250;
        double traderTickets = 30;
        double manipulatorTickets = 20;

        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judgeD.getName());
        int round;

        // first round
        session.startSession();
        round = 1;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));

        setEstimate(judgeD, 1, estimateOne);
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals((traderEndowment) + (traderTickets * actualOne), traderA.getScore(1));
        assertQEquals(judgeConstant - judgeFactor * (estimateOne - actualOne) * (estimateOne - actualOne), judgeD.getScore(1));
        assertQEquals(manipulatorEndowment + (manipulatorTickets * actualOne) + manipulatorConstant
                - (manipulatorFactor * (estimateOne - targetOne)), manipulatorC.getScore(1));

        // second round
        session.startNextRound(0);
        round = 2;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judgeD, 2, estimateTwo);
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals(traderEndowment + (traderTickets * actualTwo), traderA.getScore(2));
        assertQEquals(manipulatorEndowment + (manipulatorTickets * actualTwo) + manipulatorConstant
                   - (manipulatorFactor * (targetTwo - estimateTwo)), manipulatorC.getScore(2));
        assertQEquals(judgeConstant - judgeFactor * (actualTwo - estimateTwo) * (actualTwo - estimateTwo), judgeD.getScore(2));

        // third round
        session.startNextRound(0);
        round = 3;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judgeD, 3, estimateThree);
        assertEquals("", judgeD.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals(traderEndowment + (traderTickets * actualThree), traderA.getScore(3));
        assertQEquals(manipulatorEndowment + (manipulatorTickets * actualThree) + manipulatorConstant
                        - (manipulatorFactor * (targetThree - estimateThree)),
                manipulatorC.getScore(3));
        assertQEquals(judgeConstant - judgeFactor * (actualThree - estimateThree) * (actualThree - estimateThree), judgeD.getScore(3));

        session.startNextRound();
        assertEquals("No more rounds.", session.getErrorMessage());
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

        limitOrder(market, onePosition, "38", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("38").inverted(), 1, trader2.getUser());
        assertQEquals(200 - (100 - 38), trader2.balance());
        assertQEquals(200 - 38, trader1.balance());
        assertQEquals(1, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(-1, trader2.currentCouponCount(market.getBinaryClaim()));

        limitOrder(market, onePosition, "55", 1, trader2.getUser());
        limitOrder(market, onePosition, "77", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("77").inverted(), 1, trader2.getUser());
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
        assertMatches(">5</td><td align=center>95<", traderAExplanation);
        String traderBExplanation = trader2.getScoreExplanation();
        assertMatches(">5</td><td align=center>105<", traderBExplanation);
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

        limitOrder(market, onePosition, "38", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("38").inverted(), 1, trader2.getUser());   // trade @ 38

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
        limitOrder(market, onePosition, "77", 1, trader3.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("77").inverted(), 1, trader4.getUser());   // trade @ 77

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
                "useUnaryAssets: false\n" +
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

        Quantity trader1Balance = new Quantity(200);
        assertQEquals(trader1Balance, trader1.balance());
        Quantity trader2Balance = new Quantity(200);
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(200, trader3.balance());
        assertQEquals(200, trader4.balance());

        String p41 = "41";
        Price price41 = Price.dollarPrice(p41);

        limitOrder(market, yes, p41, 3, trader1.getUser());
        limitOrder(market, no, price41.inverted(), 3, trader2.getUser());   // insufficient reserves, no trade
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());

        limitOrder(market, no, price41.inverted(), 1, trader2.getUser());   // trade @ 41

        trader1Balance = trader1Balance.minus(price41);
        trader2Balance = trader2Balance.minus(price41.inverted().plus(new Quantity("140")));
        assertQEquals(trader1Balance, trader1.balance());
        assertQEquals(trader2Balance, trader2.balance());

        assertQEquals(1, trader1.getUser().getAccounts().couponCount(yes));
        assertQEquals(1, trader2.getUser().getAccounts().couponCount(no));
        assertQEquals(0.0, trader1.getUser().releaseReserves(Quantity.ONE, no));
        assertQEquals(140, trader2.getUser().reserveBalance(no));
        session.endTrading(false);
/////////////// ROUND 2 //////////////////////////////////////////

        trader1Balance = trader1Balance.plus(new Quantity("40"));
        assertQEquals(trader1Balance, trader1.balance());
        trader2Balance = trader2Balance.plus(new Quantity("30"));
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(70, trader2.getUser().reserveBalance(no));

        session.startNextRound();

        limitOrder(market, yes, "43", 3, trader3.getUser());
        limitOrder(market, no, Price.dollarPrice("43").inverted(), 3, trader4.getUser());   // insufficient reserves, no trade
        double trader3Balance = 200;
        assertQEquals(trader3Balance, trader3.balance());
        double trader4Balance = 200;
        assertQEquals(trader4Balance, trader4.balance());

        limitOrder(market, no, Price.dollarPrice("43").inverted(), 1, trader4.getUser());   // trade @ 43
        assertQEquals(trader3Balance -= 43, trader3.balance());
        assertQEquals(trader4Balance -= (100 - 43) + 70, trader4.balance());

        assertQEquals(1, trader3.getUser().getAccounts().couponCount(yes));
        assertQEquals(1, trader4.getUser().getAccounts().couponCount(no));
        assertQEquals(0, trader3.getUser().reserveBalance(no));
        assertQEquals(70, trader4.getUser().reserveBalance(no));
        session.endTrading(false);

        assertQEquals(trader3Balance, trader3.balance());
        assertQEquals((trader3Balance + (1 * 70)), trader3.getScore(2));
        assertQEquals(trader4Balance, trader4.balance());
        assertQEquals(70, trader4.getUser().reserveBalance(no));
        assertQEquals(trader4Balance, trader4.getScore(2));

        assertQEquals(trader1Balance, trader1.balance());

        trader1Balance = trader1Balance.plus(new Quantity("70"));
        assertQEquals(trader1Balance, trader1.getScore(2));
        assertQEquals(trader2Balance, trader2.balance());
        assertQEquals(70, trader2.getUser().reserveBalance(no));
        assertQEquals(trader2Balance, trader2.getScore(2));
    }

    public void testDividendReserves() throws IOException, ScoreException, DuplicateOrderException {
        final String dividendReserves = "sessionTitle: dividendReservesTest\n" +
                "rounds: 2\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader\n" +
                "carryForward: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "betterPriceRequired: false\n" +
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

        TradingSubject buyer =       (TradingSubject) session.getPlayer("trader1");
        TradingSubject seller =      (TradingSubject) session.getPlayer("trader2");
        TradingSubject shortSeller = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 =     (TradingSubject) session.getPlayer("trader4");

        BinaryMarket market = session.getMarket();
        Position yes = ((BinaryClaim) market.getClaim()).getYesPosition();
        Position no = yes.opposite();
        assertTrue(session.reservesAreRequired());

        session.startSession(0);
        assertQEquals(40.0, session.getDividend(1));
        assertQEquals(50, session.getDividend(2));
        assertQEquals(100, session.getRemainingDividend(seller, 1));
        assertQEquals(50, session.getRemainingDividend(seller, 2));

        assertQEquals(0, buyer.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, seller.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, shortSeller.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, trader4.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(200, buyer.balance());
        assertQEquals(200, seller.balance());
        assertQEquals(200, shortSeller.balance());
        assertQEquals(200, trader4.balance());

        Price p38 = Price.dollarPrice("38");
        limitOrder(market, yes, p38, 1, buyer.getUser());
        limitOrder(market, no, p38.inverted(), 1, seller.getUser());   // trade @ 38
        assertQEquals(162, buyer.balance());
        assertQEquals(200 + 38 - 100, seller.balance());

        limitOrder(market, yes, p38, 2, trader4.getUser());
        limitOrder(market, no, p38.inverted(), 2, shortSeller.getUser());
        assertQEquals(200 - 2 * 38, trader4.balance());
        assertQEquals(200 + 2 * (38 - 100), shortSeller.balance());

        assertQEquals(1, buyer.getUser().getAccounts().couponCount(yes));
        assertQEquals(1, seller.getUser().getAccounts().couponCount(no));
        assertQEquals(2, trader4.getUser().getAccounts().couponCount(market.getClaim()));
        assertQEquals(-2, shortSeller.getUser().getAccounts().couponCount(market.getClaim()));
        session.endTrading(false);

        assertQEquals(50, seller.getUser().reserveBalance(no));

        session.endTrading(false);
        assertQEquals(1, buyer.getScoreComponent(TradingSubject.AssetsComponent));
        session.startNextRound();

        limitOrder(market, no, "66", 5, buyer.getUser());  // Cannot afford
        limitOrder(market, no, "64", 4, buyer.getUser());  // Holdings of 1 provide extra needed for reserves
        assertQEquals(64, market.getBook().bestBuyOfferFor(no));
        limitOrder(market, yes, Price.dollarPrice("64").inverted(), 2, shortSeller.getUser());
        assertQEquals(-1, buyer.currentCouponCount((BinaryClaim) yes.getClaim()));
        assertEquals("", buyer.getScoreExplanation());

        session.endTrading(false);
        assertQEquals(-1, buyer.getScoreComponent(TradingSubject.AssetsComponent));
    }

    public void testReservesScenario() throws IOException, ScoreException, DuplicateOrderException {
        final String reservesScenario = "sessionTitle: dividendReservesScenarioTest\n" +
                "rounds: 3\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader, richTrader\n" +
                "carryForward: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: false\n" +
                "requireReserves: true\n" +
                "trader1.role: richTrader\n" +
                "trader2.role: richTrader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 400\n" +
                "tickets.trader: 0\n" +
                "endowment.richTrader: 700\n" +
                "tickets.richTrader: 0\n" +
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

        double t1 = 700;
        double t2 = 700;
        double t3 = 400;
        double t4 = 400;

        assertQEquals(t1, trader1.balance());
        assertQEquals(t2, trader2.balance());
        assertQEquals(t3, trader3.balance());
        assertQEquals(t4, trader4.balance());

        limitOrder(market, yes, "35", 1, trader1.getUser());
        marketOrder(market, no, "65", 1, trader2.getUser());
        assertQEquals( 1, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader2.currentCouponCount(claim));
        assertQEquals(totalReserves, trader2.getUser().reserveBalance(no));
        assertQEquals(t1 -= 35, trader1.balance());
        assertQEquals(t2 -= totalReserves + 65, trader2.balance());

        limitOrder(market, no, "37", 1, trader3.getUser());
        marketOrder(market, yes, "63", 1, trader1.getUser());
        assertQEquals( 2, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader3.currentCouponCount(claim));
        assertQEquals(0, trader1.getUser().reserveBalance(no));
        assertQEquals(totalReserves, trader3.getUser().reserveBalance(no));
        assertQEquals(t1 -= 63, trader1.balance());
        assertQEquals(t3 -= 37 + totalReserves, trader3.balance());

        limitOrder(market, yes, "60", 3, trader3.getUser());
        marketOrder(market, no, "40", 3, trader1.getUser());
        assertQEquals(-1, trader1.currentCouponCount(claim));
        assertQEquals( 2, trader3.currentCouponCount(claim));
        assertQEquals(totalReserves, trader1.getUser().reserveBalance(no));
        assertQEquals(0  , trader3.getUser().reserveBalance(no));
        assertQEquals(t1 += 200 - (3 * 40) - totalReserves, trader1.balance());
        assertQEquals(t3 -= (3 * 60) - 100 - totalReserves, trader3.balance());

        limitOrder(market, yes, "38", 1, trader1.getUser());
        marketOrder(market, no, "62", 1, trader2.getUser());
        assertQEquals( 0, trader1.currentCouponCount(claim));
        assertQEquals(-2, trader2.currentCouponCount(claim));
        assertQEquals(0  , trader1.getUser().reserveBalance(no));
        assertQEquals(totalReserves * 2, trader2.getUser().reserveBalance(no));
        assertQEquals(t1 += 100 - 38 + totalReserves, trader1.balance());
        assertQEquals(t2 -= 62 + totalReserves, trader2.balance());

        limitOrder(market, yes, "38", 1, trader2.getUser());
        marketOrder(market, no, "62", 1, trader3.getUser());
        assertQEquals(-1, trader2.currentCouponCount(claim));
        assertQEquals( 1, trader3.currentCouponCount(claim));
        assertQEquals(totalReserves, trader2.getUser().reserveBalance(no));
        assertQEquals(0  , trader3.getUser().reserveBalance(no));
        assertQEquals(t2 -= 38 - totalReserves - 100, trader2.balance());
        assertQEquals(t3 -= 62 - 100, trader3.balance());

        limitOrder(market, no, "42", 1, trader3.getUser());
        marketOrder(market, yes, "58", 1, trader2.getUser());
        assertQEquals(0, trader2.currentCouponCount(claim));
        assertQEquals(0, trader3.currentCouponCount(claim));
        assertQEquals(0, trader2.getUser().reserveBalance(no));
        assertQEquals(0  , trader3.getUser().reserveBalance(no));
        assertQEquals(t2 += 100 - 58 + totalReserves, trader2.balance());
        assertQEquals(t3 += 100 - 42, trader3.balance());
    }

    public void testFractionalTradeReservesScenario() throws IOException, ScoreException, DuplicateOrderException {
        final String reservesScenario = "sessionTitle: dividendReservesScenarioTest\n" +
                "rounds: 3\n" +
                "players: trader1, trader2, trader3, trader4\n" +
                "roles: trader, richTrader\n" +
                "carryForward: false\n" +
                "wholeShareTrading: true\n" +
                "timeLimit: 5\n" +
                "useUnaryAssets: true\n" +
                "betterPriceRequired: false\n" +
                "requireReserves: true\n" +
                "trader1.role: richTrader\n" +
                "trader2.role: richTrader\n" +
                "trader3.role: trader\n" +
                "trader4.role: trader\n" +
                "\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 0\n" +
                "endowment.richTrader: 700\n" +
                "tickets.richTrader: 0\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice\n" +
                "\n" +
                "commonDividendValue:          60, 40, 20\n" +
                "maxDividend: 100\n" +
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
                "noMessage:\n";
        setupSession(reservesScenario);

        TradingSubject trader1 = (TradingSubject) session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject) session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject) session.getPlayer("trader3");
        TradingSubject trader4 = (TradingSubject) session.getPlayer("trader4");

        UnaryMarket market = (UnaryMarket) session.getMarket();
        BinaryClaim claim = (BinaryClaim) market.getClaim();
        Position yes = claim.getYesPosition();
        Position no = yes.opposite();
        assertTrue(session.reservesAreRequired());
        assertTrue(market.wholeShareTradingOnly());

        session.startSession(0);

        assertQEquals(0, trader1.currentCouponCount(claim));
        assertQEquals(0, trader2.currentCouponCount(claim));
        assertQEquals(0, trader3.currentCouponCount(claim));
        assertQEquals(0, trader4.currentCouponCount(claim));

        double t1 = 700;
        double t2 = 700;
        double t3 = 200;
        double t4 = 200;

        assertQEquals(t1, trader1.balance());
        assertQEquals(t2, trader2.balance());
        assertQEquals(t3, trader3.balance());
        assertQEquals(t4, trader4.balance());

        limitOrder(market, yes, "75", 1, trader4.getUser()); // A
        limitOrder(market, yes, "82", 1, trader1.getUser()); // B
        limitOrder(market, yes, "85", 1, trader1.getUser()); // C
        limitOrder(market, yes, "90", 1, trader1.getUser()); // D

        marketOrder(market, no, "85", 1, trader3.getUser());  // accepts D
        assertQEquals(1, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader3.currentCouponCount(claim));
        assertQEquals(0, trader4.currentCouponCount(claim));
        assertQEquals(100, trader3.getUser().reserveBalance(no));
        assertQEquals(0, trader4.getUser().reserveBalance(no));
        assertQEquals(0, trader1.getUser().reserveBalance(no));

        marketOrder(market, no, "25", 1, trader4.getUser()); // accepts C
        assertQEquals(2, trader1.currentCouponCount(claim));
        assertQEquals(-1, trader3.currentCouponCount(claim));
        assertQEquals(-1, trader4.currentCouponCount(claim));

        marketOrder(market, no, "25", 1, trader3.getUser()); // accepts B
        assertQEquals(3, trader1.currentCouponCount(claim));
        assertQEquals(-2, trader3.currentCouponCount(claim));
        assertQEquals(200, trader3.getUser().reserveBalance(no));
        assertQEquals(-1, trader4.currentCouponCount(claim));
        assertQEquals(100, trader4.getUser().reserveBalance(no));
        assertQEquals(t1 -= (90 + 85 + 82), trader1.balance());
        assertQEquals(t3 += (82 + 90) - 200, trader3.balance());
        assertQEquals(t4 += 85 - 100, trader4.balance());

        marketOrder(market, no, "95", 1, trader3.getUser());  // can't afford to accept A; Should be no trade
        assertQEquals(3, trader1.currentCouponCount(claim));
        assertQEquals(-2, trader3.currentCouponCount(claim));
        assertQEquals(200, trader3.getUser().reserveBalance(no));
        assertQEquals(-1, trader4.currentCouponCount(claim));
        assertQEquals(100, trader4.getUser().reserveBalance(no));
        assertQEquals(t1, trader1.balance());
        assertQEquals(t3, trader3.balance());
        assertQEquals(t4, trader4.balance());
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
            "carryForward: all\n" +
            "useUnaryAssets: false\n" +
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

        limitOrder(market, onePosition, "38", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("38").inverted(), 1, trader2.getUser());
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        limitOrder(market, onePosition, "55", 1, trader1.getUser());
        limitOrder(market, onePosition, "77", 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("77").inverted(), 1, trader1.getUser());
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        limitOrder(market, anotherPosition, Price.dollarPrice("78").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "78",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("55").inverted(), 1, trader2.getUser());
        limitOrder(market, onePosition, "26",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("63").inverted(), 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("26").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "63",      1, trader2.getUser());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        setEstimate(judge4, 1, 50);
        assertEquals("", judgeD.getWarningsHtml());
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
        setEstimate(judge4, 2, 40);
        assertEquals("", judgeD.getWarningsHtml());
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

    public void testEventOutcomes() throws DuplicateOrderException, ScoreException, IOException {
        final String eventOutcomeScript = "sessionTitle: eventOutcomeTest\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, manipulator3, judge4\n" +
            "timeLimit: 5\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "\n" +
            "useUnaryAssets: false\n" +
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
            "dividendValue:          10,          90\n" +
            "judge.target:          20,         100\n" +
            "eventOutcome:          White,         Black\n" +
            "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "manipulator3.hint:    not100,\tnotZero\n" +
            "manipulator3.earningsHint:    worth40,\tworth40\n" +
            "manipulator3.target:  40,         40\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(eventOutcomeScript);

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

        limitOrder(market, onePosition, "38", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("38").inverted(), 1, trader2.getUser());
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        limitOrder(market, onePosition, "55", 1, trader1.getUser());
        limitOrder(market, onePosition, "77", 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("77").inverted(), 1, trader1.getUser());
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        limitOrder(market, anotherPosition, Price.dollarPrice("78").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "78",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("55").inverted(), 1, trader2.getUser());
        limitOrder(market, onePosition, "26",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("63").inverted(), 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("26").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "63",      1, trader2.getUser());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(90, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        setEstimate(judge4, 1, 50);
        assertEquals("", judgeD.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));

        assertMatches("<tr><th>Event</th><th>Investment in Black</th><th>Earnings</th></tr>\n<tr><td align=center>White</td>", judge4.getScoreExplanation());
        assertREMatches(".*<tr><th>Event</th><th>Average Investment</th><th>Earnings(.*)</th></tr>\n<tr><td align=center>White</td>.*", manipulator3.getScoreExplanation());
        assertREMatches(".*<tr><th>Event</th><th>Value</th><th>Earnings(.*)</th></tr>\n<tr><td align=center>White</td>.*", trader2.getScoreExplanation());

        assertQEquals((250 - (.02 * (50 - 20) * (50 - 20))), judge4.getScore(1));
        assertQEquals(10 * (30 - 2) + (200 + 151), trader1.getScore(1));
        assertQEquals(10 * (30 + 2) + (200 - 151), trader2.getScore(1));

        session.startNextRound(0);
        session.endTrading(false);
        setEstimate(judge4, 2, 40);
        assertEquals("", judgeD.getWarningsHtml());
        jSession.endScoringPhase();

        assertQEquals(250 - (.02 * (100 - 40) * (100 - 40)), judge4.getScore(2));
        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertEquals(2, session.getCurrentRound());
        assertMatches("<tr><th>Event</th><th>Investment in Black</th><th>Earnings</th></tr>\n<tr><td align=center>Black</td>", judge4.getScoreExplanation());
        assertREMatches(".*<tr><th>Event</th><th>Average Investment</th><th>Earnings(.*)</th></tr>\n<tr><td align=center>Black</td>.*", manipulator3.getScoreExplanation());
        assertREMatches(".*<tr><th>Event</th><th>Value</th><th>Earnings(.*)</th></tr>\n<tr><td align=center>Black</td>.*", trader2.getScoreExplanation());

        ((JudgingSession)session).endScoringPhase();  // again
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
            "displayCarryForwardScores: false\n" +
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

        limitOrder(market, onePosition, "38", 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("38").inverted(), 1, trader2.getUser());
        assertQEquals(200 + 38, trader2.balance());
        assertQEquals(200 - 38, trader1.balance());

        limitOrder(market, onePosition, "55", 1, trader1.getUser());
        limitOrder(market, onePosition, "77", 1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("77").inverted(), 1, trader1.getUser());
        assertQEquals(200 + 38 - 77, trader2.balance());
        assertQEquals(200 - 38 + 77, trader1.balance());

        limitOrder(market, anotherPosition, Price.dollarPrice("78").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "78",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("55").inverted(), 1, trader2.getUser());
        limitOrder(market, onePosition, "26",      1, trader2.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("63").inverted(), 1, trader1.getUser());
        limitOrder(market, anotherPosition, Price.dollarPrice("26").inverted(), 1, trader1.getUser());
        limitOrder(market, onePosition, "63",      1, trader2.getUser());
        assertQEquals(30 - 2, trader1.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(30 + 2, trader2.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(200 - 151, trader2.balance());
        assertQEquals(200 + 151, trader1.balance());
        assertQEquals(200, manipulator3.balance());

        assertQEquals(10, session.getDividend(1));
        assertQEquals(30, session.getDividend(2));
        JudgingSession jSession = (JudgingSession)session;

        session.endTrading(false);
        setEstimate(judge4, 1, 50);
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
        setEstimate(judge4, 2, 40);
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

    public void testScoreJudgesByTable() throws ScoreException, IOException, DuplicateOrderException {
        final String JUDGE_REWARDS_SCRIPT = "sessionTitle: testJudgesRewards\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, manipulator3, judge4\n" +
            "timeLimit: 5\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "\n" +
            "carryForward: false\n" +
            "useUnaryAssets: false\n" +
            "\n" +
            "endowment.trader: 200\n" +
            "endowment.manipulator: 200\n" +
            "tickets.trader: 30\n" +
            "tickets.manipulator: 30\n" +
            "judgeRewards:     0,5,10,15,20,25,30,35,40,45,50,54.5,58.5,62,65,67.5,69.5,71,72,72.5,72.7\n" +
            "manipulatorRewards: difference\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          100,          0\n" +
            "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "manipulator3.hint:    not100,\tnotZero\n" +
            "manipulator3.earningsHint:    worth40,\tworth40\n" +
            "manipulator3.target:  40,         40\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(JUDGE_REWARDS_SCRIPT);

        double actualOne = actualValue(1);
        double actualTwo = actualValue(2);

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject manipulator3 = (Manipulator)session.getPlayer("manipulator3");
        Judge judge4 = ((JudgingSession)session).getJudgeOrNull("judge4");
        BinaryMarket market = session.getMarket();
        Position yes = market.getBinaryClaim().getYesPosition();
        Position no = yes.opposite();

        int estimateOne = 45;
        int estimateTwo = 25;

        double endowment = 200;
        double tickets = 30;

        double rewardOne = 45;
        double rewardTwo = 67.5;

        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judge4.getName());

        double tradeOne = 20;
        double tradeTwo = 50;
        int round;

        // first round
        session.startSession();
        round = 1;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        limitOrder(market, yes, Price.dollarPrice(tradeOne), 1, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(tradeOne).inverted(), 1, manipulator3.getUser());

        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judge4, 1, estimateOne);
        assertEquals("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals(endowment - tradeOne + ((tickets + 1) * actualOne), trader1.getScore(1));
        assertQEquals(rewardOne, judge4.getScore(1));
        assertQEquals(Math.abs(actualOne - estimateOne), manipulator3.getScore(1));

        // second round
        session.startNextRound(0);
        round = 2;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        limitOrder(market, yes, Price.dollarPrice(tradeTwo), 1, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(tradeTwo).inverted(), 1, manipulator3.getUser());
        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judge4, 2, estimateTwo);
        assertEquals("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals(endowment - tradeTwo + ((tickets + 1) * actualTwo), trader1.getScore(2));
        assertQEquals(Math.abs(actualTwo - estimateTwo), manipulator3.getScore(2));
        assertQEquals(rewardTwo, judge4.getScore(2));

        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertMatches("<tr><td>55</td><td>25</td><td>80</td></tr></table><p>", manipScreen.getScoresHtml());

        session.startNextRound();
        assertEquals("No more rounds.", session.getErrorMessage());
    }
    public void testMultiplyScores() throws ScoreException, IOException, DuplicateOrderException {
        final String MULTIPLICATIVE_SCRIPT = "sessionTitle: testMultiplyScores\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, trader3, manipulator3, judge4\n" +
            "roles: trader, newTrader, manipulator, judge\n" +
            "timeLimit: 5\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "trader3.role: newTrader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "\n" +
            "carryForward: false\n" +
            "useUnaryAssets: false\n" +
            "\n" +
            "endowment.trader: 200\n" +
            "endowment.newTrader: 200\n" +
            "endowment.manipulator: 200\n" +
            "tickets.trader: 30\n" +
            "tickets.newTrader: 30\n" +
            "tickets.manipulator: 30\n" +
            "judgeRewards:     0,5,10,15,20,25,30,35,40,45,50,54.5,58.5,62,65,67.5,69.5,71,72,72.5,72.7\n" +
            "manipulatorRewards: difference\n" +
            "multiplyScore.newTrader.1: 5\n" +
            "multiplyScore.trader.2: 3\n" +
            "multiplyScore.judge.2: 7\n" +
            "multiplyScore.manipulator3.1: 1.3\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          100,          0\n" +
            "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "trader3.hint:         not40,      notZero\n" +
            "manipulator3.hint:    not100,\tnotZero\n" +
            "manipulator3.earningsHint:    worth40,\tworth40\n" +
            "manipulator3.target:  40,         40\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(MULTIPLICATIVE_SCRIPT);

        double actualOne = actualValue(1);
        double actualTwo = actualValue(2);

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject trader2 = (TradingSubject)session.getPlayer("trader2");
        TradingSubject trader3 = (TradingSubject)session.getPlayer("trader3");
        TradingSubject manipulator3 = (Manipulator)session.getPlayer("manipulator3");
        Judge judge4 = ((JudgingSession)session).getJudgeOrNull("judge4");
        BinaryMarket market = session.getMarket();
        Position yes = market.getBinaryClaim().getYesPosition();
        Position no = yes.opposite();

        int estimateOne = 45;
        int estimateTwo = 25;

        double endowment = 200;
        double tickets = 30;

        double rewardOne = 45;
        double rewardTwo = 67.5;

        JudgeScreen judgeScreen = new JudgeScreen();
        judgeScreen.setUserName(judge4.getName());

        double tradeOne = 20;
        double tradeTwo = 50;
        int round;

        // first round
        session.startSession();
        round = 1;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        limitOrder(market, yes, Price.dollarPrice(tradeOne), 1, trader1.getUser());
        limitOrder(market,  no, Price.dollarPrice(tradeOne).inverted(), 1, manipulator3.getUser());

        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judge4, 1, estimateOne);
        assertEquals("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals(endowment - tradeOne + ((tickets + 1) * actualOne), trader1.getScore(1));
        assertQEquals(endowment + (tickets * actualOne), trader2.getScore(1));
        assertQEquals((endowment + tickets * actualOne) * 5, trader3.getScore(1));
        assertQEquals(rewardOne, judge4.getScore(1));
        assertQEquals(1.30 * Math.abs(actualOne - estimateOne), manipulator3.getScore(1));
        TraderScreen manipScreen = new TraderScreen();
        manipScreen.setUserName(manipulator3.getName());
        assertREMatches(".*<th>Multiplier</th>.*>1.3</td>.*", manipulator3.getScoreExplanation());
        assertREMatches(".*<th>Multiplier</th>.*>5</td>.*", trader3.getScoreExplanation());

        // second round
        session.startNextRound(0);
        round = 2;
        assertEquals("disabled", judgeScreen.disabledFlag(round));
        limitOrder(market, yes, Price.dollarPrice(tradeTwo), 1, trader1.getUser());
        limitOrder(market, no, Price.dollarPrice(tradeTwo).inverted(), 1, manipulator3.getUser());
        session.endTrading(false);
        assertEquals("", judgeScreen.disabledFlag(round));
        setEstimate(judge4, 2, estimateTwo);
        assertEquals("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();
        assertEquals("disabled", judgeScreen.disabledFlag(round));

        assertQEquals((endowment - tradeTwo + (tickets + 1) * actualTwo) * 3, trader1.getScore(2));
        assertQEquals((endowment + (tickets * actualTwo)) * 3, trader2.getScore(2));
        assertQEquals(endowment + (tickets * actualTwo), trader3.getScore(2));
        assertQEquals(Math.abs(actualTwo - estimateTwo), manipulator3.getScore(2));
        assertQEquals(rewardTwo * 7, judge4.getScore(2));

        assertMatches("<tr><td>71.5</td><td>25</td><td>96.5</td></tr></table><p>", manipScreen.getScoresHtml());
        assertREMatches(".*<th>Multiplier</th>.*>3</td>.*", trader1.getScoreExplanation());
        assertREMatches(".*<th>Multiplier</th>.*>7</td>.*", judge4.getScoreExplanation());

        session.startNextRound();
        assertEquals("No more rounds.", session.getErrorMessage());
    }

    public void testJudgeCutoffTimers() throws IOException, DuplicateOrderException, ScoreException, InterruptedException {
        final String JUDGE_REWARDS_SCRIPT = "sessionTitle: testJudgesRewards\n" +
            "rounds: 2\n" +
            "players: trader1, trader2, manipulator3, judge4, judge5\n" +
            "timeLimit: 0:02\n" +
            "trader1.role: trader\n" +
            "trader2.role: trader\n" +
            "manipulator3.role: manipulator\n" +
            "judge4.role: judge\n" +
            "judge5.role: judge\n" +
            "\n" +
            "carryForward: false\n" +
            "useUnaryAssets: false\n" +
            "\n" +
            "earliestJudgeCutoff: 0:01\n" +
            "latestJudgeCutoff: 0:02\n" +
            "simultaneousJudgeCutoff: false\n" +
            "endowment.trader: 200\n" +
            "endowment.manipulator: 200\n" +
            "tickets.trader: 30\n" +
            "tickets.manipulator: 30\n" +
            "judgeRewards:     0,5,10,15,20,25,30,35,40,45,50,54.5,58.5,62,65,67.5,69.5,71,72,72.5,72.7\n" +
            "manipulatorRewards: difference\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          100,          0\n" +
            "\n" +
            "trader1.hint:         not100,     not40\n" +
            "trader2.hint:         not40,      notZero\n" +
            "manipulator3.hint:    not100,\tnotZero\n" +
            "manipulator3.earningsHint:    worth40,\tworth40\n" +
            "manipulator3.target:  40,         40\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
        setupSession(JUDGE_REWARDS_SCRIPT);

        TradingSubject trader1 = (TradingSubject)session.getPlayer("trader1");
        TradingSubject manipulator3 = (Manipulator)session.getPlayer("manipulator3");
        Judge judge4 = ((JudgingSession)session).getJudgeOrNull("judge4");
        Judge judge5 = ((JudgingSession)session).getJudgeOrNull("judge5");
        BinaryMarket market = session.getMarket();
        Position yes = market.getBinaryClaim().getYesPosition();
        Position no = yes.opposite();

        String estimateOne = "45";
        String estimateTwo = "45";

        JudgeScreen judge4Screen = new JudgeScreen();
        JudgeScreen judge5Screen = new JudgeScreen();
        judge4Screen.setUserName("judge4");
        judge5Screen.setUserName("judge5");

        Price tradeOne = Price.dollarPrice("20");
        int round;

        assertEquals("disabled", judge4Screen.disabledFlag(0));
        assertEquals("disabled", judge5Screen.disabledFlag(0));

        // first round
        session.startSession();
        round = 1;
        limitOrder(market, yes, tradeOne, 1, trader1.getUser());
        limitOrder(market, no, tradeOne.inverted(), 1, manipulator3.getUser());
        assertEquals("", judge4Screen.disabledFlag(round));
        assertEquals("", judge5Screen.disabledFlag(round));
        setEstimate(judge4, 1, estimateOne);
        assertEquals("", judge4.getWarningsHtml());
        Thread.sleep(800);
        setEstimate(judge5, 1, estimateTwo);
        assertEquals("", judge4.getWarningsHtml());
        Thread.sleep(1200);
        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));

        session.endTrading(false);
        assertEquals("disabled", judge4Screen.disabledFlag(round));

        setEstimate(judge4, 1, estimateOne);
        assertRENoMatch("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();

        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));

        ((JudgingSession)session).endScoringPhase();

        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));
        assertRENoMatch("", judge4.getWarningsHtml());
        assertRENoMatch("", judge5.getWarningsHtml());

        // second round
        session.startSession();
        round = 2;
        limitOrder(market, yes, tradeOne, 1, trader1.getUser());
        limitOrder(market, no, tradeOne.inverted(), 1, manipulator3.getUser());
        assertEquals("", judge4Screen.disabledFlag(round));
        assertEquals("", judge4.getWarningsHtml());
        assertEquals("", judge5Screen.disabledFlag(round));
        assertEquals("", judge5.getWarningsHtml());
        setEstimate(judge4, 2, estimateOne);
        assertEquals("", judge4Screen.disabledFlag(round));
        assertEquals("", judge4.getWarningsHtml());

        Thread.sleep(800);
        setEstimate(judge5, 2, estimateTwo);
        assertEquals("", judge5Screen.disabledFlag(round));
        assertEquals("", judge5.getWarningsHtml());
        Thread.sleep(1200);

        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));

        session.endTrading(false);
        assertEquals("disabled", judge4Screen.disabledFlag(round));

        setEstimate(judge4, 2, estimateOne);
        assertRENoMatch("", judge4.getWarningsHtml());
        ((JudgingSession)session).endScoringPhase();

        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));

        ((JudgingSession)session).endScoringPhase();

        assertEquals("disabled", judge4Screen.disabledFlag(round));
        assertEquals("disabled", judge5Screen.disabledFlag(round));
        assertRENoMatch("", judge4.getWarningsHtml());
        assertRENoMatch("", judge5.getWarningsHtml());
    }
}
