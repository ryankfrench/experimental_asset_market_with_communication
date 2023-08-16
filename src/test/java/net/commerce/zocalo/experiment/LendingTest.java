package net.commerce.zocalo.experiment;

// Copyright 2009, 2010 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.BasicTestHelper;
import net.commerce.zocalo.JspSupport.MockHttpServletRequest;
import net.commerce.zocalo.JspSupport.TraderScreen;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.role.Borrower;
import net.commerce.zocalo.experiment.role.Trader;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.service.PropertyKeywords;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.util.*;

import static net.commerce.zocalo.experiment.role.Borrower.*;

public class LendingTest extends BasicTestHelper {
    private final String script =
            "sessionTitle: LendingTestCase\n" +
            "rounds: 4\n" +
            "players: traderA, traderB, traderC\n" +
            "timeLimit: 5\n" +
            "traderA.role: trader\n" +
            "traderB.role: trader\n" +
            "traderC.role: trader\n" +
            "initialHint: Trading has not started yet.\n" +
            "markToMarket.loan.ratio: .25\n" +
            "\n" +
            "traderA.shareHoldingLimit: 25\n" +
            "traderB.shareHoldingLimit: 33\n" +
            "\n" +
            "carryForward: true\n" +
            "useUnaryAssets: true\n" +
            "endowment.trader: 100\n" +
            "tickets.trader: 30\n" +
            "\n" +
            "# These values are specified by round.\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          0,          100,         40\n" +
            "\n" +
            "# These values are specified by Player and Round.\n" +
            "\n" +
            "traderA.hint:         not100,     not40,       not100\n" +
            "traderB.hint:         not40,      notZero,     notZero\n" +
            "traderC.hint:         not100,     notZero,     notZero\n" +
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
            "tickets are worth 40\n" +
            "\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";

    protected Properties props;
    protected LendingSession session;
    protected Borrower traderA;
    protected Borrower traderB;
    private Market market;
    private BinaryClaim claim;
    private Position yes;
    private Position no;
    private final Quantity oneQuarter = new Quantity(".25");
    static private Price P1000 = new Price(q(1000));

    protected void setUp() throws Exception {
        super.setUp();
        Log4JHelper.getInstance();
        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        HibernateTestUtil.resetSessionFactory();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        SessionSingleton.resetSession();
    }

    public void testBasics() {
        assertEquals(4, session.rounds());
        assertEquals("The ticket value is not 100.", session.getPriceHint("traderA", 1));
        assertEquals("The ticket value is not 40.", session.getPriceHint("traderB", 1));
        assertEquals("Some players are trying to change the apparent price", session.message(2));
        assertEquals(5 * 60, session.timeLimit());
        assertQEquals(30.0, session.initialTraderTickets());
        assertTrue("traderA should be a trader", traderA instanceof Trader);
        assertEquals(0, session.getCurrentRound());
        assertEquals("Trading has not started yet.", traderA.getHint());
        assertTrue("Market should be inactive to start", ! session.marketIsActive());
        assertQEquals(.25, session.lendingRatio());

        assertEquals(25, session.getShareLimit(traderA));
        assertEquals(33, session.getShareLimit(traderB));
        assertEquals(0, session.getShareLimit((TradingSubject) session.getPlayer("traderC")));
    }

    public void testStateTransitions() throws DuplicateOrderException {
        assertEquals(0, session.getCurrentRound());

        session.startSession();
        limitOrder(market, yes, "20", 1, traderA.getUser());
        assertQEquals(1, market.getBook().bestQuantity(yes));
        session.endTrading(false);
        assertFalse(session.marketIsActive());
        session.startNextRound();
    }

    public void testRatios() throws DuplicateOrderException {
        assertEquals(0, session.getCurrentRound());

        assertFalse(session.marketIsActive());
        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));

        int quant = 2;
        limitOrder(market, yes, "40", quant, traderA.getUser());
        assertQEquals(quant, market.getBook().bestQuantity(yes));
        marketOrder(market, no, "60", quant, traderB.getUser());
        assertQEquals(0, market.getBook().bestQuantity(yes));
        assertQEquals(.25 * (40 * (30 + quant)), session.availableLoan(traderA));
        assertQEquals(.25 * (40 * (30 - quant)), session.availableLoan(traderB));
        session.endTrading(false);

        assertQEquals(.25 * (40 * (30 + quant)), session.availableLoan(traderA));
        assertQEquals(.25 * (40 * (30 - quant)), session.availableLoan(traderB));
    }

    public void testDisplayingScores() throws DuplicateOrderException {
        assertEquals(0, session.getCurrentRound());
        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));

        int quant = 2;
        limitOrder(market, yes, "40", quant, traderA.getUser());
        assertQEquals(quant, market.getBook().bestQuantity(yes));
        marketOrder(market, no, "60", quant, traderB.getUser());
        assertQEquals(0, market.getBook().bestQuantity(yes));
        assertQEquals(.25 * (40 * (30 + quant)), session.availableLoan(traderA));
        assertQEquals(.25 * (40 * (30 - quant)), session.availableLoan(traderB));
        session.endTrading(false);
        assertQEquals(.25 * (40 * (30 + quant)), session.availableLoan(traderA));
        assertQEquals(.25 * (40 * (30 - quant)), session.availableLoan(traderB));
        TraderScreen screen = new TraderScreen();
        screen.setUserName(traderA.getName());
        String earningsReport = screen.showEarningsSummary();
        assertREMatches(".*Loans Outstanding.*", earningsReport);
        assertREMatches(".*Net Earnings.*", earningsReport);
    }

    public void testSuppressAccountingDetail() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "suppressAccountingDetails: true\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());

        session.endTrading(false);
        TraderScreen screen = new TraderScreen();
        screen.setUserName(traderA.getName());
        String earningsReport = screen.showEarningsSummary();

        assertRENoMatch(".*Net Earnings.*", earningsReport);
        assertMatches(PropertyKeywords.DEFAULT_DIV_ADDED_TO_CASH_LABEL, earningsReport);
    }

    public void testBorrower() {
        Borrower b = (Borrower) session.getPlayer("traderB");
        assertQEquals(0, b.loanAmount());
        session.increaseLending(b, q(2000));
        assertQEquals(2000, b.loanAmount());
        b.decreaseLending(q(1000));
        assertQEquals(1000, b.loanAmount());
        b.decreaseLending(q(2000));
        assertQEquals(1000, b.loanAmount());
        b.decreaseLending(q(1000));
        assertQEquals(0, b.loanAmount());
    }

    public void testAcceptingLoanChanges() throws DuplicateOrderException {
        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));

        int quant = 2;
        limitOrder(market, yes, "40", quant, traderA.getUser());
        assertQEquals(quant, market.getBook().bestQuantity(yes));
        marketOrder(market, no, "60", quant, traderB.getUser());
        assertQEquals(0, market.getBook().bestQuantity(yes));
        assertQEquals(0, traderA.loanAmount());
        session.endTrading(false);

        assertQEquals(.25 * (40 * (30 + quant)), session.availableLoan(traderA));
        assertQEquals(.25 * (40 * (30 - quant)), session.availableLoan(traderB));
        assertQEquals(320, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(320, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderA.getScoreComponent(OutstandingLoanComponent));

        TraderScreen pageA = new TraderScreen();
        pageA.setUserName(traderA.getName());
        pageA.setClaimName(market.getClaim().getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        String earnings = pageA.showEarningsSummary();
        assertREMatches(".*<input.*", earnings);

        request.getParameterMap().put("action", "Accept");
        pageA.processRequest(request, null);
        assertTrue(traderA.acceptedLoanMod(1));

        TraderScreen pageB = new TraderScreen();
        pageB.setUserName(traderB.getName());
        pageB.setClaimName(market.getClaim().getName());
        pageB.showEarningsSummary();
        assertFalse(traderB.acceptedLoanMod(1));

        assertQEquals(320, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(320, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(320, traderA.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(280, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(280, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderB.getScoreComponent(OutstandingLoanComponent));

        earnings = pageA.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        assertQEquals(320, traderA.loanAmount());
        assertQEquals(320, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(320, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(320, traderA.getScoreComponent(OutstandingLoanComponent));
    }

    public void testLoanReduction() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: LendingTestCase\n" +
                "rounds: 5\n" +
                "loansDueInRound: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "\n" +
                "traderA.shareHoldingLimit: 8\n" +
                "traderB.shareHoldingLimit: 4\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 5\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:        changePrice, raisePrice, noMessage\n" +
                "actualValue:          20,           0,          30\n" +
                "\n" +
                "traderA.hint:         notZero      not40,       not100\n" +
                "traderB.hint:         not40,       not100,      notZero\n" +
                "traderC.hint:         not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        Quantity initialTickets = q(5);
        Quantity traderALimit = q(8);
        Quantity traderBLimit = q(4);

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");
        Tally aBalance = new Tally(q(200));
        Tally bBalance = new Tally(q(200));
        Tally cBalance = new Tally(q(200));
        Quantity q40 = new Quantity("40");
        Quantity aCoupons = initialTickets;
        Quantity bCoupons = initialTickets;
        Quantity cCoupons = initialTickets;

///////// ROUND 1 ///////////////////////////////   traderA buys high; price is high

        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));

        int quant = 1;
        Quantity trades = q(3);
        for (int i = 0 ; i < trades.asValue().intValue() ; i ++) {
            limitOrder(market, yes, "40", quant, traderA.getUser());
            marketOrder(market, no, "60", quant, traderB.getUser());
        }
        aBalance.sub(q40.times(trades));
        bBalance.add(q40.times(trades));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        aCoupons = aCoupons.plus(trades);
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        bCoupons = bCoupons.minus(trades);
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());

        limitOrder(market, yes, "80", quant, traderA.getUser());
        marketOrder(market, no, "20", quant, traderB.getUser());
        aBalance.sub("80");
        bBalance.add("80");
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        aCoupons = aCoupons.plus(q(1));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        bCoupons = bCoupons.minus(q(1));

        assertQEquals(0, traderA.loanAmount());

        session.endTrading(false);

        Quantity q20 = Price.dollarPrice(20);
        aBalance.add(aCoupons.min(traderALimit).times(q20));
        bBalance.add(initialTickets.minus(trades).minus(Quantity.ONE).times(q20));
        cBalance.add(initialTickets.times(q20));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        Price p80 = Price.dollarPrice(80);
        Quantity availA1 = oneQuarter.times(p80.times(initialTickets.plus(trades.plus(Quantity.ONE)))).roundFloor();
        Quantity availB1 = loanAvailable(bCoupons, p80);
        Quantity availC1 = oneQuarter.times(p80.times(initialTickets)).roundFloor();
        assertQEquals(availA1, session.availableLoan(traderA));
        assertQEquals(availB1, session.availableLoan(traderB));
        assertQEquals(availC1, session.availableLoan(traderC));
        assertQEquals(oneQuarter.times(q40.times(initialTickets.minus(trades))).roundFloor(), session.availableLoan(traderB));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(0, traderA.getScoreComponent(OutstandingLoanComponent));

        TraderScreen page = new TraderScreen();
        page.setUserName(traderA.getName());              // traderA accepts loan
        page.setClaimName(market.getClaim().getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        String earnings = page.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        assertQEquals(availA1, traderA.loanAmount());
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        aBalance.add(availA1);
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());

///////// ROUND 2 ///////////////////////////////   traderA buys at 40; price is low
        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());
        assertEquals(2, session.getCurrentRound());

        int round2Trades = 4;
        Quantity round2TradesQ = q(round2Trades);
        for (int i = 0 ; i < round2Trades ; i ++) {
            limitOrder(market, yes, "40", quant, traderA.getUser());
            marketOrder(market, no, "60", quant, traderC.getUser());
        }

        aBalance.sub(q40.times(round2TradesQ));
        cBalance.add(q40.times(round2TradesQ));
        aCoupons = aCoupons.plus(round2TradesQ);
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        cCoupons = cCoupons.minus(round2TradesQ);

        Price p10 = Price.dollarPrice(10);
        limitOrder(market, yes, p10, quant, traderA.getUser());
        marketOrder(market, no, p10.inverted(), quant, traderC.getUser());
        aBalance.sub(p10);
        cBalance.add(p10);
        aCoupons = aCoupons.plus(Quantity.ONE);
        cCoupons = cCoupons.minus(Quantity.ONE);
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        Quantity availA2 = loanAvailable(aCoupons, p10);
        Quantity availB2 = loanAvailable(bCoupons, p10);
        Quantity availC2 = loanAvailable(cCoupons, p10);

        session.endTrading(false);

        aBalance.sub(availA1.minus(availA2));  // traderA's loan balance reduced; no action required

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance()); // TraderC's available didn't change; didn't accept

        assertREMatches(".*Loan Amount Reduced.*", page.showEarningsSummary());
        assertQEquals(availA2, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA2.minus(availA1), traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availC2, traderC.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA2, traderA.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderC.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(availC2, traderC.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB2, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB2, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(0, traderB.getScoreComponent(OutstandingLoanComponent));

        assertQEquals(availB2, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB2, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(0, traderB.getScoreComponent(OutstandingLoanComponent));

        page.setUserName(traderB.getName());              // traderB accepts loan
        page.processRequest(request, null);

        bBalance.add(availB2);
        assertQEquals(availB2, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB2, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(availB2, traderB.getScoreComponent(OutstandingLoanComponent));

///////// ROUND 3 ///////////////////////////////   traderA sells to traderB at 60
        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());

        assertEquals(3, session.getCurrentRound());
        Price p60 = Price.dollarPrice(60);
        limitOrder(market, yes, p60, quant, traderB.getUser());
        marketOrder(market, no, p60.inverted(), quant, traderA.getUser());
        aBalance.add(p60);
        bBalance.sub(p60);
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        bCoupons = bCoupons.plus(Quantity.ONE);
        aCoupons = aCoupons.minus(Quantity.ONE);

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        assertQEquals(bCoupons, traderB.currentCouponCount(claim));
        assertQEquals(cCoupons, traderC.currentCouponCount(claim));

        session.endTrading(false);

        Quantity d30 = q(30);
        aBalance.add(traderALimit.min(aCoupons).times(d30));
        bBalance.add(traderBLimit.min(bCoupons).times(d30));
        cBalance.add(cCoupons.times(d30));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        Quantity availA3 = loanAvailable(aCoupons, p60);
        Quantity availB3 = loanAvailable(bCoupons, p60);
        Quantity availC3 = loanAvailable(cCoupons, p60);

        assertQEquals(availA3, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB3, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availC3, traderC.getScoreComponent(AvailableLoanComponent));

///////// ROUND 4 ///////////////////////////////  Loans called end of this round
        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());

        Price p30 = Price.dollarPrice(30);
        market.limitOrder(yes, p30, q(quant), traderB.getUser());
        market.marketOrder(no, p30.inverted(), q(quant), traderA.getUser());
        aBalance.add(p30);
        bBalance.sub(p30);
        bCoupons = bCoupons.plus(Quantity.ONE);
        aCoupons = aCoupons.minus(Quantity.ONE);
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());

        session.endTrading(false);

        assertQEquals(0, traderA.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderB.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderC.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderC.getScoreComponent(AvailableLoanComponent));

///////// ROUND 5 ///////////////////////////////  Loans called end of this round
        session.startNextRound();
        session.endTrading(false);

        assertQEquals(0, traderA.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderB.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderC.getScoreComponent(OutstandingLoanComponent));
        assertQEquals(0, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(0, traderC.getScoreComponent(AvailableLoanComponent));
    }

    private Quantity loanAvailable(Quantity coupons, Price lastTrade) {
        Quantity available = oneQuarter.times(lastTrade.times(coupons));
        return available.roundFloor().max(Quantity.ZERO);
    }

    public void testCapitalGain() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        // test no trades for a round
        session.endTrading(false);        
        // check gains here for round 1
        assertQEquals(0, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 1
        assertQEquals(0, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderC.getScoreComponent(NetEarningsComponent));

        ///////////////////////////// Round 2
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        marketOrder(market, no, "50", quant, traderB.getUser());
        limitOrder(market, no, "40", quant, traderA.getUser());
        marketOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, no, "35", quant, traderA.getUser());
        marketOrder(market, yes, "65", quant, traderB.getUser());
        session.endTrading(false);
        // check gains here for round 2
        assertQEquals(60, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(10, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(65, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 2
        assertQEquals(100, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(50, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(165, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(280, traderA.getScoreComponent(BalanceComponent));
        assertQEquals(40, traderA.getScoreComponent(TotalDividendComponent));

        ///////////////////////////// Round 3
        session.startNextRound();
        limitOrder(market, yes, "70", quant, traderB.getUser());
        marketOrder(market, no, "30", quant, traderA.getUser());
        limitOrder(market, yes, "80", quant, traderA.getUser());
        marketOrder(market, no, "20", quant, traderC.getUser());
        limitOrder(market, yes, "75", quant, traderB.getUser());
        marketOrder(market, no, "25", quant, traderC.getUser());
        session.endTrading(false);
        // check gains here for round 3
        assertQEquals(10, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(25, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(55, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 3
        assertQEquals(10, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(25, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(55, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(125, traderB.getScoreComponent(BalanceComponent));
        assertQEquals(0, traderB.getScoreComponent(TotalDividendComponent));

        ///////////////////////////// Round 4
        session.startNextRound();
        limitOrder(market, yes, "55", quant, traderA.getUser());
        marketOrder(market, no, "45", quant, traderB.getUser());
        limitOrder(market, yes, "45", quant, traderB.getUser());
        marketOrder(market, no, "55", quant, traderA.getUser());
        session.endTrading(false);
        // check gains for round 4
        assertQEquals(-70, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(-110, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(-90, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 4
        assertQEquals(-10, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(10, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(360, traderA.getScoreComponent(ScoreComponent));
        assertQEquals(255, traderB.getScoreComponent(ScoreComponent));
        assertQEquals(435, traderC.getScoreComponent(ScoreComponent));
    }

    public void testMarkToMarketNoLoans() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: 0\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        // test no trades for a round
        session.endTrading(false);
        // check gains here for round 1
        assertQEquals(0, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 1
        assertQEquals(0, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderC.getScoreComponent(NetEarningsComponent));

        ///////////////////////////// Round 2
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        marketOrder(market, no, "50", quant, traderB.getUser());
        limitOrder(market, no, "40", quant, traderA.getUser());
        marketOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, no, "35", quant, traderA.getUser());
        marketOrder(market, yes, "65", quant, traderB.getUser());
        session.endTrading(false);
        // check gains here for round 2
        assertQEquals(60, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(10, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(65, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 2
        assertQEquals(100, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(50, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(165, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(280, traderA.getScoreComponent(BalanceComponent));
        assertQEquals(40, traderA.getScoreComponent(TotalDividendComponent));

        ///////////////////////////// Round 3
        session.startNextRound();
        limitOrder(market, yes, "70", quant, traderB.getUser());
        marketOrder(market, no, "30", quant, traderA.getUser());
        limitOrder(market, yes, "80", quant, traderA.getUser());
        marketOrder(market, no, "20", quant, traderC.getUser());
        limitOrder(market, yes, "75", quant, traderB.getUser());
        marketOrder(market, no, "25", quant, traderC.getUser());
        session.endTrading(false);
        // check gains here for round 3
        assertQEquals(10, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(25, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(55, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 3
        assertQEquals(10, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(25, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(55, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(125, traderB.getScoreComponent(BalanceComponent));
        assertQEquals(0, traderB.getScoreComponent(TotalDividendComponent));

        ///////////////////////////// Round 4
        session.startNextRound();
        limitOrder(market, yes, "55", quant, traderA.getUser());
        marketOrder(market, no, "45", quant, traderB.getUser());
        limitOrder(market, yes, "45", quant, traderB.getUser());
        marketOrder(market, no, "55", quant, traderA.getUser());
        session.endTrading(false);
        // check gains for round 4
        assertQEquals(-70, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(-110, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(-90, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 4
        assertQEquals(-10, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(10, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(360, traderA.getScoreComponent(ScoreComponent));
        assertQEquals(255, traderB.getScoreComponent(ScoreComponent));
        assertQEquals(435, traderC.getScoreComponent(ScoreComponent));
    }

    public void testFirstRoundCapitalGain() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        marketOrder(market, no, "50", quant, traderB.getUser());
        limitOrder(market, no, "40", quant, traderA.getUser());
        marketOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, no, "35", quant, traderA.getUser());
        marketOrder(market, yes, "65", quant, traderB.getUser());
        session.endTrading(false);
        // check gains here for round 1
        assertQEquals(60, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(10, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(65, traderC.getScoreComponent(CapitalGainsComponent));
    }

    public void testFirstRoundFundamentalValueGain() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "capitalGains: fundamentalValue\n" +
                "fundamentalValue: 50, 30, 30, 0\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        marketOrder(market, no, "50", quant, traderB.getUser());
        limitOrder(market, no, "40", quant, traderA.getUser());
        marketOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, no, "35", quant, traderA.getUser());
        marketOrder(market, yes, "65", quant, traderB.getUser());
        session.endTrading(false);
        // check gains here for round 1
        assertQEquals(2 * 50, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(2 * 50, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(5 * 50, traderC.getScoreComponent(CapitalGainsComponent));
    }

    public void testHistoricCostGain() throws DuplicateOrderException, IOException {
        String script =
                "sessionTitle: CapitalGainTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "capitalGains: historicCost\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 200\n" +
                "tickets.trader: 3\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:    noMessage,    changePrice, raisePrice, noMessage\n" +
                "actualValue:         0,         20,           0,          30\n" +
                "\n" +
                "traderA.hint:     noMessage,    notZero      not40,       not100\n" +
                "traderB.hint:     noMessage,    not40,       not100,      notZero\n" +
                "traderC.hint:     noMessage,    not40,       not100,      notZero\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");

        ///////////////////////////// Round 1
        session.startNextRound();
        // test no trades for a round
        session.endTrading(false);
        // check gains here for round 1
        assertQEquals(0, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 1
        assertQEquals(0, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderC.getScoreComponent(NetEarningsComponent));

        ///////////////////////////// Round 2
        session.startNextRound();
        int quant = 1;
        limitOrder(market, yes, "45", quant, traderA.getUser());
        marketOrder(market, no, "55", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        marketOrder(market, no, "50", quant, traderB.getUser());
        limitOrder(market, no, "40", quant, traderA.getUser());
        marketOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, no, "35", quant, traderA.getUser());
        marketOrder(market, yes, "65", quant, traderB.getUser());
        session.endTrading(false);
        // check gains here for round 2
        assertQEquals(25, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(-5, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 2
        assertQEquals(65, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(35, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(100, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(280, traderA.getScoreComponent(BalanceComponent));
        assertQEquals(40, traderA.getScoreComponent(TotalDividendComponent));
        assertQEquals(95, traderA.getScoreComponent(HistoricalShareValue));
        assertQEquals(115, traderB.getScoreComponent(HistoricalShareValue));
        assertQEquals(260, traderC.getScoreComponent(HistoricalShareValue));

        ///////////////////////////// Round 3
        session.startNextRound();
        limitOrder(market, yes, "70", quant, traderB.getUser());
        marketOrder(market, no, "30", quant, traderA.getUser());
        limitOrder(market, yes, "80", quant, traderA.getUser());
        marketOrder(market, no, "20", quant, traderC.getUser());
        limitOrder(market, yes, "75", quant, traderB.getUser());
        marketOrder(market, no, "25", quant, traderC.getUser());
        session.endTrading(false);
        // check gains here for round 3
        assertQEquals(20, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(55, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 3
        assertQEquals(20, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(0, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(65, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(55, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(125, traderB.getScoreComponent(BalanceComponent));
        assertQEquals(0, traderB.getScoreComponent(TotalDividendComponent));
        assertQEquals(125, traderA.getScoreComponent(HistoricalShareValue));
        assertQEquals(260, traderB.getScoreComponent(HistoricalShareValue));
        assertQEquals(160, traderC.getScoreComponent(HistoricalShareValue));

        ///////////////////////////// Round 4
        session.startNextRound();
        limitOrder(market, yes, "55", quant, traderA.getUser());
        marketOrder(market, no, "45", quant, traderB.getUser());
        limitOrder(market, yes, "45", quant, traderB.getUser());
        marketOrder(market, no, "55", quant, traderA.getUser());
        session.endTrading(false);
        // check gains for round 4
        assertQEquals(0, traderA.getScoreComponent(CapitalGainsComponent));
        assertQEquals(5, traderB.getScoreComponent(CapitalGainsComponent));
        assertQEquals(0, traderC.getScoreComponent(CapitalGainsComponent));
        // check net earnings for round 4
        assertQEquals(60, traderA.getScoreComponent(NetEarningsComponent));
        assertQEquals(125, traderB.getScoreComponent(NetEarningsComponent));
        assertQEquals(90, traderC.getScoreComponent(NetEarningsComponent));
        assertQEquals(360, traderA.getScoreComponent(ScoreComponent));
        assertQEquals(255, traderB.getScoreComponent(ScoreComponent));
        assertQEquals(435, traderC.getScoreComponent(ScoreComponent));
        assertTrue(traderB.getScoreExplanation().substring(30).matches(".*Value of shares held</td><td align=center>0</td>.*"));
        assertTrue(traderA.getScoreExplanation().substring(30).matches(".*Value of shares held</td><td align=center>0</td>.*"));
    }

    public void testLoanDefault() throws DuplicateOrderException, IOException, InterruptedException {
        String script =
                "sessionTitle: LendingTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 500\n" +
                "tickets.trader: 8\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "# These values are specified by round.\n" +
                "\n" +
                "commonMessage:        changePrice, raisePrice, noMessage\n" +
                "actualValue:          0,           0,          0\n" +
                "\n" +
                "traderA.hint:         not100        not40,       not100\n" +
                "traderB.hint:         not40,       not100,       not40\n" +
                "traderC.hint:         not40,       not100,       not100\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        int initialTickets = 8;
        session.setPeriod(50);

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");
        Tally aBalance = new Tally(500);
        Tally bBalance = new Tally(500);
        Tally cBalance = new Tally(500);
        Quantity aCoupons = q(8);

///////// ROUND 1 ///////////////////////////////   traderA buys high; price is high
        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));

        int quant = 1;
        int trades = 6;
        for (int i = 0 ; i < trades ; i ++) {
            limitOrder(market, yes, "80", quant, traderA.getUser());
            marketOrder(market, no, "20", quant, traderB.getUser());
        }
        aBalance.sub(q(80).times(q(trades)));
        bBalance.add(q(80).times(q(trades)));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        aCoupons = aCoupons.plus(q(trades));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));


        assertQEquals(0, traderA.loanAmount());
        session.endTrading(false);

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        Quantity availA1 = oneQuarter.times(q(80).times(q(initialTickets + trades)));
        assertQEquals(availA1, session.availableLoan(traderA));
        assertQEquals(oneQuarter.times(q(80).times(q(initialTickets - trades))), session.availableLoan(traderB));

        TraderScreen page = new TraderScreen();
        page.setUserName(traderA.getName());
        page.setClaimName(market.getClaim().getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        String earnings = page.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        assertQEquals(availA1, traderA.loanAmount());
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        aBalance.add(availA1);
        assertQEquals(aBalance.val(), traderA.balance());

///////// ROUND 2 ///////////////////////////////    traderA buys high; price is low

        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());

        assertEquals(2, session.getCurrentRound());

        int round2Trades = 3;
        for (int i = 0 ; i < round2Trades ; i ++) {
            limitOrder(market, yes, "80", quant, traderA.getUser());
            marketOrder(market, no, "20", quant, traderC.getUser());
        }

        aBalance.sub(q(80).times(q(round2Trades)));
        cBalance.add(q(80).times(q(round2Trades)));
        aCoupons = aCoupons.plus(q(round2Trades));

        limitOrder(market, yes, "10", quant, traderA.getUser());
        marketOrder(market, no, "90", quant, traderC.getUser());
        aBalance.sub(q(10));
        cBalance.add(q(10));
        aCoupons = aCoupons.plus(Quantity.ONE);

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        Quantity availA2 = oneQuarter.times(aBalance.val().minus(availA1).plus(q(10).times(traderA.currentCouponCount(claim))));

        session.endTrading(false);

        assertTrue(availA2.isNegative());
        assertTrue(traderA.inDefault());
        assertFalse(traderA.canBuy(3));
        assertFalse(traderA.canSell(3));

        assertMatches("in Default", page.showEarningsSummary());

        assertQEquals(0, traderA.balance());

///////// ROUND 3 ///////////////////////////////

        Probability p90 = new Probability(".90");
        Probability p80 = new Probability(".80");
        Probability p70 = new Probability(".70");
        Probability p60 = new Probability(".60");

        session.startNextRound();

        limitOrder(market, yes, "10", quant, traderC.getUser());
        limitOrder(market, yes, "20", quant, traderC.getUser());
        limitOrder(market, yes, "30", quant, traderC.getUser());
        limitOrder(market, yes, "40", quant, traderB.getUser());
        limitOrder(market, yes, "50", quant, traderC.getUser());
        limitOrder(market, yes, "60", quant, traderC.getUser());
        limitOrder(market, yes, "70", quant, traderB.getUser());
        limitOrder(market, yes, "80", quant, traderC.getUser());
        limitOrder(market, yes, "90", quant, traderC.getUser());
        aCoupons = aCoupons.plus(q(round2Trades));

        assertQEquals(0, traderA.balance());
        assertQEquals(230, traderA.loanAmount());
        assertTrue(traderA.inDefault());
        assertQEquals(185, traderA.getDefaultAmount());

        assertTrue(p90.compareTo(currentProbability()) >= 0);

        Thread.sleep(150);
        Probability probAfter1 = currentProbability();
        assertTrue("should have made a sale by now, p is " + probAfter1, p80.compareTo(probAfter1) >= 0);

        Thread.sleep(700);
        Probability probAfter2 = currentProbability();
        assertTrue("Should have made 2 trades by now, p is " + probAfter2, p70.compareTo(probAfter2) >= 0);

        Thread.sleep(100);
        assertQEquals(p60, currentProbability());

        assertQEquals(0, traderA.getDefaultAmount());
        assertFalse(traderA.inDefault());
        assertQEquals(55, traderA.balance());
        assertQEquals(15, traderA.currentCouponCount(claim));
        assertQEquals(45, traderA.loanAmount());

        session.endTrading(false);
        assertMatches("", page.showEarningsSummary());
    }

    public void testLoanDefaultInsufficient() throws DuplicateOrderException, IOException, InterruptedException {
        String script =
                "sessionTitle: LendingTestCase\n" +
                "rounds: 4\n" +
                "players: traderA, traderB, traderC\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader\n" +
                "traderB.role: trader\n" +
                "traderC.role: trader\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .25\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader: 500\n" +
                "tickets.trader: 8\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "commonMessage:        changePrice, raisePrice, noMessage\n" +
                "actualValue:          0,           0,          0\n" +
                "\n" +
                "traderA.hint:         not100        not40,       not100\n" +
                "traderB.hint:         not40,       not100,       not40\n" +
                "traderC.hint:         not40,       not100,       not100\n" +
                "\n" +
                "# text labels can be used in hints or commonMessage\n" +
                "\n" +
                "not100: The ticket value is not 100.\n" +
                "not40: The ticket value is not 40.\n" +
                "notZero: The ticket value is not 0.\n" +
                "\n" +
                "raisePrice: Some players are trying to raise the apparent price\n" +
                "changePrice: Some players are trying to change the apparent price\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        int initialTickets = 8;
        session.setPeriod(50);

        traderA = (Borrower) session.getPlayer("traderA");
        traderB = (Borrower) session.getPlayer("traderB");
        Borrower traderC = (Borrower) session.getPlayer("traderC");
        Tally aBalance = new Tally(500);
        Tally bBalance = new Tally(500);
        Tally cBalance = new Tally(500);
        Quantity aCoupons = q(8);

///////// ROUND 1 ///////////////////////////////   traderA buys high; price is high
        session.startNextRound();
        assertQEquals(0, session.availableLoan(traderA));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));

        int quant = 1;
        int trades = 6;
        for (int i = 0 ; i < trades ; i ++) {
            limitOrder(market, yes, "80", quant, traderA.getUser());
            marketOrder(market, no, "20", quant, traderB.getUser());
        }
        aBalance.sub(q(80).times(q(trades)));
        bBalance.add(q(80).times(q(trades)));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        aCoupons = aCoupons.plus(q(trades));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));


        assertQEquals(0, traderA.loanAmount());
        session.endTrading(false);

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        Quantity availA1 = oneQuarter.times(q(80).times(q(initialTickets + trades)));
        assertQEquals(availA1, session.availableLoan(traderA));
        assertQEquals(oneQuarter.times(q(80).times(q(initialTickets - trades))), session.availableLoan(traderB));

        TraderScreen page = new TraderScreen();
        page.setUserName(traderA.getName());
        page.setClaimName(market.getClaim().getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        String earnings = page.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        assertQEquals(availA1, traderA.loanAmount());
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        aBalance.add(availA1);
        assertQEquals(aBalance.val(), traderA.balance());

///////// ROUND 2 ///////////////////////////////    traderA buys high; price is low

        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());

        assertEquals(2, session.getCurrentRound());

        int round2Trades = 3;
        for (int i = 0 ; i < round2Trades ; i ++) {
            limitOrder(market, yes, "80", quant, traderA.getUser());
            marketOrder(market, no, "20", quant, traderC.getUser());
        }

        aBalance.sub(q(80).times(q(round2Trades)));
        cBalance.add(q(80).times(q(round2Trades)));
        aCoupons = aCoupons.plus(q(round2Trades));

        limitOrder(market, yes, "10", quant, traderA.getUser());
        marketOrder(market, no, "90", quant, traderC.getUser());
        aBalance.sub(q(10));
        cBalance.add(q(10));
        aCoupons = aCoupons.plus(Quantity.ONE);

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(cBalance.val(), traderC.balance());
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        Quantity availA2 = oneQuarter.times(aBalance.val().minus(availA1).plus(q(10).times(traderA.currentCouponCount(claim))));

        session.endTrading(false);

        assertTrue(availA2.isNegative());
        assertTrue(traderA.inDefault());
        assertFalse(traderA.canBuy(3));
        assertFalse(traderA.canSell(3));

        assertMatches("in Default", page.showEarningsSummary());

        assertQEquals(0, traderA.balance());

///////// ROUND 3 ///////////////////////////////

        Probability p90 = new Probability(".90");
        Probability p80 = new Probability(".80");
        Probability p70 = new Probability(".70");
        Probability p60 = new Probability(".60");

        session.startNextRound();

        limitOrder(market, yes, "1", quant, traderC.getUser());
        limitOrder(market, yes, "2", quant, traderC.getUser());
        limitOrder(market, yes, "3", quant, traderC.getUser());
        limitOrder(market, yes, "4", quant, traderB.getUser());
        limitOrder(market, yes, "5", quant, traderC.getUser());
        limitOrder(market, yes, "6", quant, traderC.getUser());
        limitOrder(market, yes, "7", quant, traderB.getUser());
        limitOrder(market, yes, "8", quant, traderC.getUser());
        limitOrder(market, yes, "9", quant, traderC.getUser());
        limitOrder(market, yes, "11", quant, traderC.getUser());
        limitOrder(market, yes, "12", quant, traderC.getUser());
        limitOrder(market, yes, "13", quant, traderC.getUser());
        limitOrder(market, yes, "14", quant, traderB.getUser());
        limitOrder(market, yes, "15", quant, traderC.getUser());
        limitOrder(market, yes, "16", quant, traderC.getUser());
        limitOrder(market, yes, "17", quant, traderB.getUser());
        limitOrder(market, yes, "18", quant, traderC.getUser());
        limitOrder(market, yes, "19", quant, traderC.getUser());
        aCoupons = aCoupons.plus(q(round2Trades));

        assertQEquals(0, traderA.balance());
        assertQEquals(230, traderA.loanAmount());
        assertTrue(traderA.inDefault());
        assertQEquals(185, traderA.getDefaultAmount());

        assertTrue(p90.compareTo(currentProbability()) >= 0);

        Thread.sleep(150);
        Probability probAfter1 = currentProbability();
        assertTrue("should have made a sale by now, p is " + probAfter1, p80.compareTo(probAfter1) >= 0);

        Thread.sleep(700);
        Probability probAfter2 = currentProbability();
        assertTrue("Should have made 2 trades by now, p is " + probAfter2, p70.compareTo(probAfter2) >= 0);

        Thread.sleep(200);
        assertQEquals(185, traderA.getDefaultAmount());
        assertTrue(traderA.inDefault());
        assertQEquals(0, traderA.balance());
        assertQEquals(0, traderA.currentCouponCount(claim));

        session.endTrading(false);
        Thread.sleep(200);

        assertMatches("", page.showEarningsSummary());
        assertQEquals(0, traderA.balance());
        assertQEquals(0, traderA.currentCouponCount(claim));
        assertQEquals(50, traderA.loanAmount());
        assertFalse(traderA.canBuy(4));

///////// ROUND 4 ///////////////////////////////

        session.startNextRound();
        assertFalse(traderA.canBuy(4));
    }

    private Probability currentProbability() {
        return market.getBook().bestBuyOfferFor(yes).asProbability();
    }

    public void testLoanDefaultScenario() throws DuplicateOrderException, IOException, InterruptedException, ScoreException {
        String script =
            "sessionTitle: loanDefaultTestCase\n" +
                "rounds: 12\n" +
                "players: traderA, traderB\n" +
                "roles: trader1, trader2\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader1\n" +
                "traderB.role: trader2\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .5\n" +
                "maxPrice: 1000\n" +
                "loansDueInRound: 9\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader1: 1080\n" +
                "endowment.trader2: 360\n" +
                "tickets.trader1: 3\n" +
                "tickets.trader2: 5\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "actualValue:          8, 8, 28, 8, 60, 100, 50, 75, 10, 15, 7, 3\n" +
                "\n" +
                "traderA.hint:         not100        not40,       not100\n" +
                "traderB.hint:         not40,       not100,       not40\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        session.setPeriod(50);

        Borrower traderA = (Borrower) session.getPlayer("traderA");
        Borrower traderB = (Borrower) session.getPlayer("traderB");
        Tally aBalance = new Tally(1080);
        Tally bBalance = new Tally(360);
        Quantity aCoupons = q(3);
        Quantity bCoupons = q(5);

///////// ROUND 1 ///////////////////////////////   traderA buys high; price is high
        session.startSession();
        assertQEquals(0, session.availableLoan(traderA));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        limitOrder(market, yes, p1000("999"), 1, traderA.getUser());
        marketOrder(market, no, p1000("999"), 1, traderB.getUser());
        int round1Price = 999;
        aBalance.sub(q(round1Price));
        bBalance.add(q(round1Price));
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        aCoupons = aCoupons.plus(q(1));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        bCoupons = bCoupons.minus(q(1));
        assertQEquals(bCoupons, traderB.currentCouponCount(claim));

        assertQEquals(0, traderA.loanAmount());
        assertQEquals(0, traderB.loanAmount());
        session.endTrading(false);

        Quantity divOne = q(8);
        Quantity divTwo = divOne;
        aBalance.add(divOne.times(q(4)));
        bBalance.add(divOne.times(q(4)));

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(bBalance.val(), traderB.balance());
        Quantity oneHalf = new Quantity(".5");
        Quantity availA1 = oneHalf.times(q(round1Price).times(q(3 + 1)));
        assertQEquals(availA1, session.availableLoan(traderA));
        Quantity availB1 = oneHalf.times(q(round1Price).times(q(5 - 1)));
        assertQEquals(availB1, session.availableLoan(traderB));

        TraderScreen page = new TraderScreen();
        page.setClaimName(market.getClaim().getName());
        page.setUserName(traderA.getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);

        page.setUserName(traderB.getName());
        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);

        assertQEquals(availA1, traderA.loanAmount());
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        String earnings = page.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        assertQEquals(availB1, traderB.loanAmount());
        assertQEquals(availB1, traderB.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availB1, traderB.getScoreComponent(LoanChangeComponent));
        assertQEquals(availB1, traderB.getScoreComponent(OutstandingLoanComponent));

        aBalance.add(availA1);
        assertQEquals(aBalance.val(), traderA.balance());
        bBalance.add(availB1);
        assertQEquals(bBalance.val(), traderB.balance());

///////// ROUND 2 ///////////////////////////////    traderA buys; price is low

        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());

        assertEquals(2, session.getCurrentRound());

        limitOrder(market, yes, p1000("999"), 1, traderA.getUser());
        marketOrder(market, no, p1000("1"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("999"), 1, traderA.getUser());
        marketOrder(market, no, p1000("1"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("112"), 1, traderA.getUser());
        marketOrder(market, no, p1000("888"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("1"),   1, traderA.getUser());
        marketOrder(market, no, p1000("999"), 1, traderB.getUser());
        int round2Price = 999;

        aBalance.sub(q(2111));
        assertQEquals(0, aBalance.val());
        aCoupons = aCoupons.plus(q(4));
        bBalance.add(q(2111));
        bCoupons = bCoupons.minus(q(4));

        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        assertQEquals(bBalance.val(), traderB.balance());
        assertQEquals(bCoupons, traderB.currentCouponCount(claim));
        Quantity availA2 = oneHalf.times(aBalance.val().minus(availA1).plus(q(1).times(traderA.currentCouponCount(claim))));
        Quantity availB2 = oneHalf.times(bBalance.val().minus(availB1).plus(q(1).times(traderB.currentCouponCount(claim))));

        session.endTrading(false);

        aBalance.add(divTwo.times(q(8)));   // rcvd Dividends
        assertQEquals(aBalance.val(), traderA.balance());
        bBalance.sub("1998");                               // repaid loan
        assertQEquals(bBalance.val(), traderB.balance());

        assertTrue(availA2.isNegative());
        assertTrue(traderA.inDefault());
        assertFalse(traderA.canBuy(3));
        assertFalse(traderA.canSell(3));

        availB2 = Quantity.ZERO;
        assertFalse(traderB.inDefault());
        assertTrue(traderB.canBuy(3));
        assertTrue(traderB.canSell(3));
        assertQEquals(availB2, session.availableLoan(traderB));

        assertQEquals(aBalance.val(), traderA.balance());

///////// ROUND 3 ///////////////////////////////

        session.startNextRound();

        limitOrder(market, yes, p1000("1"), 1, traderB.getUser());
        Thread.sleep(100);

        bCoupons = bCoupons.plus(q(1));
        aCoupons = aCoupons.minus(q(1));

        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1998, traderA.loanAmount());
        assertQEquals(1994, traderA.getDefaultAmount());

        assertQEquals(0, traderA.balance());
        assertQEquals(0, traderA.currentCouponCount(claim));

        Thread.sleep(100);
        Quantity availA3 = oneHalf.times(aBalance.val().minus(availA2).plus(q(1).times(traderA.currentCouponCount(claim))));
        Quantity availB3 = oneHalf.times(bBalance.val().minus(availB2).plus(q(1).times(traderB.currentCouponCount(claim))));

        session.endTrading(false);
        Thread.sleep(100);

        assertMatches("", page.showEarningsSummary());
        assertQEquals(196, traderA.balance());
        assertQEquals(7, traderA.currentCouponCount(claim));
        assertQEquals(1930, traderA.getDefaultAmount());
        assertQEquals(28, session.getDividend(traderA, 3));
        assertQEquals(1998 - 64 - 1, traderA.loanAmount());
        assertFalse(traderA.canBuy(4));

///////// ROUND 4 ///////////////////////////////

        session.startNextRound();
        Thread.sleep(100);
        session.endTrading(false);
        Thread.sleep(100);

        assertFalse(traderA.canBuy(4));
        assertTrue(traderA.inDefault());
    }

    public void testOneTraderScenario() throws DuplicateOrderException, IOException, InterruptedException {
        String script =
            "sessionTitle: loanDefaultTestCase\n" +
                "rounds: 15\n" +
                "players: traderA, traderB\n" +
                "roles: trader1, trader2\n" +
                "timeLimit: 5\n" +
                "traderA.role: trader1\n" +
                "traderB.role: trader2\n" +
                "initialHint: Trading has not started yet.\n" +
                "markToMarket.loan.ratio: .5\n" +
                "maxPrice: 1000\n" +
//                "loansDueInRound: 9\n" +
                "\n" +
                "carryForward: true\n" +
                "useUnaryAssets: true\n" +
                "endowment.trader1: 1080\n" +
                "endowment.trader2: 360\n" +
                "tickets.trader1: 3\n" +
                "tickets.trader2: 5\n" +
                "coupon.basis: 50\n" +
                "\n" +
                "actualValue:          8, 8, 28, 8, 60, 8, 0, 28, 0, 15, 7, 3\n" +
                "\n" +
                "traderA.hint:         not100        not40,       not100\n" +
                "traderB.hint:         not40,       not100,       not40\n" +
                "noMessage:\n";

        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = (LendingSession) SessionSingleton.getSession();
        market = session.getMarket();
        claim = (BinaryClaim) market.getClaim();
        yes = claim.getYesPosition();
        no = claim.getNoPosition();
        session.setPeriod(50);

        Borrower traderA = (Borrower) session.getPlayer("traderA");
        Borrower traderB = (Borrower) session.getPlayer("traderB");
        Tally aBalance = new Tally(1080);
        Quantity aCoupons = q(3);

///////// ROUND 1 ///////////////////////////////
        session.startSession();
        assertQEquals(0, session.availableLoan(traderA));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        int round1Price = 999;
        limitOrder(market, yes, p1000("" + round1Price), 1, traderA.getUser());
        marketOrder(market, no, p1000("999"), 1, traderB.getUser());
        aBalance.sub(q(round1Price));
        assertQEquals(aBalance.val(), traderA.balance());
        aCoupons = aCoupons.plus(q(1));
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));

        assertQEquals(0, traderA.loanAmount());
        session.endTrading(false);

        Quantity divOne = q(8);
        aBalance.add(divOne.times(aCoupons));

        assertQEquals(aBalance.val(), traderA.balance());
        Quantity oneHalf = new Quantity(".5");
        Quantity availA1 = oneHalf.times(q(round1Price).times(aCoupons));
        assertQEquals(availA1, session.availableLoan(traderA));

        TraderScreen page = new TraderScreen();
        page.setClaimName(market.getClaim().getName());
        page.setUserName(traderA.getName());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setNextUrl("/foo");

        request.getParameterMap().put("action", "Accept");
        page.processRequest(request, null);

        assertQEquals(availA1, traderA.loanAmount());
        assertQEquals(availA1, traderA.getScoreComponent(AvailableLoanComponent));
        assertQEquals(availA1, traderA.getScoreComponent(LoanChangeComponent));
        assertQEquals(availA1, traderA.getScoreComponent(OutstandingLoanComponent));

        String earnings = page.showEarningsSummary();
        assertRENoMatch(".*<input.*", earnings);

        aBalance.add(availA1);
        assertQEquals(2111, aBalance.val());
        assertQEquals(aBalance.val(), traderA.balance());

///////// ROUND 2 ///////////////////////////////

        session.startNextRound();
        assertQEquals(aBalance.val(), traderA.balance());

        assertEquals(2, session.getCurrentRound());

        limitOrder(market, yes, p1000("999"), 1, traderA.getUser());
        marketOrder(market, no, p1000("1"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("999"), 1, traderA.getUser());
        marketOrder(market, no, p1000("1"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("112"), 1, traderA.getUser());
        marketOrder(market, no, p1000("888"), 1, traderB.getUser());
        limitOrder(market, yes, p1000("1"),   1, traderA.getUser());
        marketOrder(market, no, p1000("999"), 1, traderB.getUser());

        aCoupons = aCoupons.plus(q(4));

        aBalance.sub(aBalance.val());
        assertQEquals(0, aBalance.val());
        assertQEquals(aBalance.val(), traderA.balance());
        assertQEquals(aCoupons, traderA.currentCouponCount(claim));
        Quantity availA2 = oneHalf.times(aBalance.val().minus(availA1).plus(q(1).times(traderA.currentCouponCount(claim))));

        session.endTrading(false);

        Quantity divTwo = divOne;
        aBalance.add(divTwo.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertQEquals(1998, traderA.loanAmount());
        assertTrue(availA2.isNegative());
        assertTrue(traderA.inDefault());
        assertFalse(traderA.canBuy(3));
        assertFalse(traderA.canSell(3));

///////// ROUND 3 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1998, traderA.loanAmount());
        assertQEquals(0, traderA.currentCouponCount(claim));

        session.endTrading(false);

        Quantity divThree = q(28);
        aBalance.add(divThree.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertMatches("", page.showEarningsSummary());
        assertQEquals(8, traderA.currentCouponCount(claim));
        assertQEquals(1934, traderA.loanAmount());
        assertFalse(traderA.canBuy(4));

        assertQEquals(aCoupons, traderA.currentCouponCount(claim));

///////// ROUND 4 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1934, traderA.loanAmount());

        session.endTrading(false);

        Quantity divFour = q(8);
        aBalance.add(divFour.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(1710, traderA.loanAmount());
        assertFalse(traderA.canBuy(5));

///////// ROUND 5 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1710, traderA.loanAmount());

        session.endTrading(false);

        Quantity divFive = q(60);
        aBalance.add(divFive.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(1646, traderA.loanAmount());
        assertFalse(traderA.canBuy(6));

///////// ROUND 6 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1646, traderA.loanAmount());

        session.endTrading(false);

        Quantity divSix = q(8);
        aBalance.add(divSix.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(1166, traderA.loanAmount());
        assertFalse(traderA.canBuy(7));

///////// ROUND 7 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1166, traderA.loanAmount());

        session.endTrading(false);

        Quantity divSeven = q(0);
        aBalance.add(divSeven.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(1102, traderA.loanAmount());
        assertFalse(traderA.canBuy(7));

///////// ROUND 8 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1102, traderA.loanAmount());

        session.endTrading(false);

        Quantity divEight = q(28);
        aBalance.add(divEight.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(1102, traderA.loanAmount());
        assertFalse(traderA.canBuy(7));

///////// ROUND 9 ///////////////////////////////

        session.startNextRound();

        aBalance.sub(aBalance.val());
        assertQEquals(0, traderA.balance());
        assertTrue(traderA.inDefault());
        assertQEquals(1102, traderA.loanAmount());

        session.endTrading(false);

        Quantity divNine = q(0);
        aBalance.add(divNine.times(aCoupons));
        assertQEquals(aBalance.val(), traderA.balance());

        assertTrue(traderA.inDefault());
        assertQEquals(878, traderA.loanAmount());
        assertFalse(traderA.canBuy(7));
    }

    private Price p1000(String s) {
        return new Price(s, P1000);
    }
}

    class Tally {
        private Quantity value;
        Tally(Quantity v) {
            value = v;
        }

        public Tally(double v) {
            value = new Quantity(v);
        }

        public Quantity val() {
            return value;
        }

        public Tally add(Quantity v) {
            value = value.plus(v);
            return this;
        }

        public Tally sub(Quantity quantity) {
            value = value.minus(quantity);
            return this;
        }

        public Tally sub(String quant) {
            sub(new Quantity(quant));
            return this;
        }

        public Tally add(String v) {
            add(new Quantity(v));
            return this;
        }
    }
