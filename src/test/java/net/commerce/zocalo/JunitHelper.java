package net.commerce.zocalo;

import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequestWrapper;

import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.history.CostAccounter;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.ajax.events.BookTrade;
import net.commerce.zocalo.ajax.events.Bid;
import net.commerce.zocalo.ajax.events.Ask;
import junitx.framework.ArrayAssert;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class JunitHelper extends TestCase {
    static public void assertNoLoginTokens(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            assertNull(cookies[i].getValue());
        }
    }

    static public void assertAdminTokenOnly(HttpServletRequestWrapper request) {
        if (1 == request.getCookies().length) {
            assertEquals(Registry.ADMIN_TOKEN, request.getCookies()[0].getName());
        } else {
            boolean adminTokenFound = false;
            Cookie[] cookies = request.getCookies();
            for (int i = 0; i < cookies.length; i++) {
                if (request.getCookies()[i].getName().equals(Registry.ADMIN_TOKEN)) {
                    adminTokenFound = true;
                } else {
                    assertNull("null registration token expected", cookies[i].getValue());
                }
            }
            assertTrue(adminTokenFound);
        }
    }

    static public void assertAdminAndUserTokens(HttpServletRequestWrapper request) {
        assertEquals(2, request.getCookies().length);
        ArrayAssert.assertEquivalenceArrays(
                new String[] { Registry.ADMIN_TOKEN, Registry.REGISTRATION },
                new String[] { request.getCookies()[0].getName(), request.getCookies()[1].getName() });
        assertNotNull(request.getCookies()[0].getValue());
        assertNotNull(request.getCookies()[1].getValue());
    }

    static public void assertUserTokenOnly(HttpServletRequestWrapper request) {
        if (1 == request.getCookies().length) {
            assertEquals(Registry.REGISTRATION, request.getCookies()[0].getName());
        } else {
            assertEquals(2, request.getCookies().length);
            if (request.getCookies()[0].getName().equals(Registry.ADMIN_TOKEN)) {
                assertNull(request.getCookies()[0].getValue());
                assertEquals(Registry.REGISTRATION, request.getCookies()[1].getName());
                assertNotNull(request.getCookies()[1].getValue());
            } else if (request.getCookies()[1].getName().equals(Registry.ADMIN_TOKEN)) {
                assertEquals(Registry.REGISTRATION, request.getCookies()[0].getName());
                assertNotNull(request.getCookies()[0].getValue());
                assertNull(request.getCookies()[1].getValue());
            } else {
                assertFalse("expected one Registration Token and one Null Admin Token", true);
            }
        }
    }

    static public void assertREMatches(String expected, String result) {
        Pattern exp = Pattern.compile(expected, Pattern.DOTALL);
        assertTrue("'" + expected + "'" + " doesn't match: '" + result + "'", exp.matcher(result).matches());
    }

    static public void assertRENoMatch(String expected, String result) {
        Pattern exp = Pattern.compile(expected, Pattern.DOTALL);
        assertFalse("'" + expected + "'" + " shouldn't match: '" + result, exp.matcher(result).matches());
    }

    static public void assertMatches(String pattern, String string) {
        assertTrue("'" + pattern + "' doesn't match '" + string + "'", string.indexOf(pattern) >= 0);
    }

    static public void assertBinaryTradesBalance(BinaryMarket market, SecureUser user, double start) {
        Position yes = market.getBinaryClaim().getYesPosition();
        String YES = "yes";
        String NO = "no";

        final CostAccounter accounter = new CostAccounter(user, market);
        final Quantity yesQ = accounter.getQuantity(YES);
        final Quantity noQ = accounter.getQuantity(NO);
        assertQEquals(yesQ.minus(noQ), user.couponCount(yes.getClaim()));
        final Quantity redeemValue = accounter.getRedemptionValue(YES).plus(accounter.getRedemptionValue(NO));
        assertCostsBalance(user, market, q(start).plus(redeemValue), accounter);
    }

    static public void assertBinaryTradesBalance(BinaryMarket market, SecureUser user, Quantity start) {
        Position yes = market.getBinaryClaim().getYesPosition();
        String YES = "yes";
        String NO = "no";

        final CostAccounter accounter = new CostAccounter(user, market);
        final Quantity yesQ = accounter.getQuantity(YES);
        final Quantity noQ = accounter.getQuantity(NO);
        assertQEquals(yesQ.minus(noQ), user.couponCount(yes.getClaim()));
        final Quantity redeemValue = accounter.getRedemptionValue(YES).plus(accounter.getRedemptionValue(NO));
        assertCostsBalance(user, market, start.plus(redeemValue), accounter);
    }

    static public void assertBalancedTrades(SecureUser user, Market market, double startCash) {
        CostAccounter accts = new CostAccounter(user, market);
        final Position[] positions = market.getClaim().positions();
        Quantity balance = q(startCash);
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Quantity quantity = accts.getQuantity(position.getName());
            Quantity cost = accts.getCost(position.getName()).minus(accts.getRedemptionValue(position.getName()));
            Quantity coupons = user.couponCount(position);
            String comment = position.getName();
            assertQEquals(comment, quantity, coupons);
            balance = balance.minus(cost);
        }
        assertQEquals(balance, user.cashOnHand());
    }

    static public void assertCostsBalance(SecureUser user, Market market, Quantity startCash, CostAccounter accts) {
        final Position[] positions = market.getClaim().positions();
        Quantity balance = startCash;
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Quantity cost = accts.getCost(position.getName());
            balance = balance.minus(cost);
        }
        assertQEquals(balance, user.cashOnHand());
    }

    static public Quantity q(double quant) {
        return new Quantity(quant);
    }

    static public void assertQEquals(Quantity first, Quantity second) {
        assertTrue("expected " + first.printAsDetailedQuantity() + " but saw " + second, first.compareTo(second) == 0);
    }

    static public void assertQNotEquals(Quantity first, Quantity second) {
        assertTrue(first + " should not be equal to " + second, first.compareTo(second) != 0);
    }

    static public void assertQEquals(double first, Quantity second) {
        assertQEquals(q(first), second);
    }

    static public void assertQIsClose(double first, Quantity second, double variance) {
        assertTrue(first + " should be close to " + second, q(first + variance).compareTo(second) >= 0
                    && q(first - variance).compareTo(second) <= 0 );
    }

    static public void assertQApproaches(Quantity first, Quantity second) {
        assertTrue(first + " should be close to " + second, first.round().compareTo(second.round()) == 0);
    }

    static public void assertQEquals(String comment, Quantity first, Quantity second) {
        assertEquals(comment, 0, first.compareTo(second));
    }

    public void marketOrder(Market market, Position pos, String price, double quantity, User user) {
        Price p = Price.dollarPrice(price);
        marketOrder(market, pos, p, quantity, user);
    }

    public void marketOrder(Market market, Position pos, int price, double quantity, User user) {
        Price p = Price.dollarPrice(price);
        marketOrder(market, pos, p, quantity, user);
    }

    public void marketOrder(Market market, Position pos, Price price, double quantity, User user) {
        market.marketOrder(pos, price, q(quantity), user);
    }

    public void limitOrder(Market m, Position pos, String price, double quantity, User user) throws DuplicateOrderException {
        m.limitOrder(pos, Price.dollarPrice(price), q(quantity), user);
    }

    public void limitOrder(Market m, Position pos, int price, double quantity, User user) throws DuplicateOrderException {
        m.limitOrder(pos, Price.dollarPrice(price), q(quantity), user);
    }

    public void limitOrder(Market m, Position pos, Price price, double quantity, User buyer) throws DuplicateOrderException {
        m.limitOrder(pos, price, q(quantity), buyer);
    }

    public Order addOrder(Book b, Position pos, String price, double quant, User user) throws DuplicateOrderException, IncompatibleOrderException {
        return b.addOrder(pos, Price.dollarPrice(price), q(quant), user);
    }

    public Quantity buyFromBookOrders(Book b, Position po, String pr, double qu, User u) {
        return b.buyFromBookOrders(po, Price.dollarPrice(pr), q(qu), u);
    }

    static public BookTrade makeNewBookTrade(String user, String price, double quantity, Position pos) {
        return BookTrade.newBookTrade(user, Price.dollarPrice(price), q(quantity), pos);
    }

    static public BookTrade makeNewBookTrade(String user, String price, double quantity, Position pos, Logger logger) {
        return new BookTrade(user, Price.dollarPrice(price), q(quantity), pos, logger);
    }

    static public Bid makeNewBid(String user, String price, int quantity, Position pos) {
        return Bid.newBid(user, Price.dollarPrice(price), q(quantity), pos);
    }

    static public Bid makeNewBid(String user, String price, double quantity, Position pos, Logger logger) {
        return new Bid(user, Price.dollarPrice(price), q(quantity), pos, logger);
    }

    static public Ask makeNewAsk(String owner, String price, double quantity, Position pos) {
        return Ask.newAsk(owner, Price.dollarPrice(price), q(quantity), pos);
    }

    static public Ask makeNewAsk(String owner, String price, double quantity, Position pos, Logger logger) {
        return new Ask(owner, Price.dollarPrice(price), q(quantity), pos, logger);
    }

    static public void buyUpToQuantity(MarketMaker maker, Position pos, String probability, double quant, User user) {
        maker.buyUpToQuantity(pos, new Probability(probability), q(quant), user);
    }

    public void testSuppressWarning() {  // suppresses junit's warning about a subclass of TestCase without any tests
        assertEquals(q(100), Quantity.Q100);
    }
}
