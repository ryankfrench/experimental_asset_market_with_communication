package net.commerce.zocalo.experiment.config;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Exception thrown when an inappropriate request is made for a particular ConfigField.  */
public class TypeMisMatch extends Throwable {
    public TypeMisMatch(String fieldType, String fieldName) {
        super(fieldName + " isn't a kind of " + fieldType);
    }
}
