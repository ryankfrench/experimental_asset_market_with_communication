package net.commerce.zocalo.experiment;

import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.experiment.role.*;
import net.commerce.zocalo.user.Liquidator;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.html.HtmlSimpleElement;

import org.apache.log4j.Logger;
import org.mortbay.cometd.AbstractBayeux;
import org.antlr.stringtemplate.StringTemplate;

import java.util.*;

import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2009, 2010 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Session for experiments in which we lend to subjects based Mark-to-market valuations. */
public class LendingSession extends Session {
    private long period = 10000;
    private Quantity ratio;
    private Quantity couponBasis;
    private Integer loansDueRound;

    public LendingSession(Properties props, AbstractBayeux bayeux) {
        super(props, bayeux);
        couponBasis = PropertyHelper.parseDoubleNoWarn(COUPON_BASIS, props);
    }

    public LendingSession(Properties props, String logFile, AbstractBayeux bayeux) {
        super(props, logFile, bayeux);
    }

    public static boolean describesM2MSession(Properties props) {
        String ratio = props.getProperty(MARK_TO_MARKET_RATIO);
        if (ratio == null || ratio.equals("")) {
            return false;
        }
        Quantity r = new Quantity(ratio);
        return r != null && ! r.isNegative();
    }

    protected TraderRole basicRoleForSession(String role) {
        return new BorrowerRole(role);
    }

    public void webAction(String userName, final String parameter) {
        Session session = SessionSingleton.getSession();
        Borrower b = (Borrower) session.getPlayer(userName);
        if (b != null && parameter.equals("Accept")) {
            Quantity increase = availableLoan(b).minus(b.loanAmount());
            increaseLending(b, increase);
            b.setAccepted(getCurrentRound(), increase);
            Object loanComponentTag = Borrower.OutstandingLoanComponent;
            Object balanceTag = TradingSubject.BalanceComponent;
            b.addScoreComponent(loanComponentTag, increase.plus(b.getScoreComponent(loanComponentTag)));
            b.addScoreComponent(balanceTag, increase.plus(b.getScoreComponent(balanceTag)));
        }
    }

    protected void logSessionInitialization() {
        super.logSessionInitialization();
        Logger log = PropertyHelper.configLogger();

        ratio = PropertyHelper.parseDoubleNoWarn(MARK_TO_MARKET_RATIO, props);
        log.info("Loan ratio is " + ratio);

        loansDueRound = PropertyHelper.parseInteger(LOANS_DUE_WORD, props, 0);
        log.info("Loans are due in round " + loansDueRound);
    }

    public Quantity lendingRatio() {
        return ratio;
    }

    public void setPeriod(int mSecs) {
        period = mSecs;
    }

    public Quantity availableLoan(Borrower borrower) {
        if (loansDue()) {
            return Quantity.ZERO;
        }

        Quantity loanBasis;
        switch (getCapGainsMethod()) {
            case HISTORIC_COST:
                loanBasis = borrower.getHistoricCost();
                break;
            case FUNDAMENTAL_VALUES:
                loanBasis = borrower.couponValue(getClaim(), getFundamentalValue());
                break;
            case MARK_TO_MARKET:
            default:
                loanBasis = borrower.couponValue(getClaim(), lastActualValue());
                break;
        }
        return lendingRatio().times(loanBasis).roundFloor().max(Quantity.ZERO);
    }

    private boolean loansDue() {
        if (loansDueRound == 0) {
            return lastRound();
        }

        return getCurrentRound() >= loansDueRound;
    }

    void otherStartActions() {
        scheduleLiquidations(period);
    }

    private void scheduleLiquidations(long period) {
        Set<Borrower> defaulters = collectDefaulters();
        if (defaulters.size() == 0) {
            return;
        }
        scheduleLiquidators(period, defaulters);
    }

    private void scheduleLiquidators(long period, Set<Borrower> defaulters) {
        BinaryClaim claim = getClaim();
        long liquidatorSeparation = period / defaulters.size();
        Iterator<Borrower> defIterator = defaulters.iterator();
        long defaulterIndex = 0;
        while (defIterator.hasNext()) {
            Borrower borrower = defIterator.next();
            Liquidator liquidator = new Liquidator(borrower, period, claim, getMarket());
            liquidator.startLiquidating(defaulterIndex * liquidatorSeparation);
            defaulterIndex ++;
        }
    }

    private Set<Borrower> collectDefaulters() {
        Set<Borrower> defaulters = new HashSet<Borrower>();
        Iterator<String> names = playerNameIterator();
        while (names.hasNext()) {
            String name = names.next();
            Borrower borrower = (Borrower) getPlayer(name);
            if (borrower.inDefault()) {
                defaulters.add(borrower);
            }
        }
        return defaulters;
    }

    protected void calculateScores(Price average) throws ScoreException {
        BinaryClaim claim = getClaim();
        Liquidator.stopAllLiquidators();
        for (Iterator playerNames = playerNameIterator(); playerNames.hasNext();) {
            Borrower borrower = (Borrower) getPlayer((String)playerNames.next());
            borrower.rememberHoldings(claim);

            Quantity total = borrower.totalDividend(claim, this, getCurrentRound());
            provideCash(borrower, total);

            borrower.rememberDividends(total);
            CapitalGainsMethod method = getCapGainsMethod();
            borrower.computeCapitalGains(getTradeHistory(), getCouponBasis(), getCurrentRound(), method, getFundamentalValue());
            borrower.rememberLoanState(availableLoan(borrower));
            borrower.recordGainsAndLosses(method, props, lendingRatio().isPositive(), lastRound());
            if (lastRound()) {
                borrower.recordScore(getCurrentRound(), Quantity.ONE, lastRound());
            }
        }
    }

    private Quantity getFundamentalValue() {
        String value = PropertyHelper.indirectPropertyForRound(FUNDAMENTAL_VALUE_PROPERTY_WORD, getCurrentRound(), props);
        if (value == "") { return Quantity.ZERO; }
        return new Quantity(value);
    }

    public enum CapitalGainsMethod {
        HISTORIC_COST,
        FUNDAMENTAL_VALUES,
        MARK_TO_MARKET
    }

    private CapitalGainsMethod getCapGainsMethod() {
        String capGains = props.getProperty(CAPITAL_GAINS);
        if (capGains == null || "".equals(capGains)) {
            return CapitalGainsMethod.MARK_TO_MARKET;
        } else if (capGains.matches(HISTORIC_COST_PROPERTY_WORD)) {
            return CapitalGainsMethod.HISTORIC_COST;
        } else if (capGains.matches(FUNDAMENTAL_VALUE_PROPERTY_WORD)) {
            return CapitalGainsMethod.FUNDAMENTAL_VALUES;
        } else {
            return CapitalGainsMethod.MARK_TO_MARKET;
        }
    }

    void renderScoreAndExplanation(String userName, StringBuffer buff) {
        AbstractSubject subject = getPlayer(userName);
        if (subject == null) {
            return;
        }

        if ((loansDue()) || !isCarryForward()) {
            renderScore(buff, calculateEarnings(userName).printAsScore());
            buff.append("<p>");
        }
        addInForm(buff, subject);
    }

    private void addInForm(StringBuffer buff, AbstractSubject subject) {
        String explanation = ((Borrower) subject).lendingExplanation();
        StringTemplate st = new StringTemplate(explanation);
        String targetPage = "TraderSubSubFrame.jsp";
        buff.append(HtmlSimpleElement.formHeaderWithPost(targetPage));
        if (((Borrower) subject).acceptedLoanMod(getCurrentRound())) {
            st.setAttribute("loanMod", "Accepted");
        } else {
            Quantity loanChange = subject.getScoreComponent(Borrower.LoanChangeComponent);
            if (loanChange.isPositive()) {
                buff.append(HtmlSimpleElement.hiddenInputField("userName", subject.getName()));
                String field = HtmlSimpleElement.submitInputField("Accept", "style='background:lightgreen;'");
                st.setAttribute("loanMod", field);
            }
        }
        buff.append(st.toString());
    }

    private Quantity lastActualValue() {
        return lastTradeValue();
    }

    public void increaseLending(Borrower b, Quantity amount) {
        provideCash(b, amount);
        b.increaseLoanAmount(amount);
    }

    public Quantity getCouponBasis() {
        return couponBasis;
    }
}
