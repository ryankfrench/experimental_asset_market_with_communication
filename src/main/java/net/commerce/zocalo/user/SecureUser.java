package net.commerce.zocalo.user;

import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.service.Config;

import org.apache.commons.lang.ArrayUtils;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** User with a password */
public class SecureUser extends User {
    private byte[] hashedPassword;
    private String email;

    public SecureUser(String name, Funds funds, String password, String emailAddress) {
        super(name, funds);
        this.setPassword(password);
        email = emailAddress;
    }

    /** @deprecated */
    SecureUser() {
    }

    public void setPassword(String password) {
        setHashedPassword(PasswordUtil.hash(password));
    }

    public void reportMarketMakerPurchase(Quantity quant, Position pos, boolean isBuy) {
        if (quant.isZero()) { return; }

        if (isBuy) {
            warn("Bought " + quant.printAsQuantity() + " from MarketMaker.");
        } else {
            warn("Sold " + quant.printAsQuantity() + " to MarketMaker.");
        }
    }

    public void reportBookPurchase(Quantity quant, Position pos) {
        if (pos.isInvertedPosition() || quant.isPositive()) {
            warn("Bought " + quant.printAsQuantity() + ".");
        } else {
            warn("Sold " + quant.negate().printAsQuantity() + ".");
        }
    }

    public boolean verifyPassword(String password) {
        return ArrayUtils.isEquals(getHashedPassword(), PasswordUtil.hash(password));
    }

    public boolean useCostLimitUI() {
        return Config.getCostLimitBuying();
    }

    /** @deprecated */
    void setHashedPassword(byte[] hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    /** @deprecated */
    byte[] getHashedPassword() {
        return hashedPassword;
    }

    /** @deprecated */
    public String getEmail() {
        return email;
    }

    /** @deprecated */
    public void setEmail(String email) {
        this.email = email;
    }

    static public boolean validateUserName(String userName) {
        return null != userName && userName.matches("[\\x2ea-zA-Z1-90+_-]+");
    }

    static public boolean validatePassword(String passwd) {
        return null != passwd && passwd.length() >= 7
                && passwd.matches("\\p{Graph}*[\\p{Digit}\\p{Punct}]\\p{Graph}*");
    }
}
