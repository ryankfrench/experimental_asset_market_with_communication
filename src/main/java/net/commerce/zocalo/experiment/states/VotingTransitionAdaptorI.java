package net.commerce.zocalo.experiment.states;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Parent of {@link VotingTransitionAdaptor VotingTransitionAdaptors}.  Provides
 endVoting() to be added to the {@link TransitionAdaptor} interface. */
public interface VotingTransitionAdaptorI {
    void endVoting();
}
