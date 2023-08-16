package net.commerce.zocalo.experiment.states;

import org.apache.log4j.Logger;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.ScoreException;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** StateHolder for a standard format experiment that has states for trading
 and score display. */
public class StandardStateHolder implements StateHolder {
    private SessionState currentState;
    private final SessionStatusAdaptor adaptor;

    public StandardStateHolder(SessionStatusAdaptor adaptor) {
        this.adaptor = adaptor;
        Object secret = new Object();
        SessionState tradingState = new SessionState.TradingState(secret);
        SessionState initializedState = new SessionState.InitializedState(tradingState);
        SessionState scoringState = new SessionState.ScoringState(tradingState);
        ((SessionState.TradingState)tradingState).replaceNextState(secret, scoringState);
        currentState = initializedState;
    }

    private void transition(boolean success) {
        if (success) {
            currentState = currentState.nextState();
            logEvent(Session.startRoundText());
        }
    }

    static private void logEvent(String eventName) {
        Logger sessionLogger = Logger.getLogger(Session.class);
        sessionLogger.info(GID.log() + eventName);
    }

    public void startNextRound(TransitionAdaptor adaptor) {
        transition(currentState.startNextRound(adaptor));
    }

    public void endTrading(TransitionAdaptor adaptor) {
        transition(currentState.endTrading(adaptor));
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

    public TransitionAdaptor endTradingAdaptor(final boolean man) {
        return new NoActionTransitionAdaptor() {
            public void endTrading() {
                adaptor.endTradingEvents();
                try {
                    adaptor.calculateScores();
                } catch (ScoreException e) {
                    e.printStackTrace();
                }
            }

            public void warn(String warning) {
                if (man) {
                    adaptor.setErrorMessage(warning);
                }
            }
        };
    }
}
