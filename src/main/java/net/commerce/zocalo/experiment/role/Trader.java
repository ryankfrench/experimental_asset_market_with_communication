package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.user.User;

import java.util.*;

import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Traders exchange goods in the market.  */
public class Trader extends TradingSubject {
    protected Trader(User user, Role role) {
        super(user, role);
    }

    public String propertyWordForRole() {
        return TRADER_PROPERTY_WORD;
    }

    public Quantity recordBonusInfo(Price average, Quantity judged, Quantity actual, Quantity totalDividend, int round, Properties props) {
        if (! average.isZero()) {
            addScoreComponent(AverageComponent, average);
        }
        addScoreComponent(TotalDividendComponent, totalDividend);
        return Quantity.ZERO;
    }

    public void recordScore(int currentRound, Quantity multiplier, Properties props) {
        Quantity balance = getScoreComponent(BalanceComponent);
        Quantity assets = getScoreComponent(TotalDividendComponent);
        Quantity score = balance.plus(assets).times(multiplier);
        recordMultiplier(multiplier);

        setScore(currentRound, score);
        addScoreComponent(ScoreComponent, score);
    }

    public void recordExplanation(Properties props, boolean keepingScore, boolean carryForward, String eventOutcome) {
        if (displayCarryForwardScores(props)) {
            String dividendLabel;
            if (getScoreComponent(TotalDividendComponent).isNonNegative()) {
                dividendLabel = PropertyHelper.dividendAddedToCashLabel(props);
            } else {
                dividendLabel = PropertyHelper.divSubtractedLabel(props);
            }
            ScoreExplanationAccumulator accumulator = assetValueTable(props, dividendLabel, keepingScore, carryForward);
            saveMultiplierIfGiven(accumulator);
            StringBuffer buf = new StringBuffer();

            Quantity score = getScoreComponent(ScoreComponent);
            Quantity dividend = getScoreComponent(PublicDividendComponent);
            if (eventOutcome != null && eventOutcome != "" && score != null && dividend != null) {
                ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
                eventAccum.addEntry("Event", "", eventOutcome);
                eventAccum.addEntry(traderInvestmentLabel(props), "", dividend);
                eventAccum.addEntry(traderEarningsLabel(props), "", score);
                eventAccum.renderAsColumns(buf);
                eventAccum.log(getName(), getScoreLogger());
            } else {
                accumulator.renderAsColumns(buf);
                // accumulator.log(getName(), getScoreLogger()); // JJDM Duplication was here due to multiple calls. See RoundScorer#logRoundResults.
            }
            scoreExplanation = buf.toString();
        }
    }

    public void recordDormantInfo(Quantity judged, String outcome, Properties props) {
        ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
        eventAccum.addEntry("Event", "", outcome);
        StringBuffer buf = new StringBuffer();
        eventAccum.renderAsColumns(buf);
        scoreExplanation = buf.toString();
    }

    protected String traderEarningsLabel(Properties props) {
        String earningsLabel = props.getProperty(TRADER_EARNINGS_LABEL);
        if (earningsLabel != null && ! "".equals(earningsLabel)) {
            return earningsLabel.trim();
        }
        return DEFAULT_TRADER_EARNINGS_LABEL;
    }

    protected String traderInvestmentLabel(Properties props) {
        String investmentLabel = props.getProperty(TRADER_INVESTMENT_LABEL);
        if (investmentLabel != null && ! "".equals(investmentLabel)) {
            return investmentLabel.trim();
        }
        return DEFAULT_TRADER_INVESTMENT_LABEL;
    }

    static public Trader makeTrader(User user, Role role) {
        return new Trader(user, role);
    }
}
