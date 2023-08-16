package net.commerce.zocalo.experiment;

import java.util.Properties;
import java.io.StringBufferInputStream;
import java.io.IOException;

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.experiment.role.Manipulator;
import net.commerce.zocalo.experiment.role.TradingSubject;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class SessionTestCase extends PersistentTestHelper {
    private final String script =
            "sessionTitle: SessionTestCase\n" +
            "rounds: 3\n" +
            "players: traderA, traderB, manipulatorC, judgeD\n" +
            "timeLimit: 5\n" +
            "traderA.role: trader\n" +
            "traderB.role: trader\n" +
            "manipulatorC.role: manipulator\n" +
            "judgeD.role: judge\n" +
            "initialHint: Trading has not started yet.\n" +
            "\n" +
            "useUnaryAssets: false\n" +
            "endowment.trader: 100\n" +
            "endowment.manipulator: 50\n" +
            "tickets.trader: 30\n" +
            "tickets.manipulator: 20\n" +
            "scoringFactor.judge: 0.02\n" +
            "scoringConstant.judge: 250\n" +
            "scoringFactor.manipulator: 2\n" +
            "scoringConstant.manipulator: 200\n" +
            "\n" +
            "# These values are specified by round.\n" +
            "\n" +
            "commonMessage:        raisePrice, changePrice, noMessage\n" +
            "actualValue:          0,          100,         40\n" +
            "\n" +
            "# These values are specified by Player and Round.\n" +
            "\n" +
            "traderA.hint:         not100,     not40,       not100\n" +
            "traderB.hint:         not40,      notZero,     notZero\n" +
            "manipulatorC.hint:    not100,\tnotZero,     notZero\n" +
            "manipulatorC.earningsHint:    worth40,\tworth40,     worth100\n" +
            "manipulatorC.target:  40,         40,          100\n" +
            "\n" +
            "# text labels can be used in hints or commonMessage\n" +
            "\n" +
            "not100: The ticket value is not 100.\n" +
            "not40: The ticket value is not 40.\n" +
            "notZero: The ticket value is not 0.\n" +
            "\n" +
            "raisePrice: Some players are trying to raise the apparent price\n" +
            "changePrice: Some players are trying to change the apparent price\n" +
            "noMessage:\n" +
            "\n" +
            "worth40: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 40\n" +
            "\n" +
            "worth100: Your score will improve if the judge thinks the \\\n" +
            "tickets are worth 100";
    protected Properties props;
    protected Session session;
    protected TradingSubject traderA;
    protected TradingSubject traderB;
    protected Manipulator manipulatorC;
    protected Judge judgeD;

    protected void setUp() throws Exception {
        super.setUp();
        Log4JHelper.getInstance();
        props = new Properties();
        props.load(new StringBufferInputStream(script));
        HibernateTestUtil.resetSessionFactory();
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
        traderA = (TradingSubject) session.getPlayer("traderA");
        traderB = (TradingSubject) session.getPlayer("traderB");
        manipulatorC = (Manipulator) session.getPlayer("manipulatorC");
        judgeD = ((JudgingSession)session).getJudgeOrNull("judgeD");
        HibernateTestUtil.resetSessionFactory();
    }

    void setupSession(String script) throws IOException {
        props = new Properties();
        props.load(new StringBufferInputStream(script));
        setupBayeux();
        SessionSingleton.setSession(props, null);
        session = SessionSingleton.getSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        SessionSingleton.resetSession();
        session = null;
    }

    void setEstimate(Judge judge, int i, String estimate) {
        judge.setEstimate(i, Price.dollarPrice(estimate));
    }

    void setEstimate(Judge judge, int i, int estimate) {
        judge.setEstimate(i, Price.dollarPrice(estimate));
    }
}
