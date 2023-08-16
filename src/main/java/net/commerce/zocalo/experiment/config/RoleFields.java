package net.commerce.zocalo.experiment.config;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A collection of values for a particular named role.  Uses a FieldGroup
 to hold the valueFields.  RoleFields is also responsible for making copies
 of role descriptions. */
public class RoleFields implements RoleValueGroup {
    private String roleName;
    private String baseName;
    private FieldGroup fields;

    public RoleFields() {
    }

    public void render(StringBuffer buf) {
        buf.append("<tr id='" + baseName + "-" + roleName +  "'><td>" + roleName);
        renderButtons(buf, baseName + "-" + roleName);
        fields.render(buf);
    }

    private void renderButtons(StringBuffer buf, String name) {
        buf.append(copyButton(name) + (removeButton(name)));
    }

    private String removeButton(String name) {
        return "<input type=button class='smallFontButton' onclick=\"removeNode('" + name + "')\" value='remove'>";
    }

    private String copyButton(String name) {
        return "<input type=button class='smallFontButton' onclick=\"copyRoleNode('" + name + "')\" value='copy'>\n";
    }

    public void setAllValues(HttpServletRequest request) {
        // enumerate request looking for role values or enumerate fields?
        Enumeration fieldNames = request.getParameterNames();
        while (fieldNames.hasMoreElements()) {
            String wholeName = (String)fieldNames.nextElement();
            if (wholeName.startsWith(roleName + ".")) {
                String extension = wholeName.substring(roleName.length());
                fields.setValue(extension, request.getParameter(wholeName));
            }
        }
    }

    public RoleFields(String title, ConfigField[] fields, String baseName) {
        roleName = title;
        this.baseName = baseName;
        this.fields = new FieldGroup(title, fields);
    }

    public RoleFields(String title, ConfigField[] fields, ValueGroup[] groups, RoleGroup[] roles) {
        roleName = title;
        this.fields = new FieldGroup(title, fields, groups, roles);
    }

    public ConfigField getField(String name, String fieldName) {
        if (roleName.equals(name)) {
            return fields.getField(fieldName);
        }
        return null;
    }

    public boolean hasRoleValue(String name, String fieldName) {
        return roleName.equals(name) && fields.hasValue(fieldName);
    }

    public void setRoleValue(String roleName, String name, String newValue) {
        if (this.roleName.equals(roleName)) {
            fields.setValue(name, newValue);
        }
    }

    public String getRoleValue(String name, String fieldName) {
        if (roleName.equals(name)) {
            return fields.getValue(fieldName);
        } else {
            return null;
        }
    }

    public ConfigField getConfigField(String name, String fieldName) {
        if (roleName.equals(name)) {
            return fields.getField(fieldName);
        } else {
            return null;
        }
    }

    public String getTitle() {
        return roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public FieldGroup getFields() {
        return fields;
    }

    public void setFields(FieldGroup fields) {
        this.fields = fields;
    }

    public boolean hasRole(String name) {
        return roleName.equals(name);
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }
}
