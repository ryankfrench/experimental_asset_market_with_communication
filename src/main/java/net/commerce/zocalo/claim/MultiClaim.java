package net.commerce.zocalo.claim;

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Price;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** MultiClaims are Claims that support multiple outcomes, and manage the prices of each outcome. */
public class MultiClaim extends Claim {
    private List<Position> positions = new ArrayList<Position>();

    public static MultiClaim makeClaim(String name, User owner, String description, String[] positions) {
        return new MultiClaim(name, owner, description, positions);
    }

    public MultiClaim(String name, User owner, String description, String[] positions) {
        super(name, owner, description);
        List<Position> allPositions = new ArrayList<Position>();
        for (int i = 0; i < positions.length; i++) {
            Position position = new Position(positions[i].trim(), this);
            allPositions.add(position);
        }
        setPositions(allPositions);
    }

    /** @deprecated */
    public MultiClaim() {
    }

    public Position[] positions() {
        Position[] allPositions = new Position[positions.size()];
        int i = 0;
        for (Iterator iterator = positions.iterator(); iterator.hasNext(); i++) {
            allPositions[i] = (Position)iterator.next();
        }
        return allPositions;
    }

    /** @deprecated */
    void setPositions(List<Position> allPositions) {
        positions = allPositions;
    }

    public boolean positionsInclude(Position position) {
        return positions.contains(position);
    }

    public Price naturalPrice(Position position, Price price) {
        return price;
    }

    public String getSimpleName(Position position) {
        return position.qualifiedName();
    }

    public boolean isBuy(Position position, boolean rising) {
        return rising;
    }

    public boolean isInvertedPosition(Position position) {
        return false;
    }

    public List<Position> getPositions() {
        return positions;
    }
}
