package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.*;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Currency;
import net.commerce.zocalo.logging.GID;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;

import java.util.Properties;

import org.apache.log4j.Logger;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** base class for subjects that trade: Traders and Manipulators.  */
public abstract class TradingSubject extends AbstractSubject {
    static final public Object BalanceComponent = "BALANCE";  // actual balance
    static final public Object AssetsComponent  = "ASSETs";    // coupon count
    static final public Object AverageComponent = "AVERAGE";  // average value of trades
    static final public Object TotalDividendComponent   = "DIVIDEND";
    static final public Object PrivateDividendComponent = "PRIVATE_DIVIDEND";
    static final public Object PublicDividendComponent  = "PUBLIC_DIVIDEND";
    private User user;
    protected Role role;

    public Quantity cashFromProperties(Properties props) {
        String roleTicketProperty = ENDOWMENT_PROPERTY_WORD + DOT_SEPARATOR + propertyWordForRole();
        String valueString = props.getProperty(roleTicketProperty);
        if ("".equals(valueString)) {
            return Quantity.ZERO;
        }
        return new Quantity(Double.parseDouble(valueString) / Currency.CURRENCY_SCALE);
    }

    public Quantity accountValueFromProperties(Properties props) {
        String roleCashProperty = TICKETS_PROPERTY_WORD + DOT_SEPARATOR + propertyWordForRole();
        String valueString = props.getProperty(roleCashProperty);
        if ("".equals(valueString)) {
            return Quantity.ZERO;
        }
        return new Quantity(valueString);
    }

    abstract public String propertyWordForRole();

    public TradingSubject(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    public Quantity balance() {
        return user.cashOnHand();
    }

    public String getName() {
        return user.getName();
    }

    public String logConfigValues(Logger log, Properties props, int rounds) {
        super.logConfigValues(log, props, rounds);
        String propertyName = getName() + DOT_SEPARATOR + HINT_PROPERTY_WORD;
        log.info(GID.log() + propertyName + ": " + props.getProperty(propertyName));
        return "";
    }

    public String getHint() {
        return user.getHint();
    }

    public void setHint(String hint) {
        user.setHint(hint);
    }

    public Quantity couponValue(BinaryClaim claim, Quantity actualValue) {
        return currentCouponCount(claim).times(actualValue);
    }

    public Quantity limitedCouponValue(BinaryClaim claim, Quantity actualValue, Session session) {
        int limit = session.getShareLimit(this);
        if (limit == 0) {
            return currentCouponCount(claim).times(actualValue);
        } else {
            Quantity qLimit = new Quantity(limit);
            return qLimit.min(currentCouponCount(claim)).times(actualValue);
        }
    }

    public Quantity currentCouponCount(BinaryClaim claim) {
        return user.couponCount(claim);
    }

    public User getUser() {
        return user;
    }

    protected String linkText() {
        return getUser().getName() + ", a " + propertyWordForRole() + ".";
    }

    public void rememberHoldings(BinaryClaim claim) {
        Quantity reserves = getUser().reserveBalance(claim.getNoPosition());
        addScoreComponent(BalanceComponent, getUser().cashOnHand().plus(reserves));
        rememberAssets(claim);
    }

    public void rememberAssets(BinaryClaim claim) {
        addScoreComponent(AssetsComponent, getUser().couponCount(claim));
    }

    public void addBonus(Funds funds) {
        addScoreComponent(BonusComponent, funds.getBalance());
        getUser().receiveCash(funds);
    }

    public Funds payDividend(Quantity perShareDividend, Quantity shares, Position pos) {
        Quantity reserves = getUser().reserveBalance(pos);
        Quantity totalDividend = perShareDividend.times(shares);
        if (reserves.compareTo(totalDividend) < 0) {
            return getUser().provideCash(Quantity.ZERO);
        }
        return getUser().releaseDividends(shares, perShareDividend, pos);
    }

    public void receiveDividend(Funds funds) {
        getUser().receiveCash(funds);
    }

    public void reduceReservesTo(Quantity perShare, Position pos) {
        getUser().reduceReservesTo(perShare, pos);
    }

    public Quantity totalDividend(BinaryClaim claim, Session session, int round) throws ScoreException {
        boolean privateDividendsInUse = session.privateDividendsInUse();
        Quantity privateDividend = session.getPrivateDividend(this, round);
        Quantity commonDividend = session.getDividend(this, round).minus(privateDividend);

        if (! privateDividendsInUse) {
            addScoreComponent(PublicDividendComponent, commonDividend);
        } else {
            if (! privateDividend.isZero()) {
                addScoreComponent(PrivateDividendComponent, privateDividend);
            }
            if (! commonDividend.isZero()) {
                addScoreComponent(PublicDividendComponent, commonDividend);
            }
        }
        Quantity totalDividend = limitedCouponValue(claim, privateDividend.plus(commonDividend), session);
        addScoreComponent(TotalDividendComponent, totalDividend);
        return totalDividend;
    }

    protected ScoreExplanationAccumulator assetValueTable(Properties props, String carryForwardAssetLabel, boolean keepingScore, boolean carryForward) {
        Quantity balance = getScoreComponent(BalanceComponent);
        ScoreExplanationAccumulator assetTableAccumulator = recordActualValueExplanations(props, carryForward);
        Quantity totalDividend = getScoreComponent(TotalDividendComponent);
        if (keepingScore) {
            assetTableAccumulator.addEntry(totalAssetsLabel(props), "dividendPaid", balance.plus(totalDividend));
        } else {
            assetTableAccumulator.addEntry(carryForwardAssetLabel, "dividendPaid", totalDividend);
        }
        assetTableAccumulator.addEntryIfDefined("", ScoreComponent, "score", this);
        assetTableAccumulator.addEntryIfDefined("", BonusComponent, "bonus", this);
        assetTableAccumulator.addEntryIfDefined("", AssetsComponent, "coupons", this);
        String aveLabel = PropertyHelper.averagePriceLabel(props);
        assetTableAccumulator.addEntryIfDefined(aveLabel, AverageComponent, "", this);
        return assetTableAccumulator;
    }

    protected ScoreExplanationAccumulator recordActualValueExplanations(Properties props, boolean carryForward) {
        ScoreExplanationAccumulator accumulator = new ScoreExplanationAccumulator();
        Quantity pubDividend = getScoreComponent(PublicDividendComponent);
        Quantity privDividend = getScoreComponent(PrivateDividendComponent);
        Quantity totalDividend = getScoreComponent(TotalDividendComponent);
        if (privDividend == null && totalDividend != null) {
            String dividendValueLabel;
            if (carryForward) {
                dividendValueLabel = dividendValueLabel(props);
            } else {
                dividendValueLabel = actualValueLabel(props);
            }
            accumulator.addEntry(dividendValueLabel, "dividend", pubDividend);
        } else {
            if (pubDividend != null) {
                accumulator.addEntry(publicValueLabel(props), "pubDividend", pubDividend);
            }
            if (privDividend != null) {
                accumulator.addEntry(PropertyHelper.privateValueLabel(props), "privDividend", privDividend);
            }
        }
        return accumulator;
    }

    public String pageLink() {
        return "Trader.jsp?userName=" + getUser().getName();
    }

    public void resetOutstandingOrders() {
        user.resetOutstandingOrders();
    }

    public boolean displayCarryForwardScores(Properties props) {
        String doDisplay = props.getProperty(DISPLAY_CARRY_FORWARD_SCORES);
        return (doDisplay == null || "".equals(doDisplay) || "true".equalsIgnoreCase(doDisplay));
    }

    public String publicValueLabel(Properties props) {
        return labelFromPropertyOrDefault(props, PUBLIC_VALUE_LABEL, DEFAULT_PUBLIC_VALUE_LABEL);
    }

    public boolean canBuy(int currentRound) {
        if (dormant(currentRound)) {
            return false;
        }
        return role.canBuy(currentRound);
    }

    public boolean canSell(int currentRound) {
        if (dormant(currentRound)) {
            return false;
        }
        return role.canSell(currentRound);
    }

    protected boolean dormant(int currentRound) {
        return super.dormant(currentRound) || role.dormant(currentRound);
    }

    public String roleName() {
        return role.roleKeyword();
    }
}
