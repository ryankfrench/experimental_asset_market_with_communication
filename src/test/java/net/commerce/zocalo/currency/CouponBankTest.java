package net.commerce.zocalo.currency;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.JunitHelper;

// Copyright 2004, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class CouponBankTest extends JunitHelper {
    private BinaryClaim claim;
    private CashBank cashBank;
    private User owner;

    protected void setUp() throws Exception {
        cashBank = new CashBank("cash");
        owner = cashBank.makeEndowedUser("owner", new Quantity(1000));
        claim = BinaryClaim.makeClaim("testing", owner, "just for testing");
        HibernateTestUtil.resetSessionFactory();
        Log4JHelper.getInstance();
    }

    public void testCreation() {
        CouponBank bank = CouponBank.makeBank(claim, cashBank.noFunds());
        assertTrue(bank.getSetsMinted().isZero());
    }

    public void testIssueCouponSets() {
        CouponBank bank = CouponBank.makeBank(claim, cashBank.noFunds());
        Quantity TwoHundred = new Quantity(200);
        Coupons[] sets = bank.printNewCouponSets(TwoHundred, owner.getAccounts().provideCash(TwoHundred), Quantity.Q100);
        for (int i = 0; i < sets.length; i++) {
            Coupons set = sets[i];
            assertQEquals(TwoHundred, set.getBalance());
        }
        assertQEquals(200.0, bank.getSetsMinted());
    }
}
