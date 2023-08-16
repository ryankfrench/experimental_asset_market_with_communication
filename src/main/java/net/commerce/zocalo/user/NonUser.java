package net.commerce.zocalo.user;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A User object for the login page so it can recieve warnings. */
public class NonUser extends Warnable {
    final private String name = "User(logging in)";

    public NonUser() {
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}
