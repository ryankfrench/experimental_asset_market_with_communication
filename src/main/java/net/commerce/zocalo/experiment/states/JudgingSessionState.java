package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;
import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** These classes represent the states when we are running an experiment with
 {@link net.commerce.zocalo.experiment.role.Judge Judges} and
 {@link net.commerce.zocalo.experiment.role.Manipulator Manipulators}.  The
 states are {@link JudgingInitializedState}, {@link JudgingTradingState},
  {@link JudgingScoringState}, and  {@link JudgingState}. */
public interface JudgingSessionState {
    public final String NOT_AWAITING_FORECASTS = "Not waiting for Forecasts";

    boolean endJudging(JudgingTransitionAdaptor adaptor);
    boolean endTrading(TransitionAdaptor adaptor);
    boolean startNextRound(TransitionAdaptor adaptor);

    void informTrading(StatusAdaptor adaptor);
    void informShowingScores(StatusAdaptor adaptor);
    void informInitialized(StatusAdaptor adaptor);
    void informJudging(JudgingStatusAdaptor adaptor) throws ScoreException;
    
    JudgingSessionState nextJudgingState();
    String transitionMessage();

    abstract static class NoAction implements SessionState, JudgingSessionState {
        protected JudgingSessionState nextState;

        public NoAction(JudgingSessionState next) {
            nextState = next;
        }

        public void informInitialized(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informTrading(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informJudging(JudgingStatusAdaptor adaptor) throws ScoreException {
            // DO NOTHING
        }

        public SessionState nextState() {
            return null;
        }

        public JudgingSessionState nextJudgingState() {
            return nextState;
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.warn(Session.findSessionReplaceRoundString(CANNOT_START_ROUND));
            return false;
        }

        public boolean endTrading(TransitionAdaptor adaptor) {
            adaptor.warn(NOT_RUNNING);
            return false;
        }

        public boolean endJudging(JudgingTransitionAdaptor adaptor) {
            adaptor.warn(NOT_AWAITING_FORECASTS);
            return false;
        }
    }

    static class JudgingState extends NoAction {
        public JudgingState(JudgingSessionState next) {
            super(next);
        }

        public boolean endJudging(JudgingTransitionAdaptor adaptor) {
            return adaptor.endJudging();
        }

        public void informJudging(JudgingStatusAdaptor adaptor) throws ScoreException {
            adaptor.judging();
        }

        public String transitionMessage() {
            return Session.endTradingLabel();
        }
    }

    static class JudgingTradingState extends NoAction {
        private Object stateInitializationKey;

        public JudgingTradingState(Object secret) {
            super(null);
            stateInitializationKey = secret;
        }

        public boolean endTrading(TransitionAdaptor adaptor) {
            adaptor.endTrading();
            return true;
        }

        public void informTrading(StatusAdaptor adaptor) {
            adaptor.trading();
        }

        public String transitionMessage() {
            return Session.startRoundText();
        }

        public boolean replaceNextState(Object secret, JudgingSessionState next) {
            if (stateInitializationKey == secret) {
                nextState = next;
                return true;
            }
            return false;
        }
    }

    static class JudgingInitializedState extends NoAction {
        public JudgingInitializedState(JudgingSessionState next) {
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

    static class JudgingScoringState extends NoAction {
        public JudgingScoringState(JudgingSessionState next) {
            super(next);
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.startRound();
            return true;
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            adaptor.showingScores();
        }

        public String transitionMessage() {
            return END_SCORING_TEXT;
        }
    }
}
