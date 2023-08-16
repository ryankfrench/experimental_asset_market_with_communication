package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Parent of {@link JudgingStatusAdaptor JudgingStatusAdaptors}.  Provides
 judging() to be added to the {@link StatusAdaptor} interface. */
public interface JudgingStatusAdaptorI {
    void judging() throws ScoreException;
}
