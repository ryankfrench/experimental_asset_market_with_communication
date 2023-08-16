package net.commerce.zocalo.experiment.config;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Objects that contain collections of fields.  Can access a field by name. */
public interface RoleValueGroup extends RoleValueHolder {
    ConfigField getField(String roleName, String fieldName);
}
