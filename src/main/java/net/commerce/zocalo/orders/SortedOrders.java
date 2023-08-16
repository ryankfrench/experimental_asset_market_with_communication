package net.commerce.zocalo.orders;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.user.User;

import java.util.*;

/** A collection of Orders on a single asset.  Used by a Book to manage the orders
    and to find the best order when there is an offer.  */
public class SortedOrders {
    private SortedSet<Order> sortedOrders = new TreeSet<Order>();
    private Position position;
    private Price samplePrice;
    private long id;

    public SortedOrders(Position position, Price price) {
        this.position = position;
        samplePrice = price;
    }

    /** @deprecated */
    SortedOrders() {
    }

    public Iterator iterator() {
        return new OrdersIterator(sortedOrders.iterator());
    }

    public Order getBest() {
        if (sortedOrders.isEmpty()) {
            return null;
        }
        Order order = (Order)sortedOrders.last();
        if (order.quantity().isNegligible()) {
            remove(order);
            return getBest();
        }
        return order;
    }

    public Price bestPrice() {
        if (sortedOrders.isEmpty()) {
            return samplePrice.inverted();
        } else {
            Order order;
            try {
                order = (Order) sortedOrders.last();
            } catch (NoSuchElementException e) {   // looks like a race condition
                return samplePrice.inverted();
            }
            if (order.quantity().isNegligible()) {
                sortedOrders.remove(order);
                return bestPrice();
            }
            return order.price();
        }
    }

    public boolean hasOrders() {
        return iterator().hasNext();
    }

    public void add(Order o) throws DuplicateOrderException {
        if (o.position() != this.position) {
            throw new RuntimeException("wrong Orders for position: " + position.getName());
        }
        Set<Order> removable = new HashSet<Order>();
        for (Iterator iterator = sortedOrders.iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            if (order.quantity().isZero()) {
                removable.add(order);
                continue;
            }
            if (order.compareTo(o) == 0) {
                o.removeFromOwner();
                throw new DuplicateOrderException(o);
            }
        }
        for (Iterator iterator = removable.iterator(); iterator.hasNext();) {
            remove((Order) iterator.next());
        }
        if (! o.quantity().isZero()) {
            sortedOrders.add(o);
        }
    }

    public boolean remove(Order o) {
        boolean retVal = sortedOrders.remove(o);
        if (retVal) {
            o.removeFromOwner();
        }

        return retVal;
    }

    public Quantity getQuantityAtPrice(Price price) {
        Quantity total = Quantity.ZERO;
        for (Order sortedOrder : sortedOrders) {
            if (sortedOrder.price().compareTo(price) == 0) {
                total = total.plus(sortedOrder.quantity());
            }
        }
        return total;
    }

    public Order[] usersOwnOrders(User user, boolean increasingOrder) {
        LinkedList<Order> list = new LinkedList<Order>();
        for (Iterator iterator = sortedOrders.iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            if (order.quantity().isZero()) {
                continue;
            }
            if (user == null || order.userIsOwner(user)) {
                if (increasingOrder) {
                    list.addLast(order);
                } else {
                    list.addFirst(order);
                }
            }
        }
        return (Order[])list.toArray(new Order[0]);
    }

    public void printPrices(StringBuffer buf) {
        boolean printedAtLeastOne = false;
        for (Iterator iterator = sortedOrders.iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            if (order.quantity().isZero()) {
                continue;
            }
            Price price = order.naturalPrice();
            buf.append(price.toString());
            buf.append(" ");
            printedAtLeastOne = true;
        }
        if (! printedAtLeastOne) {
            buf.append("No Offers to display");
        }
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        boolean printedAtLeastOne = false;
        for (Iterator iterator = sortedOrders.iterator(); iterator.hasNext();) {
            Order order = (Order) iterator.next();
            buf.append(order);
            if (iterator.hasNext()) {
                buf.append(", ");
            }
            printedAtLeastOne = true;
        }
        if (! printedAtLeastOne) {
            buf.append("no orders");
        }

        return buf.toString();
    }

    /** @deprecated */
    SortedSet getSortedOrders() {
        return sortedOrders;
    }

    /** @deprecated */
    void setSortedOrders(SortedSet<Order> sortedOrders) {
        this.sortedOrders = sortedOrders;
    }

    /** @deprecated */
    Position getPosition() {
        return position;
    }

    /** @deprecated */
    void setPosition(Position position) {
        this.position = position;
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    /** @deprecated */
    public Price getSamplePrice() {
        return samplePrice;
    }

    /** @deprecated */
    public void setSamplePrice(Price samplePrice) {
        this.samplePrice = samplePrice;
    }
}
