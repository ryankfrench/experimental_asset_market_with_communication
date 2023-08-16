package net.commerce.zocalo.ajax.events;
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.experiment.role.Judge;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.logging.MockAppender;
import org.apache.log4j.Logger;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PrivateEventTest extends PersistentTestHelper {
    private String jName = "judgeDredd";
    private String expected = "\\d+# Judge '" + jName + "' timer expired for judging.";

    public void testPrivateEvents() {
        Judge judge = Judge.makeJudge(3);
        judge.setName(jName);
        IndividualTimingEvent event = Judge.judgeTimingEvent(judge);
        assertREMatches(expected, event.toLogString());
    }

    public void testAppender() {
        Logger logger = IndividualTimingEvent.getActionLogger();
        Judge judge = Judge.makeJudge(3);
        judge.setName(jName);
        MockAppender appender = new MockAppender();
        logger.addAppender(appender);

        IndividualTimingEvent event = Judge.timerEvent(judge, logger);

        assertREMatches(expected, event.toLogString());

        logger.callAppenders(event);
        IndividualTimingEvent action = (IndividualTimingEvent) appender.getEvents().iterator().next();
        assertREMatches(expected, action.toString());
    }
}
