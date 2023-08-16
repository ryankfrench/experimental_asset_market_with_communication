package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Holds the state for a {@link net.commerce.zocalo.experiment.Session}.
Implementations know what state transitions are allowed, and use
{@link net.commerce.zocalo.experiment.states.TransitionAdaptor TransitionAdaptors}
for the callbacks.   StateHolders also allow clients to choose an appropriate
action depending on the current Session state by providing a callback based on
{@link net.commerce.zocalo.experiment.states.StatusAdaptor StatusAdaptors}. */
public interface StateHolder {
    void startNextRound(TransitionAdaptor adaptor);
    void endTrading(TransitionAdaptor adaptor);

    void informTrading(StatusAdaptor adaptor);
    void informInitialized(StatusAdaptor adaptor);
    void informShowingScores(StatusAdaptor adaptor);

    public TransitionAdaptor endTradingAdaptor(boolean manual);
}
