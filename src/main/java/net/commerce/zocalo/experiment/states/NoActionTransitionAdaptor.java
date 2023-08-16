package net.commerce.zocalo.experiment.states;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Parent of {@link TransitionAdaptor TransitionAdaptors}.  Override methods to implement. */
abstract public class NoActionTransitionAdaptor implements TransitionAdaptor {
    public void endTrading() {
        // Do Nothing
    }

    public void startRound() {
        // Do Nothing
    }
}
