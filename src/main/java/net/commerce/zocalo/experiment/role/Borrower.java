package net.commerce.zocalo.experiment.role;

// Copyright 2009, 2010 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.ajax.dispatch.MockBayeuxChannel;
import net.commerce.zocalo.ajax.dispatch.TradeEventDispatcher;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;

import java.util.*;

import static net.commerce.zocalo.experiment.LendingSession.CapitalGainsMethod;
import static net.commerce.zocalo.experiment.LendingSession.CapitalGainsMethod.MARK_TO_MARKET;
import static net.commerce.zocalo.service.PropertyKeywords.*;

public class Borrower extends Trader {
    static final public Object LastTradePriceComponent  = "LAST TRADE PRICE";  // last trade price (== current market price)
    static final public Object CapitalGainsComponent    = "CAPITAL GAINS";  // increase in value of shares
    static final public Object NetEarningsComponent     = "NET EARNINGS";  // increase in net worth
    static final public Object LoanChangeComponent      = "LOAN CHANGE";  // allowed change in loan balance this period
    static final public Object AvailableLoanComponent   = "AVAILABLE_LOAN";
    static final public Object OutstandingLoanComponent = "OUTSTANDING_LOAN";
    static final public Object HistoricalShareValue     = "HISTORICAL_SHARE_VALUE";  // Share value based on Historical costs.
    static final public Object FundamentalShareValue    = "FUNDAMENTAL_SHARE_VALUE";  // Share value based on Fundamentals.

    static final public Object DefaultComponent         = "IN DEFAULT";   // signals that loan is in default
    private Quantity outstandingLoan;
    private boolean acceptedLoanMod[];
    private boolean inDefault = false;
    private Quantity defaultAmount;

    protected Borrower(User user, Role role) {
        super(user, role);
        outstandingLoan = Quantity.ZERO;
    }

    static public Trader makeTrader(User user, Role role) {
        return new Borrower(user, role);
    }

    void resetScores(int rounds) {
        super.resetScores(rounds);
        acceptedLoanMod = new boolean[rounds + 1];
        for (int i = 0; i < acceptedLoanMod.length; i++) {
            acceptedLoanMod[i] = false;
        }
    }

    public Quantity loanAmount() {
        return outstandingLoan;
    }

    public void increaseLoanAmount(Quantity amount) {
        outstandingLoan = outstandingLoan.plus(amount);
    }

    public Funds decreaseLending(Quantity amount) {
        Funds funds = getUser().getAccounts().provideCash(amount);
        decreaseLoanAmount(funds.getBalance());
        return funds;
    }

    public void decreaseLoanAmount(Quantity amount) {
        outstandingLoan = outstandingLoan.minus(amount);
    }

    public void setAccepted(int currentRound, Quantity increase) {
        acceptedLoanMod[currentRound] = true;
        Logger logger = getScoreLogger();
        logger.info("Borrower " + getName() + " accepted loan increase of " + increase + ".");
    }

    public boolean acceptedLoanMod(int round) {
        return acceptedLoanMod[round];
    }

    public void rememberLoanState(Quantity available) {
        Quantity outstanding = loanAmount();
        addScoreComponent(OutstandingLoanComponent, outstanding);
        addScoreComponent(AvailableLoanComponent, available);
        addScoreComponent(LoanChangeComponent, available.minus(outstanding));
        if (outstanding.compareTo(available) > 0) {
            marginCall(outstanding, available);
        }
    }

    public void rememberDividends(Quantity totalDividend) {
        addScoreComponent(TotalDividendComponent, totalDividend);
    }

    // trader's loan balance must be reduced. Try to cover with cash, else seize account and sell assets
    private void marginCall(Quantity outstanding, Quantity available) {
        Quantity balance = getScoreComponent(BalanceComponent);
        Quantity overLimit = outstanding.minus(available);
        Funds discardedBalance;
        if (balance.compareTo(overLimit) > 0) {
            discardedBalance = decreaseLending(overLimit);
        } else {
            inDefault = true;
            discardedBalance = decreaseLending(balance);
            defaultAmount = overLimit.minus(balance);
            addScoreComponent(DefaultComponent, defaultAmount);
        }
        addScoreComponent(BalanceComponent, balance.minus(discardedBalance.getBalance()));
    }

    public boolean canBuy(int currentRound) {
        if (inDefault) {
            return false;
        }
        return super.canBuy(currentRound);
    }

    public boolean canSell(int currentRound) {
        if (inDefault) {
            return false;
        }
        return super.canSell(currentRound);
    }

    public Quantity getDefaultAmount() {
        return defaultAmount;
    }

    public boolean inDefault() {
        return inDefault;
    }

    public void endDefault(Funds funds) {
        inDefault = false;
        increaseLoanAmount(defaultAmount.negate());
        defaultAmount = Quantity.ZERO;
        getUser().receiveCash(funds);
    }

    public void recordGainsAndLosses(CapitalGainsMethod method, Properties props, boolean makingLoans, boolean lastRound) {
        addScoreComponent(Borrower.OutstandingLoanComponent, loanAmount());
        addScoreComponent(NetEarningsComponent, netEarnings());
        ScoreExplanationAccumulator leftSide = new ScoreExplanationAccumulator();
        addEntryForDividend(props, leftSide);
        Quantity totalDividend = getScoreComponent(TotalDividendComponent);
        String dividendLabel = PropertyHelper.dividendAddedToCashLabel(props);
        leftSide.addEntry(dividendLabel, "dividendPaid", totalDividend);
        Quantity capitalGains = getScoreComponent(CapitalGainsComponent);
        leftSide.addEntry(capitalGainsLabel(props), "gains", capitalGains);
        Quantity netEarnings = getScoreComponent(NetEarningsComponent);
        leftSide.addEntry(netEarningsLabel(props), "", netEarnings);
        Quantity lastTrade = getScoreComponent(LastTradePriceComponent);
        leftSide.addEntry(lastTradePriceLabel(props), "", lastTrade);

        ScoreExplanationAccumulator rightSide = new ScoreExplanationAccumulator();
        Quantity defaultAmount = getScoreComponent(DefaultComponent);
        if (makingLoans) {
            loanStatusExplanation(props, rightSide, defaultAmount);
        }
        Quantity balance = getScoreComponent(BalanceComponent).plus(totalDividend);
        rightSide.addEntry(balanceLabel(props), "balance",  balance);
        Quantity shareValue;
        Quantity assetQuantity = getScoreComponent(TradingSubject.AssetsComponent);
        switch (method) {
            case HISTORIC_COST:
                shareValue = lastRound
                        ? Quantity.ZERO
                        : getScoreComponent(HistoricalShareValue); 
                break;
            case FUNDAMENTAL_VALUES:
                shareValue = assetQuantity.times(getScoreComponent(FundamentalShareValue));
                break;
            case MARK_TO_MARKET:
            default:
                shareValue = lastRound
                        ? Quantity.ZERO
                        : assetQuantity.times(lastTrade);
                break;
        }
        rightSide.addEntry(shareValueLabel(props), "", shareValue);
        rightSide.addEntry(totalLabel(props), "", balance.plus(shareValue));
        if (makingLoans) {
            Quantity loansOutstanding = getScoreComponent(Borrower.OutstandingLoanComponent);
            rightSide.addEntry(loansLabel(props), "loans", loansOutstanding);
            rightSide.addEntry(netWorthLabel(props), "", balance.plus(shareValue).minus(loansOutstanding));
        } else {
            rightSide.addEntry(netWorthLabel(props), "", balance.plus(shareValue));
        }

        StringBuffer buf = new StringBuffer();
        if (suppressAccounting(props)) {
            rightSide.addEntry(dividendLabel, "dividendPaid", totalDividend);
            ScoreExplanationAccumulator.renderAsOneColumn(rightSide, buf);
        } else {
            ScoreExplanationAccumulator.renderAsTwoColumns(leftSide, rightSide, buf);
        }
        leftSide.log(getName(), getScoreLogger());
        rightSide.log(getName(), getScoreLogger());
        scoreExplanation = buf.toString();
    }

    private Quantity netEarnings() {
        Quantity totalDividend = getScoreComponent(TotalDividendComponent, Quantity.ZERO);
        return totalDividend.plus(getScoreComponent(CapitalGainsComponent, Quantity.ZERO));
    }

    public void computeCapitalGains(MockBayeuxChannel tradeHistory, Quantity initialCouponValue, 
            int currentRound, CapitalGainsMethod capitalGainsMethod, Quantity fundamentalValue) {
        Quantity gain = Quantity.ZERO;
        Quantity lastTradePrice = initialCouponValue;
        int prevEventRound = 0;
        int numberOfCoupons = role.getInitialCoupons().asValue().intValue();
        LinkedList<Quantity> coupons = new LinkedList<Quantity>();
        initializePrices(lastTradePrice, numberOfCoupons, coupons);

        List<Map> events = tradeHistory.getEvents(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        String userName = getName();
        if (events.size() == 0) {
            addScoreComponent(LastTradePriceComponent, initialCouponValue);
        } else {
            for (Map event : events) {
                int round = Integer.parseInt(eventGetString(event, "round"));
                if (prevEventRound < round) {  // we're starting to process events for the next round
                    if (round > currentRound) {
                        // we've processed the events for the round we are interested in and we have all the data we need
                        break;
                    }
                    if (capitalGainsMethod == MARK_TO_MARKET) {
                        // reset the "base" price for coupons held to the last price of the previous round
                        coupons.clear();
                        initializePrices(lastTradePrice, numberOfCoupons, coupons);
                    }
                    gain = Quantity.ZERO;  // reset the capital gain for the new round
                }
                prevEventRound = round;
                Quantity price = eventGetQuantity(event, "traded");
                lastTradePrice = price;
                if (userName.equals(eventGetString(event, "traderName"))) { // this trade is of interest
                    boolean isQuantityNegative = eventGetQuantity(event, "quantity").isNegative();
                    boolean buy;
                    if (eventGetString(event, "tradeType").contains("LimitTrade")) {
                        buy = isQuantityNegative;
                    } else { // tradeType must be bookTrade
                        buy = ! isQuantityNegative;
                    }

                    // if buy, add the share to the list
                    if (buy) {
                        coupons.add(price);
                    } else {
                        // if sell, calculate gain or loss from sale
                        gain = gain.plus(price).minus(coupons.removeFirst());
                    }
                }
                numberOfCoupons = coupons.size();
            }
        }
        addScoreComponent(LastTradePriceComponent, lastTradePrice);

        switch (capitalGainsMethod) {
            case HISTORIC_COST:
                Quantity shareValue = Quantity.ZERO;
                for (int i = 0; i < numberOfCoupons; i++) {
                    shareValue = shareValue.plus(coupons.remove());
                }
                addScoreComponent(HistoricalShareValue, shareValue);
                break;
            case FUNDAMENTAL_VALUES:
                gain = fundamentalValue.times(new Quantity(numberOfCoupons));
                addScoreComponent(FundamentalShareValue, fundamentalValue);
                break;
            case MARK_TO_MARKET:
            default:
                for (int i = 0; i < numberOfCoupons; i++) {
                    gain = gain.plus(lastTradePrice.minus(coupons.remove()));
                }
                break;
        }
        addScoreComponent(CapitalGainsComponent, gain);
    }

    public void recordScore(int currentRound, Quantity multiplier, boolean lastRound) {
        Quantity balance = getScoreComponent(BalanceComponent);
        Quantity lastTradePrice = getScoreComponent(LastTradePriceComponent);
        Quantity historicalShareValue = getScoreComponent(HistoricalShareValue);
        Quantity assets = getScoreComponent(TradingSubject.AssetsComponent);
        Quantity totalDividend = getScoreComponent(TotalDividendComponent, Quantity.ZERO);

        Quantity shareValue;
        if (lastRound) {
            shareValue = Quantity.ZERO;
        } else if (historicalShareValue != null) {
            shareValue = historicalShareValue;
        } else if (lastTradePrice != null && assets != null) {
            shareValue = lastTradePrice.times(assets);
        } else {
            shareValue = Quantity.ZERO;
        }

        Quantity score = balance.plus(shareValue).times(multiplier).plus(totalDividend);
        recordMultiplier(multiplier);

        setScore(currentRound, score);
        addScoreComponent(ScoreComponent, score);
    }

    public Quantity getHistoricCost() {
        return getScoreComponent(HistoricalShareValue);
    }

    private void initializePrices(Quantity lastTradePrice, int numberOfCoupons, LinkedList<Quantity> coupons) {
        for (int i = 0; i < numberOfCoupons; i++) {
            coupons.add(lastTradePrice);
        }
    }

    private Quantity eventGetQuantity(Map event, String key) {
        return new Quantity(eventGetString(event, key));
    }

    private String eventGetString(Map event, String key) {
        return String.valueOf(event.get(key));
    }

    public String lendingExplanation() {
        return scoreExplanation.toString();
    }

    private void loanStatusExplanation(Properties props, ScoreExplanationAccumulator rightSide, Quantity defaultAmount) {
        if (defaultAmount != null) {
            rightSide.addEntry(HtmlSimpleElement.redSpan("Loan in Default"), "Defaulted", defaultAmount);
        } else {
            Quantity loanChange = getScoreComponent(LoanChangeComponent);
            if (loanChange.isPositive()) {
                String loanModKeyWord = "$loanMod$";    // see LendingSession.addInForm() for expansion
                rightSide.addEntry(loanChangeLabel(props) +": " + loanChange.printAsScore(), "", loanModKeyWord);
            } else if (loanChange.isZero()){
                rightSide.addEntry(loanUnchangedLabel(props), "", Quantity.ZERO);
            } else {
                String logMsg = "loan reduced by";
                rightSide.addEntry(loanReducedLabel(props), logMsg, loanChange.negate());
            }
        }
    }

    private void addEntryForDividend(Properties props, ScoreExplanationAccumulator accumulator) {
        Quantity pubDividend = getScoreComponent(PublicDividendComponent);
        Quantity privDividend = getScoreComponent(PrivateDividendComponent);
        Quantity totalDividend1 = getScoreComponent(TotalDividendComponent);
        if (privDividend == null && totalDividend1 != null) {
            String dividendValueLabel;
            dividendValueLabel = dividendValueLabel(props);
            accumulator.addEntry(dividendValueLabel, "dividend", pubDividend);
        } else {
            if (pubDividend != null) {
                accumulator.addEntry(publicValueLabel(props), "pubDividend", pubDividend);
            }
            if (privDividend != null) {
                accumulator.addEntry(PropertyHelper.privateValueLabel(props), "privDividend", privDividend);
            }
        }
    }

    private String balanceLabel(Properties props) {
        return labelFromPropertyOrDefault(props, BALANCE_LABEL, DEFAULT_BALANCE_LABEL);
    }

    private String shareValueLabel(Properties props) {
        return labelFromPropertyOrDefault(props, SHARE_VALUE_LABEL, DEFAULT_SHARE_VALUE_LABEL);
    }

    private String totalLabel(Properties props) {
        return labelFromPropertyOrDefault(props, TOTAL_VALUE_LABEL, DEFAULT_TOTAL_VALUE_LABEL);
    }

    private String loansLabel(Properties props) {
        return labelFromPropertyOrDefault(props, LOAN_VALUE_LABEL, DEFAULT_LOAN_VALUE_LABEL);
    }

    private boolean suppressAccounting(Properties props) {
        return PropertyHelper.parseBoolean(SUPPRESS_ACCOUNTING_DETAILS, props, false);
    }

    private String netWorthLabel(Properties props) {
        return labelFromPropertyOrDefault(props, NET_WORTH_LABEL, DEFAULT_NET_WORTH_LABEL);
    }

    private String capitalGainsLabel(Properties props) {
        return labelFromPropertyOrDefault(props, CAPITAL_GAINS_LABEL, DEFAULT_CAPITAL_GAINS_LABEL);
    }

    private String netEarningsLabel(Properties props) {
        return labelFromPropertyOrDefault(props, NET_EARNINGS_LABEL, DEFAULT_NET_EARNINGS_LABEL);
    }

    private String lastTradePriceLabel(Properties props) {
        return labelFromPropertyOrDefault(props, LAST_TRADE_PRICE_LABEL, DEFAULT_LAST_TRADE_PRICE_LABEL);
    }

    private String loanReducedLabel(Properties props) {
        return labelFromPropertyOrDefault(props, LOAN_REDUCED_LABEL, DEFAULT_LOAN_REDUCED_LABEL);
    }

    private String loanUnchangedLabel(Properties props) {
        return labelFromPropertyOrDefault(props, LOAN_UNCHANGED_LABEL, DEFAULT_LOAN_UNCHANGED_LABEL);
    }

    private String loanChangeLabel(Properties props) {
        return labelFromPropertyOrDefault(props, LOAN_CHANGE_LABEL, DEFAULT_LOAN_CHANGE_LABEL);
    }
}
