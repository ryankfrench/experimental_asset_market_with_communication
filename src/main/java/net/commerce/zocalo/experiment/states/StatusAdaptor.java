package net.commerce.zocalo.experiment.states;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** StatusAdaptor are used to take a state-dependent action.  Override the method for the state
 of interest, and pass the adaptor to the current
 {@link net.commerce.zocalo.experiment.states.StateHolder}, and the adpator will be invoked if the
 named state is active. */
public interface StatusAdaptor {
    void trading();
    void showingScores();
    void initialized();
}
