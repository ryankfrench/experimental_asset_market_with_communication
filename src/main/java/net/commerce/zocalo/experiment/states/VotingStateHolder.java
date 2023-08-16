package net.commerce.zocalo.experiment.states;

import org.apache.log4j.Logger;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Refinement of {@link StateHolder} when Voting.   */
public class VotingStateHolder implements StateHolder {
    private final SessionStatusAdaptor adaptor;
    private VotingSessionState currentState;

    public VotingStateHolder(SessionStatusAdaptor adaptor) {
        this.adaptor = adaptor;
        Object secret = new Object();
        VotingSessionState tradingState = new VotingSessionState.VotingTradingState(secret);
        VotingSessionState initializedState = new VotingSessionState.VotingInitializedState(tradingState);
        VotingSessionState scoringState = new VotingSessionState.VotingScoringState(tradingState);
        VotingSessionState VotingState = new VotingSessionState.VotingState(scoringState);
        ((VotingSessionState.VotingTradingState)tradingState).replaceNextState(secret, VotingState);
        currentState = initializedState;
    }

    private void transition(boolean success) {
        if (success) {
            currentState = currentState.nextVotingState();
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
        boolean endTrade = currentState.endTrading(adaptor);
        transition(endTrade);
    }

    public void endVoting(VotingTransitionAdaptor adaptor) {
        transition(currentState.endVoting(adaptor));
    }

    public void informVoting(VotingStatusAdaptor adaptor) {
        currentState.informVoting(adaptor);
    }

    public void informTrading(StatusAdaptor adaptor) {
        currentState.informTrading(adaptor);
    }

    public void informInitialized(StatusAdaptor adaptor) {
        currentState.informInitialized(adaptor);
    }

    public void informShowingScores(StatusAdaptor adaptor) {
        currentState.informShowingVotes(adaptor);
    }

    public TransitionAdaptor endTradingAdaptor(final boolean manual) {
        return new NoActionTransitionAdaptor() {
            public void endTrading() {
                adaptor.endTradingEvents(); 
                try {
                    adaptor.calculateScores();
                } catch (ScoreException e) {
                    // ScoreExceptions only occur when judges haven't entered estimates.  Shouldn't happen here.
                    e.printStackTrace();
                }
            }

            public void warn(String warning) {
                if (manual) {
                    adaptor.setErrorMessage(warning);
                }
            }
        };
    }

}
