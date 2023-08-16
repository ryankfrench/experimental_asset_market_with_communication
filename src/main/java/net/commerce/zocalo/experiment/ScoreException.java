package net.commerce.zocalo.experiment;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class ScoreException extends Exception {
    public ScoreException(Throwable throwable) {
        super(throwable);
    }

    public ScoreException(String s) {
        super(s);
    }
}
