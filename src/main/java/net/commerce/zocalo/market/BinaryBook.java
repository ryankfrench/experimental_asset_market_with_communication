package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.orders.SortedOrders;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.currency.*;

// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** holds book orders received for later execution in a market with only paired goods. */
public class BinaryBook extends Book {
    public BinaryBook(Claim claim, Market market) {
        super(claim, market);
    }

    /** @deprecated */
    BinaryBook() {
    }

    /** There are zero to many orders in the book, and ACCEPTOR wants to increase its holdings of
     POSITION with a limit of PRICE.  We'll find the best-priced Order and trade with it if possible.
     Repeat until quantityExchanged reaches QUANTITY, the best price available is worse than PRICE, or
     acceptor runs out of resources.

     Four possible approaches to trading with each Order:
         a) both acceptor and order have appropriate coupons to trade (coupons are combined and liquidated)
         b) order has coupons and acceptor has funds (acceptor will buy coupons from Order)
         c) acceptor has coupons and order has funds (acceptor will sell coupons to Order)
         d) both parties have funds.  (coupons will be created to enable the trade.)
     After each trade within the FOR loop, we record the trade for the order.  After exhausting the user's ability
    to purchase, we record the total trade for the acceptor */
    Quantity consumateExchange(Position position, Price price, Quantity quantity, User acceptor) {
        SortedOrders offers = getOffers(position.opposite());
        Quantity quantityRemaining = quantity;
        Quantity quantityExchanged = Quantity.ZERO;
        Quantity totalCostToAcceptor = Quantity.ZERO;
        while (quantityRemaining.isPositive()) {
            Order order = offers.getBest();
            if (order == null || order.price().compareTo(price.inverted()) < 0) {
                break;
            }

            if (order.userIsOwner(acceptor)) {
                cancelOrder(acceptor, order);
                return quantity;
            }

            Quantity quantityTraded;
            Quantity orderAvailableCoupons = quantityRemaining.min(order.availableCoupons(position));
            Quantity acceptorAvailableCoupons = quantityRemaining.min(order.quantity().min(acceptor.couponCount(position.opposite())));
            Quantity bothAvailableCoupons = orderAvailableCoupons.min(acceptorAvailableCoupons);
            Price acceptorPrice = order.price().inverted();
            Quantity usableFundsFromAcceptor = acceptor.cashOnHand().min(quantityRemaining.times(acceptorPrice));

            if (bothAvailableCoupons.isPositive()) {                            // CASE A: liquidateFromBoth
                quantityTraded = liquidateFromBothParties(order, position, bothAvailableCoupons, acceptor);
                totalCostToAcceptor = totalCostToAcceptor.plus(acceptorPrice.times(bothAvailableCoupons));
            } else if (orderAvailableCoupons.isPositive() && usableFundsFromAcceptor.compareTo(acceptorPrice.times(orderAvailableCoupons)) >= 0) {  // CASE B: buyFromBookOrder
                quantityTraded = buyFromBookOrder(order, position, orderAvailableCoupons, acceptor, acceptorPrice);
                if (quantityTraded.isZero() && order.quantity().compareTo(order.affordableQuantity(order.price(), quantity)) > 0) {
                    order.makeRemovalRecord();
                    removeOrder(order);
                    break;
                }
                totalCostToAcceptor = totalCostToAcceptor.plus(acceptorPrice.times(quantityTraded));
            } else if (acceptorAvailableCoupons.isPositive()) {                // CASE C: sellToBookOrder
                quantityTraded = sellToBookOrder(order, position, acceptorAvailableCoupons, acceptor, order.price());
                if (quantityTraded.isZero() && order.quantity().compareTo(order.affordableQuantity(order.price(), quantity)) > 0) {
                    order.makeRemovalRecord();
                    removeOrder(order);
                    break;
                }
                totalCostToAcceptor = totalCostToAcceptor.plus(acceptorPrice.times(quantityTraded));
            } else {                                                 // CASE D: sellCouponsToBoth
                Quantity targetQuantity = order.affordableQuantity(order.price(), quantityRemaining);
                Quantity acceptorCost = acceptorPrice.times(targetQuantity);

                if (usableFundsFromAcceptor.compareTo(acceptorCost) < 0) {
                    Quantity reduction = acceptor.cashOnHand().div(acceptorCost);
                    if (getMarket().wholeShareTradingOnly()) {
                        targetQuantity = targetQuantity.times(reduction).roundFloor();
                    } else {
                        targetQuantity = targetQuantity.times(reduction);
                    }
                    if (targetQuantity.isNegligible()) {
                        break;
                    }
                }

                Funds combinedFunds = order.provideFundsForTrade(targetQuantity, order.price());
                quantityTraded = combinedFunds.getBalance().div(order.price());
                acceptor.getAccounts().provideCash(acceptorPrice.times(quantityTraded)).transfer(combinedFunds);
                useFundsToPurchaseNewCoupons(combinedFunds, position, quantityTraded, acceptor, order);
                totalCostToAcceptor = totalCostToAcceptor.plus(acceptorPrice.times(quantityTraded));
            }
            order.settleOffsettingPositions(getMarket());
            if (quantityTraded.isPositive() && getMarket().requireReserves()) {
                getMarket().withholdReserves(order, quantityTraded.negate(), position);
            }

            quantityRemaining = quantityRemaining.minus(quantityTraded);
            quantityExchanged = quantityExchanged.plus(quantityTraded);
            getMarket().recordLimitTrade(order, quantityTraded, position);
            if (order.quantity().isNegligible()) {
                removeOrder(order);
            }
        }
        acceptor.settle(getMarket());
        if (! quantityExchanged.isZero()) {
            Price acceptorPrice = new Price(totalCostToAcceptor.div(quantityExchanged), price);
            recordBookTrade(acceptor, acceptorPrice, quantityExchanged, position);
        }

        return quantityExchanged;
    }

    private Quantity sellToBookOrder(Order order, Position position, Quantity quantity, User acceptor, Price price) {
        if (! quantity.isPositive()) { return Quantity.ZERO; }

        Funds fundsFromOrder = order.provideFundsForTrade(quantity, price);
        Quantity quantityToTrade = quantity.min(fundsFromOrder.getBalance().div(price));
        Coupons acceptorCoupons = acceptor.getAccounts().provideCoupons(position.opposite(), quantityToTrade);
        acceptor.getAccounts().receiveCash(fundsFromOrder);
        order.receive(acceptorCoupons);
        return quantityToTrade;
    }

    private Quantity buyFromBookOrder(Order order, Position position, Quantity quantity, User acceptor, Price price) {
        if (! quantity.isPositive()) { return Quantity.ZERO; }

        Funds acceptorFunds = acceptor.getAccounts().provideCash(quantity.times(price));
        Quantity quantityToTrade = quantity.min(acceptorFunds.getBalance().div(price));
        Coupons sellerCoupons = order.provideCoupons(position, quantityToTrade);
        order.returnCash(acceptorFunds);
        acceptor.getAccounts().addCoupons(sellerCoupons);
        order.reduceQuantityDueToSale(quantityToTrade);
        return quantityToTrade;
    }

    private Quantity liquidateFromBothParties(Order order, Position position, Quantity quantity, User acceptor) {
        if (quantity.isNegative() || quantity.isZero()) { return Quantity.ZERO; }

        Coupons sellerCoupons = order.provideCoupons(position, quantity);
        Coupons acceptorCoupons = acceptor.getAccounts().provideCoupons(position.opposite(), quantity);
        if (quantity.equals(sellerCoupons.getBalance()) && quantity.equals(acceptorCoupons.getBalance())) {
            Accounts accounts = liquidateCoupons(sellerCoupons, acceptorCoupons);
            Quantity acceptorValue = order.price().asProbability().times(accounts.cashValue());
            acceptor.receiveCash(accounts.provideCash(acceptorValue));
            order.returnCash(accounts.provideCash(accounts.cashValue()));
            order.reduceQuantityDueToSale(quantity);
            return quantity;
        } else {
            order.receive(sellerCoupons);
            acceptor.getAccounts().addCoupons(acceptorCoupons);
            return Quantity.ZERO;
        }
    }

    private void useFundsToPurchaseNewCoupons(Funds bothFunds, Position position, Quantity quantity, User buyer, Order order) {
        Coupons[] couponsArray = getMarket().printNewCouponSets(quantity, bothFunds);
        for (int i = 0; i < couponsArray.length; i++) {
            Coupons coupons = couponsArray[i];
            if (coupons.getPosition() == position) {
                buyer.endow(coupons);
            } else if (coupons.getPosition() == position.opposite()) {
                order.receive(coupons);
            }
        }
        buyer.settle(getMarket());
        order.settleOffsettingPositions(getMarket());
    }
}
