package net.commerce.zocalo.user;

// Copyright 2009, 2010 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.ajax.events.IndividualTimingEvent;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.experiment.role.Borrower;
import net.commerce.zocalo.market.BinaryMarket;

import org.apache.log4j.Logger;

import java.util.*;

import jjdm.zocalo.ZocaloSetupServlet;

/** A Liquidator is used in some experiment scenarios in which traders can borrow against their holdings.
 If the trader's net worth is less than the loan, a liquidator will sequester the trader's assets and sell them
 off for the best available price. */
public class Liquidator {
    static private Set<Liquidator> activeLiquidators = new HashSet<Liquidator>();
    private Borrower deadbeat;
    private User seller;
    private long period;
    private RepeatableTask liqudate;
    final private BinaryMarket market;
    private Position yes;
    private final Price highPrice;

    public Liquidator(Borrower b, long mSecs, BinaryClaim claim, BinaryMarket market) {
        deadbeat = b;
        period = mSecs;
        Accounts borrowerAcct = b.getUser().getAccounts();
        yes = claim.getYesPosition();
        this.market = market;
        createLiquidationTask();
        deadbeat.getUser().warn("Your shares will be sold to repay your loan.");
        seller = new User(b.getName() + "'s liquidator", b.getUser().provideCash(borrowerAcct.cashValue()));
        seller.endow(borrowerAcct.provideCoupons(yes, borrowerAcct.couponCount(yes)));
        highPrice = market.scaleToPrice(new Quantity(.99999));
        activeLiquidators.add(this);
    }

    static public void stopAllLiquidators() {
        for (Liquidator liquidator : activeLiquidators) {
            liquidator.finalProcessing();
        }
    }

    private void createLiquidationTask() {
        final Liquidator thisLiquidator = this;
        liqudate = new RepeatableTask() {
            public void run() {
                try {
                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    Logger.getLogger(Liquidator.class).warn("Sleep interrupted for " + name(), e);
                }

                synchronized (ZocaloSetupServlet.SERVLET_LOCK) {
                    market.marketOrder(yes.opposite(), highPrice, Quantity.ONE, seller);
                }
            }

            public boolean isDone() {
                return seller.cashOnHand().compareTo(deadbeat.getDefaultAmount()) > 0
                    || couponsRemaining().isZero();
            }

            public void finalProcessing() {
                thisLiquidator.finalProcessing();
            }

            public String name() {
                return "liquidating " + deadbeat.getName();
            }
        };
    }

    private void finalProcessing() {
        BinaryClaim claim = market.getBinaryClaim();
        Quantity sellerCash = seller.cashOnHand();
        if (sellerCash.compareTo(deadbeat.getDefaultAmount()) > 0) {
            returnAssetsEndDefault(claim);
            repaidDebtEvent(deadbeat);
        } else if (seller.couponCount(claim).isPositive()) {
            returnAssetsContinueDefault(claim);
        } else {
            deadbeat.decreaseLoanAmount(sellerCash);
            Funds discard = seller.provideCash(sellerCash);
            unrepaidDebtEvent(deadbeat);
        }
        activeLiquidators.remove(this);
    }

    private void returnAssetsEndDefault(BinaryClaim claim) {
        deadbeat.endDefault(seller.provideCash(seller.cashOnHand().minus(deadbeat.getDefaultAmount())));
        // Seller may still have some money, but session doesn't need it back.
        deadbeat.decreaseLoanAmount(deadbeat.getDefaultAmount());
        returnCoupons(seller, claim);
    }

    private void returnAssetsContinueDefault(BinaryClaim claim) {
        Quantity sellerCash = seller.cashOnHand();
        deadbeat.decreaseLoanAmount(sellerCash);
        Funds discard = seller.provideCash(sellerCash);
        returnCoupons(seller, claim);
    }

    private void returnCoupons(User seller, BinaryClaim claim) {
        Accounts sellerAcct = seller.getAccounts();
        Quantity couponCount = sellerAcct.couponCount(claim);
        Coupons coupons = sellerAcct.provideCoupons(claim.getYesPosition(), couponCount);
        deadbeat.getUser().endow(coupons);
    }

    public Quantity couponsRemaining() {
        return seller.getAccounts().couponCount(yes);
    }

    public void startLiquidating(long initialDelay) {
        RepeatedScheduler scheduler = new RepeatedScheduler(liqudate, initialDelay);
        scheduler.startScheduling();
        Logger logger = Logger.getLogger(Liquidator.class);
        logger.debug("started liquidator: " + liqudate.name());
    }

    public static IndividualTimingEvent repaidDebtEvent(Borrower deadbeat) {
        String logString = "Trader '" + deadbeat.getName() + "' finished repaying debt.";
        Logger logger = IndividualTimingEvent.getActionLogger();
        return new IndividualTimingEvent(deadbeat, logger, "repaidDebt", logString);
    }

    public static IndividualTimingEvent unrepaidDebtEvent(Borrower deadbeat) {
        String logString = "Trader '" + deadbeat.getName() + "' unable to repay debt.";
        Logger logger = IndividualTimingEvent.getActionLogger();
        return new IndividualTimingEvent(deadbeat, logger, "unrepaidDebt", logString);
    }
}
