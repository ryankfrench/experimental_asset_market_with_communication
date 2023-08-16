package net.commerce.zocalo.experiment.states;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** TransitionAdaptors are used when requesting a state transition.  If the
 transition is successful (if the StateHolder's currentState returns true to
 the transition request message, the appropriate transition method of the
 adaptor is invoked. */
public interface TransitionAdaptor {
    void endTrading();
    void startRound();
    void warn(String warning);
}
