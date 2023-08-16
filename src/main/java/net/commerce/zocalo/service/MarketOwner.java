package net.commerce.zocalo.service;

import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.UnconfirmedUser;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.MultiMarket;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.ajax.events.Transition;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.apache.log4j.Logger;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Manage ownership of Markets and Users for general Prediction Markets */
public class MarketOwner {
    final static public String ROOT_CASH_BANK_NAME     = "rootCashBank";
    final static public String DEFAULT_CLAIM_DESCRIPTION = "no description yet";
    final static public Registry knownUsers = new Registry();

    public static String registerAdminToken() {
        String token = Registry.passwdGen.nextToken(8);
        knownUsers.addAdminToken(token);
        return token;
    }

    static public SecureUser registryLookup(HttpServletRequest request) {
        return knownUsers.lookupUser(request);
    }

    static public Market getMarket(String marketName) {
        return HibernateUtil.getMarketByName(marketName);
    }

    static public Cookie login(String userName, String password) throws HibernateException {
        SecureUser user = HibernateUtil.getUserByName(userName, HibernateUtil.currentSession());
        if (user != null && user.verifyPassword(password)) {
            return knownUsers.register(user);
        }
        return null;
    }

    static public Cookie createUser(String userName, String password, String emailAddress) {
        int funding = Config.getInitialUserFunds();
        Session session = HibernateUtil.currentSession();
        SecureUser user = createUser(userName, funding, password, emailAddress, session);
        return knownUsers.register(user);
    }

    static public SecureUser createUser(String userName, int funds, String password, String email, Session session) {
        CashBank rootBank = getBankFromDBOrCreateIt();
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(funds), password, email);
        session.save(user);
        return user;
    }

    static public void newMarket(String marketName, String userName) {
        SecureUser user = HibernateUtil.getUserByName(userName);
        newBinaryMarket(marketName, user);
    }

    static public BinaryMarket newBinaryMarket(String marketName, SecureUser user) {
        return newBinaryMarket(marketName, user, Quantity.Q100, 2);
    }

    static public BinaryMarket newBinaryMarket(String name, SecureUser user, Quantity maxPrice, int scale) {
        BinaryClaim claim = BinaryClaim.makeClaim(name, user, DEFAULT_CLAIM_DESCRIPTION);
        HibernateUtil.save(claim);

        CashBank rootBank = getBankFromDBOrCreateIt();
        BinaryMarket market = BinaryMarket.make(claim, user, rootBank.noFunds(), maxPrice, scale);
        HibernateUtil.save(market);
        new Transition("new Binary Market", name);
        return market;
    }

    static public MultiMarket newMultiMarket(String marketName, SecureUser user, String[] positions) {
        MultiClaim claim = MultiClaim.makeClaim(marketName, user, DEFAULT_CLAIM_DESCRIPTION, positions);
        HibernateUtil.save(claim);
        CashBank rootBank = getBankFromDBOrCreateIt();
        MultiMarket market = MultiMarket.make(user, claim, rootBank.noFunds());
        HibernateUtil.save(market);
        new Transition("new Multi Market", marketName);
        return market;
    }

    static public boolean marketsExist() {
        return HibernateUtil.marketsExist();
    }

    static public SecureUser getUser(String userName) {
        return HibernateUtil.getUserByName(userName);
    }

    static public SecureUser registryLookup(Cookie cookie) {
        return knownUsers.lookupUser(cookie.getValue());
    }

    static public boolean detectAdminCookie(Cookie cookie) {
        String token = cookie.getValue();
        return token != null && Registry.ADMIN_TOKEN.equals(cookie.getName()) && knownUsers.isAdminToken(token);
    }

    static public boolean detectAdminCookie(HttpServletRequest request) {
        return null != adminCookieIfPresent(request);
    }

    static public Cookie adminCookieIfPresent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie != null && detectAdminCookie(cookie)) {
                return cookie;
            }
        }
        return null;
    }

    static public CashBank getBankFromDBOrCreateIt() {
        return HibernateUtil.getOrMakePersistentRootBank(ROOT_CASH_BANK_NAME);
    }

    static public UnconfirmedUser createUnconfirmedUserAndNotifyOwner(String userName, String password, String emailAddress, String requestURL) {
        UnconfirmedUser unconfirmedUser = new UnconfirmedUser(userName, password, emailAddress);
        try {
            unconfirmedUser.sendEmailNotification(requestURL);
        } catch (Exception e) {
            e.printStackTrace();
            unconfirmedUser.addWarning("unable to send mail; please notify administrator.");
            Logger log = Logger.getLogger(MarketOwner.class);
            log.warn("Unable to send message. Is mail configured correctly?  user: '" + userName + "' email: '" + emailAddress + "'.");
            return null;
        }
        HibernateUtil.currentSession().save(unconfirmedUser);
        knownUsers.register(unconfirmedUser);
        return unconfirmedUser;
    }

    static public void removeUnconfirmed(String name, String token) {
        knownUsers.removeUnconfirmed(token);
        HibernateUtil.removeUnconfirmedUserByName(name, HibernateUtil.currentSession());
    }
}
