package net.commerce.zocalo.claim;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.Price;

import java.util.*;

/** The subject of a Prediction Market.  Claims have Positions.  Coupons
    are created to allow trading in the outcome of a claim.  The implementations 
    currently supported include BinaryClaims and MultiClaims.  */

public abstract class Claim {
    private String name;
    private User owner;
    private String description;
    private Long id;

    public Claim(String name, User owner, String description) {
        this.name = name;
        this.owner = owner;
        this.description = description;
    }

    /** @deprecated */
    Claim() {
        // for Hibernate
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    abstract public net.commerce.zocalo.claim.Position[] positions();

    public SortedSet<Position> sortPositions() {
        Position[] positions = positions();
        SortedSet<Position> sorted = new TreeSet<Position>(
                new Comparator<Position>() {
                    public int compare(Position p1, Position p2) {
                        return p1.comparePersistentId(p2);
                    }
        });
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            sorted.add(position);
        }
        return sorted;
    }

    /** @deprecated */
    abstract List<Position> getPositions();

    public String printPositions() {
        StringBuffer buf = new StringBuffer();
        Position[] positions = positions();
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            buf.append(position.getName());
            if (i < positions.length - 1) {
                buf.append(", ");
            }
        }
        return buf.toString();
    }

    public String[] positionNames() {
        Position[] positions = positions();
        String[] labels = new String[positions.length];
        for (int i = 0; i < positions.length; i++) {
            labels[i] = positions[i].getName();
        }
        return labels;
    }

    /** @deprecated */
    abstract void setPositions(List<Position> positions);
    abstract public boolean positionsInclude(Position position);

    public Position lookupPosition(String name) {
        for (Iterator iterator = getPositions().iterator(); iterator.hasNext();) {
            Position pos = (Position) iterator.next();
            if (name.equals(pos.getName())) {
                return pos;
            }
        }
        return null;
    }

    abstract public Price naturalPrice(Position position, Price price);

    abstract public String getSimpleName(Position position);

    abstract public boolean isBuy(Position position, boolean rising);

    abstract public boolean isInvertedPosition(Position position);

    /** @deprecated */
    private void setName(String name) {
        this.name = name;
    }

    /** @deprecated */
    private Long getId() {
        return id;
    }

    /** @deprecated */
    private void setId(Long id) {
        this.id = id;
    }

    /** @deprecated */
    private User getOwner() {
        return owner;
    }

    /** @deprecated */
    private void setOwner(User owner) {
        this.owner = owner;
    }
}
