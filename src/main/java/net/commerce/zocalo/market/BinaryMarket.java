package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.ajax.events.LimitTrade;
import net.commerce.zocalo.ajax.events.BookTrade;
import net.commerce.zocalo.orders.Order;

import java.util.Properties;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** a Market in a claim; always supports book orders, sometimes supports a marketMaker.
    Responsibilities include reporting on price levels and book contents as well as
    buying claims. */
public class BinaryMarket extends Market {
    private BinaryClaim claim;
    private MarketMaker maker;

/////////// FACTORY //////////////////////////////////////////////////////////

    protected BinaryMarket(CouponBank bank, SecureUser owner, BinaryClaim claim, Quantity maxPrice, int scale) {
        super(bank, owner, maxPrice);
        setBook(new BinaryBook(claim, this));
        this.claim = claim;
    }

    static public BinaryMarket make(SecureUser owner, BinaryClaim claim, Funds empty) {
        return make(claim, owner, empty, Price.ONE_DOLLAR);
    }

    public static BinaryMarket make(BinaryClaim claim, SecureUser owner, Funds empty, ReserveVerifier v) {
        BinaryMarket market = make(claim, owner, empty, Price.ONE_DOLLAR);
        if (v != null) {
            market.setReserveVerifier(v);
        }
        return market;
    }

    public static BinaryMarket make(BinaryClaim claim, SecureUser owner, Funds empty, Price maxPrice) {
        return make(claim, owner, empty, maxPrice, null);
    }

    public static BinaryMarket make(BinaryClaim claim, SecureUser owner, Funds empty, Price maxPrice, ReserveVerifier v) {
        CouponBank couponMint = CouponBank.makeBank(claim, empty);
        BinaryMarket market = new BinaryMarket(couponMint, owner, claim, maxPrice, 0);
        HibernateUtil.save(market);
        HibernateUtil.save(market.getBook());
        if (v != null) {
            market.setReserveVerifier(v);
        }
        return market;
    }

    public static BinaryMarket make(BinaryClaim claim, SecureUser user, Funds empty, Quantity maxPrice, int scale) {
        CouponBank couponMint = CouponBank.makeBank(claim, empty);
        BinaryMarket market = new BinaryMarket(couponMint, user, claim, maxPrice, scale);
        HibernateUtil.save(market);
        HibernateUtil.save(market.getBook());
        return market;
    }


    /** This factory method allows the owner to create unbalanced pairs of coupons. */
    static public BinaryMarket make(BinaryClaim claim, CouponBank couponMint, SecureUser owner, Properties props, ReserveVerifier verifier, int scale) {
        Quantity maxTradePrice = PropertyHelper.getMaxTradingPrice(props);
        BinaryMarket market = new BinaryMarket(couponMint, owner, claim, maxTradePrice, scale);
        HibernateUtil.save(market);
        HibernateUtil.save(market.getBook());
        market.setPriceBetteringRequired(PropertyHelper.getBetterPriceRequired(props));
        market.setWholeShareTradingOnly(PropertyHelper.getWholeShareTradingOnly(props));
        if (verifier != null) {
            market.setReserveVerifier(verifier);
        }
        return market;
    }

    public MarketMaker makeMarketMaker(SecureUser owner, Quantity endowment) {
        return makeMarketMaker(owner, endowment, Probability.HALF);
    }

    public MarketMaker makeMarketMaker(SecureUser owner, Quantity endowment, Probability initialProbability) {
        if (maker == null) {
            maker = new BinaryMarketMaker(this, owner, endowment, initialProbability);
            if (maker.cashInAccount().compareTo(endowment) != 0) {
                maker = null;
                return null;
            }
            HibernateUtil.save(maker);
        }
        return maker;
    }

    /** @deprecated */
    BinaryMarket() {
    }

//// ACCESSORS ///////////////////////////////////////////////////////////////

    public Claim getClaim() {
        return claim;
    }

    public BinaryClaim getBinaryClaim() {
        return claim;
    }

    public String getName() {
        return getClaim().getName();
    }

    public void setClaim(Claim claim) {
        this.claim = (BinaryClaim) claim;
    }

//// EXCHANGE ////////////////////////////////////////////////////////////////

    public void buyOrAddBookOrder(Position pos, Price price, Quantity quantity, User user) throws DuplicateOrderException {
        if (gateKeeper(pos, price, quantity, user, false)) {
            return;
        }

        Quantity quantityPurchased = purchaseAtMarketPrice(pos, price, quantity, user);

        if (! quantity.approaches(quantityPurchased)) {
            bookRemainderIfAffordable(pos, price, quantity, user, quantityPurchased);
        }
    }

    protected void buyAtMarketPrice(Position pos, Price price, Quantity quantity, User user) {
        if (gateKeeper(pos, price, quantity, user, true)) {
            return;
        }

        Quantity quantityPurchased = purchaseAtMarketPrice(pos, price, quantity, user);

        if (quantityPurchased.isZero()) {
            warnUserNoSale(price, quantity, user);
        }
    }

    protected Quantity purchaseAtMarketPrice(Position pos, Price price, Quantity quantity, User user) {
        Quantity quantityPurchased;
        if (! hasMaker()) {
            quantityPurchased = getBook().buyFromBookOrders(pos, price, quantity, user);
        } else if (! getBook().hasOrdersToSell(pos)
                && currentProbability(pos).compareTo(price.asProbability()) < 0) {
            quantityPurchased = getMaker().buyUpToQuantity(pos, price.asProbability(), quantity, user);
            user.reportMarketMakerPurchase(quantityPurchased, pos, ! pos.isInvertedPosition());
        } else {
            quantityPurchased = buyFromBothBookAndMaker(pos, price, quantity, user);
        }

        postPurchaseActions(pos, user, quantityPurchased);
        return quantityPurchased;
    }

    protected void postPurchaseActions(Position pos, User user, Quantity quantityPurchased) {
        if (! quantityPurchased.isNegligible()) {
            updateLastTraded();
        }
        if (requireReserves()) {
            withholdReserves(user, quantityPurchased, pos);
        }
    }

    private void bookRemainderIfAffordable(Position pos, Price price, Quantity quantity, User user, Quantity quantityPurchased) throws DuplicateOrderException {
        Quantity outstandingCosts = user.outstandingOrderCost(pos);
        if (user.canAfford(pos, quantity.minus(quantityPurchased), price, outstandingCosts)) {
            try {
                getBook().addOrder(pos, price, quantity.minus(quantityPurchased), user);
            } catch (IncompatibleOrderException e) {
                e.printStackTrace();
                user.warn("An error prevented the server from placing a book order for any unmatched part of your order.");
            }
        } else {
            if (quantityPurchased.isZero()) {
                user.warn("You can't afford that order.");
            } else {
                user.warn("order partially processed; remainder unaffordable.");
            }
        }
    }

    private void warnUserNoSale(Price price, Quantity quantity, User user) {
        String msg = "No shares available";
        String quantString = "";
        if (quantity.compareTo(Quantity.ONE) != 0) {
            quantString = " for " + quantity;
        }
        String priceString = " at " + price;
        user.warn(msg, "told " + user.getName() + " " + msg + " for a market order" + quantString + priceString);
    }

    public Quantity buyWithCostLimit(Position position, Price price, Quantity costLimit, User user, boolean isBuy) {
        if (getMarketClosed()) {
            warnMarketClosedCostLimit(position, price, costLimit, user);
            return Quantity.ZERO;
        }
        if (! isBuy) {
            return buyWithCostLimit(position.opposite(), price,  costLimit, user);
        } else {
            return buyWithCostLimit(position, price,  costLimit, user);
        }
    }

    MarketMaker getMaker() {
        return maker;
    }

    public void marketCallBack(MarketCallback callback) {
        callback.binaryMarket(this);
    }

    public void setMaker(MarketMaker maker) {
        this.maker = maker;
    }

    public Quantity buyWithCostLimit(Position pos, Price price, Quantity costLimit, User user) {
        Quantity startingCash = user.cashOnHand();
        Quantity actualLimit = costLimit.min(startingCash);
        Quantity availableCash = actualLimit;
        Quantity cumulativeShares = Quantity.ZERO;

        while (availableCash.isPositive()) {
            Quantity shares;
            if (maker == null) {
                Price bestPrice = getBook().bestSellOfferFor(pos);
                if (bestPrice == null || bestPrice.equals(maxPrice)) {
                    break;
                }
                shares = getBook().buyFromBestBookOrders(pos, bestPrice, availableCash.div(bestPrice), user);
            } else {
                Probability targetProbability = price.asProbability().min(maker.newPFromBaseC(pos, availableCash));
                if (getBook() == null) {
                    shares = maker.buyWithCostLimit(pos, targetProbability, availableCash, user);
                } else {
                    Price bestBookPrice = getBook().bestSellOfferFor(pos);
                    if (Probability.ALWAYS.equals(bestBookPrice.asProbability())
                            || targetProbability.compareTo(bestBookPrice.asProbability()) < 0) {
                        shares = maker.buyWithCostLimit(pos, bestBookPrice.asProbability(), availableCash, user);
                    } else {
                        shares = getBook().buyFromBestBookOrders(pos, bestBookPrice, availableCash.div(bestBookPrice), user);
                    }
                }
            }
            if (shares.isZero()) {
                break;
            }
            cumulativeShares = cumulativeShares.plus(shares);
            Quantity spent = startingCash.minus(user.cashOnHand());
            availableCash = actualLimit.minus(spent);
        }

        if (! cumulativeShares.isNegligible()) {
            updateLastTraded();
        }

        return cumulativeShares;
    }

    /* See http://pancrit.blogspot.com/2007/01/integrating-book-orders-and-market.html for some commentary
     on the role played by this method. */
    private Quantity buyFromBothBookAndMaker(Position position, Price price, Quantity quantity, User user) {
        Quantity quantityBought = Quantity.ZERO;
        Quantity quantityBoughtFromMaker = Quantity.ZERO;
        if (bestPrice(position).compareTo(price) > 0) {
            return Quantity.ZERO;
        }

        while (quantity.compareTo(quantityBought) > 0) {
            Price bestBookPrice = getBook().bestSellOfferFor(position);
            if (getMaker().roundedPriceEqualsRoundedProb(position, bestBookPrice, lowRes)) {
                Quantity incrementalPurchase = getBook().buyFromBestBookOrders(position, price, quantity.minus(quantityBought), user);
                if (incrementalPurchase.isZero()) {
                    break;
                }
                quantityBought = quantityBought.plus(incrementalPurchase);
            } else {
                Probability minProbability = bestBookPrice.asProbability().min(price.asProbability());
                Quantity thisQuant = getMaker().buyUpToQuantity(position, minProbability, quantity.minus(quantityBought), user);
                if (thisQuant.isZero()) {
                    break;
                }
                quantityBoughtFromMaker = quantityBoughtFromMaker.plus(thisQuant);
                quantityBought = quantityBought.plus(thisQuant);
            }

            if (getMaker().roundedPriceEqualsRoundedProb(position, price, lowRes) || user.negligibleCashOnHand()) {
                break;
            }
        }

        user.reportMarketMakerPurchase(quantityBoughtFromMaker, position, ! position.isInvertedPosition());
        if (! quantityBought.isNegligible()) {
            updateLastTraded();
        }
        return quantityBought;
    }

    public void recordLimitTrade(Order order, Quantity quantityTraded, Position opposite) {
        Price price = order.price();
        String userName = order.ownerName();
        LimitTrade.newLimitTrade(userName, price, quantityTraded, opposite.opposite());
    }

    public void recordBookTrade(User acceptor, Price price, Quantity quantity, Position position) {
        BookTrade.newBookTrade(acceptor.getName(), price, quantity, position);
        acceptor.reportBookPurchase(quantity, position);
    }

    public Price maxPrice() {
        return getMaxPrice();
    }

    /** @deprecated */
    void setClaim(BinaryClaim claim) {
        this.claim = claim;
    }
}
