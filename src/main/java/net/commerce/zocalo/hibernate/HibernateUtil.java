package net.commerce.zocalo.hibernate;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.CurrencyToken;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.UnconfirmedUser;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;

import java.sql.*;
import java.util.List;
import java.util.Iterator;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**
Utilities for dealing with Hibernate.  Everything from general tools for creating and
managing connections to specific queries of general use.
 */

public class HibernateUtil {
    protected static SessionFactory sessionFactory = new NoDBSessionFactory();;
    protected static final ThreadLocal sessionHolder = new ThreadLocal();
    static public final String SCHEMA_CREATE = "create";
    static public final String SCHEMA_CREATE_DROP = "create-drop";
    static public final String SCHEMA_UPDATE = "update";

    static public void initializeSessionFactory(String connectionURL, String schemaCreateMode) {
        if (sessionFactory != null && ! (sessionFactory instanceof NoDBSessionFactory)) {
            return;
        }
        try {
            Configuration cfg = new Configuration();
            addClasses(cfg);
            if (connectionURL != null && ! "".equals(connectionURL)) {
                cfg.setProperty("hibernate.connection.url", connectionURL);
            }
            cfg.setProperty("hibernate.hbm2ddl.auto", schemaCreateMode);

            sessionFactory = cfg.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    static public String connectionUrl(String dbFilePath, boolean create) {
        // edit hibernate.properties when you switch this
        return hsqlFileUrl(dbFilePath);
//        return derbyUrl(dbFilePath, create);
    }

    static public String derbyUrl(String dbFilePath, boolean create) {
        return "jdbc:derby:" + dbFilePath + (create ? ";create=true" : "");
    }

    static public String hsqlFileUrl(String dbFilePath) {
        return "jdbc:hsqldb:file:" + dbFilePath;
    }

    public static Session currentSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        if (sessionFactory != null) {
            if (s == null || ! s.isOpen()) {
                s = sessionFactory.openSession();
                sessionHolder.set(s);
            }
        }
        return s;
    }

    public static void closeSession() throws HibernateException {
        Session s = (Session) sessionHolder.get();
        if (s != null)
            s.close();
        sessionHolder.set(null);
    }

    static void addClasses(Configuration configuration) {
        configuration.addClass(net.commerce.zocalo.claim.Claim.class);
        configuration.addClass(net.commerce.zocalo.claim.Position.class);
        configuration.addClass(net.commerce.zocalo.currency.CurrencyToken.class);
        configuration.addClass(net.commerce.zocalo.currency.Coupons.class);
        configuration.addClass(net.commerce.zocalo.currency.CashBank.class);
        configuration.addClass(net.commerce.zocalo.currency.CouponBank.class);
        configuration.addClass(net.commerce.zocalo.currency.Funds.class);
//        configuration.addClass(net.commerce.zocalo.currency.Currency.class); // Using Table-per-concrete-subclass for Currency/Funds/Coupons
        configuration.addClass(net.commerce.zocalo.currency.Accounts.class);
        configuration.addClass(net.commerce.zocalo.orders.Order.class);
        configuration.addClass(net.commerce.zocalo.orders.SortedOrders.class);
        configuration.addClass(net.commerce.zocalo.user.SecureUser.class);
        configuration.addClass(net.commerce.zocalo.user.UnconfirmedUser.class);
        configuration.addClass(net.commerce.zocalo.market.Market.class);
        configuration.addClass(net.commerce.zocalo.market.MarketMaker.class);
        configuration.addClass(net.commerce.zocalo.market.Book.class);
        configuration.addClass(net.commerce.zocalo.market.Outcome.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.Ask.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.Bid.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.BestAsk.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.BestBid.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.Trade.class);  // using table-per-hierarchy for BookTrade/MakerTrade
        configuration.addClass(net.commerce.zocalo.ajax.events.SelfDealing.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.OrderRemoval.class);
        configuration.addClass(net.commerce.zocalo.ajax.events.Redemption.class);
//        configuration.addClass(net.commerce.zocalo.currency.Quantity.class);        // Always a component
//        configuration.addClass(net.commerce.zocalo.currency.Probability.class);     // Always a component
//        configuration.addClass(net.commerce.zocalo.currency.Price.class);           // Always a component
    }

    public static void save(Object o) {
        Session session = currentSession();
        if (session != null) {
            session.save(o);
        }
    }

    public static void delete(Object o) {
        Session session = currentSession();
        if (session != null) {
            session.delete(o);
        }
    }

    static public CashBank getOrMakePersistentRootBank(String rootCashBankName) throws HibernateException {
        CashBank result;
        Session session = currentSession();
        Criteria tokenCriterion = session.createCriteria(CurrencyToken.class);
        tokenCriterion.add(Expression.eq("name", rootCashBankName));
        List tokens = tokenCriterion.list();
        if (tokens.size() > 1) {
            throw new HibernateException("too many root banks");
        } else if (tokens.size() == 1) {
            CurrencyToken t = (CurrencyToken) tokens.get(0);
            result = t.lookupCashBank(session);
        } else {
            result = new CashBank(rootCashBankName);
            session.save(result);
        }
        return result;
    }

    public static SecureUser getUserByName(String name) {
        return getUserByName(name, currentSession());
    }

    public static SecureUser getUserByName(String name, Session session) {
        Criteria userCriterion = session.createCriteria(SecureUser.class);
        userCriterion.add(Expression.eq("name", name));
        userCriterion.setCacheable(true);
        return (SecureUser) userCriterion.uniqueResult();
    }

    public static UnconfirmedUser getUnconfirmedUserByName(String name, Session session) {
        Criteria userCriterion = session.createCriteria(UnconfirmedUser.class);
        userCriterion.add(Expression.eq("name", name));
        return (UnconfirmedUser) userCriterion.uniqueResult();
    }

    public static void removeUnconfirmedUserByName(String name, Session session) {
        Criteria userCriterion = session.createCriteria(UnconfirmedUser.class);
        userCriterion.add(Expression.eq("name", name));
        UnconfirmedUser user = (UnconfirmedUser) userCriterion.uniqueResult();
        session.delete(user);
        return;
    }

    public static boolean marketsExist() {
        Criteria marketCriterion = currentSession().createCriteria(Market.class);
        marketCriterion.setMaxResults(1);
        return marketCriterion.list().size() > 0;
    }

    public static List allOpenBinaryMarkets() {
        Criteria marketCriterion = currentSession().createCriteria(BinaryMarket.class);
        marketCriterion.add(Expression.eq("marketClosed", Boolean.FALSE));
        marketCriterion.setCacheable(true);
        return marketCriterion.list();
    }

    public static List allOpenMultiMarkets() {
        Criteria marketCriterion = currentSession().createCriteria(MultiMarket.class);
        marketCriterion.add(Expression.eq("marketClosed", Boolean.FALSE));
        marketCriterion.setCacheable(true);
        return marketCriterion.list();
    }

    public static List allClosedMarkets() {
        Criteria marketCriterion = currentSession().createCriteria(Market.class);
        marketCriterion.add(Expression.eq("marketClosed", Boolean.TRUE));
        marketCriterion.addOrder(Order.desc("id"));  // we don't have the time of closure
        marketCriterion.setCacheable(true);
        return marketCriterion.list();
    }

    public static List tradeListForJsp(String claimName) {
        Claim claim = getClaimByName(claimName);
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select t from Trade t " +
                    "join t.pos p " +
                        "where t.pos = p.id and p.claim = :cl " +
                        "order by t.time");
        mQ.setEntity("cl", claim);
        mQ.setCacheable(true);
        return mQ.list();
    }

    public static List maxTradeTime(String claimName) {
        Claim claim = getClaimByName(claimName);
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select max(t.time) from Trade t " +
                    "join t.pos p " +
                        "where t.pos = p.id and p.claim = :cl ");
        mQ.setEntity("cl", claim);
        mQ.setCacheable(true);
        return mQ.list();
    }

    public static Market getMarketByName(String name) {
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select m from Market m " +
                    "join m.claim c " +
                    "where m.claim = c.id and c.name = :name");
        mQ.setString("name", name);
        mQ.setCacheable(true);
        return (Market) mQ.uniqueResult();
    }

    public static MultiMarket getMultiMarketByName(String name) {
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select m from MultiMarket m " +
                    "join m.claim c " +
                    "where m.claim = c.id and c.name = :name");
        mQ.setString("name", name);
        mQ.setCacheable(true);
        return (MultiMarket) mQ.uniqueResult();
    }

    public static BinaryClaim getBinaryClaimByName(String name) {
        Session session = HibernateUtil.currentSession();
        Criteria claimCriterion = session.createCriteria(BinaryClaim.class);
        claimCriterion.add(Expression.eq("name", name));
        claimCriterion.setCacheable(true);
        return (BinaryClaim) claimCriterion.uniqueResult();
    }

    public static Claim getClaimByName(String name) {
        Session session = HibernateUtil.currentSession();
        Criteria claimCriterion = session.createCriteria(Claim.class);
        claimCriterion.add(Expression.eq("name", name));
        claimCriterion.setCacheable(true);
        return (Claim) claimCriterion.uniqueResult();
    }

    public static List couponOwners(Claim claim) {
        Session session = currentSession();

        //TODO: I can't see how to access the Claims that are the keys of acct's positions map.
        //TODO: that would require two fewer joins!
            //select  owner.user_id" +
            //from public.USER owner, public.accounts acct, PUBLIC.ACCOUNT_HOLDINGS assets, PUBLIC.POSITIONS pos " +
            //  where  owner.accounts = acct.accounts_id" +
            //         and acct.accounts_id = assets.accounts_id"
            //         and assets.POSITIONS_ID = pos.positions_id " +
            //         and pos.claims_ID=2 " +

        Query qa = session.createQuery("select u \n" +
                "from SecureUser u \n" +
                "   join u.accounts acct\n" +
                "   join acct.positions coupon  \n" +
                "where u.accounts = acct.id and coupon.position.claim = :claim ");
        qa.setEntity("claim", claim);
        qa.setCacheable(true);
        return qa.list();
    }

    public static void refresh(Object o) {
        currentSession().refresh(o);
    }

    public static Transaction beginTransactionForJsp() {
        Session session = currentSession();
        return session.beginTransaction();
    }

    public static List getOrdersForUser(User user) {
        Query query = currentSession().createQuery(
                "select o from Order o " +
                    "join o.owner u " +
                    "where o.owner = u.id and u.name = :name");
        query.setString("name", user.getName());
        query.setCacheable(true);
        return query.list();
    }

    static public List getTrades(SecureUser user) {
        Query query = currentSession().createQuery(
                "select t from Trade t " +
                    "join t.pos p " +
                    "join p.claim cl " +
                "where t.pos = p.id and t.owner = :name " +
                   "order by cl.id, t.time"
           );
        query.setString("name", user.getName());
        query.setCacheable(true);
        return query.list();
    }

    public static List getTrades(SecureUser user, Market market) {
        Query query = currentSession().createQuery(
                "select t from Trade t " +
                    "join t.pos p " +
                    "join p.claim cl " +
                "where t.pos = p.id and t.owner = :name and cl.name = :claimName " +
                   "order by t.time desc"
           );
        query.setString("name", user.getName());
        query.setString("claimName", market.getName());
        query.setCacheable(true);
        return query.list();
    }

    public static List getRedemptions(SecureUser user, Market market) {
        Query query = currentSession().createQuery(
                "select r from Redemption r " +
                    "join r.pos p " +
                    "join p.claim cl " +
                "where r.pos = p.id and r.owner = :name and cl.name = :claimName "
           );
        query.setString("name", user.getName());
        query.setCacheable(true);
        query.setString("claimName", market.getName());
        query.setCacheable(true);
        return query.list();
    }

    static public void shutdown(String connectionURL) throws SQLException {
        Connection con = DriverManager.getConnection(connectionURL, "sa", "");
        con.createStatement().execute("SHUTDOWN");
        Logger.getLogger(HibernateUtil.class).warn("Hibernate session has been closed");
    }

    /** Upgrade Database to format for 2007.5; Markets store their owners directly. */
    static public void updateDB2007_5(String connectionURL) throws SQLException {
        Connection con = DriverManager.getConnection(connectionURL, "sa", "");
        Statement prepped = con.createStatement();
        prepped.executeUpdate(
                "update PUBLIC.MARKET set market.owner = " +
                        "(select claims.owner from public.claims where claims.claims_id = market.claim)");
        return;
    }

    /** See if Database has been upgraded to format for 2007.5;
            afterwards Markets store their owners directly. */
    public static List getUnowedMarket() {
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select m from Market m where m.owner is null");
        return mQ.list();
    }

    /** See if Database has been upgraded to format for 2008.3;
            afterwards MarketMakers store their Beta. */
    public static List getMakerWithoutBeta() {
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select mm from MarketMaker mm where mm.beta.quant is null");
        return mQ.list();
    }

    /** see if database has been upgraded for 2008.4.  Markets now represent
     * their outcome.  */
     public static List getMarketsSansOutcome() {
        Session session = currentSession();
        Query mQ = session.createQuery(
                "select m from Market m where m.outcome is null");
        return mQ.list();
    }

    /** see if database has been upgraded for 2009.1.  Positions are now ordered.  */
     public static void getPositionSansIndex() {
        List listB = allOpenBinaryMarkets();
        for (Iterator iterator = listB.iterator(); iterator.hasNext();) {
            BinaryMarket market = (BinaryMarket)iterator.next();
            market.getClaim().positions();
        }
        List listM = allOpenMultiMarkets();
        for (Iterator iterator = listM.iterator(); iterator.hasNext();) {
            MultiMarket market = (MultiMarket)iterator.next();
            market.getClaim().positions();
        }
    }

    /** Upgrade Database to format for 2008.3; MarketMakers store Beta, not subsidy.  This method
       sets all Betas to a single (barely) plausible value.  There's a shell script (and python)
     program that uses sqlTool to do a better job.  */
    static public void updateDB2008_3(String connectionURL) throws SQLException {
        Connection con = DriverManager.getConnection(connectionURL, "sa", "");

        Session session = currentSession();
        Query query = session.createQuery("select c from Claim c");
        List claims = query.list();

        Query confirmation = currentSession().createQuery("select mm.beta " +
                "from MarketMaker mm \n" +
                "        join mm.market m" +
                "        join m.claim c" +
                "     where mm.market = m \n" +
                "     and m.claim = c \n" +
                "     and c.name = :name");

        for (Iterator it = claims.iterator(); it.hasNext();) {
            Claim claim = (Claim) it.next();

            confirmation.setString("name", claim.getName());
            if ((confirmation.uniqueResult() == null)) {
                Statement update = con.createStatement();
                double newBeta = 100.0 / Math.log(3);
                try {
                    update.executeUpdate("update MarketMaker set MarketMaker.beta = " + newBeta);

                    confirmation.setString("name", claim.getName());
                    if (newBeta != ((Double)confirmation.uniqueResult()).doubleValue()) {
                        throw new RuntimeException("didn't update beta for " + claim.getName());
                    }
                    break;
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
    }

    public static void updateDB2008_4(String dbFileURL) {
        Session session = currentSession();
        Transaction transaction = session.beginTransaction();

        Outcome open = Outcome.newOpen(false);
        session.save(open);
        List markets = HibernateUtil.getMarketsSansOutcome();
        for (Iterator iterator = markets.iterator(); iterator.hasNext();) {
            Market market = (Market)iterator.next();
            market.setOutcome(open);
        }
        transaction.commit();
    }

    public static boolean isStatisticsEnabled() {
        return currentSession().getSessionFactory().getStatistics().isStatisticsEnabled();
    }
}
