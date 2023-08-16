package net.commerce.zocalo.currency;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import java.util.List;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** CurrencyToken is a token that makes it possible to find out whether two
    Currency objects are compatible.  */
public class CurrencyToken {
    private Long id;
    private Object transientId;
    private String name;

    public CurrencyToken(String n) {
        name = n;
        transientId = new Object();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurrencyToken)) return false;

        final CurrencyToken currencyToken = (CurrencyToken) o;

        // This seems to give Hibernate the opportunity to page in the instVars
        return currencyToken.equalsIndirectly(this);
    }

    public boolean equalsIndirectly(CurrencyToken o) {
        if (this == o) return true;

        if (objectsNotEqual(id(), o.id())) return false;
        if (objectsNotEqual(getName(), o.getName())) return false;
        if (objectsNotEqual(transientId, o.transientId)) return false;

        return true;
    }

    private boolean objectsNotEqual(Object myTransId, Object hisTransId) {
        if (myTransId == null) {
            return hisTransId != null;
        } else {
            return !myTransId.equals(hisTransId);
        }
    }

    public int hashCode() {
        int result;
        result = (id() != null ? id().hashCode() : 0);
        result = 29 * result + (getName() != null ? getName().hashCode() : 0);
        result = 37 * result + (transientId != null ? transientId.hashCode() : 0);
        return result;
    }

    public CashBank lookupCashBank(Session session) throws HibernateException {
        Criteria bankCriterion = session.createCriteria(CashBank.class);
        bankCriterion.add(Expression.eq("id", id()));
        List tokens = bankCriterion.list();
        if (tokens.size() == 1) {
            return (CashBank) tokens.get(0);
        } else {
            throw new HibernateException("no unique root bank");
        }
    }

    public String getName() {
        return name;
    }

    /** @deprecated */
    void setName(String name) {
        this.name = name;
    }

    /** @deprecated */
    CurrencyToken() {
        // for Hibernate
    }

    private Long id() {
        return getId();
    }

    /** @deprecated */
    Long getId() {
        return id;
    }

    /** @deprecated */
    void setId(Long id) {
        this.id = id;
    }
}
