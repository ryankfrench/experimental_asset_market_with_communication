package net.commerce.zocalo.experiment.role;

import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;

import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.experiment.ExperimentSubject;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.ScoreExplanationAccumulator;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Abstract base class for experiment subjects.  Knows how to calculate
    and record scores. */
public abstract class AbstractSubject implements ExperimentSubject {
    static public final Object BonusComponent = "BONUS";
    static public final Object ScoreComponent = "SCORE";
    static public final Object MultiplierComponent = "MUTLIPLER";
    private Quantity[] scores;
    protected String scoreExplanation = "";
    private Map<Object, Quantity> scoreComponents = new HashMap<Object, Quantity>();
    private boolean[] dormant;

    public String logConfigValues(Logger log, Properties props, int rounds) {
        String prop = getName() + DOT_SEPARATOR + ROLE_PROPERTY_WORD;
        log.info(GID.log() + prop + ": " + roleName());
        if (null == roleName() || roleName().trim().length() == 0) {
            return getName() + " doesn't have a defined role";
        }
        return "";
    }

    public void setHintsForRound(int currentRound, Session session) {
        setHint(session.getPriceHint(getName(), currentRound));
    }

    public abstract String roleName();

    abstract public String getName();

    public void linkHtml(StringBuffer buff) {
        buff.append("<li><a href=\"")
            .append(pageLink())
            .append("\">")
            .append(linkText())
            .append("</a></li>\n");
    }

    protected abstract String linkText();

///////// SCORING  //////////////////////////////////////////

    public static Logger getScoreLogger() {
        return Logger.getLogger("Score");
    }

    public Quantity totalScore() {
        Quantity totalScore = Quantity.ZERO;
        for (int i = 0; i < scores.length; i++) {
            totalScore = totalScore.plus(scores[i]);
        }
        return totalScore;
    }

    void resetScores(int rounds) {
        scores = new Quantity[rounds + 1];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = Quantity.ZERO;
        }
    }

    public void setScore(int round, Quantity score) {
        scores[round] = score;
    }

    public Quantity getScore(int round) {
        return scores[round];
    }

    public String actualValueLabel(Properties props) {
        String actualLabel = props.getProperty(ACTUAL_VALUE_LABEL);
        if (actualLabel != null && ! "".equals(actualLabel)) {
            return actualLabel.trim();
        }
        String dividendLabel = props.getProperty(DIVIDEND_VALUE_LABEL);
        if (dividendLabel != null && !"".equals(dividendLabel)) {
            return dividendLabel.trim();
        }
        return DEFAULT_ACTUAL_TICKET_VALUE_LABEL;
    }

    static public String dividendValueLabel(Properties props) {
        String dividendLabel = props.getProperty(DIVIDEND_VALUE_LABEL);
        if (dividendLabel != null && !"".equals(dividendLabel)) {
            return dividendLabel.trim();
        }
        String actualLabel = props.getProperty(ACTUAL_VALUE_LABEL);
        if (actualLabel != null && ! "".equals(actualLabel)) {
            return actualLabel.trim();
        }
        return DEFAULT_DIV_VALUE_LABEL;
    }

    static public String labelFromPropertyOrDefault(Properties props, String labelName, String defaultValue) {
        String label = props.getProperty(labelName);
        if (label == null || "".equals(label)) {
            return defaultValue;
        }
        return label.trim();
    }

    public String totalAssetsLabel(Properties props) {
        return labelFromPropertyOrDefault(props, TOTAL_ASSETS_LABEL, DEFAULT_TOTAL_ASSETS_LABEL);
    }

    public String getScoreExplanation() {
        return scoreExplanation;
    }

    public void rememberHoldings(BinaryClaim claim) {
        // default is to do nothing.  Override in roles that need balance for score.
    }

    public void rememberAssets(BinaryClaim claim) {
        // default is to do nothing.  Override in roles that need balance for score.
    }

    public void addScoreComponent(Object key, Quantity value) {
        scoreComponents.put(key, value);
    }

    public Quantity getScoreComponent(Object key) {
        return scoreComponents.get(key);
    }

    public Quantity getScoreComponent(Object key, Quantity defaultValue) {
        Quantity aQuantity = scoreComponents.get(key);
        if (null == aQuantity) {
            return defaultValue;
        }
        return aQuantity;
    }

    abstract public Quantity recordBonusInfo(Price average, Quantity judged, Quantity judgingTarget, Quantity totalDividend, int round, Properties props);
    abstract public void addBonus(Funds funds);
    abstract public Quantity totalDividend(BinaryClaim claim, Session session, int round) throws ScoreException;
    abstract public Funds payDividend(Quantity perShareDividend, Quantity shares, Position noPosition);
    abstract public void receiveDividend(Funds funds);
    abstract public void recordScore(int currentRound, Quantity multiplier, Properties props);
    abstract public void recordExplanation(Properties props, boolean keepingScore, boolean carryForward, String eventOutcome);
    abstract public void reduceReservesTo(Quantity perShare, Position pos);
    abstract public void recordDormantInfo(Quantity judged, String outcome, Properties props);

    public void resetScoreInfo() {
        scoreExplanation = "";
        scoreComponents = new HashMap<Object, Quantity>();
    }

    public void saveMultiplierIfGiven(ScoreExplanationAccumulator accumulator) {
        Quantity mult = getScoreComponent(MultiplierComponent);
        if (mult != null) {
            accumulator.addEntry("Multiplier", "multiplier", mult);
        }
    }

    public void recordMultiplier(Quantity multiplier) {
        if (multiplier.compareTo(Quantity.ONE) != 0) {
            addScoreComponent(MultiplierComponent, multiplier);
        }
    }

    protected boolean dormant(int currentRound) {
        if (dormant == null) {
            return false;
        }
        return dormant[currentRound];
    }

    public void initializeDormancy(boolean[] booleans) {
        dormant = booleans;
    }

    public boolean[] dormantPeriods() {
        return dormant;
    }

    public boolean isDormant(int currentRound) {
        return dormant(currentRound);
    }
}
