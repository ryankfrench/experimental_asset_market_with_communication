package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.logging.GID;
import org.apache.log4j.Logger;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Refinement of {@link StateHolder} when Judging.   */
public class JudgingStateHolder implements StateHolder {
    private final SessionStatusAdaptor adaptor;
    private JudgingSessionState currentState;

    public JudgingStateHolder(SessionStatusAdaptor adaptor) {
        this.adaptor = adaptor;
        Object secret = new Object();
        JudgingSessionState tradingState = new JudgingSessionState.JudgingTradingState(secret);
        JudgingSessionState initializedState = new JudgingSessionState.JudgingInitializedState(tradingState);
        JudgingSessionState scoringState = new JudgingSessionState.JudgingScoringState(tradingState);
        JudgingSessionState judgingState = new JudgingSessionState.JudgingState(scoringState);
        ((JudgingSessionState.JudgingTradingState)tradingState).replaceNextState(secret, judgingState);
        currentState = initializedState;
    }

    private void transition(boolean success) {
        if (success) {
            currentState = currentState.nextJudgingState();
            logEvent(currentState.transitionMessage());
        }
    }

    static private void logEvent(String eventName) {
        Logger sessionLogger = Logger.getLogger(Session.class);
        sessionLogger.info(GID.log() + eventName);
    }

    /** after first round, ends the display of scores. */
    public void startNextRound(TransitionAdaptor adaptor) {
        transition(currentState.startNextRound(adaptor));
    }

    public void endTrading(TransitionAdaptor adaptor) {
        transition(currentState.endTrading(adaptor));
    }

    public void endJudging(JudgingTransitionAdaptor adaptor) {
        transition(currentState.endJudging(adaptor));
    }

    public void informTrading(StatusAdaptor adaptor) {
        currentState.informTrading(adaptor);
    }

    public void informInitialized(StatusAdaptor adaptor) {
        currentState.informInitialized(adaptor);
    }

    public void informShowingScores(StatusAdaptor adaptor) {
        currentState.informShowingScores(adaptor);
    }

    public TransitionAdaptor endTradingAdaptor(final boolean manual) {
        return new NoActionTransitionAdaptor() {
            public void endTrading() { adaptor.endTradingEvents(); }
            public void warn(String warning) {
                if (manual) {
                    adaptor.setErrorMessage(warning);
                }
            }
        };
    }

    public void informJudging(JudgingStatusAdaptor adaptor) throws ScoreException {
        currentState.informJudging(adaptor);
    }
}
