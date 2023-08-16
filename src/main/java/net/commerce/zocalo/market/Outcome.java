package net.commerce.zocalo.market;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Probability;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Outcome for a market. When a market is closed, we persistently
 record what the outcome was, and who was paid off in the settlement.
 While the market is open, we represent that fact and the choice as to
 whether there will eventually be a single outcome. */
abstract public class Outcome {
    private long id;
    
    abstract public boolean isContinuous();
    abstract public boolean isOpen();
    abstract public Probability outcome(Position pos);

    /** @deprecated */
    Outcome() {
    }

    public static Outcome newOpen(Boolean isContinuous) {
        OpenOutcome outcome = new OpenOutcome(isContinuous);
        HibernateUtil.save(outcome);
        return outcome;
    }

    static Outcome newSingle(Position pos) {
        SingleOutcome outcome = new SingleOutcome(pos);
        HibernateUtil.save(outcome);
        return outcome;
    }

    /** @deprecated */
    private long getId() {
        return id;
    }

    /** @deprecated */
    private void setId(long id) {
        this.id = id;
    }

    abstract public String description();

    private static class OpenOutcome extends Outcome {
        private Boolean continuous;

        public OpenOutcome(Boolean continuous) {
            this.continuous = continuous;
        }

        /** @deprecated */
        OpenOutcome() {
        }

        public String description() {
            return "undecided.";
        }

        public Outcome makeOpenOutcome(Boolean isContinuous) {
            return new OpenOutcome(isContinuous);
        }

        /** @deprecated */
        public Boolean getContinuous() {
            return continuous;
        }

        /** @deprecated */
        public void setContinuous(Boolean continuous) {
            this.continuous = continuous;
        }

        public boolean isContinuous() {
            return continuous;
        }

        public boolean isOpen() {
            return true;
        }

        public Probability outcome(Position pos) {
            throw new RuntimeException("Open Outcomes have no Outcomes.");
        }
    }

    private static class SingleOutcome extends Outcome {
        private Position position;

        /** @deprecated */
        SingleOutcome() {
        }

        public String description() {
            return position.getName();
        }

        /** @deprecated */
        private Position getPosition() {
            return position;
        }

        /** @deprecated */
        private void setPosition(Position position) {
            this.position = position;
        }

        public boolean isContinuous() {
            return false;
        }

        public boolean isOpen() {
            return false;
        }

        public SingleOutcome(Position pos) {
            position = pos;
        }

        public Probability outcome(Position pos) {
            if (position.equals(pos)) {
                return Probability.ALWAYS;
            } else if (position.getClaim().positionsInclude(pos)) {
                return Probability.NEVER;
            } else {
                throw new RuntimeException("Not a valid position.");
            }
        }
    }
}
