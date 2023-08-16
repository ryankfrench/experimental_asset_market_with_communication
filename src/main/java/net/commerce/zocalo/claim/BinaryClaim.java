package net.commerce.zocalo.claim;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Price;

import java.util.*;

/** BinaryClaims have two positions, commonly "yes" and "no".  */
public class BinaryClaim extends Claim {
    private static final String YES = "yes";
    private static final String NO = "no";
    private List<Position> positions = new ArrayList<Position>();

    private BinaryClaim(String claimName, User user, String shortDescription) {
        super(claimName, user, shortDescription);
        positions.add(new Position(YES, this));
        positions.add(new Position(NO, this));
    }

    static public BinaryClaim makeClaim(String claimName, User user, String shortDescription) {
        return new BinaryClaim(claimName, user, shortDescription);
    }

    /** @deprecated */
    BinaryClaim() {
        setPositions(new ArrayList<Position>());
    }

    public Position[] positions() {
        return new Position[] { getYesPosition(), getNoPosition() };
    }

    /** @deprecated */
    void setPositions(List<Position> positions) {
        this.positions = positions;
    }

    List getPositions() {
        return positions;
    }

    public boolean positionsInclude(Position position) {
        return getYesPosition().equals(position) || getNoPosition().equals(position);
    }

    public Price naturalPrice(Position position, Price price) {
        if (position == getYesPosition()) {
            return price;
        } else if (position == getNoPosition()) {
            return price.inverted();
        } else {
            throw new RuntimeException("requested pricing for position that doesn't belong");
        }
    }

    public boolean isInvertedPosition(Position position) {
        return NO.equals(position.getName());
    }

    public String getSimpleName(Position position) {
        return getName();
    }

    public boolean isBuy(Position position, boolean rising) {
        if (position.isInvertedPosition()) {
            return !rising;
        } else {
            return rising;
        }
    }

    public Position getNoPosition() {
        return lookupPosition(NO);
    }

    public Position getYesPosition() {
        return lookupPosition(YES);
    }
}
