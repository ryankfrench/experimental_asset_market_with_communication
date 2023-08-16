package net.commerce.zocalo.orders;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junitx.framework.ArrayAssert;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.IncompatibleOrderException;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.JspSupport.MarketDisplay;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.claim.Position;

public class OrdersTest extends OrdersTestCase {
    public void testAddOrders() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "37", 20, makeUser("someone"));
        addOrder(book, yes, "37", 50, makeUser("anyone"));
        assertEquals(0, new Quantity(70.0).compareTo(yesOrders.getQuantityAtPrice(yesOrders.bestPrice())));
    }

    public void testCleanUpDuplicateOrders() throws DuplicateOrderException, IncompatibleOrderException {
        User someone = makeUser("someone");
        addOrder(book, yes, "37", 20, someone);
        try {
            addOrder(book, yes, "37", 20, someone);
            fail("duplicate Order shouldn't be accepted");
        } catch (DuplicateOrderException e) {
            assertTrue(someone.getOrders().size() == 1);
        }
        assertEquals(0, new Quantity(20.0).compareTo(yesOrders.getQuantityAtPrice(yesOrders.bestPrice())));
    }

    public void testOrderSort() {
        User george = makeUser("george");
        User sam = makeUser("sam");
        Order buyLow = makeNewOrder(yes, "30", 20, george);
        Order buyHigh = makeNewOrder(yes, "40", 15, sam);
        assertEquals(1, buyHigh.compareTo(buyLow));

        Order sellLow = makeNewOrder(no, "30", 20, george);
        Order sellHigh = makeNewOrder(no, "40", 15, sam);
        assertEquals(1, sellHigh.compareTo(sellLow));
    }

    private Order makeNewOrder(Position pos, String price, int quantity, User u) {
        return new Order(pos, Price.dollarPrice(price), new Quantity(quantity), u);
    }

    public void testViewOrders() throws DuplicateOrderException, IncompatibleOrderException {
        assertFalse(yesOrders.hasOrders());
        assertQEquals(0.0, yesOrders.bestPrice());
        addOrder(book, yes, "15", 40, makeUser("One"));
        assertQEquals(15, yesOrders.bestPrice());
        assertTrue(yesOrders.hasOrders());
        addOrder(book, yes, "20", 30, makeUser("Two"));
        assertQEquals(20, yesOrders.bestPrice());
        addOrder(book, yes, "35", 50, makeUser("Three"));
        assertQEquals(35, yesOrders.bestPrice());
        addOrder(book, yes, "54", 50, makeUser("Four"));
        assertQEquals(54, yesOrders.bestPrice());

        assertQEquals(0.0, noOrders.bestPrice());
        addOrder(book, no, "28", 35, makeUser("Five"));
        assertQEquals(28, noOrders.bestPrice());
        addOrder(book, no, "30", 35, makeUser("Six"));

        assertQEquals(54, yesOrders.bestPrice());
        assertQEquals(30, noOrders.bestPrice());
    }

    public void testVerifyPriceLimits() {
        assertQEquals(0, yesOrders.bestPrice());
    }

    public void testDequeuingOrders() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "15", 40, makeUser("One"));
        addOrder(book, yes, "20", 30, makeUser("Two"));
        addOrder(book, yes, "35", 50, makeUser("Three"));
        addOrder(book, yes, "54", 50, makeUser("Four"));
        addOrder(book, no, "28", 35, makeUser("Five"));
        addOrder(book, no, "30", 35, makeUser("Six"));

        Order bestBuy = yesOrders.getBest();
        assertQEquals(54, bestBuy.price());

        bestBuy.setQuantity(new Quantity(20));

        bestBuy = yesOrders.getBest();
        assertQEquals(20, bestBuy.quantity());

        yesOrders.remove(bestBuy);
        bestBuy = yesOrders.getBest();
        assertQEquals(35, bestBuy.price());
        assertQEquals(50, bestBuy.quantity());

        bestBuy = yesOrders.getBest();
        yesOrders.remove(yesOrders.getBest());

        bestBuy = yesOrders.getBest();
        assertQEquals(20, bestBuy.price());

        Order bestSell = noOrders.getBest();
        assertQEquals(30, bestSell.price());
        assertQEquals(35, bestSell.quantity());
    }

    public void testGettingEmptyOrders() throws DuplicateOrderException, IncompatibleOrderException {
        addOrder(book, yes, "37", 0, makeUser("someone"));
        assertFalse(yesOrders.iterator().hasNext());


        Order fullfilledOrder = addOrder(book, yes, "37", 20, makeUser("someone"));
        fullfilledOrder.reduceQuantityDueToSale(new Quantity(20));
        assertFalse(yesOrders.iterator().hasNext());
    }

    public void testBestN() {
        ArrayAssert.assertEquals( new Order[] { }, MarketDisplay.bestOrders(3, false, new Order[] { } ));
        ArrayAssert.assertEquals( new Order[] { }, MarketDisplay.bestOrders(3, true, new Order[] { } ));

        User userA = makeUser("'A'");
        Order orderA = makeNewOrder(yes, "37", 20, userA);
        Order[] oneOrder = new Order[] { orderA };
        ArrayAssert.assertEquals( new Order[] { orderA }, MarketDisplay.bestOrders(3, true, oneOrder));
        ArrayAssert.assertEquals( new Order[] { orderA }, MarketDisplay.bestOrders(3, false, oneOrder));

        User userB = makeUser("'B'");
        Order orderB = makeNewOrder(no, "64", 10, userB);
        Order[] twoOrders = new Order[] { orderA, orderB };
        ArrayAssert.assertEquals( new Order[] { orderB, orderA }, MarketDisplay.bestOrders(3, true, twoOrders));
        ArrayAssert.assertEquals( new Order[] { orderB, orderA }, MarketDisplay.bestOrders(3, false, twoOrders));

        User userC = makeUser("'C'");
        Order orderC = makeNewOrder(yes, "34", 5, userC);
        Order[] fiveOrders = new Order[] { orderA, orderB, orderB, orderA, orderC };
        ArrayAssert.assertEquals( new Order[] { orderC, orderA, orderB }, MarketDisplay.bestOrders(3, true, fiveOrders));
        ArrayAssert.assertEquals( new Order[] { orderB, orderB, orderA }, MarketDisplay.bestOrders(3, false, fiveOrders));
    }
}
