package net.commerce.zocalo.experiment.states;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Parent of {@link VotingStatusAdaptor VotingStatusAdaptors}.
 Override methods to implement. */
public class NoActionVotingStatusAdaptor extends VotingStatusAdaptor {
    private int currentRound;

    public void voting() {
        // Override to do something
    }

    public int currentRound() {
        return currentRound;
    }

    public NoActionVotingStatusAdaptor(int round) {
        currentRound = round;
    }

    public void trading() {
        // Override to do something
    }

    public void showingScores() {
        // Override to do something
    }

    public void initialized() {
        // Override to do something
    }
}
