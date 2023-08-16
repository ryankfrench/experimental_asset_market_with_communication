package net.commerce.zocalo.user;

import net.commerce.zocalo.currency.CashBank;

import junit.framework.TestCase;

import java.security.NoSuchAlgorithmException;
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PasswordTest extends TestCase {
    public void testPasswordEntry() throws NoSuchAlgorithmException {
        PasswordUtil pwd = PasswordUtil.make(0);

        CashBank bank = new CashBank("test");
        SecureUser joe = new SecureUser("joe", bank.makeFunds(100), "goo", "joe@example.com");
        String password = pwd.nextToken(10);
        joe.setPassword(password);
        assertFalse(joe.verifyPassword("goo"));
        assertTrue(joe.verifyPassword(password));
    }
}
