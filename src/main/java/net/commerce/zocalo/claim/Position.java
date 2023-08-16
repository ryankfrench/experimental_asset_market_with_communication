package net.commerce.zocalo.claim;

import java.util.Set;
import java.util.HashSet;

// Copyright 2008 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** One of the Positions of a claim.  */

public class Position {
    private String name;
    private Claim claim;
    private Long id;

    public Position(String name, Claim claim) {
        this.name = name;
        this.claim = claim;
    }

    /** @deprecated */
    Position() {
        // for Hibernate
    }

    public Claim getClaim() {
        return claim;
    }

    public boolean isInvertedPosition() {
        return getClaim().isInvertedPosition(this);
    }

    public String qualifiedName() {
        return claim.getName() + ":" + name;
    }

    public boolean isBuy(boolean rising) {
        return getClaim().isBuy(this, rising);
    }

    public String getSimpleName() {
        return getClaim().getSimpleName(this);
    }

    /** @deprecated */
    void setName(String name) {
        this.name = name;
    }

    /** @deprecated */
    void setClaim(Claim claim) {
        this.claim = claim;
    }

    public String getName() {
        return name;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Position)) {
            return false;
        }
        Position p = (Position) o;

        if (! p.getClaim().equals(getClaim())) {
            return false;
        }
        if (getName() != null && p.getName() != null) {
            if (getName().equals(p.getName())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public String toString() {
        return qualifiedName();
    }

    public int comparePersistentId(Position other) {
        if (this.equals(other)) { return 0; }
        if (this.getId() == null) { return 0; }

        if (other.getId() == null) {
            return - other.comparePersistentId(this);  // other might be lazily initialized
        }

        return this.getId() < other.getId() ? -1 : 1;
    }

    public Position opposite() {
        Set<Position> compSet = complement();
        if (compSet.size() == 1) {
            return compSet.iterator().next();
        } else {
            throw new RuntimeException("Position has no unique opposite.");
        }
    }

    public Set<Position> complement() {
        Set<Position> comp = new HashSet<Position>();
        for (int i = 0; i < getClaim().positions().length; i++) {
            Position position = getClaim().positions()[i];
            if (! this.equals(position)) {
                comp.add(position);
            }
        }
        return comp;
    }

    /** @deprecated */
    private Long getId() {
        return id;
    }

    /** @deprecated */
    private void setId(Long id) {
        this.id = id;
    }
}
