package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import static net.commerce.zocalo.service.PropertyKeywords.*;
import net.commerce.zocalo.currency.Quantity;

import java.util.Properties;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** JudgeRole indicates that {@link Judge Judges} don't have accounts and aren't backed by users.  */
public class JudgeRole extends Role {
    public boolean requiresJudges() {
        return true;
    }

    public String roleKeyword() {
        return JUDGE_PROPERTY_WORD;
    }

    public AbstractSubject createSubject(User user) {
        return null;
    }

    public boolean needsUser() {
        return false;
    }

    public AbstractSubject createSubject(User user, int rounds, String playerName) {
        Judge judge = Judge.makeJudge(rounds);
        judge.setName(playerName);
        judge.resetScores(rounds);
        return judge;
    }

    public boolean canBuy(int currentRound) {
        return false;
    }

    public boolean canSell(int round) {
        return false;
    }
}
