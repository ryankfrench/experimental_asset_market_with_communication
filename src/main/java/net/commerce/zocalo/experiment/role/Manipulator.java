package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.GID;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;

import java.util.Properties;

import org.apache.log4j.Logger;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Manipulators have special rules for calculating their score.  */
public class Manipulator extends TradingSubject {
    static final public Object BonusComponent = "BONUS";
    static final public Object DividendComponent = "DIVIDEND";
    static final public Object TargetComponent = "TARGET";
    static final public Object FactorComponent = "FACTOR";
    static final public Object ConstantComponent = "CONSTANT";
    static final public Object JudgedComponent = "JUDGED_VALUE";
    static final public Object PlusBonusComponent = "DIV_OR_ASETS_PLUS_BONUS";
    static final public String MANIPULATOR_REWARDS_TOKEN = "manipulatorRewards";
    static final public String DIFFERENCE_TOKEN = "difference";

    private Manipulator(User user, ManipulatorRole role) {
        super(user, role);
    }

    static public Manipulator makeManipulator(User user, ManipulatorRole role) {
        return new Manipulator(user, role);
    }

    public String propertyWordForRole() {
        return MANIPULATOR_PROPERTY_WORD;
    }

    public Quantity recordBonusInfo(Price average, Quantity judged, Quantity judgingTarget, Quantity totalDividend, int round, Properties props) {
        if (! average.isZero()) {
            addScoreComponent(AverageComponent, average);
        }
        addScoreComponent(TotalDividendComponent, totalDividend);
        addScoreComponent(JudgedComponent, judged);

        Quantity bonus;
        if (DIFFERENCE_TOKEN.equals(getRewardsString(props).trim())) {
            Quantity pubDividend = getScoreComponent(PublicDividendComponent);
            bonus = pubDividend.minus(judged).abs();
        } else {
            Quantity target = targetValue(round, props);
            Quantity constant = constant(props);
            Quantity factor = factor(props);
            addScoreComponent(ConstantComponent, constant);
            addScoreComponent(FactorComponent, factor);
            addScoreComponent(TargetComponent, target);

            bonus = constant.minus(factor.times(judged.minus(target).abs()));
        }
        addScoreComponent(BonusComponent, bonus);

        return bonus;
    }

    private String getRewardsString(Properties props) {
        String token = props.getProperty(MANIPULATOR_REWARDS_TOKEN);
        if (token == null) {
            token = "";
        }
        return token;
    }

    public void recordScore(int currentRound, Quantity multiplier, Properties props) {
        Quantity bonusScore;
        Quantity holdingsScore;
        Quantity constant = getScoreComponent(ConstantComponent);
        recordMultiplier(multiplier);
        if (null == constant) {
            bonusScore = getScoreComponent(BonusComponent).times(multiplier);
            holdingsScore = Quantity.ZERO;
        } else {
            Quantity balance = getScoreComponent(BalanceComponent);
            Quantity totalDiv = getScoreComponent(TotalDividendComponent);
            holdingsScore = balance.plus(totalDiv).times(multiplier);
            addScoreComponent(PlusBonusComponent, holdingsScore);
            Quantity factor = getScoreComponent(FactorComponent);
            Quantity judgedValue = getScoreComponent(JudgedComponent);
            Quantity target = getScoreComponent(TargetComponent);
            bonusScore = constant.minus(factor.times(judgedValue.minus(target).abs())).times(multiplier);
        }

        Quantity totalScore = holdingsScore.plus(bonusScore);
        setScore(currentRound, totalScore);
        addScoreComponent(ScoreComponent, totalScore);
    }

    // If it's a carryforward experiment, we're adding score to assets, otherwise, scores are kept separately.
    // If it's carryforward and displayCarryForward, then we're displaying the amounts added to assets.  If it's
    // carryForward, but not displayCarryForward, then we'll record the results, but only display assets, and
    // not explain the changes.  If it's not carryForward, then we'll explain all the amounts added to the score.
    public void recordExplanation(Properties props, boolean keepingScore, boolean carryForward, String eventOutcome) {
        StringBuffer buf = new StringBuffer();

        boolean displayCarryForward = displayCarryForwardScores(props);
        if (carryForward && displayCarryForward) {
            ScoreExplanationAccumulator assetTableAccumulator = assetValueTable(props, PropertyHelper.totalDividendLabel(props), keepingScore, carryForward);
            buf.append("<br>");
            assetTableAccumulator.log(getName(), getScoreLogger());
            assetTableAccumulator.renderAsColumns(buf);
            renderCarryForwardExplanation(buf, props);
        }

        if (! carryForward || displayCarryForward) {
            ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
            Quantity multiplier = getScoreComponent(MultiplierComponent);

            Quantity score = getScoreComponent(ScoreComponent);
            Quantity judged = getScoreComponent(JudgedComponent);
            if (eventOutcome != null && eventOutcome != "" && score != null && judged != null) {
                eventAccum.addEntry("Event", "", eventOutcome);
                eventAccum.addEntry(manipulatorInvestmentLabel(props), "", judged);
                addMultiplier(multiplier, eventAccum);
                eventAccum.addEntry(manipulatorEarningsLabel(props), "", score);
            } else {
                addMultiplier(multiplier, eventAccum);
            }
            eventAccum.renderAsColumns(buf);
        }
        scoreExplanation = buf.toString();
    }

    private void addMultiplier(Quantity multiplier, ScoreExplanationAccumulator eventAccum) {
        if (multiplier != null && multiplier.compareTo(Quantity.ONE) != 0) {
            eventAccum.addEntry("Multiplier", "", multiplier);
        }
    }

    public void recordDormantInfo(Quantity judged, String outcome, Properties props) {
        ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
        eventAccum.addEntry("Event", "", outcome);
        eventAccum.addEntry(manipulatorInvestmentLabel(props), "", judged);
        StringBuffer buf = new StringBuffer();
        eventAccum.renderAsColumns(buf);
        scoreExplanation = buf.toString();
    }

    private String manipulatorEarningsLabel(Properties props) {
        String earningsLabel = props.getProperty(MANIPULATOR_EARNINGS_LABEL);
        if (earningsLabel != null && ! "".equals(earningsLabel)) {
            return earningsLabel.trim();
        }
        return DEFAULT_MANIPULATOR_EARNINGS_LABEL;
    }

    private String manipulatorInvestmentLabel(Properties props) {
        String investmentLabel = props.getProperty(MANIPULATOR_INVESTMENT_LABEL);
        if (investmentLabel != null && ! "".equals(investmentLabel)) {
            return investmentLabel.trim();
        }
        return DEFAULT_MANIPULATOR_INVESTMENT_LABEL;
    }

    private void renderCarryForwardExplanation(StringBuffer buf, Properties props) {
        Quantity constant = getScoreComponent(ConstantComponent);
        ScoreExplanationAccumulator bonusTableAccumulator = new ScoreExplanationAccumulator();
        if (null == constant) {
            bonusTableAccumulator.addEntry("Bonus<br><font size='-2'>from table</font>", 0, "score", getScoreComponent(BonusComponent));
        } else {
            Quantity factor = getScoreComponent(FactorComponent);
            String bonusDescription = "Bonus<br><font size='-2'>(" + constant + " - (" + factor + " * diff))</font>";

            bonusTableAccumulator.addEntry("Target",           10, "",      getScoreComponent(TargetComponent));
            bonusTableAccumulator.addEntry("Judges' Estimate", 10, "",      getScoreComponent(JudgedComponent));
            bonusTableAccumulator.addEntry(bonusDescription,    0, "score", getScoreComponent(BonusComponent));
            boolean keepingScore = (null != getScoreComponent(BalanceComponent));
            if (keepingScore) {
                bonusTableAccumulator.addEntry(PropertyHelper.assetsPlusBonusLabel(props), "", getScoreComponent(PlusBonusComponent));
            } else {
                bonusTableAccumulator.addEntry(PropertyHelper.dividendsPlusBonusLabel(props), "", getScoreComponent(TotalDividendComponent));
            }
        }

        buf.append("\n");
        bonusTableAccumulator.renderAsColumns(buf);
        bonusTableAccumulator.log(getName(), getScoreLogger());

    }

    private Quantity targetValue(int round, Properties props) {
        String targetStrings = getUser().getName() + DOT_SEPARATOR + TARGET_PROPERTY_WORD;
         return new Quantity(PropertyHelper.indirectPropertyForRound(targetStrings, round, props));
    }

    private Quantity factor(Properties props) {
        String factorString =
                props.getProperty(SCORING_FACTOR_PROPERTY_WORD + DOT_SEPARATOR + propertyWordForRole());
        return new Quantity(factorString);
    }
    private Quantity constant(Properties props) {
        String constantString =
                props.getProperty(SCORING_CONSTANT_PROPERTY_WORD + DOT_SEPARATOR + propertyWordForRole());
        return new Quantity(constantString);
    }

    public String logConfigValues(Logger log, Properties props, int rounds) {
        super.logConfigValues(log, props, rounds);
        String targetPropertyName = getName() + DOT_SEPARATOR + TARGET_PROPERTY_WORD;
        String targetValue = props.getProperty(targetPropertyName);
        log.info(GID.log() + targetPropertyName + ": " + targetValue);
        String earningsHintPropertyName = getName() + DOT_SEPARATOR + EARNINGS_HINT_PROPERTY_WORD;
        log.info(GID.log() + earningsHintPropertyName + ": " + props.getProperty(earningsHintPropertyName));
        if (null == targetValue || targetValue.trim().length() == 0) {
            return targetPropertyName + " is null.";
        }
        return "";
    }

    public void setHintsForRound(int currentRound, Session session) {
        String priceHint = session.getPriceHint(getName(), currentRound);
        String earningsHint = session.getEarningsHint(getName(), currentRound);
        setHint(priceHint + "<br>" + earningsHint);
    }

    public String roleName() {
        return MANIPULATOR_PROPERTY_WORD;
    }
}
