package net.commerce.zocalo.market;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.ajax.events.Redemption;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import org.hibernate.Criteria;
import org.hibernate.Transaction;

import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;
// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentMarketTest extends PersistentTestHelper {
    private long theMarketId;
    private final String a = "userA";
    private final String b = "userB";
    private final String c = "userC";
    private final String d = "userD";
    private final String e = "userE";
    private final String f = "userF";
    private final String g = "userG";
    private final String j = "userJ";
    private final String l = "userL";
    private final String m = "userM";
    private final String n = "userN";

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate("data/PersistentMarketTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = new CashBank("money");
        storeObject(bank);
        SecureUser owner = new SecureUser("joe", bank.makeFunds(30000), "secure", "someone@example.com");
        storeObject(owner);
        BinaryClaim weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
        storeObject(weather);
        BinaryMarket market = BinaryMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        theMarketId = market.getId();
        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicPersistence() throws Exception {
        manualSetUpForUpdate("data/PersistentMarketTest");

        Market m = getMarket(theMarketId);
        assertEquals(m.getName(), "rain");
        manualTearDown();
    }

    public void testPersistentBook() throws Exception {
        manualSetUpForUpdate("data/PersistentMarketTest");

        Market m = getMarket(theMarketId);
        Book book = m.getBook();
        assertEquals(m.getName(), ((BinaryClaim) book.getClaim()).getName());

        manualTearDown();
    }

    public void testPersistentMarketMaker() throws Exception {
        int mmEndow = 5000;
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            SecureUser owner = getUser("joe");
            BinaryMarket m = (BinaryMarket) getMarket(theMarketId);
            Quantity mmEndowAsQuant = q(mmEndow);
            m.makeMarketMaker(owner, mmEndowAsQuant, new Probability(".7"));
            assertQEquals(mmEndowAsQuant, m.getMaker().cashInAccount());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Quantity mmEndowAsQuant = q(mmEndow);

            Probability seventyPercent = new Probability(".7");
            Market m = getMarket(theMarketId);
            Position yes = getClaim().getYesPosition();
            Position no = getClaim().getNoPosition();
            assertQEquals(q(.7), m.currentProbability(yes));
            assertQEquals(mmEndowAsQuant, m.getMaker().cashInAccount());
            CashBank bank = getBank();
            int saraEndow = 50000;
            SecureUser sara = new SecureUser("sara", bank.makeFunds(saraEndow), "secure", "someone@example.com");
            storeObject(sara);
            assertQEquals(mmEndowAsQuant, m.getMaker().cashInAccount());
            assertQEquals(q(saraEndow), sara.cashOnHand());

            m.getMaker().buyUpToQuantity(yes, new Probability(".75"), q(10), sara);

            assertQEquals(q(mmEndow + saraEndow), sara.cashOnHand().plus(m.getMaker().cashInAccount()).plus(m.mintBalance()));
            assertTrue(m.getMaker().currentProbability(yes).compareTo(seventyPercent) > 0);
            assertTrue(q(saraEndow).compareTo(sara.cashOnHand()) > 0);

            Probability thirtyFivePercent = new Probability(".35");
            m.getMaker().buyUpToQuantity(no, thirtyFivePercent, q(10), sara);
            Quantity coupons = m.getMaker().getAccounts().couponCount(yes);
            Quantity couponHoldings = sara.couponCount(no).plus(coupons);
            Quantity cashHoldings = sara.cashOnHand().plus(m.getMaker().cashInAccount());
            assertQEquals(mmEndow + saraEndow, cashHoldings.plus(couponHoldings));
            assertQEquals(seventyPercent, m.getMaker().currentProbability(yes));
        }
        manualTearDown();
    }

    public void testPersistentOrders() throws Exception {
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            Market m = getMarket(theMarketId);
            CashBank bank = getBank();
            SecureUser sue = new SecureUser("someone", bank.makeFunds(50000), "secure", "someone@example.com");
            storeObject(sue);
            Book book = m.getBook();

            BinaryClaim weatherAgain = ((BinaryClaim) m.getBook().getClaim());
            addOrder(book, sue, weatherAgain.getYesPosition(), "40", 20);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            Book book = getMarket(theMarketId).getBook();

            Position yes = ((BinaryClaim) book.getClaim()).getYesPosition();
            assertQEquals(40, book.bestBuyOfferFor(yes));
            assertQEquals(20, book.bestQuantity(yes));

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        manualTearDown();
    }

    public void testDecideClaim() throws Exception {
        final int purchaseAmount = 30;

        Map<String, BigDecimal> coupons = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            BinaryMarket m = (BinaryMarket)getMarket(theMarketId);
            CashBank bank = getBank();
            final int initialCash = 5000;
            SecureUser sue = new SecureUser("sue", bank.makeFunds(initialCash), "secure", "sue@example.com");
            storeObject(sue);
            SecureUser bob = new SecureUser("bob", bank.makeFunds(initialCash), "secure", "bob@example.com");
            storeObject(bob);
            Book book = m.getBook();

            Position yes = ((BinaryClaim) book.getClaim()).getYesPosition();
            Position no = yes.opposite();

            assertBinaryTradesBalance((BinaryMarket) m, sue, initialCash);
            assertBinaryTradesBalance((BinaryMarket) m, bob, initialCash);

            limitOrder(m, yes, "40", purchaseAmount, sue);
            limitOrder(m, no, "65", purchaseAmount, bob);

            Quantity boughtAtForty = q(initialCash - (purchaseAmount * 40));
            Quantity boughtAtSixty = q(initialCash - (purchaseAmount * 60));
            rememberHoldings("sue", sue, coupons, balances, yes);
            rememberHoldings("bob", bob, coupons, balances, yes);

            assertQEquals(boughtAtForty, sue.cashOnHand());
            assertQEquals(boughtAtSixty, bob.cashOnHand());
            assertQEquals(q(purchaseAmount), sue.couponCount(yes));
            assertBinaryTradesBalance((BinaryMarket) m, sue, initialCash);
            assertBinaryTradesBalance((BinaryMarket) m, bob, initialCash);
            assertTrue(m.isOpen());

            assertEquals(0, countClaimRedemptions());
            try {
                assertTrue(m.outcome(no).isZero());
                fail("undecided claim has no outcome");
            } catch (RuntimeException r) {
                // correct outcome.
            }
            m.decideClaimAndRecord(yes);
            assertEquals(2, countClaimRedemptions());
            assertQEquals(boughtAtForty.plus(q(purchaseAmount * 100.0)), sue.cashOnHand());
            assertQEquals(boughtAtSixty, bob.cashOnHand());
            assertTrue(sue.couponCount(yes).isZero());
            assertTrue(bob.couponCount(no).isZero());
            assertFalse(m.isOpen());
            assertBinaryTradesBalance((BinaryMarket) m, sue, initialCash);
            assertBinaryTradesBalance((BinaryMarket) m, bob, initialCash);
            assertQEquals(1, m.outcome(yes));
            assertTrue(m.outcome(no).isZero());
            assertEquals("yes", m.describeOutcome());


            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            assertBalanced(coupons, balances, "sue", getUser("sue"));
            assertBalanced(coupons, balances, "bob", getUser("bob"));

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    public void limitOrder(BinaryMarket market, Position run, String price, int quantity, SecureUser userB) throws DuplicateOrderException {
        market.limitOrder(run, Price.dollarPrice(price), q(quantity), userB);
    }

    public void testDecideClaimBug() throws Exception {
        long marketId;
        double epsilon = .01;
        final int initialCash = 10000;

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            CashBank bank = getBank();
            SecureUser mcDuck = new SecureUser("McDuck", bank.makeFunds(20000), "secure", "someone@example.com");
            storeObject(mcDuck);
            MultiClaim football = MultiClaim.makeClaim("football", mcDuck, "Will OK run or pass?", new String[] { "run", "pass" });
            storeObject(football);
            MultiMarket market = MultiMarket.make(mcDuck, football, bank.noFunds());
            storeObject(market);
            marketId = market.getId();
            MarketMaker maker = new MultiMarketMaker(market, q(1000), mcDuck);
            storeObject(maker);
            createUser(initialCash, a);
            createUser(initialCash, b);
            createUser(initialCash, c);
            createUser(initialCash, d);
            createUser(initialCash, e);
            createUser(initialCash, f);
            createUser(initialCash, g);
            createUser(initialCash, j);
            createUser(initialCash, l);
            createUser(initialCash, m);
            createUser(initialCash, n);
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        Map<String, BigDecimal> coupons = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            MultiMarket market = (MultiMarket)getMarket(marketId);
            Claim football = market.getClaim();
            Position pass = football.lookupPosition("pass");
            Position run = football.lookupPosition("run");

            SecureUser userB = getUser(b);
            SecureUser userC = getUser(c);
            SecureUser userD = getUser(d);
            SecureUser userE = getUser(e);
            SecureUser userF = getUser(f);
            SecureUser userG = getUser(g);
            SecureUser userJ = getUser(j);
            SecureUser userL = getUser(l);
            SecureUser userM = getUser(m);
            SecureUser userN = getUser(n);

            String ninetyNine = "99";
            limitOrder(market, run, ninetyNine, 20, userB);
            limitOrder(market, pass, ninetyNine, 2499.534, userB);

            limitOrder(market, pass, ninetyNine, 20, userC);
            limitOrder(market, run, ninetyNine, 174.389, userC);

            limitOrder(market, run, ninetyNine, 20, userD);
            limitOrder(market, pass, ninetyNine, 8718.474, userD);

            limitOrder(market, pass, ninetyNine, 1711.022, userE);
            limitOrder(market, run, ninetyNine, 571.493, userF);
            limitOrder(market, run, ninetyNine, 1630.123, userG);
            limitOrder(market, pass, ninetyNine, 9168.234, userJ);

            limitOrder(market, run, ninetyNine, 20, userL);
            limitOrder(market, pass, ninetyNine, 22.06, userL);

            limitOrder(market, run, ninetyNine, 1218.938, userM);
            limitOrder(market, run, ninetyNine, 1154.772, userN);

            rememberHoldings(b, userB, coupons, balances, run);
            rememberHoldings(c, userC, coupons, balances, run);
            rememberHoldings(d, userD, coupons, balances, run);
            rememberHoldings(e, userE, coupons, balances, run);
            rememberHoldings(f, userF, coupons, balances, run);
            rememberHoldings(g, userG, coupons, balances, run);
            rememberHoldings(j, userJ, coupons, balances, run);
            rememberHoldings(l, userL, coupons, balances, run);
            rememberHoldings(m, userM, coupons, balances, run);
            rememberHoldings(n, userN, coupons, balances, run);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            assertEquals(0, countClaimRedemptions());

            MultiMarket market = (MultiMarket)getMarket(marketId);
            Claim football = market.getClaim();
            Position run = football.lookupPosition("run");
            SecureUser userC = getUser(c);

            market.decideClaimAndRecord(run);
            assertTrue(userC.couponCount(run).isZero());
            assertFalse(market.isOpen());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            SecureUser userB = getUser(b);
            SecureUser userC = getUser(c);
            SecureUser userD = getUser(d);
            SecureUser userE = getUser(e);
            SecureUser userF = getUser(f);
            SecureUser userG = getUser(g);
            SecureUser userJ = getUser(j);
            SecureUser userL = getUser(l);
            SecureUser userM = getUser(m);
            SecureUser userN = getUser(n);

            assertEquals(balances.get(b), userB.cashOnHand().asValue());
            assertEquals(balances.get(d), userD.cashOnHand().asValue());
            assertEquals(balances.get(e), userE.cashOnHand().asValue());
            assertEquals(balances.get(j), userJ.cashOnHand().asValue());
            assertEquals(balances.get(l), userL.cashOnHand().asValue());

            assertBalanced(coupons, balances, c, userC);
            assertBalanced(coupons, balances, f, userF);
            assertBalanced(coupons, balances, g, userG);
            assertBalanced(coupons, balances, m, userM);
            assertBalanced(coupons, balances, n, userN);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    public void testDecideClaim2() throws Exception {
        long marketId;

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            CashBank bank = getBank();
            SecureUser mcDuck = new SecureUser("McDuck", bank.makeFunds(20000), "secure", "someone@example.com");
            storeObject(mcDuck);
            MultiClaim football = MultiClaim.makeClaim("football", mcDuck, "Will OK run or pass?", new String[] { "run", "pass" });
            storeObject(football);
            MultiMarket market = MultiMarket.make(mcDuck, football, bank.noFunds());
            storeObject(market);
            marketId = market.getId();
            final Quantity initialCash = new Quantity(10000);
            MarketMaker maker = new MultiMarketMaker(market, initialCash, mcDuck);
            storeObject(maker);
            tx.commit();

            createUser(12695, b);
            createUser(11931, c);
            createUser(10022, d);
            createUser(9230, e);
            createUser(10013, f);
            createUser(4488.5, g);
            createUser(7980, j);
            createUser(16653, l);
            createUser(5795, m);
            createUser(12897.4, n);
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        Map<String, BigDecimal> coupons = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
        {
            atomicCostLimitBuy(n, "run", .52, 1156, marketId);
            atomicCostLimitBuy(f, "run", .5299, 571.5, marketId);
            atomicCostLimitBuy(d, "pass", .5056, 2050.0, marketId);
            atomicCostLimitBuy(j, "pass", 0.55, 2572, marketId);
            atomicCostLimitBuy(d, "pass", 0.58014, 1770, marketId);
            atomicCostLimitBuy(d, "pass", 0.6, 3000, marketId);
            atomicCostLimitBuy(d, "pass", 0.6268, 3200, marketId);
            atomicCostLimitBuy(d, "pass", 0.6518, 3200, marketId);
            resetProbs(marketId, .6268);
            atomicCostLimitBuy(g, "run", 0.4, 4000, marketId);
            atomicCostLimitBuy(m, "run", .42044, 1219, marketId);
            atomicCostLimitBuy(d, "pass", 0.6077, 1685, marketId);
            atomicCostLimitBuy(d, "pass", 0.634, 1610, marketId);
            atomicCostLimitBuy(b, "pass", .57, 4000, marketId);
            atomicCostLimitBuy(e, "pass", 0.5988, 1711, marketId);
            atomicCostLimitBuy(j, "pass", .60, 73, marketId);
            atomicCostLimitBuy(l, "pass", 0.6028, 166.3, marketId);
            resetProbs(marketId, .6);
            atomicCostLimitBuy(b, "run", .39, 605, marketId);
            atomicCostLimitBuy(l, "pass", 0.6127, 163.5, marketId);
            atomicCostLimitBuy(b, "run", .37, 1062, marketId);
            resetProbs(marketId, .61);
            atomicCostLimitBuy(j, "pass", .65, 2478, marketId);
            atomicCostLimitBuy(c, "pass", 0.6279, 1385, marketId);
            resetProbs(marketId, .6127);
            atomicCostLimitBuy(l, "pass", .61, 164, marketId);  // TODO something weird happens here!
//            atomicCostLimitBuy(b, "run", .3545, 4020, marketId);  // two trades; one transaction
            atomicCostLimitBuy(b, "run", .34, 3020, marketId);
            resetProbs(marketId, .61);
            atomicCostLimitBuy(l, "pass", 0.60725, 255, marketId);
            atomicCostLimitBuy(l, "pass", 0.6058, 255, marketId);
            atomicCostLimitBuy(j, "pass", .65, 2733, marketId);
            atomicCostLimitBuy(l, "pass", 0.6524, 153, marketId);
            atomicCostLimitBuy(b, "pass", 0.6642, 760, marketId);
            atomicCostLimitBuy(d, "pass", .6198, 3000, marketId);
            atomicCostLimitBuy(b, "pass", .63272, 800, marketId);

            atomicCostLimitBuy(l, "pass", .6353, 160, marketId);
            atomicCostLimitBuy(l, "pass", .63778, 160, marketId);
            atomicCostLimitBuy(l, "pass", .63337, 275, marketId);
            atomicCostLimitBuy(l, "pass", .63168, 110, marketId);
            atomicCostLimitBuy(l, "pass", .629, 170, marketId);

            atomicCostLimitBuy(j, "pass", .65, 1315, marketId);
            atomicCostLimitBuy(l, "pass", 0.6524, 155, marketId);
            atomicCostLimitBuy(c, "pass", .66, 485, marketId);

            atomicCostLimitBuy(l, "pass", 0.6602, 13, marketId);
            atomicCostLimitBuy(l, "pass", 0.6623, 140, marketId);
            atomicCostLimitBuy(l, "pass", 0.6602, 140, marketId);
            atomicCostLimitBuy(l, "pass", 0.6577, 160, marketId);
            atomicCostLimitBuy(l, "pass", 0.66013, 160, marketId);
            atomicCostLimitBuy(l, "pass", 0.66019, 4, marketId);
            atomicCostLimitBuy(l, "pass", 0.66248, 150, marketId);
            atomicCostLimitBuy(l, "pass", 0.66481, 150, marketId);
            atomicCostLimitBuy(l, "pass", 0.66022, 300, marketId);

            resetProbs(marketId, .66481);
            atomicCostLimitBuy(c, "pass", 0.67623, 746, marketId);
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            MultiMarket market = (MultiMarket)getMarket(marketId);
            Claim football = market.getClaim();
            Position run = football.lookupPosition("run");

            rememberHoldings(b, getUser(b), coupons, balances, run);
            rememberHoldings(c, getUser(c), coupons, balances, run);
            rememberHoldings(d, getUser(d), coupons, balances, run);
            rememberHoldings(e, getUser(e), coupons, balances, run);
            rememberHoldings(f, getUser(f), coupons, balances, run);
            rememberHoldings(g, getUser(g), coupons, balances, run);
            rememberHoldings(j, getUser(j), coupons, balances, run);
            rememberHoldings(l, getUser(l), coupons, balances, run);
            rememberHoldings(m, getUser(m), coupons, balances, run);
            rememberHoldings(n, getUser(n), coupons, balances, run);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            assertEquals(0, countClaimRedemptions());

            MultiMarket market = (MultiMarket)getMarket(marketId);
            Claim football = market.getClaim();
            Position run = football.lookupPosition("run");
            SecureUser userC = getUser(c);
            assertFalse(market.outcomeIsContinuous());
            try {
                assertTrue(market.outcome(run).isZero());
                fail("undecided claim has no outcome");
            } catch (RuntimeException r) {
                // correct outcome.
            }

            tx.commit();

            market.decideClaimAndRecord(run);
            tx.commit();
            assertTrue(userC.couponCount(run).isZero());
            assertFalse(market.isOpen());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            SecureUser userB = getUser(b);
            SecureUser userC = getUser(c);
            SecureUser userD = getUser(d);
            SecureUser userE = getUser(e);
            SecureUser userF = getUser(f);
            SecureUser userG = getUser(g);
            SecureUser userJ = getUser(j);
            SecureUser userL = getUser(l);
            SecureUser userM = getUser(m);
            SecureUser userN = getUser(n);

            assertBalanced(coupons, balances, b, userB);
            assertBalanced(coupons, balances, c, userC);
            assertBalanced(coupons, balances, d, userD);
            assertBalanced(coupons, balances, e, userE);
            assertBalanced(coupons, balances, f, userF);
            assertBalanced(coupons, balances, g, userG);
            assertBalanced(coupons, balances, j, userJ);
            assertBalanced(coupons, balances, l, userL);
            assertBalanced(coupons, balances, m, userM);
            assertBalanced(coupons, balances, n, userN);
            Market market = getMarket(marketId);
            assertQEquals(1, market.outcome(market.getClaim().lookupPosition("run")));

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {  /// try to force transactions to log
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            assertEquals(15, countClaimRedemptions());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    /* provide a place to "correct" probabilities to those in the buggy log.   */
    Quantity resetProbs(long marketId, double newPassProb) throws Exception {
        MultiMarket market = (MultiMarket)getMarket(marketId);
        MarketMaker maker = market.getMaker();
        Claim football = market.getClaim(); // market.maker.probabilities.put(market.getClaim().lookupPosition("run"), new Probability(1.0 - newPassProb))
        return maker.cashInAccount();       // market.maker.probabilities.put(football.lookupPosition("pass"), new Probability(newPassProb))
    }

    private void atomicCostLimitBuy(String name, String posName, double prob, double limit, long marketId) throws Exception {
//        manualSetUpForUpdate("data/PersistentMarketTest");
//        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

        MultiMarket market = (MultiMarket)getMarket(marketId);
        Claim football = market.getClaim();
        Position pos = football.lookupPosition(posName);
        MarketMaker maker = market.getMaker();

        maker.buyWithCostLimit(pos, new Probability(q(prob)), q(limit), getUser(name));
        tx.commit();
//        HibernateTestUtil.currentSession().flush();
//        HibernateTestUtil.closeSession();
    }

    private void assertBalanced(Map<String, BigDecimal> coupons, Map<String, BigDecimal> balances, String name, SecureUser user) {
        BigDecimal rememberedValue = balances.get(name).add(coupons.get(name).multiply(new BigDecimal(100)));
        assertQEquals(new Quantity(rememberedValue), user.cashOnHand());
    }

    private void rememberHoldings(String name, SecureUser user, Map<String, BigDecimal> coupons, Map<String, BigDecimal> balances, Position position) {
        coupons.put(name, user.couponCount(position).asValue());
        balances.put(name, user.cashOnHand().asValue());
    }

    private SecureUser createUser(double initialCash, String name) {
        CashBank bank = getBank();
        SecureUser sue = new SecureUser(name, bank.makeFunds(new Quantity(initialCash)), "secure", name + "@example.com");
        storeObject(sue);
        return sue;
    }

    public static int countClaimRedemptions() {
        Criteria redemptions = HibernateUtil.currentSession().createCriteria(Redemption.class);
        return redemptions.list().size();
    }

    public void testBookOrderRemoval() throws Exception {
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            CashBank bank = getBank();
            Market m = getMarket(theMarketId);
            User buyer1 = new SecureUser("buyer1", bank.makeFunds(30000), "secure", "buyer1@example.com");
            storeObject(buyer1);
            User buyer2 = new SecureUser("buyer2", bank.makeFunds(30000), "secure", "buyer2@example.com");
            storeObject(buyer2);
            assertEquals(0, buyer1.getOrders().size());
            assertEquals(0, buyer2.getOrders().size());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            Market m = getMarket(theMarketId);
            Book book = m.getBook();
            SecureUser buyer1 = getUser("buyer1");
            SecureUser buyer2 = getUser("buyer2");
            assertEquals(0, buyer1.getOrders().size());
            assertEquals(0, buyer2.getOrders().size());

            Claim claim = m.getClaim();
            Position yes = ((BinaryClaim)claim).getYesPosition();
            addOrder(book, buyer1, yes, "37", 20);
            assertEquals(1, buyer1.getOrders().size());

            Order buyer2Order = addOrder(book, buyer2, yes, "40", 10);
            assertEquals(1, buyer2.getOrders().size());
            assertQEquals(40, book.bestBuyOfferFor(yes));

            book.removeOrder(buyer2Order);
            assertEquals(0, buyer2.getOrders().size());

            assertQEquals(37, book.bestBuyOfferFor(yes));
            book.removeOrder(buyer1.getName(), Price.dollarPrice("37"), yes);
            assertEquals(0, buyer1.getOrders().size());

            assertTrue(book.bestBuyOfferFor(yes).isZero());
            assertEquals(false, book.removeOrder(buyer1.getName(), Price.dollarPrice("30"), yes));
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    private Order addOrder(Book book, SecureUser buyer1, Position pos, String price, double quantity) throws DuplicateOrderException, IncompatibleOrderException {
        return book.addOrder(pos, Price.dollarPrice(price), new Quantity(quantity), buyer1);
    }

    public void testFromNishak() throws Exception {
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank bank = getBank();
            storeObject(new SecureUser("XXX", bank.makeFunds(50000), "YXX", "XXX@example.com"));
            storeObject(new SecureUser("YYY", bank.makeFunds(50000), "XYY", "YYY@example.com"));

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        {
            manualSetUpForUpdate("data/PersistentMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            Market binMarket = getMarket(theMarketId);
            BinaryClaim binClaim = ((BinaryClaim) binMarket.getBook().getClaim());
            SecureUser buyer = HibernateUtil.getUserByName("XXX");
            SecureUser seller = HibernateUtil.getUserByName("YYY");
            Position buyPos = binClaim.getYesPosition();
            Position sellPos = binClaim.getNoPosition();

            // buying
            Price OnePercent = Price.dollarPrice("01");
            Price buyPrice = OnePercent;
            Quantity buyQty = q(1);
            binMarket.limitOrder(buyPos, buyPrice, buyQty, buyer);
            assertQEquals(1, binMarket.getBook().bestBuyOfferFor(buyPos));
            assertQEquals(1, binMarket.getBook().bestQuantity(buyPos));

            buyPrice = Price.dollarPrice("02");
            buyQty = q(2);
            binMarket.limitOrder(buyPos, buyPrice, buyQty, buyer);
            assertQEquals(2, binMarket.getBook().bestBuyOfferFor(buyPos));
            assertQEquals(2, binMarket.getBook().bestQuantity(buyPos));

            // selling
            Price sellPrice = OnePercent;
            Quantity sellQty = q(4);
            sellPrice = sellPrice.inverted();
            binMarket.limitOrder(sellPos, sellPrice, sellQty, seller);
            assertQEquals(99, binMarket.getBook().bestBuyOfferFor(sellPos));
            assertQEquals(1, binMarket.getBook().bestQuantity(sellPos));
            assertTrue(binMarket.getBook().bestBuyOfferFor(buyPos).isZero());
            assertTrue(binMarket.getBook().bestQuantity(buyPos).isZero());

            buyPrice = OnePercent;
            buyQty = q(1);
            binMarket.limitOrder(buyPos, buyPrice, buyQty, buyer);
            System.out.println("******** CreateMarketCmd.createMarket()reached at test trade commit()");
            assertTrue(binMarket.getBook().bestQuantity(sellPos).isZero());

//            printTradeHistory("XXX", "YYY");

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    //    private void printTradeHistory(String buyerName, String sellerName)throws Exception {
//        SecureUser buyer = HibernateUtil.getUserByName(buyerName);
//        SecureUser seller = HibernateUtil.getUserByName(sellerName);
//
//        System.out.println("*****************GetUserTradeHistoryCmd.getUserTradeHistory() Printing History for buyer *********");
//        List historyList = HibernateUtil.getTrades(buyer);
//        printTradeHistory(historyList);
//
//        System.out.println("*****************GetUserTradeHistoryCmd.getUserTradeHistory() Printing History for seller *********");
//        historyList = HibernateUtil.getTrades(seller);
//        printTradeHistory(historyList);
//
//    }
//
//    private void printTradeHistory(List historyList)throws Exception {
//        for (Object aHistoryList : historyList) {
//
//            Trade trade = (Trade)aHistoryList;
//            String posGetSimpleName = trade.getPos().getSimpleName();
//            String posGetName = trade.getPos().getName();
//            String qty = NumberDisplay.printAsQuantity(trade.getQuantity());
////            double price = PriceFromTrades((trade.getPrice()), trade.getPos());
//            String price = NumberDisplay.printAsPrice(trade.getPrice());
//            Date tradeDate = trade.getTime();
//            String tradeType = trade.getPos().getName().compareToIgnoreCase("yes") == 0 ? "Buy" : "Sell";
//            String tradeOwner = trade.getOwner();
//
//            System.out.println("****** .getUserTradeHistory() posGetSimpleName posGetName qty  price  tradeDate tradeType  tradeOwner *******");
//            System.out.println("****** .getUserTradeHistory(): " + posGetSimpleName + ":" + posGetName + ":" + qty + ":" + price + ":" + tradeDate + ":" + tradeType + ":" + tradeOwner);
//        }
//    }
}
