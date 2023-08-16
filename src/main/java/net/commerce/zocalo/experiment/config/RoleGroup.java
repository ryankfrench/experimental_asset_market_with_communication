package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlTable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A collection of descriptions of roles for an experiment.  baseFields are
    associated with the kind of experiment, while each of the roles represents
    a particular combination of parameters available for the experiment.  The
    initial set of roles specifies the kinds of roles available, and we should
    ensure that each type continues to be available.  Each type can be copied
    multiple times to describe each separate combination of parameters within
    that role.  */
public class RoleGroup implements RoleValueHolder, ValueGroup {
    private String roleGroupName;
    private RoleFields[] roles;
    private FieldGroup baseFields;

    public RoleGroup() { }

    public RoleGroup(String roleGroupName) {
        this.roleGroupName = roleGroupName;
        baseFields = new FieldGroup("empty", new ConfigField[0]);
        roles = new RoleFields[0];
    }

    public RoleGroup(String title, String baseName, ConfigField[] baseFields, ConfigField[] roleFields) {
        roleGroupName = title;
        this.baseFields = new FieldGroup(title, baseFields);
        roles = new RoleFields[1];
        roles[0] = new RoleFields(title + "A", roleFields, baseName);
    }

    /** @deprecated */
    public String getRoleGroupName() {
        return roleGroupName;
    }

    /** @deprecated */
    public void setRoleGroupName(String roleGroupName) {
        this.roleGroupName = roleGroupName;
    }

    public int roleCount() {
        return roles.length;
    }

    public void deleteRole(int index) {
        roles = copyWithout(index, roles);
    }

    private RoleFields[] copyWithout(int index, RoleFields[] existing) {
        RoleFields[] newRoles = new RoleFields[existing.length - 1];
        for (int i = 0; i < newRoles.length; i++) {
            if (i < index) {
                newRoles[i] = existing[i];
            } else {
                newRoles[i] = existing[i + 1];
            }
        }
        return newRoles;
    }

    public void setRoleName(int index, String name) {
        if (roles[index].getTitle().equals(name)) {
            return;
        } else if (getRoleNameList().contains(name)) {
            return;
        } else {
            roles[index].setRoleName(name);
        }
    }

    private Collection<String> getRoleNameList() {
        Collection<String> names = new HashSet<String>();
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            names.add(role.getTitle());
        }
        return names;
    }

    public String getRoleValue(String roleName, String fieldName) {
        RoleFields role = getRole(roleName);
        if (role == null) {
            return null;
        } else {
            return role.getRoleValue(roleName, fieldName);
        }
    }

    public ConfigField getConfigField(String roleName, String fieldName) {
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            if (role.getTitle().equals(roleName)) {
                return role.getConfigField(roleName, fieldName);
            }
        }
        return null;
    }

    public boolean hasValue(String fieldName) {
        return baseFields.hasValue(fieldName);
    }

    public void setValue(String fieldName, String newValue) {
        baseFields.setValue(fieldName, newValue);
    }

    public String getValue(String fieldName) {
        return baseFields.getValue(fieldName);
    }

    public String asString(String fieldName) {
        return baseFields.asString(fieldName);
    }

    public String getTitle() {
        return roleGroupName;
    }

    public ConfigField getConfigField(String fieldName) {
        return baseFields.getConfigField(fieldName);
    }

    public boolean hasRole(String roleName) {
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            if (role.hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    private RoleFields getRole(String roleName) {
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            if (role.getTitle().equals(roleName)) {
                return role;
            }
        }
        return null;
    }

    public boolean hasRoleValue(String roleName, String fieldName) {
        RoleFields role = getRole(roleName);
        return role != null && role.hasRoleValue(roleName, fieldName);
    }

    public void setRoleValue(String roleName, String fieldName, String value) {
        RoleFields role = getRole(roleName);
        if (role == null) {
            return;
        } else {
            role.setRoleValue(roleName, fieldName, value);
        }
    }

    public void render(StringBuffer buf) {
        HtmlTable.start(buf);
        renderBody(buf);
        HtmlTable.endTag(buf);
    }

    private void renderBody(StringBuffer buf) {
        buf.append("<tr><td><b>").append(roleGroupName).append("</tr>\n<tr><td>");
        baseFields.render(buf);
        buf.append("</td></tr>\n");
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            role.render(buf);
        }
    }

    public void renderVisible(StringBuffer buf, boolean visible) {
        HtmlTable table = new HtmlTable();
        table.add("id", roleGroupName);
        HtmlTable.addVisibleStyle(visible, table);
        table.render(buf);
        renderBody(buf);
        HtmlTable.endTag(buf);
    }

    public void setAllValues(HttpServletRequest request) {
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            role.setAllValues(request);
        }
    }

    public RoleFields[] getRoles() {
        return roles;
    }

    public void setRoles(RoleFields[] roles) {
        this.roles = roles;
    }

    public FieldGroup getBaseFields() {
        return baseFields;
    }

    public void setBaseFields(FieldGroup baseFields) {
        this.baseFields = baseFields;
    }

    public ConfigField getField(String fieldName) {
        return baseFields.getField(fieldName);
    }

    public ConfigField getField(String roleName, String fieldName) {
        for (int i = 0; i < roles.length; i++) {
            RoleFields role = roles[i];
            if (role.hasRoleValue(roleName, fieldName)) {
                return role.getField(roleName, fieldName);
            }
        }
        return null;
    }
}
