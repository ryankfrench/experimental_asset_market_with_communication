package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.experiment.role.Role;
import net.commerce.zocalo.experiment.role.AbstractSubject;
// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** TraderRole indicates that {@link Trader Traders} are the normal case.  */
public class TraderRole extends Role {
    private String roleName;

    public TraderRole(String roleName) {
        this.roleName = roleName;
    }

    public String roleKeyword() {
        return roleName;
    }

    public AbstractSubject createSubject(User user, int rounds, String playerName) {
        Trader t = Trader.makeTrader(user, this);
        t.resetScores(rounds);
        return t;
    }
}
