package net.commerce.zocalo.experiment.config;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Classes holding values related to roles in a lab experiment.  Can access
    or modify values given the names of the role and the field. Will also answer
    whether particular roles or fields are present. */
public interface RoleValueHolder extends ConfigHolder {
    boolean hasRoleValue(String roleName, String fieldName);
    void setRoleValue(String roleName, String fieldName, String newValue);
    String getRoleValue(String roleName, String fieldName);
    ConfigField getConfigField(String roleName, String fieldName);
    String getTitle();
    boolean hasRole(String roleName);
}
