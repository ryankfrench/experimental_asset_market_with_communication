package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** These classes represent the states when we are running standard experiments.  The
 states are {@link InitializedState}, {@link TradingState}, and {@link ScoringState}. */
public interface SessionState {
    final String CANNOT_START_ROUND = "Must have finished the round and displayed scores to start next round.";
    public final String NOT_RUNNING = "Not currently Running.";

    boolean endTrading(TransitionAdaptor adaptor);
    boolean startNextRound(TransitionAdaptor adaptor);
    SessionState nextState();
    void informTrading(StatusAdaptor adaptor);
    void informShowingScores(StatusAdaptor adaptor);
    void informInitialized(StatusAdaptor adaptor);
    String transitionMessage();

    abstract static class NoAction implements SessionState {
        protected SessionState nextState;

        public NoAction(SessionState next) {
            nextState = next;
        }

        public void informTrading(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informInitialized(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public SessionState nextState() {
            return nextState;
        }

        public boolean endTrading(TransitionAdaptor adaptor) {
            adaptor.endTrading();
            adaptor.warn(NOT_RUNNING);
            return false;
        }
    }

    public static class TradingState extends NoAction {
        private Object stateInitializationKey;

        public TradingState(Object secret) {
            super(null);
            stateInitializationKey = secret;
        }

        public boolean endTrading(TransitionAdaptor adaptor) {
            adaptor.endTrading();
            return true;
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.warn(Session.cannotStartRoundMessage());
            return false;
        }

        public void informTrading(StatusAdaptor adaptor) {
            adaptor.trading();
        }

        public String transitionMessage() {
            return Session.startRoundText();
        }

        public boolean replaceNextState(Object secret, SessionState next) {
            if (stateInitializationKey == secret) {
                nextState = next;
                return true;
            }
            return false;
        }
    }

    public static class InitializedState extends NoAction {
        public InitializedState(SessionState next) {
            super(next);
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.startRound();
            return true;
        }

        public void informInitialized(StatusAdaptor adaptor) {
            adaptor.initialized();
        }

        public String transitionMessage() {
            return "";
        }
    }

    public static class ScoringState extends NoAction {
        public ScoringState(SessionState next) {
            super(next);
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            adaptor.showingScores();
        }

        public String transitionMessage() {
            return Session.endTradingLabel();
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.startRound();
            return true;
        }
    }
}
