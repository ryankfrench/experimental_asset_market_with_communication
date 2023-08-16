package net.commerce.zocalo.market;

import net.commerce.zocalo.currency.Price;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.experiment.ScoreException;

import org.apache.log4j.Logger;
import java.util.*;
import java.math.MathContext;
import java.math.BigDecimal;

/** a Market in a claim; always supports book orders, sometimes supports a marketMaker.
    Responsibilities include reporting on price levels and book contents as well as
    buying claims. */
public abstract class Market {
    final static String CANNOT_AFFORD_WARNING = "You can't afford that order in combination with your other orders.  Please delete a buy order before submitting a new one";
    final static String NO_MONEY_WARNING = "You can't afford that order.";
    final static String INSUFFICIENT_ASSETS_WARNING = "You don't have any certificates to sell.";
    final static String TOO_MANY_SALES_WARNING = "You have already placed sell offers for all your certificates.  Please delete a sell offer before submitting a new one";
    final static String OUT_OF_BOUNDS = "Prices are more than 0 and less than 100.";
    final static MathContext lowRes = Quantity.NINE_DIGITS;

    private CouponBank couponMint;
    private Book book;
    private SecureUser owner;
    private boolean marketClosed = false;
    private boolean priceBetteringRequired = false;
    private boolean wholeShareTradingOnly = false;
    private boolean reservesRequired = false;
    private ReserveVerifier reserveVerifier;
    private Outcome outcome;
    private long id;
    private Date lastTrade;
    protected Price maxPrice;  // Could be a quantity, but if it's a Price, we can use it to scale other prices

    /** @deprecated */
    Market() {
    }

    Market(CouponBank bank, SecureUser owner, Quantity maxPrice) {
        setCouponMint(bank);
        this.owner = owner;
        outcome = Outcome.newOpen(false);
        initPrices(maxPrice);
        HibernateUtil.save(outcome);
        lastTrade = new Date();
    }

    private void initPrices(Quantity topPrice) {
        setMaxPrice(new Price(topPrice, topPrice));
    }

//// ACCESSORS ///////////////////////////////////////////////////////////////

    public Book getBook() {
        return book;
    }

    public Probability currentProbability(Position position) {
        if (! hasMaker()) {
            return Probability.NEVER;
        }
        return getMaker().currentProbability(position);
    }

    public Price currentPrice(Position position) {
        Probability prob = currentProbability(position);
        return new Price(prob, maxPrice());
    }

    public String getName() {
        return getClaim().getName();
    }

    abstract public void setClaim(Claim claim);
    abstract public Claim getClaim();
    abstract public void setMaker(MarketMaker mm);

    abstract public void recordLimitTrade(Order order, Quantity quantityTraded, Position position);
    public abstract void recordBookTrade(User acceptor, Price price, Quantity quantity, Position position);

    Price bestPrice(Position position) {
        Price bestPrice = getBook().bestSellOfferFor(position);
        Probability bestPriceAsProb = bestPrice.asProbability();
        Probability curProb = getMaker().currentProbability(position);
        if (curProb.compareTo(bestPriceAsProb) >= 0) {
            return bestPrice;
        } else {
            return scaleToPrice(curProb);
        }
    }

    abstract public Price maxPrice();

    public boolean verifyPriceRange(Quantity price) {
        return price.isPositive() && price.compareTo(maxPrice()) < 0;
    }

    public Price asPrice(Quantity q) {
        return new Price(q, maxPrice());
    }

    public Price asPrice(BigDecimal decimal) {
        return new Price(decimal, maxPrice());
    }

    public Price scaleToPrice(Quantity price) {
        return new Price(price.times(maxPrice()), maxPrice());
    }

    public boolean hasMaker() {
        return getMaker() != null;
    }

//// EXCHANGE ////////////////////////////////////////////////////////////////

    /** buy TOTALQUANTITY of coupons, while not raising the price above PRICE.  If price
       is above the market level, immediately buy coupons up to the market price,
       totalQuantity or user's spending limit.  If price is lower, enter a book only
       order.  Enter a book order for coupons not purchased immediately. */
    public void limitOrder(Position pos, Price price, Quantity quantity, User user) throws DuplicateOrderException {
        if (getMarketClosed()) {
            warnMarketClosed(pos, price, quantity, user);
            return;
        }
        buyOrAddBookOrder(pos, price, quantity, user);
    }

    /** buy TOTALQUANTITY of coupons, while not raising the price above PRICE.  If price
       is above the market level, immediately buy coupons up to the market price,
       totalQuantity or user's spending limit.  If price is lower, do nothing. */
    public void marketOrder(Position pos, Price price, Quantity quantity, User user) {
        if (getMarketClosed()) {
            warnMarketClosed(pos, price, quantity, user);
            return;
        }
        buyAtMarketPrice(pos, price, quantity, user);
    }

    abstract protected void buyAtMarketPrice(Position pos, Price price, Quantity quantity, User user);
    abstract protected void buyOrAddBookOrder(Position pos, Price price, Quantity quantity, User user) throws DuplicateOrderException;

    protected boolean gateKeeper(Position pos, Price price, Quantity quantity, User user, boolean marketOrder) {
        if (price.isNegative() || price.compareTo(maxPrice()) > 0) {
            user.warn(OUT_OF_BOUNDS);
            return true;
        }
        if (quantity.isNegative()) {
            user.warn("quantity must be positive");
            return true;
        }
        if (getMarketClosed()) {
            BinaryMarket.warnMarketClosed(pos, price, quantity, user);
            return true;
        }
        if (priceBetteringViolation(pos, price, user, marketOrder)) {
            return true;
        }
        if (requireReserves() && ! verifyReserves(user, pos, price, quantity)) {
            return true;
        }
        return false;
    }

    boolean priceBetteringViolation(Position pos, Price price, User user, boolean marketOrder) {
        if (marketOrder || ! priceBetteringRequired) {
            return false;
        }

        if (price.compareTo(getBook().getOffers(pos).bestPrice()) <= 0) {
            String warning = pos.isInvertedPosition()
                    ? "price must be lower than existing sell offers"
                    : "price must be higher than existing buy offers";
            user.warn(warning);
            return true;
        }
        return false;
    }

    abstract public Quantity buyWithCostLimit(Position position, Price price, Quantity costLimit, User user, boolean isBuy);

    public void sellHoldings(User user, Position position) {
        if (getMarketClosed()) {
            warnMarketClosed(position, user);
            return;
        }
        Quantity sold = getMaker().sellHoldings(user, position);
        user.warn("Sold " + sold.printAsDetailedQuantity());
    }

    static void warnInsufficientAssets(Quantity assets, User user) {
        if (assets.isZero()) {
            user.warn(INSUFFICIENT_ASSETS_WARNING);
        } else {
            user.warn(TOO_MANY_SALES_WARNING);
        }
    }

    static void warnCantAfford(User user) {
        if (user.cashOnHand().isZero()) {
            user.warn(NO_MONEY_WARNING);
        } else {
            user.warn(CANNOT_AFFORD_WARNING);
        }
    }

    static void warnMarketClosed(Position position, Price price, Quantity totalQuantity, User user) {
        String logMessage = "Market is closed.  Order to buy " + totalQuantity + "@" + price +
                " of '" + position.getName() + "' not processed";
        user.warn("market closed", logMessage);
    }

    static void warnMarketClosed(Position position, User user) {
        String logMessage = "Market is closed.  Order to sell holdings of " +
                " of '" + position.getName() + "' not processed";
        user.warn("market closed", logMessage);
    }

    static void warnMarketClosedCostLimit(Position position, Price price, Quantity costLimit, User user) {
        String logMessage = "Market is closed.  Order to buy up to " + costLimit + " shares at " + price +
                " of '" + position.getName() + "' not processed";
        user.warn("market closed", logMessage);
    }

    public void close() {
        marketClosed = true;
    }

    public void open() {
        marketClosed = false;
    }

    public boolean isOpen() {
        return ! marketClosed;
    }

    public void resetBook() {
        getBook().resetOrders();
    }

    void updateLastTraded() {
        setLastTrade(new Date());
    }

    private void setLastTrade(Date lastTrade) {
        this.lastTrade = lastTrade;
    }

    public Date getLastTrade() {
        return lastTrade;
    }

    public Accounts settle(Quantity quantity, Map couponsMap) {
        return getCouponMint().settle(quantity, couponsMap, maxPrice());
    }

    public Coupons[] printNewCouponSets(Quantity quantity, Funds bothFunds) {
        return getCouponMint().printNewCouponSets(quantity, bothFunds, maxPrice());
    }

    public Quantity decideClaimAndRecord(Position pos) {
        close();
        final List ownersRedundant = HibernateUtil.couponOwners(pos.getClaim());
        Set owners = new HashSet(ownersRedundant);
        MarketMaker maker = getMaker();
        Quantity couponsRedeemed = getCouponMint().redeem(pos, owners, this, maker);
        if (null != maker) {
            Quantity refundAmount = maker.cashInAccount();
            getOwner().receiveCash(maker.provideCash(refundAmount));
            getOwner().warn(refundAmount.printAsDollars() + " was refunded at Market close.");
        }
        setOutcome(Outcome.newSingle(pos));
        if (HibernateUtil.isStatisticsEnabled()) {
            Logger log = Logger.getLogger("QueryStatistics");
            String[] queries = HibernateUtil.currentSession().getSessionFactory().getStatistics().getQueries();
            for (int i = 0; i < queries.length; i++) {
                String query = queries[i];
                log.info(query);
                log.info(HibernateUtil.currentSession().getSessionFactory().getStatistics().getQueryStatistics(query));
            }
        }

        return couponsRedeemed;
    }

    public void setWholeShareTradingOnly() {
        wholeShareTradingOnly = true;
    }

    public boolean wholeShareTradingOnly() {
        return wholeShareTradingOnly;
    }

    abstract MarketMaker getMaker();

    boolean getMarketClosed() {
        return marketClosed;
    }

    public boolean requireReserves() {
        return reservesRequired;
    }

    protected void withholdReserves(User user, Quantity quantityPurchased, Position pos) {
        if (reservesRequired && null != reserveVerifier) {
            reserveVerifier.enforce(user, quantityPurchased, pos);
        }
    }

    public void withholdReserves(Order order, Quantity quantity, Position position) {
        if (reservesRequired && null != reserveVerifier) {
            order.withholdReserves(reserveVerifier, quantity, position);
        }
    }

    protected boolean verifyReserves(User user, Position pos, Price price, Quantity quantity) {
        if (reservesRequired && null != reserveVerifier) {
            return reserveVerifier.verify(user, pos, price, quantity);
        }
        return true;
    }

    public Quantity requiredReserves(User user) throws ScoreException {
        if (reservesRequired) {
            return reserveVerifier.requiredReserves(user);
        } else {
            return Quantity.ZERO;
        }
    }

    protected void setReserveVerifier(ReserveVerifier reserveVerifier) {
        reservesRequired = reserveVerifier != null;
        this.reserveVerifier = reserveVerifier;
        reserveVerifier.addMarket(this);
    }

    /** @deprecated */
    public void setRequireReserves(boolean requireReserves) {
        reservesRequired = requireReserves;
    }

    /** @deprecated */
    void setMarketClosed(boolean marketClosed) {
        this.marketClosed = marketClosed;
    }

    /** @deprecated */
    boolean isPriceBetteringRequired() {
        return priceBetteringRequired;
    }

    /** @deprecated */
    void setPriceBetteringRequired(boolean priceBetteringRequired) {
        this.priceBetteringRequired = priceBetteringRequired;
    }

    /** @deprecated */
    boolean isWholeShareTradingOnly() {
        return wholeShareTradingOnly;
    }

    /** @deprecated */
    void setWholeShareTradingOnly(boolean wholeShareTradingOnly) {
        this.wholeShareTradingOnly = wholeShareTradingOnly;
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
    CouponBank getCouponMint() {
        return couponMint;
    }

    /** @deprecated */
    void setCouponMint(CouponBank couponMint) {
        this.couponMint = couponMint;
    }

    public User getOwner() {
        return owner;
    }

    /** @deprecated */
    public void setOwner(SecureUser owner) {
        this.owner = owner;
    }

    /** @deprecated */
    public Outcome getOutcome() {
        return outcome;
    }

    /** @deprecated */
    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    void setBook(Book book) {
        /** @deprecated */
        this.book = book;
    }

    public boolean identifyCouponBank(CouponBank bank) {
        return getCouponMint().identify(bank);
    }

    public Quantity mintBalance() {
        return getCouponMint().balance();
    }

    public Probability outcome(Position pos) {
        return getOutcome().outcome(pos);
    }

    public boolean outcomeIsContinuous() {
        return getOutcome().isContinuous();
    }

    public String describeOutcome() {
        return getOutcome().description();
    }

    public abstract void marketCallBack(MarketCallback callback);

    /** @deprecated */
    public void setMaxPrice(Price maxP) {
        maxPrice = maxP;
    }

    /** @deprecated */
    public Price getMaxPrice() {
        return maxPrice;
    }
}
