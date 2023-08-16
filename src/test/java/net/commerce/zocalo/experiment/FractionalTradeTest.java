package net.commerce.zocalo.experiment;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

import java.io.IOException;

public class FractionalTradeTest extends SessionTestCase {
    private BinaryMarket market;
    private Position yes;
    private Position no;

    public void testFractionalShares() throws DuplicateOrderException, ScoreException, IOException {
        final String FRACTIONAL_TRADING_SCRIPT = "sessionTitle: FractionalShares\n" +
                "rounds: 15\n" +
                "players: traderAA, traderBB, traderCB\n" +
                "roles: trader1, trader2, trader3\n" +
                "timeLimit: 5\n" +
                "traderAA.role: trader1\n" +
                "traderBB.role: trader2\n" +
                "traderCB.role: trader3\n" +
                "\n" +
                "carryForward: all\n" +
                "useUnaryAssets: false\n" +
                "showEarnings: true\n" +
                "WholeShareTradingOnly: true\n" +
                "maxPrice: 900\n" +
                "\n" +
                "endowment.trader1: 180\n" +
                "endowment.trader2: 540\n" +
                "endowment.trader3: 900\n" +
                "tickets.trader1: 4\n" +
                "tickets.trader2: 3\n" +
                "tickets.trader3: 2\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice, noMessage\n" +
                "actualValue:          8, 60, 0, 28, 8, 60, 0, 60, 28, 28, 8, 60, 0, 8, 8\n" +
                "\n" +
                "traderAA.hint:         not100,     not40\n" +
                "traderBB.hint:         not40,      notZero\n" +
                "traderCB.hint:    not100,\tnotZero\n" +
                "traderCB.earningsHint:    worth40,\tworth40\n" +
                "traderCB.target:  40,         40\n" +
                "worth100: Your score will improve if the judge thinks the tickets are worth 100";
        setupSession(FRACTIONAL_TRADING_SCRIPT);

        TradingSubject traderAA = (TradingSubject) session.getPlayer("traderAA");
        TradingSubject traderBB = (TradingSubject) session.getPlayer("traderBB");
        TradingSubject traderCB = (TradingSubject) session.getPlayer("traderCB");

        market = session.getMarket();
        yes = ((BinaryClaim)market.getClaim()).getYesPosition();
        no = ((BinaryClaim)market.getClaim()).getNoPosition();
        Price maxPrice = market.maxPrice();

        session.startSession(0);

        buy(100, traderCB, maxPrice);
        buy(359, traderBB, maxPrice);
        sell(359, traderAA, maxPrice);
        sell(200, traderCB, maxPrice);
        buy(200, traderAA, maxPrice);
        sell(200, traderAA, maxPrice);
        buy(200, traderCB, maxPrice);
        buy(120, traderAA, maxPrice);
        removeBuyOrder(traderAA, 120, maxPrice);

        session.endTrading(false);
        assertQEquals(3, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(4, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(2, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(180.0 + 359 - 200 + 200 + (3 * 8), traderAA.getUser().cashOnHand());
        assertQEquals(540.0 - 359 + (4 * 8), traderBB.getUser().cashOnHand());
        assertQEquals(900.0 + 200 - 200 + (2 * 8), traderCB.getUser().cashOnHand());

        session.startNextRound(0);
        sell(500, traderBB, maxPrice);
        sell(450, traderBB, maxPrice);
        sell(440, traderBB, maxPrice);
        sell(410, traderBB, maxPrice);
        buy(410, traderCB, maxPrice);
        buy(440, traderAA, maxPrice);
        session.endTrading(false);
        assertQEquals(4, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(2, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(563 - 440 + (60 * 4), traderAA.getUser().cashOnHand());
        assertQEquals(213.0 + 410 + 440 + (60 * 2), traderBB.getUser().cashOnHand());
        assertQEquals(916 - 410 + (60 * 3), traderCB.getUser().cashOnHand());
        session.startNextRound(0);

        sell(450, traderBB, maxPrice);   // a
        sell(430, traderBB, maxPrice);   // b
        buy(180, traderBB, maxPrice);    // c
        sell(380, traderCB, maxPrice);   // d
        sell(180, traderAA, maxPrice); // =c
        buy(300, traderBB, maxPrice);    // e
        sell(300, traderAA, maxPrice); // =e
        buy(380, traderBB, maxPrice); // =d
        sell(400, traderCB, maxPrice);  //f
        buy(200, traderCB, maxPrice);    //g
        sell(200, traderAA, maxPrice); //=g
        buy(250, traderCB, maxPrice);    //h
        sell(250, traderAA, maxPrice); //=h
        buy(299, traderCB, maxPrice);    //i

        assertQEquals(4 - 4, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(2 + 3, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3 + 1, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(363.0 + 180 + 300 + 200 + 250, traderAA.getUser().cashOnHand());
        assertQEquals(1183.0 - 180 - 300 - 380, traderBB.getUser().cashOnHand());
        assertQEquals(686.0 + 380 - 200 - 250, traderCB.getUser().cashOnHand());

        sell(299, traderBB, maxPrice);  //=i
        buy(400, traderAA, maxPrice); //=f
        buy(288, traderCB, maxPrice);   //k
        buy(430, traderAA, maxPrice); //=b
        buy(450, traderAA, maxPrice); //=a
        sell(288, traderAA, maxPrice); //=k
        sell(800, traderCB, maxPrice); //l
        sell(700, traderCB, maxPrice);  //m
        sell(400, traderCB, maxPrice);  //n
        buy(400, traderBB, maxPrice); //=n
        buy(200, traderCB, maxPrice);   //o
        buy(301, traderCB, maxPrice);   //p
        sell(301, traderAA, maxPrice); //=p
        sell(200, traderAA, maxPrice); //=o
        sell(400, traderCB, maxPrice); //q
        buy(200, traderCB, maxPrice);    //r
        sell(250, traderBB, maxPrice);   //s
        buy(250, traderAA, maxPrice);  //=s
        buy(400, traderBB, maxPrice);   //=q

        assertQEquals(1293.0 - 400 - 430 - 450 + 288 + 301 + 200 - 250, traderAA.getUser().cashOnHand());
        assertQEquals(323.0 + 299 - 400 + 430 + 450 + 250 - 400,  traderBB.getUser().cashOnHand());
        assertQEquals(616.0 - 299 + 400 + 400 - 288 - 301 - 200 + 400, traderCB.getUser().cashOnHand());

        session.endTrading(false);
        assertQEquals(0 + 1, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(5 - 2, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(4 + 1, traderCB.currentCouponCount(market.getBinaryClaim()));

        session.startNextRound(0);
        buy(200, traderBB, maxPrice);   // a
        buy(215, traderBB, maxPrice);
        buy(216, traderBB, maxPrice);   // b
        buy(301, traderBB, maxPrice);   // c
        sell(400, traderCB, maxPrice);
        sell(301, traderCB, maxPrice); //=c
        sell(216, traderAA, maxPrice); //=b
        sell(300, traderCB, maxPrice);   // d
        buy(240, traderAA, maxPrice);    // e
        sell(299, traderCB, maxPrice);
        sell(298, traderBB, maxPrice);
        sell(288, traderCB, maxPrice);
        sell(285, traderBB, maxPrice);
        sell(277, traderCB, maxPrice);
        sell(240, traderBB, maxPrice); // =e

        session.endTrading(false);
        assertQEquals(1, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(3 + 1, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(5 - 1, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(552.0 + 216 - 240 + (28 * 1), traderAA.getUser().cashOnHand());
        assertQEquals(952.0 - 301 - 216 + 240 + (28 * 4), traderBB.getUser().cashOnHand());
        assertQEquals(728.0 + 301 + (28 * 4), traderCB.getUser().cashOnHand());

        session.startNextRound(0);
        sell(250, traderCB, maxPrice);  // a
        buy(100, traderCB, maxPrice);
        buy(250, traderBB, maxPrice);   // =a
        buy(101, traderCB, maxPrice);
        buy(102, traderCB, maxPrice);
        buy(103, traderCB, maxPrice);
        buy(104, traderCB, maxPrice);
        buy(105, traderCB, maxPrice);
        buy(106, traderCB, maxPrice);
        buy(107, traderCB, maxPrice);
        sell(271, traderBB, maxPrice);    // c
        buy(108, traderCB, maxPrice);
        sell(270, traderBB, maxPrice);    // d
        buy(109, traderCB, maxPrice);
        buy(270, traderAA, maxPrice); //=d
        buy(110, traderCB, maxPrice);     // e
        buy(271, traderAA, maxPrice); //=c
        buy(111, traderCB, maxPrice);     // g
        buy(115, traderCB, maxPrice);     // h
        sell(115, traderAA, maxPrice);  //=h
        sell(111, traderAA, maxPrice);  //=g
        sell(110, traderAA, maxPrice);  //=e
        buy(150, traderBB, maxPrice);
        buy(151, traderBB, maxPrice);
        buy(152, traderBB, maxPrice);   // f
        sell(152, traderCB, maxPrice); //=f
        buy(153, traderBB, maxPrice);
        buy(154, traderBB, maxPrice);
        sell(400, traderCB, maxPrice);
        buy(155, traderBB, maxPrice);
        buy(156, traderBB, maxPrice);    // g
        sell(250, traderBB, maxPrice);   // i
        sell(249, traderBB, maxPrice);   // j
        buy(157, traderCB, maxPrice);    // k
        sell(248, traderBB, maxPrice);   // l
        sell(157, traderBB, maxPrice); //=k
        buy(248, traderCB, maxPrice);  //=l
        sell(156, traderCB, maxPrice); //=g
        buy(249, traderCB, maxPrice);  //=j
        buy(250, traderCB, maxPrice);  //=i

        session.endTrading(false);
        assertQEquals(1 - 1, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(4 - 3, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(4 + 4, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(556.0 - 270 + 110 + 111 - 271 + 115 + (8 * 0), traderAA.getUser().cashOnHand());
        assertQEquals(787.0 - 250 + 270 + 271 + 157 - 152 + 248 - 156 + 249 + 250 + (8 * 1), traderBB.getUser().cashOnHand());
        assertQEquals(1141.0 + 250 - 110 - 111 - 115 - 157 + 152 - 248 + 156 - 249 - 250 + (8 * 8), traderCB.getUser().cashOnHand());

        session.startNextRound(0);
        buy(188, traderCB, maxPrice);     // a
        buy(211, traderCB, maxPrice);     // b
        sell(211, traderBB, maxPrice); //=b
        sell(210, traderBB, maxPrice);    // c

        assertQEquals(0, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(9, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(351.0, traderAA.getUser().cashOnHand());
        assertQEquals(1682.0 + 211, traderBB.getUser().cashOnHand());
        assertQEquals(523.0 - 211, traderCB.getUser().cashOnHand());

        buy(210, traderAA, maxPrice); //=c
        sell(188, traderAA, maxPrice); //=a
        buy(120, traderBB, maxPrice);     // d

        assertQEquals(0, traderAA.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(0 - 1, traderBB.currentCouponCount(market.getBinaryClaim()));
        assertQEquals(9 + 1, traderCB.currentCouponCount(market.getBinaryClaim()));

        assertQEquals(351.0 - 210 + 188, traderAA.getUser().cashOnHand());
        assertQEquals(1893.0 - (900 - 210), traderBB.getUser().cashOnHand());
        assertQEquals(312.0 - 188, traderCB.getUser().cashOnHand());

        sell(120, traderAA, maxPrice); //=d
        buy(130, traderBB, maxPrice);

        session.endTrading(false);
    }

    public void testOrderExceedsUnaryLimit() throws DuplicateOrderException, ScoreException, IOException {
        final String UNARY_LIMIT_SCRIPT = "sessionTitle: UnaryLimit\n" +
                "rounds: 15\n" +
                "players: traderAE, traderBB, traderCB\n" +
                "roles: trader1, trader2, trader3\n" +
                "timeLimit: 5\n" +
                "traderAE.role: trader1\n" +
                "traderBB.role: trader2\n" +
                "traderCB.role: trader3\n" +
                "\n" +
                "carryForward: all\n" +
                "showEarnings: true\n" +
                "WholeShareTradingOnly: true\n" +
                "maxPrice: 900\n" +
                "\n" +
                "endowment.trader1: 180\n" +
                "endowment.trader2: 250\n" +
                "endowment.trader3: 900\n" +
                "tickets.trader1: 2\n" +
                "tickets.trader2: 3\n" +
                "tickets.trader3: 2\n" +
                "\n" +
                "commonMessage:        raisePrice, changePrice, noMessage\n" +
                "actualValue:          8, 60, 0, 28, 8, 60, 0, 60, 28, 28, 8, 60, 0, 8, 8\n" +
                "\n" +
                "traderAE.hint:         not100,     not40\n" +
                "traderBB.hint:         not40,      notZero\n" +
                "traderCB.hint:    not100,\tnotZero\n" +
                "traderCB.earningsHint:    worth40,\tworth40\n" +
                "traderCB.target:  40,         40\n" +
                "worth100: Your score will improve if the judge thinks the tickets are worth 100";
        setupSession(UNARY_LIMIT_SCRIPT);

        TradingSubject traderAE = (TradingSubject) session.getPlayer("traderAE");
        TradingSubject traderBB = (TradingSubject) session.getPlayer("traderBB");

        market = session.getMarket();
        yes = ((BinaryClaim)market.getClaim()).getYesPosition();
        no = ((BinaryClaim)market.getClaim()).getNoPosition();
        Price maxPrice = market.maxPrice();

        session.startSession(0);
        assertQEquals(3, traderBB.currentCouponCount((BinaryClaim) market.getClaim()));

        buy(150, traderBB, maxPrice);
        assertQEquals(150.0, traderBB.getUser().outstandingOrderCost(yes));  // TODO SCALING
        sell(240, traderAE, maxPrice);
        buy(240, traderBB, maxPrice);

        assertQEquals(150, traderBB.getUser().outstandingOrderCost(yes));   // TODO SCALING?
        assertQEquals(3, traderBB.currentCouponCount((BinaryClaim) market.getClaim()));
    }

    private void removeBuyOrder(TradingSubject trader, double price, Price max) {
        market.getBook().removeOrder(trader.getName(), new Price(price, max), yes);
    }

    private void sell(double price, TradingSubject trader, Price max) throws DuplicateOrderException {
        market.limitOrder(no, new Price((900 - price), max), Quantity.ONE, trader.getUser());
    }

    private void buy(double price, TradingSubject trader, Price max) throws DuplicateOrderException {
        market.limitOrder(yes, new Price(price, max), Quantity.ONE, trader.getUser());
    }
}
