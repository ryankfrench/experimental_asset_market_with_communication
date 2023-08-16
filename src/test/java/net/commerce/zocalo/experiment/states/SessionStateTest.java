package net.commerce.zocalo.experiment.states;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;

public class SessionStateTest extends TestCase {
    private final String BLANK = "";
    private final String TRADING = "Trading";
    private final String INITIALIZED = "initialized";
    private final String SCORING = "scoring";
    private final String END_ROUND = Session.END_TRADING_TRANSITION;
    private final String JUDGING = "Judging";
    public SessionStatusAdaptor endTradingAdaptor = new SessionStatusAdaptor() {
        public void endTradingEvents() { }
        public void calculateScores() throws ScoreException { }
        public void setErrorMessage(String warning) { }
    };

    public void testMockAdaptor() {
        MockTransitionAdaptor adaptor = new MockTransitionAdaptor();
        MockStateHolder mockStateHolder = new MockStateHolder();
        mockStateHolder.startNextRound(adaptor);
        assertEquals(Session.startRoundTransitionLabel(), adaptor.lastMsg);
        assertEquals(TRADING, mockStateHolder.state);
        mockStateHolder.endTrading(adaptor);
        assertEquals(END_ROUND, adaptor.lastMsg);
        assertEquals(SCORING, mockStateHolder.state);
    }

    public void testMockTradingState() {
        MockTransitionAdaptor tAdaptor = new MockTransitionAdaptor();
        MockTradingState tradingState = new MockTradingState();

        assertTrue(tradingState.endTrading(tAdaptor));
        assertEquals(END_ROUND, tAdaptor.lastMsg);
        assertEquals(BLANK, tAdaptor.lastWarning);

        assertFalse(tradingState.startNextRound(tAdaptor));
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertEquals(SessionState.CANNOT_START_ROUND, tAdaptor.lastWarning);

        MockStatusAdaptor sAdaptor = new MockStatusAdaptor();
        sAdaptor.trading();
        assertEquals(TRADING, sAdaptor.lastStatus);
        sAdaptor.judging();
        assertEquals(JUDGING, sAdaptor.lastStatus);
        sAdaptor.showingScores();
        assertEquals(SCORING, sAdaptor.lastStatus);
        sAdaptor.initialized();
        assertEquals(INITIALIZED, sAdaptor.lastStatus);
    }

    public void testStates() {
        MockTransitionAdaptor adaptor = new MockTransitionAdaptor();
        MockStatusAdaptor sAdaptor = new MockStatusAdaptor();

        MockTradingState tempState = new MockTradingState();
        SessionState tradingState = new SessionState.TradingState(tempState);
        SessionState initializedState = new SessionState.InitializedState(tradingState);
        SessionState scoringState = new SessionState.ScoringState(tradingState);
        ((SessionState.TradingState)tradingState).replaceNextState(tempState, scoringState);

        tradingState.endTrading(adaptor);
        assertEquals(END_ROUND, adaptor.lastMsg);
        assertEquals(BLANK, adaptor.lastWarning);
        tradingState.startNextRound(adaptor);
        assertEquals(BLANK, adaptor.lastMsg);
        assertEquals(SessionState.CANNOT_START_ROUND, adaptor.lastWarning);
        assertStateInforms(tradingState, sAdaptor, TRADING, BLANK);
        assertEquals(scoringState, tradingState.nextState());

        scoringState.endTrading(adaptor);
        assertEquals(BLANK, adaptor.lastMsg);
        assertEquals(SessionState.NOT_RUNNING, adaptor.lastWarning);
        scoringState.startNextRound(adaptor);
        assertEquals(Session.startRoundTransitionLabel(), adaptor.lastMsg);
        assertEquals(BLANK, adaptor.lastWarning);
        assertStateInforms(scoringState, sAdaptor, BLANK, SCORING);
        assertEquals(tradingState, scoringState.nextState());

        initializedState.endTrading(adaptor);
        assertEquals(BLANK, adaptor.lastMsg);
        assertEquals(SessionState.NOT_RUNNING, adaptor.lastWarning);
        initializedState.startNextRound(adaptor);
        assertEquals(Session.startRoundTransitionLabel(), adaptor.lastMsg);
        assertEquals(BLANK, adaptor.lastWarning);
        assertStateInforms(initializedState, sAdaptor, BLANK, BLANK);
        assertEquals(tradingState, initializedState.nextState());
    }

    private void assertStateInforms(SessionState state, MockStatusAdaptor adapt, String trading, String showing) {
        state.informTrading(adapt);
        assertEquals(trading, adapt.lastStatus());
        state.informShowingScores(adapt);
        assertEquals(showing, adapt.lastStatus());
    }

    public void testStateHolder() {
        MockTransitionAdaptor tAdaptor = new MockTransitionAdaptor();
        MockStatusAdaptor sAdaptor = new MockStatusAdaptor();

        StateHolder std = new StandardStateHolder(endTradingAdaptor);            // INITIALIZED
        assertEquals(INITIALIZED, sAdaptor.lastStatus());

        std.endTrading(tAdaptor);
        assertEquals(SessionState.NOT_RUNNING, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertEquals(BLANK, sAdaptor.lastStatus());  // because lastStatus() reset it.

        std.startNextRound(tAdaptor);                           // TRADING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(Session.startRoundTransitionLabel(), tAdaptor.lastMsg);
        assertTradingStatuses(std, sAdaptor, TRADING, BLANK, BLANK);

        std.startNextRound(tAdaptor);
        assertEquals(SessionState.CANNOT_START_ROUND, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertTradingStatuses(std, sAdaptor, TRADING, BLANK, BLANK);

        std.endTrading(tAdaptor);                                 // SCORING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(END_ROUND, tAdaptor.lastMsg);
        assertTradingStatuses(std, sAdaptor, BLANK, BLANK, SCORING);

        std.endTrading(tAdaptor);
        assertEquals(SessionState.NOT_RUNNING, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertTradingStatuses(std, sAdaptor, BLANK, BLANK, SCORING);

        std.startNextRound(tAdaptor);                           // TRADING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(Session.startRoundTransitionLabel(), tAdaptor.lastMsg);
        assertTradingStatuses(std, sAdaptor, TRADING, BLANK, BLANK);
    }

    public void testJudgingStateHolder() throws ScoreException {
        // in each state, we try to change to each of the unreachable states, then to
        // the one reachable one.  After each attempt, we check lastMsg, lastWarning,
        // and assert that the inform() msgs work
        MockTransitionAdaptor tAdaptor = new MockTransitionAdaptor();
        MockStatusAdaptor sAdaptor = new MockStatusAdaptor();

        JudgingStateHolder jSH = new JudgingStateHolder(endTradingAdaptor);            // INITIALIZED
        assertEquals(INITIALIZED, sAdaptor.lastStatus());

        jSH.endTrading(tAdaptor);
        assertEquals(SessionState.NOT_RUNNING, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertEquals(BLANK, sAdaptor.lastStatus());  // because lastStatus() reset it.

        jSH.endJudging(tAdaptor);
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertInitializedJudgeStatus(sAdaptor, jSH);

        jSH.startNextRound(tAdaptor);                           // TRADING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(Session.startRoundTransitionLabel(), tAdaptor.lastMsg);
        assertTradingJudgeStatus(sAdaptor, jSH);

        jSH.startNextRound(tAdaptor);
        assertEquals(SessionState.CANNOT_START_ROUND, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertTradingJudgeStatus(sAdaptor, jSH);

        jSH.endJudging(tAdaptor);
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertTradingJudgeStatus(sAdaptor, jSH);

        jSH.endTrading(tAdaptor);                                 // JUDGING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(END_ROUND, tAdaptor.lastMsg);
        assertJudgingStatus(sAdaptor, jSH);

        jSH.endTrading(tAdaptor);
        assertEquals(SessionState.NOT_RUNNING, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertJudgingStatus(sAdaptor, jSH);

        jSH.startNextRound(tAdaptor);
        assertEquals(SessionState.CANNOT_START_ROUND, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertJudgingStatus(sAdaptor, jSH);

        jSH.endJudging(tAdaptor);                              // SHOWING SCORES
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(SCORING, tAdaptor.lastMsg);
        assertScoringJudgeStatus(sAdaptor, jSH);

        jSH.endJudging(tAdaptor);
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertScoringJudgeStatus(sAdaptor, jSH);

        jSH.endTrading(tAdaptor);
        assertEquals(SessionState.NOT_RUNNING, tAdaptor.lastWarning);
        assertEquals(BLANK, tAdaptor.lastMsg);
        assertScoringJudgeStatus(sAdaptor, jSH);

        jSH.startNextRound(tAdaptor);                           // TRADING
        assertEquals(BLANK, tAdaptor.lastWarning);
        assertEquals(Session.startRoundTransitionLabel(), tAdaptor.lastMsg);
        assertTradingJudgeStatus(sAdaptor, jSH);
    }

    private void assertScoringJudgeStatus(MockStatusAdaptor sAdaptor, JudgingStateHolder jSH) throws ScoreException {
        assertJudgeTradingStatuses(jSH, sAdaptor, BLANK, BLANK, SCORING, BLANK);
    }

    private void assertJudgingStatus(MockStatusAdaptor sAdaptor, JudgingStateHolder jSH) throws ScoreException {
        assertJudgeTradingStatuses(jSH, sAdaptor, BLANK, BLANK, BLANK, JUDGING);
    }

    private void assertTradingJudgeStatus(MockStatusAdaptor sAdaptor, JudgingStateHolder jSH) throws ScoreException {
        assertJudgeTradingStatuses(jSH, sAdaptor, TRADING, BLANK, BLANK, BLANK);
    }

    private void assertInitializedJudgeStatus(MockStatusAdaptor sAdaptor, JudgingStateHolder jSH) throws ScoreException {
        assertJudgeTradingStatuses(jSH, sAdaptor, BLANK, INITIALIZED, BLANK, BLANK);
    }

    private void assertTradingStatuses(StateHolder holder, MockStatusAdaptor adaptor, String tradingResult, String initializedResult, String showingResult) {
        holder.informTrading(adaptor);
        assertEquals(tradingResult, adaptor.lastStatus());
        holder.informInitialized(adaptor);
        assertEquals(initializedResult, adaptor.lastStatus());
        holder.informShowingScores(adaptor);
        assertEquals(showingResult, adaptor.lastStatus());
    }

    private void assertJudgeTradingStatuses(JudgingStateHolder holder, MockStatusAdaptor adaptor, String tradingResult, String initializedResult, String showingResult, String judgingResult) throws ScoreException {
        holder.informTrading(adaptor);
        assertEquals(tradingResult, adaptor.lastStatus());
        holder.informInitialized(adaptor);
        assertEquals(initializedResult, adaptor.lastStatus());
        holder.informShowingScores(adaptor);
        assertEquals(showingResult, adaptor.lastStatus());
        holder.informJudging(adaptor);
        assertEquals(judgingResult, adaptor.lastStatus());
    }

    public void testJudgingStates() throws ScoreException {
        MockTransitionAdaptor adaptor = new MockTransitionAdaptor();
        MockStatusAdaptor sAdaptor = new MockStatusAdaptor();

        Object secret = new Object();
        JudgingSessionState tradingState = new JudgingSessionState.JudgingTradingState(secret);
        JudgingSessionState initializedState = new JudgingSessionState.JudgingInitializedState(tradingState);
        JudgingSessionState scoringState = new JudgingSessionState.JudgingScoringState(tradingState);
        JudgingSessionState judgingState = new JudgingSessionState.JudgingState(scoringState);
        ((JudgingSessionState.JudgingTradingState)tradingState).replaceNextState(secret, judgingState);

        tradingState.endTrading(adaptor);
        assertEquals(END_ROUND, adaptor.lastMsg());
        assertEquals(BLANK, adaptor.lastWarning());
        tradingState.startNextRound(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(SessionState.CANNOT_START_ROUND, adaptor.lastWarning());
        tradingState.endJudging(adaptor);
        assertEquals(BLANK, adaptor.lastMsg);
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, adaptor.lastWarning());
        assertJudgingStateInforms(tradingState, sAdaptor, TRADING, BLANK, BLANK);
        assertEquals(judgingState, tradingState.nextJudgingState());

        scoringState.endTrading(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(SessionState.NOT_RUNNING, adaptor.lastWarning());
        scoringState.startNextRound(adaptor);
        assertEquals(Session.startRoundTransitionLabel(), adaptor.lastMsg());
        assertEquals(BLANK, adaptor.lastWarning());
        scoringState.endJudging(adaptor);
        assertEquals(BLANK, adaptor.lastMsg);
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, adaptor.lastWarning());
        assertJudgingStateInforms(scoringState, sAdaptor, BLANK, SCORING, BLANK);
        assertEquals(tradingState, scoringState.nextJudgingState());

        initializedState.endTrading(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(SessionState.NOT_RUNNING, adaptor.lastWarning());
        initializedState.startNextRound(adaptor);
        assertEquals(Session.startRoundTransitionLabel(), adaptor.lastMsg());
        assertEquals(BLANK, adaptor.lastWarning());
        initializedState.endJudging(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(JudgingSessionState.NOT_AWAITING_FORECASTS, adaptor.lastWarning());
        assertJudgingStateInforms(initializedState, sAdaptor, BLANK, BLANK, BLANK);
        assertEquals(tradingState, initializedState.nextJudgingState());

        judgingState.endTrading(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(SessionState.NOT_RUNNING, adaptor.lastWarning());
        judgingState.startNextRound(adaptor);
        assertEquals(BLANK, adaptor.lastMsg());
        assertEquals(SessionState.CANNOT_START_ROUND, adaptor.lastWarning());

        judgingState.endJudging(adaptor);
        assertEquals(SCORING, adaptor.lastMsg());
        assertEquals(BLANK, adaptor.lastWarning());

        assertJudgingStateInforms(judgingState, sAdaptor, BLANK, BLANK, JUDGING);
        assertEquals(scoringState, judgingState.nextJudgingState());
    }

    private void assertJudgingStateInforms(JudgingSessionState state, MockStatusAdaptor adapt, String trading, String showing, String judging) throws ScoreException {
        state.informTrading(adapt);
        assertEquals(trading, adapt.lastStatus());
        state.informShowingScores(adapt);
        assertEquals(showing, adapt.lastStatus());
        state.informJudging(adapt);
        assertEquals(judging, adapt.lastStatus());
    }

    private class MockTransitionAdaptor extends JudgingTransitionAdaptor implements TransitionAdaptor {
        private String lastMsg;
        private String lastWarning;

        String lastMsg() {
            String msg = lastMsg;
            lastMsg = "";
            return msg;
        }

        String lastWarning() {
            String warning = lastWarning;
            lastWarning = "";
            return warning;
        }

        public void endTrading() { lastMsg = END_ROUND; lastWarning = BLANK; }
        public void warn(String warning) { lastWarning = warning; lastMsg = BLANK; }
        public boolean endJudging() { lastMsg = SCORING; lastWarning = BLANK; return true; }
        public void startRound() {
            lastMsg = Session.startRoundTransitionLabel();
            lastWarning = BLANK;
        }
    }

    private class MockStateHolder implements StateHolder {
        public String state;

        public void startNextRound(TransitionAdaptor adaptor) {
            state = TRADING;
            adaptor.startRound();
        }

        public void endTrading(TransitionAdaptor adaptor) {
            state = SCORING;
            adaptor.endTrading();
        }

        public void informTrading(StatusAdaptor adaptor) {
            if (state.equals(TRADING)) {
                adaptor.trading();
            }
        }

        public void informInitialized(StatusAdaptor adaptor) {
            if (state.equals(INITIALIZED)) {
                adaptor.initialized();
            }
        }

        public void informShowingScores(StatusAdaptor adaptor) {
            if (state.equals(SCORING)) {
                adaptor.showingScores();
            }
        }

        public TransitionAdaptor endTradingAdaptor(boolean manual) {
            return new TransitionAdaptor() {

                public void endTrading() { }
                public void startRound() { }
                public void warn(String warning) { }
            };
        }
    }

    private class MockTradingState implements SessionState {
        public boolean endTrading(TransitionAdaptor adaptor) {
            adaptor.endTrading();
            return true;
        }

        public boolean startNextRound(TransitionAdaptor adaptor) {
            adaptor.warn(SessionState.CANNOT_START_ROUND);
            return false;
        }

        public SessionState nextState() { return this; }
        public void informTrading(StatusAdaptor adaptor) {
            adaptor.trading();
        }

        public void informShowingScores(StatusAdaptor adaptor) { }
        public void informInitialized(StatusAdaptor adaptor) { }

        public String transitionMessage() {
            return "";
        }

        public void replaceNextState(SessionState tempState, SessionState next) { }
    }

    private class MockStatusAdaptor extends NoActionJudgingStatusAdaptor implements StatusAdaptor {
        private String lastStatus = INITIALIZED;
        public void trading() { lastStatus = TRADING; }
        public void showingScores() { lastStatus = SCORING; }
        public void initialized() { lastStatus = INITIALIZED; }
        public void judging() { lastStatus = JUDGING; }

        public String lastStatus() {
            String status = lastStatus;
            lastStatus = "";
            return status;
        }
    }
}
