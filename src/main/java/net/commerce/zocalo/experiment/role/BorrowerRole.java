package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
// Copyright 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.


public class BorrowerRole extends TraderRole {
    public BorrowerRole(String role) {
        super(role);
    }

    public AbstractSubject createSubject(User user, int rounds, String playerName) {
        Trader t = Borrower.makeTrader(user, this);
        t.resetScores(rounds);
        return t;
    }
}
