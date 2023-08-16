package net.commerce.zocalo.experiment;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.Quantity;

import java.util.Properties;

import org.apache.log4j.Logger;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Interface for experiment subjects.  Subjects track score, and know the
    User object if there is one. */
public interface ExperimentSubject {
    String getHint();
    void setHint(String hint);
    void setHintsForRound(int currentRound, Session session);

    Quantity accountValueFromProperties(Properties props);
    Quantity cashFromProperties(Properties props);

    public Quantity getScore(int round);
    public void setScore(int round, Quantity Score);
    public Quantity totalScore();

    String getScoreExplanation();

    Quantity currentCouponCount(BinaryClaim claim);

    void resetOutstandingOrders();

    String logConfigValues(Logger log, Properties props, int rounds);

    void linkHtml(StringBuffer buff);
    String pageLink();

    public boolean canBuy(int currentRound);
    public boolean canSell(int currentRound);

    boolean isDormant(int currentRound);
}
