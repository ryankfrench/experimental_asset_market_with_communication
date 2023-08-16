package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Parent of {@link JudgingStatusAdaptor JudgingStatusAdaptors}.
 Override methods to implement. */
public class NoActionJudgingStatusAdaptor extends JudgingStatusAdaptor {
    public void judging() throws ScoreException {
        // Override to do something
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
