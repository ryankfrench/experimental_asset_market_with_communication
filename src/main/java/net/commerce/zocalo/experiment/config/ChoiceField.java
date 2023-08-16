package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.html.HtmlSimpleElement;

import javax.servlet.http.HttpServletRequest;
// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** A field that provides a choice which may specify further configuration parameters.
    The choice made by this field specifies one of its groups as the active one.  */
public class ChoiceField implements ValueGroup {
    private String title;
    private ValueGroup[] groups;
    private String choice;

    public ChoiceField() { }

    public ChoiceField(String title, ValueGroup[] configurations) {
        this.title = title;
        this.groups = configurations;
        choice = configurations[0].getTitle();
    }

    public String asString(String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            if (configuration.hasValue(fieldName)) {
                return configuration.asString(fieldName);
            }
        }
        return null;
    }

    public boolean hasValue(String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            if (configuration.hasValue(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void setValue(String fieldName, String newValue) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            if (configuration.hasValue(fieldName)) {
                configuration.setValue(fieldName, newValue);
            }
        }
    }

    public String getValue(String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            if (configuration.hasValue(fieldName)) {
                return configuration.getValue(fieldName);
            }
        }
        return null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setGroups(ValueGroup[] groups) {
        this.groups = groups;
    }

    public boolean hasRole(String roleName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    public ConfigField getField(String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasValue(fieldName)) {
                return group.getField(fieldName);
            }
        }
        return null;
    }

    public ConfigField getField(String roleName, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(roleName, fieldName)) {
                return group.getField(roleName, fieldName);
            }
        }
        return null;
    }

    public ConfigField getConfigField(String fieldName) {
        return getField(fieldName);
    }

    public ValueGroup[] getGroups() {
        return groups;
    }

    public void setGroups(FieldGroup[] groups) {
        this.groups = groups;
    }

    public void setAllValues(HttpServletRequest request) {
        setChoice(request.getParameter(title));

        for (int i = 0; i < groups.length; i++) {
            ValueGroup field = groups[i];
            field.setAllValues(request);
        }
    }

    public void render(StringBuffer buf) {
        HtmlTable tbl = new HtmlTable();
        tbl.add(HtmlTable.BORDER, "0");
        tbl.render(buf);
        renderBody(buf);
        HtmlTable.endTag(buf);
    }

    private void renderBody(StringBuffer buf) {
        buf.append("\n<tr><td align=center>\n");
        renderChoiceButtons(buf);
        buf.append("\n");
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            boolean visible = configuration.getTitle().equals(choice);
            configuration.renderVisible(buf, visible);
        }
    }

    public void renderVisible(StringBuffer buf, boolean visible) {
        if (groups.length > 0) {
            HtmlTable table = new HtmlTable();
            table.add("id", title);
            HtmlTable.addVisibleStyle(visible, table);
            table.render(buf);
            renderBody(buf);
            HtmlTable.endTag(buf);
        } else {
            buf.append("&nbsp;");
        }
    }

    private void renderChoiceButtons(StringBuffer buf) {
        String[] buttons = new String[groups.length];
        for (int i = 0; i < groups.length; i++) {
            ValueGroup configuration = groups[i];
            buttons[i] = configuration.getTitle();
        }
        HtmlSimpleElement.visibleRadioButtons(buf, title, choice, buttons);
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public boolean hasRoleValue(String roleName, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(roleName, fieldName)) {
                return true;
            }
        }
        return false;
    }

    public void setRoleValue(String roleName, String fieldName, String newValue) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(roleName, fieldName)) {
                group.setRoleValue(roleName, fieldName, newValue);
                return;
            }
        }
    }

    public String getRoleValue(String roleName, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(roleName, fieldName)) {
                return getRoleValue(roleName, fieldName);
            }
        }
        return null;
    }

    public ConfigField getConfigField(String roleName, String fieldName) {
        for (int i = 0; i < groups.length; i++) {
            ValueGroup group = groups[i];
            if (group.hasRoleValue(roleName, fieldName)) {
                return group.getConfigField(roleName, fieldName);
            }
        }
        return null;
    }
}
