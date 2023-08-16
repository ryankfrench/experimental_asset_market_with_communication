package net.commerce.zocalo.currency;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.JunitHelper;

public class CouponsTest extends JunitHelper {
    private final double epsilon = 0.01;
    private final CashBank cashBank = new CashBank("cash");
    private final User owner = cashBank.makeEndowedUser("sam", new Quantity(5000.0));
    private final BinaryClaim claim = BinaryClaim.makeClaim("weather", owner, "wish you were here.");
    private CouponBank bank;
    private static Quantity OneThousand = new Quantity(1000);

    private final Position yes = claim.getYesPosition();

    private Coupons yesCoupons;

    protected void setUp() throws Exception {
        if (bank == null) {
            bank = CouponBank.makeBank(claim, cashBank.noFunds());
        }
        Coupons couponSets[] = bank.printNewCouponSets(OneThousand, owner.getAccounts().provideCash(OneThousand), Quantity.Q100);
        for (int i = 0; i < couponSets.length; i++) {
            Coupons couponSet = couponSets[i];
            if (couponSet.getPosition().equals(claim.getYesPosition())) {
                yesCoupons = couponSet;
            }
        }
        HibernateTestUtil.resetSessionFactory();
        Log4JHelper.getInstance();
    }

    public void testBasicCouponsCreation() {
        assertQEquals(1000.0, yesCoupons.getBalance());
    }

    public void testCurrenciesNotEqual() {
        Coupons climate = Coupons.newPosition(new CurrencyToken("climate"), OneThousand, yes);
        assertQEquals(1000.0, climate.getBalance());
        assertNotSame(yesCoupons, climate);
        assertQEquals(200.0, climate.transfer(q(200), climate));
        assertQEquals(1000.0, climate.getBalance());;
    }

    public void testSimpleTransfer() {
        Coupons claimSets[] = bank.printNewCouponSets(OneThousand, owner.getAccounts().provideCash(OneThousand), Quantity.Q100);
        assertTrue(yesCoupons.sameCurrency(claimSets[0]));
        assertQEquals(200, yesCoupons.transfer(q(200), claimSets[0]));
        assertQEquals(1200.0, claimSets[0].getBalance());
        assertQEquals(800.0, yesCoupons.getBalance());
    }

    public void testCurrenciesCantExchange() {
        Coupons moolah = Coupons.newPosition(new CurrencyToken("cash"), OneThousand, yes);
        assertQEquals(1000.0, moolah.getBalance());
        assertNotSame(yesCoupons, moolah);
        assertTrue(yesCoupons.transfer(q(200), moolah).isZero());
        assertQEquals(1000.0, moolah.getBalance());
        assertQEquals(1000.0, yesCoupons.getBalance());
    }

    public void testNoNegativeQuantities() {
        Coupons moolah = Coupons.newPosition(new CurrencyToken("cash"), q(500).negate(), yes);
        assertTrue(moolah.getBalance().isZero());
        Coupons empty = yesCoupons.makeEmpty();
        yesCoupons.transfer(Quantity.Q100.negate(), empty);
        assertTrue(empty.getBalance().isZero());
        assertTrue(yesCoupons.provide(q(50).negate()).getBalance().isZero());
        assertQEquals(1000.0, yesCoupons.getBalance());
    }

    public void testQuantitiesInRange() {
        Coupons target = yesCoupons.makeEmpty();
        yesCoupons.transfer(Quantity.Q100, target);
        assertQEquals(Quantity.Q100, target.getBalance());
        assertQEquals(900, yesCoupons.getBalance());
        assertTrue(yesCoupons.provide(OneThousand).getBalance().isZero());
        yesCoupons.transfer(OneThousand, target);
        assertQEquals(900.0, yesCoupons.getBalance());
        assertQEquals(Quantity.Q100, target.getBalance());
    }
}
