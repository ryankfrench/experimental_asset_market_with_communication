package net.commerce.zocalo.experiment.states;

import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** These classes represent the states when we are running an experiment in
    which  the subjects vote on messages to be displayed.  The
    states are {@link VotingInitializedState}, {@link VotingTradingState},
  {@link VotingScoringState}, and {@link VotingState}. */
interface VotingSessionState {
    boolean endVoting(VotingTransitionAdaptor adaptor);
    boolean endTrading(TransitionAdaptor adaptor);
    boolean startNextRound(TransitionAdaptor adaptor);

    void informTrading(StatusAdaptor adaptor);
    void informShowingVotes(StatusAdaptor adaptor);
    void informInitialized(StatusAdaptor adaptor);
    void informVoting(VotingStatusAdaptor adaptor);

    VotingSessionState nextVotingState();
    String transitionMessage();

    abstract static class NoAction implements SessionState, VotingSessionState {
        protected VotingSessionState nextState;
        private static final String NOT_WAITING_FOR_VOTES = "Not waiting for votes.";

        public NoAction(VotingSessionState next) {
            nextState = next;
        }

        public void informInitialized(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informTrading(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informShowingVotes(StatusAdaptor adaptor) {
            // DO NOTHING
        }

        public void informVoting(VotingStatusAdaptor adaptor){
            // DO NOTHING
        }

        public SessionState nextState() {
            return null;
        }

        public VotingSessionState nextVotingState() {
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

        public boolean endVoting(VotingTransitionAdaptor adaptor) {
            adaptor.warn(NOT_WAITING_FOR_VOTES);
            return false;
        }
    }

    static class VotingState extends NoAction {
        public VotingState(VotingSessionState next) {
            super(next);
        }

        public boolean endVoting(VotingTransitionAdaptor adaptor) {
            adaptor.endVoting();
            return true;
        }

        public void informVoting(VotingStatusAdaptor adaptor) {
            adaptor.voting();
        }

        public String transitionMessage() {
            Session session = SessionSingleton.getSession();
            return session.endTradingLabel();
        }
    }

    static class VotingTradingState extends NoAction {
        private Object stateInitializationKey;

        public VotingTradingState(Object secret) {
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

        public boolean replaceNextState(Object secret, VotingSessionState next) {
            if (stateInitializationKey == secret) {
                nextState = next;
                return true;
            }
            return false;
        }
    }

    static class VotingInitializedState extends NoAction {
        public VotingInitializedState(VotingSessionState next) {
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

    static class VotingScoringState extends NoAction {
        public VotingScoringState(VotingSessionState next) {
            super(next);
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            adaptor.showingScores();
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.startRound();
            return true;
        }

        public void informShowingVotes(StatusAdaptor adaptor) {
            adaptor.showingScores();
        }

        public String transitionMessage() {
            return END_SCORING_TEXT;
        }
    }
}
