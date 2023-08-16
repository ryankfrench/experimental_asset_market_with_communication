package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.freechart.ChartScheduler;
import net.commerce.zocalo.currency.Accounts;
import net.commerce.zocalo.currency.Coupons;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.orders.SortedOrders;
import net.commerce.zocalo.ajax.events.Ask;
import net.commerce.zocalo.ajax.events.SelfDealing;
import net.commerce.zocalo.ajax.events.Bid;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.logging.GID;
import org.apache.log4j.Logger;
import org.antlr.stringtemplate.StringTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** A Book holds orders entered by Users to buy and sell positions on a particular Claim.  */
public abstract class Book {
    private Map<Position, SortedOrders> offers;
    private Claim claim;
    private Market market;
    private long id;

    public Book(Claim claim, Market market) {
        this.claim = claim;
        this.market = market;
        initializeOffers(claim);
    }

    /** @deprecated */
    Book() {
    }

    /** buy up to price worth or quantity coupons from the orders in the book.  This method
     is called when there is no marketMaker.  This method will only fill the portion of the order
     that can be satisfied immediately.  */
    public Quantity buyFromBookOrders(Position position, Price price, Quantity quantity, User buyer) {
        Price bestOffer = bestSellOfferFor(position);
        if (bestOffer.compareTo(price) <= 0) {
            return consumateExchange(position, price, quantity, buyer);
        } else {
            return Quantity.ZERO;
        }
    }

    abstract Quantity consumateExchange(Position position, Price price, Quantity quantity, User buyer);

    /** buy up to price worth or quantity coupons from the orders at the book's best price.
     This is used when coupons are purchased from the book and the marketMaker.  In order
     to ensure that the best price is always used, the market has to buy book orders until
     the price changes, then buy from the maker until the next price frontier is reached. */
    public Quantity buyFromBestBookOrders(Position position, Price price, Quantity quantity, User user) {
        Quantity quantityRemaining = quantity.min(bestQuantity(position.opposite()));
        return consumateExchange(position, price, quantityRemaining, user);
    }

    private void initializeOffers(Claim claim) {
        offers = new HashMap<Position, SortedOrders>();
        for (int i = 0; i < claim.positions().length; i++) {
            Position position = claim.positions()[i];
            SortedOrders orders = new SortedOrders(position, market.maxPrice());
            HibernateUtil.save(orders);
            offers.put(position, orders);
        }
    }

    public Order addOrder(Position position, Price price, Quantity quantity, User user) throws DuplicateOrderException, IncompatibleOrderException {
        SortedOrders orders = offers.get(position);
        Order oldBest = orders.getBest();
        Price bestSellOffer = bestSellOfferFor(position);
        Price bestBuyOffer = bestBuyOfferFor(position.opposite());
        Price maxPrice = getMarket().maxPrice();
        boolean sellOfferExists = maxPrice.compareTo(bestSellOffer) > 0;
        boolean pricesOverlap = price.plus(bestBuyOffer).compareTo(maxPrice) > 0;
        if (sellOfferExists && pricesOverlap) {
            throwIncompatibleOrderException(position, price, quantity, user);
        }
        Order order = new Order(position, price, quantity, user);
        HibernateUtil.save(order);
        orders.add(order);

        logNewOffer(position, user, order);
        logNewBestOffer(orders, oldBest, position, order);
        return order;
    }

    private void throwIncompatibleOrderException(Position position, Price price, Quantity quantity, User user) throws IncompatibleOrderException {
        StringTemplate st = new StringTemplate("Cannot create order whose price overlaps existing orders: u($user$), $quant$@$price$, pos: $pos$.");
        st.setAttribute("user", user);
        st.setAttribute("price", price);
        st.setAttribute("quant", quantity);
        st.setAttribute("pos", position);
        throw new IncompatibleOrderException(st.toString());
    }

    private void logNewBestOffer(SortedOrders orders, Order oldBest, Position position, Order order) {
        if (oldBest == null || orders.getBest() == null || orders.getBest().naturalPrice() != oldBest.naturalPrice()) {
            if (position.isInvertedPosition()) {
                order.makeBestAsk();
            } else {
                order.makeBestBid();
            }
        }
    }

    private void logNewOffer(Position position, User user, Order order) {
        if (position.isInvertedPosition()) {
            Ask.newAsk(user.getName(), order.naturalPrice(), order.quantity(), position);
        } else {
            Bid.newBid(user.getName(), order.price(), order.quantity(), position);
        }
    }

    public boolean removeOrder(Order order) {
        SortedOrders orders = (SortedOrders)offers.get(order.position());
        HibernateUtil.delete(order);
        return orders.remove(order);
    }

    public boolean removeOrder(String userName, Price price, Position pos) {
        SortedOrders orders = (SortedOrders)offers.get(pos);
        Iterator iterator = orders.iterator();
        while (iterator.hasNext()) {
            Order order = (Order) iterator.next();
            if (order.ownerName().equals(userName) && order.naturalPrice().equals(price)) {
                if (orders.remove(order)) {
                    order.makeRemovalRecord();
                    HibernateUtil.delete(order);
                    return true;
                }
            }
        }
        return false;
    }

    protected void recordBookTrade(User acceptor, Price price, Quantity quantity, Position position) {
        if (quantity.isZero()) { return; }

        market.recordBookTrade(acceptor, price, quantity, position);
        scheduleChartGeneration();
    }

    private void scheduleChartGeneration() {
        ChartScheduler chartScheduler = ChartScheduler.find(market.getName());
        if (chartScheduler != null) {
            chartScheduler.generateNewChart();
        }
    }

    protected void cancelOrder(User buyer, Order order) {
        Logger warnLogger = Logger.getLogger(Book.class);
        buyer.addWarning("You accepted your own order; the order was removed without recording a trade.");
        Price price = order.naturalPrice();
        warnLogger.warn(buyer.getName() + " removed an order for " + price.toString());
        removeOrder(order);
        SelfDealing.newSelfDealing(buyer.getName(), price, order.quantity(), order.position());
    }

    Accounts liquidateCoupons(Coupons sellerCoupons, Coupons buyerCoupons) {
        Map<Position, Coupons> couponsMap = new HashMap<Position, Coupons>();
        couponsMap.put(sellerCoupons.getPosition(), sellerCoupons);
        couponsMap.put(buyerCoupons.getPosition(), buyerCoupons);
        return getMarket().settle(sellerCoupons.getBalance(), couponsMap);
    }

    public void resetOrders() {
        Logger warnLogger = Logger.getLogger(Book.class);
        warnLogger.warn(GID.log() + "Resetting Order Book for claim '" + claim.getName() + "'.");
        initializeOffers(claim);
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim claim) {
        this.claim = claim;
    }

    public SortedOrders getOffers(Position position) {
        SortedOrders orders = (SortedOrders)offers.get(position);
        if (orders == null) {
            orders = new SortedOrders(position, market.maxPrice());
        }
        return orders;
    }

    public Price bestSellOfferFor(Position position) {
        Price oppositePrice = getOffers(position.opposite()).bestPrice();
        return oppositePrice.inverted();
    }

    public Price bestBuyOfferFor(Position position) {
        return (getOffers(position)).bestPrice();
    }

    public Quantity bestQuantity(Position position) {
        return (getOffers(position)).getQuantityAtPrice(getOffers(position).bestPrice());
    }

    public boolean hasOrdersToSell(Position position) {
        return getOffers(position.opposite()).hasOrders();
    }

    public boolean hasOrdersToSell() {
        Position[] positions = getClaim().positions();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            if (hasOrdersToSell(position)) {
                return true;
            }
        }
        return false;
    }

    public Iterator iterateOffers(Position pos) {
        return getOffers(pos).iterator();
    }

    /** @deprecated */
    Map getOffers() {
        return offers;
    }

    /** @deprecated */
    void setOffers(Map<Position, SortedOrders> offers) {
        this.offers = offers;
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    protected Market getMarket() {
        return market;
    }

    /** @deprecated */
    void setMarket(Market market) {
        this.market = market;
    }

    public String buildPriceList(Position pos) {
        Iterator offers = iterateOffers(pos);
        StringBuffer value = new StringBuffer();
        while (offers.hasNext()) {
            Order order = (Order) offers.next();
            value.append(order.naturalPrice().printAsWholeCents());
            if (offers.hasNext()) {
                value.append(",");
            }
        }
        return value.toString();
    }
}
