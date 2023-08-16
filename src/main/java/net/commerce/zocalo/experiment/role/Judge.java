package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.ajax.events.IndividualTimingEvent;
import net.commerce.zocalo.user.Warnable;
import net.commerce.zocalo.html.HtmlSimpleElement;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.GID;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.service.PropertyKeywords;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;
import net.commerce.zocalo.experiment.JudgingSession;

import java.util.Properties;

import java.util.TimerTask;
import java.util.Date;

import org.apache.log4j.Logger;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Judge calculates scores for experiment participants playing this role.
    Judges don't trade, so they have no User object.  They don't actually
    participate in the market, but they are part of the market.  */
public class Judge extends AbstractSubject {
    final private Price[] estimates;
    private String hint;
    private String name;
    private Object FactorComponent = "FACTOR";
    private Object ConstantComponent = "CONSTANT";
    private Object EstimateComponent = "ESTIMATE";
    private Object ActualValueComponent = "ACTUAL_VALUE";
    private TimerTask cutoffTimer;
    Warnable warnings;

    private Judge(int rounds) {
        estimates = new Price[rounds];
        Price fifty = new Price(50, Price.ONE_DOLLAR);
        for (int i = 0; i < estimates.length; i++) {
            estimates[i] = fifty;
        }
        warnings = new Warnable() { public String getName() { return name; } };
    }

    static public Judge makeJudge(int rounds) {
        return new Judge(rounds);
    }

    public Quantity accountValueFromProperties(Properties props) {
        return Quantity.ZERO;  // Judges don't have accounts to set up.
    }

    public Quantity cashFromProperties(Properties props) {
        return Quantity.ZERO;  // Judges don't have cash either.
    }

    public Quantity currentCouponCount(BinaryClaim claim) {
        return Quantity.ZERO;
    }

    public void resetOutstandingOrders() {
        // Do nothing.
    }

    public void setEstimate(int round, Price estimate) {
        if (! getScore(round).isZero()) {
            return;  // Don't allow the judge to change the estimate after scores have been calculated.
        }
        if (round < 1) {
            warn("Please wait before entering estimates.");
        } else if (judgingCutoff()) {
            warn("Too late to enter new estimate.  Time is up.");
        } else if (! getScore(round).isZero()) {
            warn("Too late to enter new estimate.  Scores have been calculated.");
        } else {
            Logger log = getScoreLogger();
            log.info(GID.log() + getName() + " estimate: " + estimate);

            this.estimates[round - 1] = estimate;
        }
    }

    public boolean judgingCutoff() {
        if (cutoffTimer == null) {
            return false;
        } else if (cutoffTimer == JudgingSession.DEAD_TASK) {
            return true;
        } else {
            long cutoffTime = cutoffTimer.scheduledExecutionTime();
            Date now = new Date();
            if (cutoffTime < now.getTime()) {
                return true;
            }
        }
        return false;
    }

    public void warn(String s) {
        warnings.warn(s);
    }

    public boolean hasWarnings() {
        return warnings.hasWarnings();
    }

    public String getWarningsHtml() {
        return warnings.getWarningsHTML();
    }

    public Price getEstimate(int round) {
        if (round < 1) {
            return Price.ZERO_DOLLARS;
        }
        return estimates[round - 1];
    }

//////// HTML  ///////////////////////////////////////

    protected String linkText() {
        return name + ", a Judge.";
    }

    public String pageLink() {
        return "Judge.jsp?userName=" + name;
    }

//////// SCORING  ///////////////////////////////////////

    public Quantity recordBonusInfo(Price average, Quantity judged, Quantity target, Quantity totalDividend, int round, Properties props) {
        Quantity estimate = getEstimate(round);
        addScoreComponent(EstimateComponent, estimate);
        Quantity diff;
        if (estimate == null) {
            diff = average.plus(average.inverted());
        } else {
            diff = target.minus(estimate);
        }
        addScoreComponent(ActualValueComponent, target);

        String rewards = (String) props.get(PropertyKeywords.JUDGE_REWARDS);
        Quantity bonus;
        if (null == rewards || rewards.length() == 0) {
            Quantity constant = scoreConstant(props);
            addScoreComponent(ConstantComponent, constant);
            Quantity factor = scoreFactor(props);
            addScoreComponent(FactorComponent, factor);
            bonus = Quantity.ZERO.max(constant.minus(factor.times(diff.times(diff))));
            addScoreComponent(BonusComponent, bonus);
            setScore(round, bonus);
        } else {
            bonus = calculateBonusFromTable(rewards, estimate, target, PropertyHelper.getMaxTradingPrice(props));
        }

        return bonus;
    }

    private Quantity calculateBonusFromTable(String rewards, Quantity estimate, Quantity actual, Quantity maxTradingPrice) {
        String[] rewardArray = rewards.split(PropertyKeywords.COMMA_SPLIT_RE);
        Quantity step = maxTradingPrice.div(rewardArray.length - 1);
        if (! estimate.remainder(step).isZero()) {
            Quantity newEst = estimate.minus(estimate.remainder(step));
            getScoreLogger().warn(getName() + " entered an estimate of " + estimate + " but reward calculated using " + newEst);
            estimate = newEst;
        }
        Quantity absDiff = actual.minus(estimate).abs();
        Quantity choice = (maxTradingPrice.minus(absDiff).div(step));
        Quantity bonus = new Quantity(rewardArray[choice.asValue().intValue()]);
        addScoreComponent(BonusComponent, bonus);
        return bonus;
    }

    public void addBonus(Funds funds) {
        // We don't need the money, and it came directly from rootbank, so it's safe to drop it.
        addScoreComponent(BonusComponent, funds.getBalance());
    }

    public Funds payDividend(Quantity perShareDividend, Quantity shares, Position noPosition) {
        return null;  //Judges don't have assets
    }

    public void receiveDividend(Funds funds) {
        return; //Judges don't have assets
    }

    public void reduceReservesTo(Quantity perShare, Position pos) {
        return; // Judges don't have reserves
    }

    public void recordDormantInfo(Quantity judged, String outcome, Properties props) {
        ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
        eventAccum.addEntry("Event", "", outcome);
        StringBuffer buf = new StringBuffer();
        eventAccum.renderAsColumns(buf);
        scoreExplanation = buf.toString();
    }


    public Quantity totalDividend(BinaryClaim claim, Session session, int round) {
        return Quantity.ZERO;  //Judges don't have assets
    }

    public void recordScore(int currentRound, Quantity multiplier, Properties props) {
        recordMultiplier(multiplier);
        Quantity bonus = getScoreComponent(BonusComponent).times(multiplier);
        setScore(currentRound, bonus);
    }

    public void recordExplanation(Properties props, boolean keepingScore, boolean carryForward, String eventOutcome) {
        ScoreExplanationAccumulator accumulator = new ScoreExplanationAccumulator();

        Quantity constant = getScoreComponent(ConstantComponent);
        accumulator.addEntry(actualValueLabel(props), 30, "",      getScoreComponent(ActualValueComponent));
        Quantity estimate = getScoreComponent(EstimateComponent);
        accumulator.addEntry("Your Estimate",         25, "", estimate);
        Quantity bonus = getScoreComponent(BonusComponent);
        if (constant != null) {
            Quantity factor = getScoreComponent(FactorComponent);
            String bonusDescription = "Bonus<br><font size='-2'>(" + constant + " - (" + factor + " * diff<sup>2</sup>))</font>";
            accumulator.addEntry(bonusDescription,         0, "score", bonus);
        }
        saveMultiplierIfGiven(accumulator);

        accumulator.log(getName(), getScoreLogger());
        StringBuffer buf = new StringBuffer();

        if (eventOutcome != null && eventOutcome != "") {
            ScoreExplanationAccumulator eventAccum = new ScoreExplanationAccumulator();
            eventAccum.addEntry("Event", "", eventOutcome);
            eventAccum.addEntry(judgeInvestmentLabel(props), "", estimate);
            eventAccum.addEntry(judgeEarningsLabel(props), "", bonus);
            eventAccum.renderAsColumns(buf);
            eventAccum.log(getName(), getScoreLogger());
        } else {
            accumulator.renderAsColumns(buf);
            accumulator.log(getName(), getScoreLogger());
        }

        scoreExplanation = buf.toString();
    }

    private String judgeEarningsLabel(Properties props) {
        String earningsLabel = props.getProperty(JUDGE_EARNINGS_LABEL);
        if (earningsLabel != null && ! "".equals(earningsLabel)) {
            return earningsLabel.trim();
        }
        return DEFAULT_JUDGE_EARNINGS_LABEL;
    }

    private String judgeInvestmentLabel(Properties props) {
        String investmentLabel = props.getProperty(JUDGE_INVESTMENT_LABEL);
        if (investmentLabel != null && ! "".equals(investmentLabel)) {
            return investmentLabel.trim();
        }
        return DEFAULT_JUDGE_INVESTMENT_LABEL;
    }

    static private Quantity scoreFactor(Properties props) {
        String factorString =
                props.getProperty(SCORING_FACTOR_PROPERTY_WORD + DOT_SEPARATOR + JUDGE_PROPERTY_WORD);
        return new Quantity(factorString);
    }

    static private Quantity scoreConstant(Properties props) {
        String constantString =
                props.getProperty(SCORING_CONSTANT_PROPERTY_WORD + DOT_SEPARATOR + JUDGE_PROPERTY_WORD);
        return new Quantity(constantString);
    }

    public boolean canSell(int currentRound) {
        return false;
    }

    public boolean canBuy(int currentRound) {
        return false;
    }

    public String roleName() {
        return JUDGE_PROPERTY_WORD;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String estimateAsString(int round) {
        Price estimate = getEstimate(round);
        if (estimate == null) {
            return "-";
        } else {
            return estimate.toString();
        }
    }

    public void getGuessesRow(StringBuffer buf, Session session) {
        for (int i = 0; i < session.rounds(); i++) {
            String estimateString = estimateAsString(i + 1);
            buf.append(HtmlSimpleElement.printTableCell(estimateString));
        }
    }

    public TimerTask getCutoffTimer() {
        final Judge thisJudge = this;
        if (cutoffTimer == null || cutoffTimer == JudgingSession.DEAD_TASK) {
            cutoffTimer = new TimerTask() {
                public void run() {
                    judgeTimingEvent(thisJudge);
                }
            };
        }
        return cutoffTimer;
    }

    public static IndividualTimingEvent timerEvent(Judge judge, Logger logger) {
        String logString = "Judge '" + judge.getName() + "' timer expired for judging.";
        return new IndividualTimingEvent(judge,  logger, "disableJudging", logString);
    }

    public static IndividualTimingEvent judgeTimingEvent(Judge judge) {
        return timerEvent(judge, IndividualTimingEvent.getActionLogger());
    }

    public void cancelCutOffTimer() {
        if (cutoffTimer != null) {
            cutoffTimer.cancel();
            cutoffTimer = JudgingSession.DEAD_TASK;
        }
    }
}
