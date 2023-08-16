package net.commerce.zocalo.currency;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.JunitHelper;

public class CashBankTest extends JunitHelper {
    public void testBankCreatesUsers() {
        CashBank rootBank = new CashBank("cash");
        User buyer = rootBank.makeEndowedUser("manager", new Quantity(1000));

        assertQEquals(1000, buyer.cashOnHand());
    }
}
