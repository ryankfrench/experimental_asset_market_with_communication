package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** {@link StatusAdaptor StatusAdaptors} are used to take an action if the
session is in a particular state.  They are invoked by passing one as an
argument to a message to the {@link StateHolder} naming the state of interest. */
public interface SessionStatusAdaptor {
    void endTradingEvents();
    void calculateScores() throws ScoreException;
    void setErrorMessage(String warning);
}
