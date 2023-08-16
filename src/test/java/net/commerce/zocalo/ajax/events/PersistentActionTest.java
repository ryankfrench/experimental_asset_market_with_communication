package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.ajax.dispatch.MockDatum;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.File;

public class PersistentActionTest extends PersistentTestHelper  {
    CashBank root = new CashBank("cash");
    SecureUser owner = new SecureUser("owner", root.makeFunds(1000), "bygones", "owner@example.com");
    private BinaryClaim claim = BinaryClaim.makeClaim("persistentClaim", owner, "answer the question");

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicPersistence() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        MockDatum dat = new MockDatum();
        dat.setTitle("testing");
        assertEquals(null, dat.getId());

        storeObject(dat);
        assertNotNull(HibernateTestUtil.currentSession());
        manualTearDown();
    }

    public void testPersistentAction() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        Ask ask1;
        {
            ask1 = new Ask();
            initializeActionAndAssertOwnerName(ask1, "someone", 50, 20);
            assertNotNull(HibernateTestUtil.currentSession());

            HibernateTestUtil.closeSession();
        }

        Ask ask2;
        {
            Session newSession = HibernateTestUtil.currentSession();
            Transaction tx = newSession.beginTransaction();
            ask2 = (Ask) newSession.get(Ask.class, new Long(1));
            assertEquals("someone", ask2.getOwner());
            ask2.setPrice(Price.dollarPrice(80));
            newSession.update(ask2);
            tx.commit();
            HibernateTestUtil.closeSession();
        }

        Ask ask3;
        {
            Session session3 = HibernateTestUtil.currentSession();
            Transaction tx3 = session3.beginTransaction();
            ask3 = (Ask) session3.get(Ask.class, new Long(1));
            assertEquals("someone", ask3.getOwner());
            assertQEquals(80, ask3.getPrice());
            tx3.commit();
            HibernateTestUtil.closeSession();
        }

        assertQEquals(50, ask1.getPrice());
        assertQEquals(80, ask2.getPrice());
        assertQEquals(80, ask3.getPrice());
        manualTearDown();
    }

    public void testPersistentBid() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        Bid bid1;
        {
            bid1 = new Bid();
            initializeActionAndAssertOwnerName(bid1, "someone", 50, 20);
            HibernateTestUtil.closeSession();
        }

        Bid bid2;
        {
            Session newSession = HibernateTestUtil.currentSession();
            Transaction tx = newSession.beginTransaction();
            bid2 = (Bid) newSession.get(Bid.class, new Long(1));
            assertEquals("someone", bid2.getOwner());
            setPrice(bid2, 80);
            newSession.update(bid2);
            tx.commit();
            HibernateTestUtil.closeSession();
        }

        Bid bid3;
        {
            Session session3 = HibernateTestUtil.currentSession();
            Transaction tx3 = session3.beginTransaction();
            bid3 = (Bid) session3.get(Bid.class, new Long(1));
            assertEquals("someone", bid3.getOwner());
            assertQEquals(80, bid3.getPrice());
            tx3.commit();
            HibernateTestUtil.closeSession();
        }

        assertQEquals(50, bid1.getPrice());
        assertQEquals(80, bid2.getPrice());
        assertQEquals(80, bid3.getPrice());
        manualTearDown();
    }

    private void setPrice(PriceAction b, double val) {
        b.setPrice(Price.dollarPrice(val));
    }

    public void testPersistentTrade() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        Trade trade1;
        {
            trade1 = new BookTrade();
            initializeActionAndAssertOwnerName(trade1, "someone", 50, 20);
            HibernateTestUtil.closeSession();
        }

        Trade trade2;
        {
            Session newSession = HibernateTestUtil.currentSession();
            Transaction tx = newSession.beginTransaction();
            trade2 = (Trade) newSession.get(Trade.class, new Long(1));
            assertEquals("someone", trade2.getOwner());
            setPrice(trade2, 80);
            newSession.update(trade2);
            tx.commit();
            HibernateTestUtil.closeSession();
        }

        Trade trade3;
        {
            Session session3 = HibernateTestUtil.currentSession();
            Transaction tx3 = session3.beginTransaction();
            trade3 = (Trade) session3.get(Trade.class, new Long(1));
            assertEquals("someone", trade3.getOwner());
            assertQEquals(80, trade3.getPrice());
            tx3.commit();
            HibernateTestUtil.closeSession();
        }

        assertQEquals(50, trade1.getPrice());
        assertQEquals(80, trade2.getPrice());
        assertQEquals(80, trade3.getPrice());
        manualTearDown();
    }

    public void testPersistentOrderRemovals() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        OrderRemoval removal1;
        SelfDealing selfDeal1;
        {
            removal1 = new OrderRemoval();
            initializeActionAndAssertOwnerName(removal1, "someone", 50, 20);

            selfDeal1 = new SelfDealing();
            initializeActionAndAssertOwnerName(selfDeal1, "someone", 50, 20);

            HibernateTestUtil.closeSession();
        }

        OrderRemoval removal2;
        SelfDealing selfDeal2;
        {
            Session newSession = HibernateTestUtil.currentSession();
            Transaction tx = newSession.beginTransaction();

            removal2 = (OrderRemoval) newSession.get(OrderRemoval.class, new Long(1));
            assertEquals("someone", removal2.getOwner());
            setPrice(removal2, 80);
            newSession.update(removal2);

            selfDeal2 = (SelfDealing) newSession.get(SelfDealing.class, new Long(1));
            assertEquals("someone", selfDeal2.getOwner());
            setQuantity(selfDeal2, 30);
            newSession.update(selfDeal2);

            tx.commit();
            HibernateTestUtil.closeSession();
        }

        OrderRemoval removal3;
        SelfDealing selfDeal3;

        {
            Session session3 = HibernateTestUtil.currentSession();
            Transaction tx3 = session3.beginTransaction();

            removal3 = (OrderRemoval) session3.get(OrderRemoval.class, new Long(1));
            assertEquals("someone", removal3.getOwner());
            assertQEquals(80, removal3.getPrice());

            selfDeal3 = (SelfDealing) session3.get(SelfDealing.class, new Long(1));
            assertEquals("someone", selfDeal3.getOwner());
            assertQEquals(30, selfDeal3.getQuantity());

            tx3.commit();
            HibernateTestUtil.closeSession();
        }

        assertQEquals(50, removal1.getPrice());
        assertQEquals(80, removal2.getPrice());
        assertQEquals(80, removal3.getPrice());

        assertQEquals(50, selfDeal1.getPrice());
        assertQEquals(30, selfDeal2.getQuantity());
        assertQEquals(30, selfDeal3.getQuantity());
        manualTearDown();
    }

    private void setQuantity(PriceAction action, double v) {
        action.setQuantity(q(v));
    }

    private void initializeActionAndAssertOwnerName(PriceAction action, String traderName, double price, int quantity) {
        storeObject(root);
        storeObject(owner);
        storeObject(claim);

        action.setOwner(traderName);
        setPrice(action, price);
        setQuantity(action, quantity);
        action.setPos(claim.getYesPosition());
        storeObject(action);

        assertEquals(traderName, action.getOwner());
    }

    public void testStoreCommitExpungeRestore() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");
        storeObject(root);
        storeObject(owner);
        storeObject(claim);
        Ask ask1;
        Ask ask2;
        Ask ask3;
        Bid bid1;
        Bid bid2;
        Bid bid3;
        Trade trade1;
        Trade trade2;

        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        Quantity q20 = q(20);
        {
            Position yes = claim.lookupPosition("yes");
            ask1 = Ask.newAsk("p1", Price.dollarPrice(70), q20, yes);
            Quantity q10 = q(10);
            bid1 = Bid.newBid("p2", Price.dollarPrice(30), q10, yes);
            ask2 = Ask.newAsk("p3", Price.dollarPrice(53), q20, yes);
            trade1 = BookTrade.newBookTrade("p4", Price.dollarPrice(53), q10, yes);
            bid2 = Bid.newBid("p4", Price.dollarPrice(42), q10, yes);
            ask3 = Ask.newAsk("p3", Price.dollarPrice(65), q20, yes);
            trade2 = BookTrade.newBookTrade("p2", Price.dollarPrice(65), q20, yes);
            bid3 = Bid.newBid("p1", Price.dollarPrice(45), q10, yes);
            storeObject(ask1);
            storeObject(bid1);
            storeObject(ask2);
            storeObject(bid2);
            storeObject(trade1);
            storeObject(ask3);
            storeObject(bid3);
            storeObject(trade2);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        Session session = HibernateTestUtil.currentSession();

        assertEquals("p3", ((Ask) session.get(Ask.class, new Long(3))).getOwner());
        assertQEquals(65, ((Trade) session.get(BookTrade.class, new Long(2))).getPrice());
        assertQEquals(45, ((Bid) session.get(Bid.class, new Long(3))).getPrice());
        assertQEquals(20, ((Ask) session.get(Ask.class, new Long(2))).getQuantity());
        assertQEquals(42, ((Bid) session.get(Bid.class, new Long(2))).getPrice());
        assertQEquals(70, ((Ask) session.get(Ask.class, new Long(1))).getPrice());
        assertQEquals(30, ((Bid) session.get(Bid.class, new Long(1))).getPrice());
        manualTearDown();
    }

    public void testPersistentBestBidAsk() throws Exception {
        manualSetUpForCreate("data/PersistentActionTest");

        String buyer = "buyer";
        String seller = "seller";
        BestBid bestBid1 = new BestBid();
        BestAsk bestAsk1 = new BestAsk();
        {
            initializeActionAndAssertOwnerName(bestBid1, buyer, 50, 20);
            initializeActionAndAssertOwnerName(bestAsk1, seller, 70, 20);

            assertEquals(buyer, bestBid1.getOwner());
            assertEquals(seller, bestAsk1.getOwner());

            HibernateTestUtil.closeSession();
        }

        BestBid bestBid2;
        BestAsk bestAsk2;
        {
            Session newSession = HibernateTestUtil.currentSession();
            Transaction tx = newSession.beginTransaction();
            bestBid2 = (BestBid) newSession.get(BestBid.class, new Long(1));
            bestAsk2 = (BestAsk) newSession.get(BestAsk.class, new Long(1));
            assertEquals("buyer", bestBid2.getOwner());
            assertEquals("seller", bestAsk2.getOwner());
            setPrice(bestBid2, 40);
            setPrice(bestAsk2, 80);
            newSession.update(bestBid2);
            newSession.update(bestAsk2);
            tx.commit();
            HibernateTestUtil.closeSession();
        }

        BestBid bestBid3;
        BestAsk bestAsk3;
        {
            Session session3 = HibernateTestUtil.currentSession();
            Transaction tx3 = session3.beginTransaction();
            bestBid3 = (BestBid) session3.get(BestBid.class, new Long(1));
            bestAsk3 = (BestAsk) session3.get(BestAsk.class, new Long(1));
            assertEquals("buyer", bestBid3.getOwner());
            assertEquals("seller", bestAsk3.getOwner());
            assertQEquals(40, bestBid3.getPrice());
            assertQEquals(80, bestAsk3.getPrice());
            tx3.commit();
            HibernateTestUtil.closeSession();
        }

        assertQEquals(50, bestBid1.getPrice());
        assertQEquals(70, bestAsk1.getPrice());
        assertQEquals(40, bestBid2.getPrice());
        assertQEquals(80, bestAsk2.getPrice());
        assertQEquals(40, bestBid3.getPrice());
        assertQEquals(80, bestAsk3.getPrice());

        manualTearDown();
    }
}
