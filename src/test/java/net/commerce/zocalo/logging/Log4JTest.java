package net.commerce.zocalo.logging;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;

import net.commerce.zocalo.ajax.events.Ask;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.JunitHelper;

public class Log4JTest extends JunitHelper {
    private final static Logger logger = Logger.getLogger(Log4JTest.class);
    private MockAppender mockAppender;

    static {
        Log4JHelper.getInstance();
    }

    protected void setUp() throws Exception {
        super.setUp();
        mockAppender = new MockAppender();
        logger.addAppender(mockAppender);
        Log4JHelper.getInstance();
    }

    public void testEmptyLog() {
        logger.info("Entering application.");
        assertEquals(3, 3);
    }

    public void testMockLogger() {
        int count = mockAppender.messageCount();
        logger.warn("Something happened.");
        assertTrue(count != mockAppender.messageCount());
    }

    public void testAppendActions() {
        BinaryClaim claim = BinaryClaim.makeClaim("loggable", new User("joe", null), "a loggable claim");
        final Ask ask = new Ask("seller", Price.dollarPrice(0.3), q(20), claim.getYesPosition(), logger);
        int count = mockAppender.messageCount();
        logger.info(ask);
        assertTrue(count != mockAppender.messageCount());
    }

    public void testLogSequenceNumbers() {
        long first = GID.next();
        assert(first > 0);
        GID.next();
        assertEquals((first + 2) + "# ", GID.log());
        GID.next();
        long another = GID.next();
        assertEquals(first + 4, another);
        assertEquals((first + 5) + "# ", GID.log());
    }
}
