package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.orders.SortedOrders;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.experiment.ScoreException;

import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** holds book orders received for later execution in a market with only one good. */
public class UnaryBook extends Book {
    public UnaryBook(BinaryClaim claim, UnaryMarket market) {
        super(claim, market);
    }

    /** @deprecated */
        UnaryBook() {
    }

    /** There are zero to many orders in the book, and ACCEPTOR wants to increase its holdings of
     POSITION (equivalently reduce its holdings of the opposite position) with a limit of PRICE.
     We'll find the best-priced Order and trade with it if possible.  Repeat until quantityExchanged
     reaches QUANTITY, the best price available is worse than PRICE, or acceptor runs out of resources.
     Buyer must have (negative) coupons (which may be associated with reserves) or sufficient funds.
     Seller must have (positive) coupons or sufficient reserves.

     Four possible approaches to trading with each Order:
         a) both acceptor and order have appropriate coupons to trade (coupons are combined and liquidated)
         b) order has coupons and acceptor has funds (acceptor will buy coupons from Order)
         c) acceptor has coupons and order has funds (acceptor will sell coupons to Order)
         d) both parties have funds.  (coupons will be created to enable the trade.)
     After each trade within the FOR loop, we record the trade for the order.  After exhausting the user's ability
    to purchase, we record the total trade for the acceptor */
    Quantity consumateExchange(Position position, Price acceptorPrice, Quantity quantity, User acceptor) {
        SortedOrders offers = getOffers(position.opposite());
        Quantity quantityRemaining = quantity;            // acceptor's unfilled order
        Quantity quantityExchanged = Quantity.ZERO;       // amount sold to acceptor so far
        Quantity totalCostToAcceptor = Quantity.ZERO;     // amount spent by acceptor so far
        while (! quantityRemaining.isNegligible()) {      // at most one transaction per loop
            Order order = offers.getBest();
            if (order == null || order.price().compareTo(acceptorPrice.inverted()) < 0) {
                break;
            }

            if (order.userIsOwner(acceptor)) {
                cancelOrder(acceptor, order);
                return quantity;
            }

            Quantity quantityTraded;
            Quantity orderAvailableCoupons = quantityRemaining.min(order.availableCoupons(position));
            Quantity acceptorAvailableCoupons = order.quantity().min(quantityRemaining).min(acceptor.couponCount(position.opposite()));
            Quantity bothAvailableCoupons = orderAvailableCoupons.min(acceptorAvailableCoupons);
            Price price;
            if (position.isInvertedPosition()) {
                price = order.price();
            } else {
                price = order.price().inverted();
            }

            if (bothAvailableCoupons.isPositive()) {                                           // CASE A: liquidateFromBoth
                quantityTraded = liquidateFromBothParties(order, position, bothAvailableCoupons, acceptor, price);
                totalCostToAcceptor = totalCostToAcceptor.plus(price.times(bothAvailableCoupons));
            } else {
                Quantity orderPriceWithReserve = price.plus(requiredReserves(order));
                Quantity orderAffordableReservesQ = wholeShareQuantity(orderAffordableQ(position, quantityRemaining, order, price, orderPriceWithReserve, orderAvailableCoupons));
                Quantity acceptorPriceWithReserve = price.plus(reservesRequired(acceptor));
                Quantity acceptorAffordableWithReserve =
                        acceptorAffordableQ(position, quantityRemaining, price, acceptor.cashOnHand(), acceptorAvailableCoupons, acceptorPriceWithReserve, quantityRemaining);

                if (acceptorAvailableCoupons.isPositive()) {                                   // CASE C: sellToBookOrder
                    if (removeOrderIfUnaffordable(order, orderAffordableReservesQ)) { break; }

                    Quantity targetSalesQuant = wholeShareQuantity(orderAffordableReservesQ.min(acceptorAvailableCoupons));
                    if (targetSalesQuant.isNegligible()) { break; }

                    quantityTraded = sellToBookOrder(order, position, targetSalesQuant, acceptor, price);
                } else if (orderAvailableCoupons.isZero()) {                                 // CASE D: sellCouponsToBoth
                    if (removeOrderIfUnaffordable(order, orderAffordableReservesQ)) { break; }
                    if (warnAcceptorIfInsufficientReserves(acceptor, acceptorAffordableWithReserve)) { break; }

                    Quantity bothAffordable = wholeShareQuantity(orderAffordableReservesQ.min(acceptorAffordableWithReserve));
                    if (bothAffordable.isNegligible()) { break; }

                    sellCouponsToBoth(position, acceptor, order, price, bothAffordable);
                    quantityTraded = bothAffordable;
                } else {                                                                      // CASE B: buyFromBookOrder
                    if (warnAcceptorIfInsufficientReserves(acceptor, acceptorAffordableWithReserve)) { break; }

                    Quantity bothAvailable = wholeShareQuantity(orderAvailableCoupons.min(acceptorAffordableWithReserve));
                    quantityTraded = buyFromBookOrder(order, position, bothAvailable, acceptor, price);
                }

                if (quantityTraded.isZero() && order.quantity().compareTo(order.affordableQuantity(price, quantity)) > 0) {
                    removeOrderNSF(order);
                    break;
                }

                totalCostToAcceptor = totalCostToAcceptor.plus(price.times(quantityTraded));
            }

            quantityRemaining = quantityRemaining.minus(quantityTraded);
            quantityExchanged = quantityExchanged.plus(quantityTraded);
            getMarket().recordLimitTrade(order, quantityTraded, position);
            if (quantityTraded.isZero()) {
                Logger logger = Logger.getLogger(Book.class);
                logger.warn("recorded a zero unit trade with: " + acceptor.getName());
                break;
            }
            if (order.quantity().isNegligible()) {
                    removeOrder(order);
            }
        }
        acceptor.settle(getMarket());
        if (! quantityExchanged.isZero()) {
            Price priceToAcceptor = new Price(totalCostToAcceptor.div(quantityExchanged), acceptorPrice);
            recordBookTrade(acceptor, priceToAcceptor, quantityExchanged, position);
        }

        return quantityExchanged;
    }

    private boolean warnAcceptorIfInsufficientReserves(User acceptor, Quantity acceptorAffordableWithReserve) {
        if (wholeShareQuantity(acceptorAffordableWithReserve).isZero()) {
            acceptor.warn("You don't have sufficient reserves for that trade.");
            return true;
        }
        return false;
    }

    private boolean removeOrderIfUnaffordable(Order order, Quantity orderAffordableReservesQ) {
        if (wholeShareQuantity(orderAffordableReservesQ).isNegligible())  {
            removeOrderNSF(order);
            return true;
        }
        return false;
    }

    // order's owner no longer has sufficient funds
    private void removeOrderNSF(Order order) {
        order.makeRemovalRecord();
        removeOrder(order);
        return;
    }

    private Quantity acceptorAffordableQ(Position position, Quantity quantityRemaining, Price price, Quantity acceptorCash,
                                         Quantity acceptorAvailableCoupons, Quantity priceWithReserve, Quantity targetQuantity) {
        Quantity acceptorCost;
        if (position.isInvertedPosition()) {
            Quantity acceptorAvailableCost = price.times(acceptorAvailableCoupons);
            Quantity reserveCost = priceWithReserve.minus(price).times(quantityRemaining);
            acceptorCost = acceptorAvailableCost.plus(reserveCost);
        } else {
             acceptorCost = price.times(targetQuantity);
         }

        Quantity usableAcceptorFunds = acceptorCash.min(targetQuantity.times(acceptorCost));

        if (usableAcceptorFunds.compareTo(acceptorCost) < 0) {
            targetQuantity = targetQuantity.times(usableAcceptorFunds.div(acceptorCost));
        }
        return targetQuantity;
    }

    private Quantity orderAffordableQ(Position position, Quantity quantityRemaining, Order order, Price price, Quantity priceWithReserve, Quantity availableCoupons) {
        if (position.isInvertedPosition()) {
            return order.affordableQuantity(price, quantityRemaining).plus(availableCoupons);
        } else {
            return order.affordableQuantity(priceWithReserve, quantityRemaining).plus(availableCoupons);
        }
    }

    private Quantity wholeShareQuantity(Quantity targetQuantity) {
        if (getMarket().wholeShareTradingOnly()) {
            targetQuantity = targetQuantity.roundFloor();
        }
        return targetQuantity;
    }

    private Quantity reservesRequired(User acceptor) {
        Quantity currentReserveValue;
        try {
            currentReserveValue = getMarket().requiredReserves(acceptor);
        } catch (ScoreException e) {
            Logger logger = Logger.getLogger("Score");
            logger.warn("calculating reserves required for Book Order", e);
            currentReserveValue = Quantity.ZERO;
        }
        return currentReserveValue;
    }

    private void sellCouponsToBoth(Position position, User acceptor, Order order, Price price, Quantity targetQuantity) {
        Coupons[] couponses = getMarket().printNewCouponSets(targetQuantity, null);
        Coupons yesCoupons = couponses[0];
        Coupons noCoupons = couponses[1];
        if (position.isInvertedPosition()) {
            Funds orderFunds = order.provideFundsForTrade(targetQuantity, price);
            acceptor.receiveCash(orderFunds);
            order.receive(yesCoupons);
            acceptor.endow(noCoupons);
            getMarket().withholdReserves(acceptor, targetQuantity, position);
        } else {
            Funds acceptorFunds = acceptor.getAccounts().provideCash(targetQuantity.times(price));
            order.returnCash(acceptorFunds);
            order.receive(noCoupons);
            acceptor.endow(yesCoupons);
            getMarket().withholdReserves(order, targetQuantity, position);
        }
    }

    // order is buying or shorting, acceptor is selling or covering
    // 1. Transfer coupons from acceptor to order.
    // 2. transfer reserves if necessary (short/corver vs. buy/sell)
    // 3. transfer money from order to acceptor
    private Quantity sellToBookOrder(Order order, Position position, Quantity quantity, User acceptor, Price price) {
        if (! quantity.isPositive()) { return Quantity.ZERO; }

        Quantity quantityToTrade = quantity;
        Quantity coupons;
        if (position.isInvertedPosition()) { // order buys and acceptor sells
            Funds fundsFromOrder = order.provideFundsForTrade(quantity, price);
            quantityToTrade = quantity.min(fundsFromOrder.getBalance().div(price));
            acceptor.getAccounts().receiveCash(fundsFromOrder);
            coupons = transferCouponsAcceptorToOrder(order, position, acceptor, quantityToTrade);
        } else {  // order shorts and acceptor covers
            final Quantity requiredReserves = requiredReserves(order);
            Quantity availableCash = acceptor.getAccounts().cashValue();
            if (requiredReserves.compareTo(price) < 0) {
                Quantity maxAcceptorPurchase = availableCash.div(price.minus(requiredReserves));
                quantityToTrade = quantity.min(maxAcceptorPurchase);
            }
            coupons = transferCouponsAcceptorToOrder(order, position, acceptor, quantityToTrade);
            order.returnCash(acceptor.provideCash(coupons.times(price)));
            acceptor.releaseReserves(coupons, position.opposite());
            ReserveVerifier verifier = new ReserveVerifier(null) {
                public boolean verify(User user, Position pos, Price price, Quantity quantity) { return false; }
                public Quantity requiredReserves(User user) throws ScoreException { return Quantity.ZERO; }
                public Quantity costToSell(Price price, Quantity quantity, Quantity remainingDividend) { return Quantity.ZERO; }
                public void addMarket(Market market) { }

                public void enforce(User user, Quantity quantityPurchased, Position pos) {
                    user.reserveFunds(requiredReserves, quantityPurchased, pos.opposite());
                }
            };
            order.withholdReserves(verifier, coupons, position);
        }

        return coupons;
    }

    private Quantity transferCouponsAcceptorToOrder(Order order, Position position, User acceptor, Quantity quantity) {
        Coupons sellerCoupons = acceptor.getAccounts().provideCoupons(position.opposite(), quantity);
        Quantity coupons = sellerCoupons.getBalance();
        order.receive(sellerCoupons);
        return coupons;
    }

    // order is selling or covering, acceptor is buying or shorting
    // 1. Transfer coupons from order to acceptor.
    // 2. transfer reserves if necessary (short/corver vs. buy/sell)
    // 3. transfer money from acceptor to order
    private Quantity buyFromBookOrder(Order order, Position position, Quantity quantity, User acceptor, Price price) {
        if (! quantity.isPositive()) { return Quantity.ZERO; }

        Quantity quantityToTrade = quantity;
        Quantity coupons;
        if (position.isInvertedPosition()) { // order covers and acceptor shorts
            Quantity requiredReserves = requiredReserves(order);
            Quantity availableCash = acceptor.getAccounts().cashValue();
            if (requiredReserves.compareTo(price) > 0) {
                Quantity maxAcceptorPurchase = availableCash.div(requiredReserves.plus(price));
                quantityToTrade = quantity.min(maxAcceptorPurchase);
                if (getMarket().wholeShareTradingOnly()) {
                    quantityToTrade = quantityToTrade.roundFloor();
                    if (quantityToTrade.isZero()) {
                        removeOrderNSF(order);
                    }
                }
            }
            coupons = transferCouponsOrderToAcceptor(order, position, acceptor, quantityToTrade);
            acceptor.receiveCash(order.provideFundsForTrade(coupons, price));
            order.releaseReserves(coupons, position);
            getMarket().withholdReserves(acceptor, coupons, position);
        } else {  // acceptor buys and order sells
            Funds acceptorFunds = acceptor.getAccounts().provideCash(quantity.times(price));
            quantityToTrade = quantity.min(acceptorFunds.getBalance().div(price));
            if (getMarket().wholeShareTradingOnly()) {
                quantityToTrade = quantityToTrade.roundFloor();
            }
            coupons = transferCouponsOrderToAcceptor(order, position, acceptor, quantityToTrade);
            order.returnCash(acceptorFunds);
        }

        order.reduceQuantityDueToSale(coupons);
        return coupons;
    }

    private Quantity requiredReserves(Order order) {
        Quantity requiredReserves;
        try {
            requiredReserves = order.requiredReserves(getMarket());
        } catch (ScoreException e) {
            Logger logger = Logger.getLogger("Score");
            logger.warn("calculating reserves required for Book Order", e);
            requiredReserves = Quantity.ZERO;
        }
        return requiredReserves;
    }

    private Quantity transferCouponsOrderToAcceptor(Order order, Position position, User acceptor, Quantity quantityToTrade) {
        Coupons sellerCoupons = order.provideCoupons(position, quantityToTrade);
        Quantity coupons = sellerCoupons.getBalance();
        acceptor.endow(sellerCoupons);
        return coupons;
    }

    private Quantity liquidateFromBothParties(Order order, Position position, Quantity quantity, User acceptor, Price price) {
        if (! quantity.isPositive()) { return Quantity.ZERO; }

        Coupons sellerCoupons = order.provideCoupons(position, quantity);
        Coupons acceptorCoupons = acceptor.getAccounts().provideCoupons(position.opposite(), quantity);
        if (quantity.equals(sellerCoupons.getBalance()) && quantity.equals(acceptorCoupons.getBalance())) {
            Accounts accounts = liquidateCoupons(sellerCoupons, acceptorCoupons);
            if (! accounts.cashValue().isZero()) {
                Logger log = Logger.getLogger(CouponBank.class);
                log.warn("UnaryMarket liquidated " + quantity + " coupons, and received money back.");
            }
            if (position.isInvertedPosition()) {
                // JJDM 20170114 - releasing reserves first, before moving cash
                order.releaseReserves(quantity, position);
                acceptor.receiveCash(order.provideFundsForTrade(quantity, price));
            } else {
                // JJDM 20170114 - releasing reserves first, before moving cash
                acceptor.releaseReserves(quantity, position);
                order.returnCash(acceptor.provideCash(price));
            }
            order.reduceQuantityDueToSale(quantity);
            return quantity;
        } else {
            order.receive(sellerCoupons);
            acceptor.getAccounts().addCoupons(acceptorCoupons);
            return Quantity.ZERO;
        }
    }
}
