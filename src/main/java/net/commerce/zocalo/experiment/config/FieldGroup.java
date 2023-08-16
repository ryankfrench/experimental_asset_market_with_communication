package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlTable;

import javax.servlet.http.HttpServletRequest;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A named group of value associations.  Can contain undifferentiated fields, roles,
    or subgroups containing yet more fields organized in other patterns.  roles,
    groups, and fields can each be empty when not needed */
public class FieldGroup implements ValueGroup {
    private String name;

    /** fields are individual value-holding fields at this level of the hierarchy. */
    private ConfigField[] fields;

    /** roles contains a collection of objects, each of which specifies the details for
     a specific role in an experiment.  The subjects described by that role will have
     the same initial holdings, and will see the same messages throughout the experiment.
     roles will normally only be used if this FieldGroup is directly held by a ChoiceField
     distinguishing between different possible experiments with different sets of roles. */
    private RoleValueHolder[] roles;

    /** groupings of related fields.  Might be an undifferentiated collection, or might
     be by choice or role.  */
    private ValueGroup[] groups;

    public FieldGroup(String title, ConfigField[] basicFields) {
        name = title;
        fields = basicFields;
        roles = new RoleGroup[0];
        groups = new FieldGroup[0];
    }

    public FieldGroup(String title, ValueGroup[] groups) {
        name = title;
        fields = new ConfigField[0];
        roles = new RoleGroup[0];
        this.groups = groups;
    }

//    public FieldGroup(String title, RoleGroup[] roles) {
//        name = title;
//        fields = new ValueHolder[0];
//        this.roles = roles;
//        groups = new FieldGroup[0];
//    }

    public FieldGroup(String title, ConfigField[] basicFields, ValueGroup[] groups, RoleGroup[] roles) {
        name = title;
        fields = basicFields;
        this.groups = groups;
        this.roles = roles;
    }

    /** @deprecated */
    public FieldGroup() {
//        fields = new ConfigField[0];
    }

    public void render(StringBuffer buf) {
        if (fields.length + roles.length + groups.length > 0) {
            HtmlTable.start(buf);
            renderBody(buf);
            HtmlTable.endTag(buf);
        } else {
            buf.append("&nbsp;");
        }
    }

    public void renderVisible(StringBuffer buf, boolean visible) {
        if (fields.length + roles.length + groups.length > 0) {
            HtmlTable table = new HtmlTable();
            table.add("id", name);
            HtmlTable.addVisibleStyle(visible, table);
            table.render(buf);
            renderBody(buf);
            HtmlTable.endTag(buf);
        } else {
            buf.append("&nbsp;");
        }
    }

    private void renderBody(StringBuffer buf) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            HtmlRow.startTag(buf);
            buf.append("<td>");
            field.render(buf);
            HtmlRow.endTag(buf);
            buf.append("\n");
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            HtmlRow.startTag(buf);
            buf.append("<td>");
            group.render(buf);
            HtmlRow.endTag(buf);
            buf.append("\n");
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder role = roles[i];
            HtmlRow.startTag(buf);
            buf.append("<td>");
            role.render(buf);
            HtmlRow.endTag(buf);
            buf.append("\n");
        }
    }

    public boolean hasValue(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            if (field.hasValue(fieldName)) {
                return true;
            }
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void setValue(String fieldName, String newValue) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            if (field.hasValue(fieldName)) {
                field.setValue(fieldName, newValue);
                return;
            }
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                group.setValue(fieldName, newValue);
                return;
            }
        }
    }

    public String getValue(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            if (field.hasValue(fieldName)) {
                return field.getValue(fieldName);
            }
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                return group.getValue(fieldName);
            }
        }
        return null;
    }

    public String getTitle() {
        return name;
    }

    public boolean hasRole(String roleName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRole(roleName)) {
                return true;
            }
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder holder = roles[i];
            if (holder.hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    public String asString(String fieldName) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            if (field.hasValue(fieldName)) {
                return field.asString(fieldName);
            }
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                return group.asString(fieldName);
            }
        }
        return null;
    }

    public ConfigField getField(String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                return group.getField(fieldName);
            }
        }
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            if (field.hasValue(fieldName)) {
                return field.getConfigField(fieldName);
            }
        }
        return null;
    }

    public ConfigField getField(String roleName, String fieldName) {
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder holder = roles[i];
            if (holder.hasRole(roleName)) {
                return holder.getConfigField(roleName, fieldName);
            }
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.getTitle().equals(roleName)) {
                return group.getField(roleName, fieldName);
            }
        }
        return null;
    }

    public ConfigField getConfigField(String fieldName) {
        return null;
    }

    public ConfigField getConfigField(String roleName, String fieldName) {
        return null;
    }

    public void setAllValues(HttpServletRequest request) {
        for (int i = 0; i < fields.length; i++) {
            ValueHolder field = fields[i];
            field.setAllValues(request);
        }
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            group.setAllValues(request);
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder role = roles[i];
            role.setAllValues(request);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigField[] getFields() {
        return fields;
    }

    public void setFields(ConfigField[] fields) {
        this.fields = fields;
    }

    public boolean hasRoleValue(String key, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(key, fieldName)) {
                return true;
            }
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder role = roles[i];
            if (role.hasRoleValue(key, fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void setRoleValue(String key, String fieldName, String newValue) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(key, fieldName)) {
                group.setRoleValue(key, fieldName, newValue);
                return;
            }
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder role = roles[i];
            if (role.hasRoleValue(key, fieldName)) {
                role.setRoleValue(key, fieldName, newValue);
                return;
            }
        }
    }

    public String getRoleValue(String key, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(key, fieldName)) {
                return group.getRoleValue(key, fieldName);
            }
        }
        for (int i = 0; i < roles.length; i++) {
            RoleValueHolder role = roles[i];
            if (role.hasRoleValue(key, fieldName)) {
                return role.getRoleValue(key, fieldName);
            }
        }
        return null;
    }

    public RoleValueHolder[] getRoles() {
        return roles;
    }

    public void setRoles(RoleValueHolder[] roles) {
        this.roles = roles;
    }

    public ValueGroup[] getGroups() {
        return groups;
    }

    public void setGroups(ValueGroup[] groups) {
        this.groups = groups;
    }
}
