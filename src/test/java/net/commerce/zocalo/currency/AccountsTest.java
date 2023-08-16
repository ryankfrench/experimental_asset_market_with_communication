package net.commerce.zocalo.currency;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.JunitHelper;
import net.commerce.zocalo.JspSupport.AccountDisplay;

public class AccountsTest extends JunitHelper {
    private final CashBank rootBank = new CashBank("cash");
    private final Quantity SOME_COUPONS = new Quantity("85");
    private final Quantity MANY_COUPONS = new Quantity("200");

    CouponBank issuer;
    private final double epsilon = 0.01;
    private Quantity initialCash = new Quantity("3000");
    private User mgr = rootBank.makeEndowedUser("manager", initialCash);
    private BinaryClaim claim = BinaryClaim.makeClaim("rain", mgr, "when will it rain?");
    private final Position yes = claim.getYesPosition();
    private User trader;
    private User seller;
    private Accounts traderAccount;
    private Accounts sellerAccount;

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
        Log4JHelper.getInstance();

        trader = rootBank.makeEndowedUser("trader", initialCash);
        traderAccount = trader.getAccounts();
        seller = rootBank.makeEndowedUser("seller", initialCash);
        issuer = CouponBank.makeBank(claim, rootBank.noFunds());
        seller.endow(issuer.issueUnpairedCoupons(MANY_COUPONS, yes));
        sellerAccount = seller.getAccounts();
    }

    public void testAccountCreation() {
        assertQEquals(initialCash, traderAccount.cashValue());
        assertTrue(traderAccount.couponCount(yes).isZero());
        assertQEquals(initialCash, sellerAccount.cashValue());
        assertEquals(0, MANY_COUPONS.compareTo(sellerAccount.couponCount(yes)));
    }

    public void testMovingCoupons() {
        assertTrue(traderAccount.couponCount(yes).isZero());
        assertEquals(0, MANY_COUPONS.compareTo(sellerAccount.couponCount(yes)));
        Coupons coupons = sellerAccount.provideCoupons(yes, SOME_COUPONS);
        traderAccount.addCoupons(coupons);

        assertTrue(traderAccount.countBalancedSets(claim).isZero());
        assertTrue(sellerAccount.countBalancedSets(claim).isZero());
        assertEquals(0, SOME_COUPONS.compareTo(traderAccount.couponCount(yes)));
        assertEquals(0, MANY_COUPONS.minus(SOME_COUPONS).compareTo(sellerAccount.couponCount(yes)));
    }

    public void testMovingCash() {
        Quantity halfBalance = initialCash.div(new Quantity("2"));
        assertFalse(sellerAccount.cashSameOrGreaterThan(initialCash.plus(halfBalance)));
        Funds funds = traderAccount.provideCash(halfBalance);
        sellerAccount.receiveCash(funds);

        assertEquals(0, initialCash.minus(halfBalance).compareTo(traderAccount.cashValue()));
        assertEquals(0, initialCash.plus(halfBalance).compareTo(sellerAccount.cashValue()));
        assertTrue(sellerAccount.cashSameOrGreaterThan(initialCash.plus(halfBalance)));
        assertFalse(traderAccount.neglibleCash());
        assertFalse(sellerAccount.neglibleCash());

        Funds moreFunds = traderAccount.provideCash(halfBalance);
        sellerAccount.receiveCash(moreFunds);

        assertTrue(traderAccount.cashValue().isZero());
        assertEquals(0, initialCash.plus(halfBalance.plus(halfBalance)).compareTo(sellerAccount.cashValue()));
        assertTrue(sellerAccount.cashSameOrGreaterThan(initialCash.plus(halfBalance)));
        assertTrue(traderAccount.neglibleCash());
        assertFalse(sellerAccount.neglibleCash());
    }

    public void testHandlingCouponsSets() {
        CouponBank couponBank = CouponBank.makeBank(claim, rootBank.noFunds());
        Coupons[] couponses = couponBank.printNewCouponSets(SOME_COUPONS, sellerAccount.provideCash(Quantity.Q100), Quantity.Q100);
        traderAccount.addAll(couponses);
        assertEquals(0, SOME_COUPONS.compareTo(traderAccount.countBalancedSets(claim)));
    }

    public void testDisplay() {
        StringBuffer buf1 = new StringBuffer();
        trader.displayAccounts(buf1, null, AccountDisplay.claimHoldingsBinaryMarketPrinter());
        assertTrue(buf1.toString().indexOf("User has no Holdings") > 0);
        assertFalse(buf1.toString().indexOf(yes.getName()) > 0);

        StringBuffer buf2 = new StringBuffer();
        seller.displayAccounts(buf2, AccountDisplay.allHoldingsPrinter(seller));
        assertTrue(buf2.toString().indexOf(claim.getName()) > 0);

        BinaryClaim sleet = BinaryClaim.makeClaim("sleet", mgr, "How likely is it to Sleet?");

        seller.endow(Coupons.newPosition(new CurrencyToken("sleet"), MANY_COUPONS, sleet.getYesPosition()));

        StringBuffer buf3 = new StringBuffer();
        seller.displayAccounts(buf3, AccountDisplay.allHoldingsPrinter(seller));
        assertTrue(buf3.toString().indexOf(claim.getName()) > 0);
        assertTrue(buf3.toString().indexOf("sleet") > 0);
    }

    public void testCouponCounts() {
        Position no = this.claim.getNoPosition();
        User yesHolder = rootBank.makeEndowedUser("couponHolder", initialCash);
        Accounts yesAccount = yesHolder.getAccounts();
        User noHolder = rootBank.makeEndowedUser("noHolder", initialCash);
        Accounts noAccount = noHolder.getAccounts();
        trader.endow(Coupons.newPosition(new CurrencyToken("rain"), SOME_COUPONS, no));

        assertEquals(0, MANY_COUPONS.compareTo(sellerAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.negate().compareTo(traderAccount.couponCount(claim)));
        assertEquals(0, Quantity.ZERO.compareTo(noAccount.couponCount(claim)));
        assertEquals(0, Quantity.ZERO.compareTo(yesAccount.couponCount(claim)));

        noAccount.addCoupons(traderAccount.provideCoupons(no, SOME_COUPONS));
        yesAccount.addCoupons(sellerAccount.provideCoupons(yes, SOME_COUPONS));

        assertEquals(0, MANY_COUPONS.minus(SOME_COUPONS).compareTo(sellerAccount.couponCount(claim)));
        assertEquals(0, Quantity.ZERO.compareTo(traderAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.negate().compareTo(noAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.compareTo(yesAccount.couponCount(claim)));

        traderAccount.addCoupons(sellerAccount.provideCoupons(yes, SOME_COUPONS));

        Quantity manyLessSomeTwice = MANY_COUPONS.minus(SOME_COUPONS.times(new Quantity("2")));
        assertEquals(0, manyLessSomeTwice.compareTo(sellerAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.compareTo(traderAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.negate().compareTo(noAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.compareTo(yesAccount.couponCount(claim)));

        sellerAccount.addCoupons(noAccount.provideCoupons(no, SOME_COUPONS));

        assertEquals(0, manyLessSomeTwice.compareTo(sellerAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.compareTo(traderAccount.couponCount(claim)));
        assertEquals(0, Quantity.ZERO.compareTo(noAccount.couponCount(claim)));
        assertEquals(0, SOME_COUPONS.compareTo(yesAccount.couponCount(claim)));
    }

    public void testMultiClaimCounts() {
        String sun = "sun";
        String fog = "fog";
        String rain = "rain";
        MultiClaim weather = new MultiClaim("weather", mgr, "What weather should we expect?", new String[]{sun, fog, rain});
        Position sunPos = weather.lookupPosition(sun);
        Position rainPos = weather.lookupPosition(rain);
        Position fogPos = weather.lookupPosition(fog);
        User t1 = rootBank.makeEndowedUser("trader1", initialCash);
        User t2 = rootBank.makeEndowedUser("trader2", initialCash);
        User t3 = rootBank.makeEndowedUser("trader3", initialCash);
        Quantity fifty = new Quantity("50");
        Quantity q75 = new Quantity("75");
        t1.endow(Coupons.newPosition(new CurrencyToken(sun), SOME_COUPONS, sunPos));
        t1.endow(Coupons.newPosition(new CurrencyToken(rain), fifty, rainPos));
        t2.endow(Coupons.newPosition(new CurrencyToken(fog), q75, fogPos));
        assertEquals(0, SOME_COUPONS.compareTo(t1.getAccounts().couponCount(sunPos)));
        assertEquals(0, fifty.compareTo(t1.getAccounts().couponCount(rainPos)));
        assertTrue(t1.getAccounts().couponCount(fogPos).isZero());
        assertEquals(0, q75.compareTo(t2.getAccounts().couponCount(fogPos)));
        assertTrue(t2.getAccounts().couponCount(sunPos).isZero());
        assertTrue(t3.getAccounts().couponCount(sunPos).isZero());
        assertTrue(t3.getAccounts().couponCount(rainPos).isZero());

        assertEquals(0, fifty.compareTo(t1.getAccounts().minCouponsVersus(fogPos)));
        assertTrue(t1.getAccounts().minCouponsVersus(sunPos).isZero());
        assertTrue(t2.getAccounts().minCouponsVersus(sunPos).isZero());
        assertTrue(t2.getAccounts().minCouponsVersus(fogPos).isZero());
        assertTrue(t3.getAccounts().minCouponsVersus(fogPos).isZero());

        StringBuffer buf = new StringBuffer();
        t1.displayAccounts(buf, weather, AccountDisplay.claimHoldingsBinaryMarketPrinter());
        String someCoupons = SOME_COUPONS.printAsDetailedQuantity();
        assertREMatches(".*(50.000|" + someCoupons + ")</td>.*(50.000|" + someCoupons + ")</td>.*", buf.toString());
    }
}
