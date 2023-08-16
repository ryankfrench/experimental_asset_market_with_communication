package net.commerce.zocalo.market;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copybe
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.freechart.ChartScheduler;

import java.util.Dictionary;
import java.util.Set;
import java.math.MathContext;

import org.apache.log4j.Logger;

/** A MarketMaker offers to buy and sell coupons for a particular claim.
     See Robin Hanson's paper on <a href="http://hanson.gmu.edu/mktscore.pdf">Scoring
    Rules</a> for details.   The price is
    determined by the history of past sales.  Each time the MarketMaker (sometimes
    referred to as AMM) sells, the price increases, each time it buys, the price
    decreases.  This MarketMaker uses an exponential rule to determine how much to change
    the price.<p>

    The market maker follows an exponential rule based on its current price, which means
    that when its current price (p) is changing to newP, the user pays log(newP/p) coupons
    on one side to get log((1 - newP) / (1 - p)) coupons on the other side.  After each such
    transaction, the marketMaker moves the probability to newP.  Notice that this is
    expressed in terms of trading coupons for coupons.<p>

    Since the user thinks in terms of paying money for coupons, we need to translate.  The
    user uses |log((1 - newP) / (1 - p))| [which we call baseC] (times the cost of a coupon)
    in funds to buy baseC
    sets of coupons, and then trades those coupons on one side for |log(newProb/prob)|
    [which we call incrC] coupons on the other side.  The net effect is that the user is
    down by baseC (* couponCost) in funds, and up by |incrC - baseC| [which we call totalC]
    coupons.  The
    transaction actually happens by having the marketMaker accept the funds, buy the coupons,
    and make the trade.  Notice that baseC and incrC have opposite signs appropriately
    reflecting the user's costs and gains, but we always transfer positive quantities.<p>

    We use this new terminology because the ratios apply to different steps for different
    transactions.  The user may be buying or selling a single position.  (In the binary
    case these are symmetrical, but not in the multi-outome case.)  Complementary coupons
    (complementary sets in the multi-outcome case) can be used in the purchase instead of
    cash.  I use "C" in these names because it's short, and it evokes "cost", "coupons",
    and "cash".  None of these would be right, since each name sometimes refers to cost
    and other times to gain, and each applies to cash and coupons in different
    transactions.<p>

    The simplest case is the user providing funds in exchange for coupons.  The user
    spends baseC in funds, and receives totalC in coupons (the AMM adds incrC in funds to
    the user's baseC to buy totalC sets from the bank, and keeps the unwanted coupons in
    its account.)  The next simplest case is selling a position when the user already has
    sufficient coupons.  The user provides totalC coupons and receives baseC in funds,
    which the AMM gets by combining the user's coupons with its own and turning them in
    to the bank for cash, of which it keeps incrC.  If the user wants to sell and doesn't
    have coupons, it costs incrC, which earns totalC in complementary assets.  In this
    case, the AMM contributes baseC in funds.  So the ratios apply differently in buying
    and selling and sometimes refer to the user's contribution and sometimes the AMM's.<p>

    Traders can specify the size of a trade in terms of the target probability (or
    equivalently price), the maximum number of shares, or the maximum cost (a maximum cost
    is also imposed by the trader's budget.)  We have to be able to compute newP from any
    of these choices, and find the most restrictive limit.  We sometimes need to compute
    newP from incrC, baseC, or totalC.<p>

    Another item worth clarifying about the explanation above: I spoke in terms of "sets".
    For binary markets I would have said "pairs", which would have been reasonably clear.
    In the multi-outome case, the arithmetic works out that the user can specify any subset
    of the outcomes, and everything works out.  If a claim has 5 possible outcomes (a, b,
    c, d, and e) and the user wants to buy a and c in preference to b, d, and e, then you
    add the probabilities for a and c to find p (the others add up to [1-p]) and follow
    the same procedure.  The user pays baseC for baseC sets (which means baseC of each
    outcome), and then trades the baseC coupons of b, d, and e for incrC coupons of a and
    incrC of c.  The user is out baseC in funds, and now has totalC of a and totalC of c.
    The market maker has spent incrC in funds and has totalC of b, d, and e.  The bank has
    collected totalC in funds and issued totalC complete sets.  The code currently only
    supports specifying one outcome to purchase, but that's mostly because the UI issues
    for specifying other combinations are complicated.<p>
  */

public abstract class MarketMaker {
    private Quantity beta;  /** the constant factor in the scoring rule  */
    private Accounts accounts;
    private Market market;
    private long id;
    public static final double EPSILON = 0.0001;

    public MarketMaker(Market market, Quantity subsidy, User owner) {
        this.market = market;
        accounts = new Accounts(owner.getAccounts().provideCash(subsidy));
    }

    /** Beta is the scaling factor in the scoring rule.
     @see <a href="http://hanson.gmu.edu/combobet.pdf">Combinatorial Information Market Design</a> By Robin Hanson*/
    void initBetaExplicit(Quantity endowment, Probability minProbability) {
//        beta = endowment / Math.log(1.0 / minProbability);
//        beta = endowment.div(Quantity.ONE.div(minProbability).absLog());
        setBeta(endowment.div(maxPrice()).div(Quantity.ONE.div(minProbability).absLog()));
    }

    void initBeta(Quantity endowment, double outcomeCount) {
        Quantity logOutcomes = new Quantity(Math.log(outcomeCount));
        setBeta(endowment.div(maxPrice()).div(logOutcomes));
    }

    /** @deprecated */
    MarketMaker() {
    }

//// ACCESSORS ///////////////////////////////////////////////////////////////
    public boolean roundedPriceEqualsRoundedProb(Position position, Price price, MathContext context) {
        Probability prob = currentProbability(position);
        return 0 == price.asProbability().round(context).compareTo(prob.round(context));
    }

    public abstract Probability currentProbability(Position position);

//// TRADING /////////////////////////////////////////////////////////////////

    /** purchase from the marketMaker up to a probability of TARGETPROBABILITY,
        and up to COSTLIMIT total funds, whichever is less. */
    public Quantity buyWithCostLimit(Position position, Probability targetProbability, Quantity costLimit, User user) {
        if (targetProbability.equals(currentProbability(position))) {
            return Quantity.ZERO;
        }

        boolean buying = targetProbability.compareTo(currentProbability(position)) > 0;
        Probability prob = targetProbability;
        if (buying) {
            if (Probability.ALWAYS.equals(targetProbability) ||
                    baseC(position, targetProbability).times(maxPrice()).compareTo(costLimit) > 0) {
                prob = newPFromBaseC(position, costLimit.div(maxPrice()));
            }
        } else {
            Quantity costToReachTarget = incrC(position, targetProbability).times(maxPrice());
            if (costToReachTarget.compareTo(costLimit) > 0) {
                prob = newPFromIncrC(position, costLimit.negate().div(maxPrice()));
            }
        }

        Quantity quantityBought = purchase(position, prob, user);
        user.reportMarketMakerPurchase(quantityBought, position, buying);
        return quantityBought;
    }

    public Quantity buyOrSellUpToQuantity(Position position, Price price, Quantity quantity, User user) {
        Quantity quantityBought = buyUpToQuantity(position, price.asProbability(), quantity, user);
        boolean buying = price.asProbability().compareTo(currentProbability(position)) > 0;
        user.reportMarketMakerPurchase(quantityBought, position, buying);
        return quantityBought;
    }

    /** buy from the marketMaker to make the probability reach TARGETPROBABILITY,
        and up to MAXQUANTITY total coupons, whichever is less. */
    public Quantity buyUpToQuantity(Position position, Probability targetProbability, Quantity maxQuantity, User user) {
        Quantity quantityForFullProbabilityMovement = totalC(position, targetProbability);
        Probability prob = targetProbability;
        if (quantityForFullProbabilityMovement.compareTo(maxQuantity) > 0) {
            prob = probLimit(position, targetProbability, maxQuantity);
        }
        return purchase(position, prob, user);
    }

    private Probability probLimit(Position pos, Probability targetProb, Quantity maxQuantity) {
        Probability curProb = currentProbability(pos);
        if (targetProb.compareTo(curProb) > 0) {
            Probability quantLimitP = newPFromTotalC(pos, maxQuantity.negate());
            return curProb.max(targetProb.min(quantLimitP));
        } else {
            Probability quantLimitP = newPFromTotalC(pos, maxQuantity);
            return curProb.min(targetProb.max(quantLimitP));
        }
    }

    Quantity purchase(Position position, Probability desiredProb, User user) {
        boolean buying = desiredProb.compareTo(currentProbability(position)) > 0;
        Quantity complementsBought = buyComplementsFromTrader(position, user, desiredProb);
        Probability newProb = currentProbability(position);
        Dictionary<Position, Probability> allStartProbs = currentProbabilities(position);
        boolean finished = buying
                ? desiredProb.compareTo(newProb) <= 0
                : desiredProb.compareTo(newProb) >= 0;
        Quantity quantityPurchased = Quantity.ZERO;
        Quantity paymentAmount = Quantity.ZERO;
        if (! finished) {
            Probability affordableProb = affordable(position, desiredProb, user, buying);
            paymentAmount = maxPrice().times(buying
                    ? baseC(position, affordableProb)
                    : incrC(position, affordableProb));

            quantityPurchased = sellCouponsToTrader(position, user, buying, paymentAmount, affordableProb);
        }
        if (! quantityPurchased.isZero() && ! paymentAmount.isZero() && ! quantityPurchased.isNegligible()) {
            recordTrade(user.getName(), quantityPurchased, paymentAmount, position, allStartProbs);
        }
        return quantityPurchased.plus(complementsBought);
    }

    private Quantity sellCouponsToTrader(Position position, User user, boolean buying, Quantity paymentAmount, Probability affordableProb) {
        Quantity quantityPurchased;
        Funds payment = user.getAccounts().provideCash(paymentAmount);
        if (payment.getBalance().compareTo(paymentAmount) < 0) {
            user.receiveCash(payment);
            Logger log = Logger.getLogger(MarketMaker.class);
            log.warn("User (" + user.getName() +
                    ") couldn't provide promised cash (" + paymentAmount + ").");
            return Quantity.ZERO;
        }

        receiveCash(payment);
        quantityPurchased = totalC(position, affordableProb);

        Set coupons = provideCouponSet(position, quantityPurchased, buying);
        addAllCoupons(coupons, user);
        accounts().settle(market());
        scaleProbabilities(position, affordableProb);
        return quantityPurchased;
    }

    private Quantity buyComplementsFromTrader(Position position, User user, Probability desiredProb) {
        boolean buying = desiredProb.compareTo(currentProbability(position)) > 0;
        Dictionary<Position, Probability> startProbs = currentProbabilities(position);

        Quantity availableCoupons;
        Probability targetProb;
        if (buying) {
            availableCoupons = user.minCouponsVersus(position);
            Probability affordableProb = newPFromTotalC(position, availableCoupons.negate());
            targetProb = affordableProb.min(desiredProb);
        } else {
            availableCoupons = user.couponCount(position);
            Probability affordableProb = newPFromTotalC(position, availableCoupons);
            targetProb = affordableProb.max(desiredProb);
        }
        if (availableCoupons.isNegligible()) {
            return Quantity.ZERO;
        }

        Quantity couponsRequired = totalC(position, targetProb);
        if (couponsRequired.isZero()) {
            return couponsRequired;
        }
        Quantity cashValue = maxPrice().times(buying
                ? incrC(position, targetProb)
                : baseC(position, targetProb));

        Set<Coupons> compCoupons = user.getAccounts().provideCouponSets(position, couponsRequired, ! buying);
        accounts().addCoupons(compCoupons);
        accounts().settle(market());
        Funds funds = provideCash(cashValue);
        user.receiveCash(funds);
        scaleProbabilities(position, targetProb);
        recordTrade(user.getName(), couponsRequired, cashValue, position, startProbs);
        return couponsRequired;
    }

    public Quantity sellHoldings(User user, Position pos) {
        Quantity holdings = user.couponCount(pos);
        Probability targetProb = newPFromTotalC(pos, holdings);
        Quantity sharesSold = buyComplementsFromTrader(pos, user, targetProb);
        scheduleChartGeneration();
        return sharesSold;
    }

    private Probability affordable(Position position, Probability desiredProb, User user, boolean buying) {
        Quantity desiredPayment = buying
                ? baseC(position, desiredProb)
                : incrC(position, desiredProb);

        if (user.cashSameOrGreaterThan(desiredPayment.times(maxPrice()))) {
            return desiredProb;
        } else if (buying) {
            return newPFromBaseC(position, user.cashOnHand().div(maxPrice()));
        } else {
            return newPFromBaseC(position, user.cashOnHand().div(maxPrice()).negate());
        }
    }

    public void addAllCoupons(Set couponsSet, User user) {
        user.getAccounts().addCoupons(couponsSet);
        user.settle(market());
    }

    abstract Set<Coupons> provideCouponSet(Position position, Quantity shares, boolean buying);

    /** Record trade suitably for type of market. */
    abstract void recordTrade(String name, Quantity coupons, Quantity cost, Position position, Dictionary<Position, Probability> startProbs);

    void scheduleChartGeneration() {
        ChartScheduler chartScheduler = ChartScheduler.find(market().getName());
        if (chartScheduler != null) {
            chartScheduler.generateNewChart();
        }
    }

    abstract Dictionary<Position, Probability> currentProbabilities(Position pos);

    Funds provideCash(Quantity amount) {
        return accounts().provideCash(amount);
    }

    Quantity cashInAccount() {
        return accounts().cashValue();
    }

    void receiveCash(Funds funds) {
        accounts().receiveCash(funds);
    }

    Price scaleToPrice(Probability probability) {
        return market().scaleToPrice(probability);
    }

    abstract void scaleProbabilities(Position position, Probability newProb);

    Coupons provideCoupons(Position position, Quantity amount) {
        ensureSetsAvailable(position, amount);
        return accounts().provideCoupons(position, amount);
    }

    void ensureSetsAvailable(Position position, Quantity amount) {
        Quantity available = accounts().couponCount(position);
        if (available.compareTo(amount) < 0) {
            if (available.plus(cashInAccount()).compareTo(amount) > 0) {
                Quantity quantityToBuy = amount.minus(available);
                Funds funds = provideCash(quantityToBuy.times(maxPrice()));
                buyMoreCoupons(quantityToBuy, funds);
            } else {
                throw new RuntimeException("MarketMaker cannot afford to buy sufficient tickets!");
            }
        }
    }

    void ensureOpposingSetsAvailable(Position position, Quantity amount) {
        Quantity available = accounts().minCouponsVersus(position);
        if (available.compareTo(amount) < 0) {
            if (available.plus(cashInAccount()).compareTo(amount) >= 0 ) {
                Funds funds = provideCash(amount.minus(available).times(maxPrice()));
                buyMoreCoupons(amount.minus(available), funds);
            } else {
                throw new RuntimeException("MarketMaker can't afford to buy sufficient tickets!");
            }
        }
    }

    void buyMoreCoupons(Quantity amount, Funds funds) {
        Market m = market();
        accounts().addAll(m.printNewCouponSets(amount, funds));
    }

//// COMPUTING PRICES, PROBABILITIES, AND QUANTITIES /////////////////////////

    /** How many pairs (totC) will be purchased to move the probability (price) to
        targetProb?  In order to move the probability from p to newP,
        |log((1 - newP) / (1 - P))| (=baseC) (* couponCost) in funds is used to buy baseC sets of coupons.
        The new undesired coupons are traded for |log(newP/p)| (=incrC) more of the desired
        coupons.  The user ends up down by baseC (* couponCost) in funds and up by
        |baseC - incrC| (=totalC) in coupons. */

    Quantity incrC(Position position, Probability targetProbability) {
        return beta().times(targetProbability.div(currentProbability(position)).absLog());
    }

    /** what would the probability be after buying QUANT coupons? */
    private Probability newPFromIncrC(Position position, Quantity quantity) {
        return new Probability(currentProbability(position).times(quantity.div(beta()).exp()));
//        return currentProbability(position) * Math.exp(quantity / beta());
    }

    /** The money price charged to move the probability from p to newP is |B * log((1 - newP)/(1 - p)| * couponCost*/
    protected Quantity baseC(Position position, Probability targetProbability) {
        Probability currentInverted = currentProbability(position).inverted();
        Probability targetInverted = targetProbability.inverted();
        if (targetInverted.isZero() || currentInverted.isZero()) {
            throw new ArithmeticException("probabilities can't be zero or one.");
        }
        return beta().times(targetInverted.div(currentInverted).absLog());
    }

    /** what would the probability be after spending COST?   After spending COST,
     the user has gained COST new pairs, and will trade the undesired coupons for
     desireable ones.  The new probability will be (1 - ((1-p)*exp(COST)).   */
    Probability newPFromBaseC(Position position, Quantity cost) {
        Quantity baseC = cost.div(beta());
        Probability curPNot = currentProbability(position).inverted();
        return new Probability(curPNot.div(baseC.exp())).inverted();
    }

    private Probability newPFromTotalC(Position position, Quantity totalC) {
        Quantity expTotalC = totalC.div(beta()).exp();
        Probability curProb = currentProbability(position);
        Probability curPNot = curProb.inverted();
        return new Probability(curProb.div(curProb.plus(curPNot.times(expTotalC))));
    }

    //  totalC is |baseC - incrC|.  (BaseC and IncrC have opposite signs)
    //  baseC = beta*|log((1 - newP) / (1 - p))|      incrC =  beta*|log(newProb/prob)|
    //     totalC = beta * | log((1 - newP) / (1 - p)) / (newProb/prob)) |
    //  so totalC = beta * | log(newP * (1 - p) / (p * (1 - newP))) |
    private Quantity totalC(Position position, Probability newP) {
        Probability curP = currentProbability(position);
        Probability curPNot = curP.inverted();
        Probability newPNot = newP.inverted();
        return beta().times(newP.times(curPNot).div(curP.times(newPNot)).absLog());
    }

    /** return my accounts only if the requestor knows my market's couponBank.  Since Market doesn't
        hand out its couponBank, it's someone with access to the market's closely-held secrets.  */
    public Accounts redeem(CouponBank couponBank) {
        if (market.identifyCouponBank(couponBank)) {
            return accounts();
        }
        return null;
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    Quantity beta() {
        return getBeta();
    }

    Accounts accounts() {
        return getAccounts();
    }

    /** @deprecated */
    Accounts getAccounts() {
        return accounts;
    }

    /** @deprecated */
    void setAccounts(Accounts accounts) {
        this.accounts = accounts;
    }

    Market market() {
        return getMarket();
    }

    /** @deprecated */
    Market getMarket() {
        return market;
    }

    /** @deprecated */
    void setMarket(Market market) {
        this.market = market;
    }

    /** @deprecated */
    public Quantity getBeta() {
        return beta;
    }

    /** @deprecated */
    public void setBeta(Quantity beta) {
        this.beta = beta;
    }

    public Price maxPrice() {
        return getMarket().maxPrice();
    }
}
