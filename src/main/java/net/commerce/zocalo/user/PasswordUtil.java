package net.commerce.zocalo.user;

import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PasswordUtil {
    private Random random;
    private static MessageDigest shaDigestor = null;

    private PasswordUtil(long seed) {
        random = new Random(seed);
        initializeSHA();
    }

    static private void initializeSHA() {
        if (shaDigestor == null) {
            try {
                shaDigestor = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException e) {
                Logger logger = Logger.getLogger(PasswordUtil.class);
                logger.warn("Unable to load SHA algorithm; passwords in database will be stored as Base64.", e);
                shaDigestor = null;
            }
        }
    }

    static public PasswordUtil make(long seed) {
        return new PasswordUtil(seed);
    }

    public String nextToken(int length) {
        byte[] bytes = new byte[length + 10];
        random.nextBytes(bytes);
        return Base64.encodeBytes(bytes).substring(0, length);
    }

    static public byte[] hash(String password) {
        initializeSHA();
        if (shaDigestor == null) {
            return Base64.encodeBytes(password.getBytes()).getBytes();
        } else {
            return shaDigestor.digest(password.getBytes());
        }
    }
}
