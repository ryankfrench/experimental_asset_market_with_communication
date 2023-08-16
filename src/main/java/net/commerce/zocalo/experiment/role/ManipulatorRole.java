package net.commerce.zocalo.experiment.role;

import net.commerce.zocalo.user.User;
import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** ManipulatorRole indicates that {@link Manipulator Manipulators} are only used when {@link Judge Judges} are present.  */
public class ManipulatorRole extends Role {
    public boolean requiresJudges() {
        return true;
    }

    public String roleKeyword() {
        return MANIPULATOR_PROPERTY_WORD;
    }

    public AbstractSubject createSubject(User user, int rounds, String playerName) {
        Manipulator m = Manipulator.makeManipulator(user, this);
        m.resetScores(rounds);
        return m;
    }
}
