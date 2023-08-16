package net.commerce.zocalo.experiment.config;

// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.JunitHelper;
import net.commerce.zocalo.html.HtmlSimpleElement;

public class ConfigFieldTest extends JunitHelper {
    private String priceBettering = "priceBettering";
    private String wholeShareTrading = "wholeShareTrading";

    public void testBooleanField() {
        BooleanField bf = new BooleanField(priceBettering, true);

        StringBuffer buf = new StringBuffer();
        bf.render(buf);
        assertEquals("<label><input type=checkbox name='" + priceBettering + "' checked>" + priceBettering + "</label>", buf.toString());
        assertEquals("true", bf.asString(priceBettering));
        bf.setValue(priceBettering, "false");
    }

    public void testStringField() {
        String val = "value";
        String name = "test";
        StringField sf = new StringField(name, val);
        assertEquals(val, sf.asString(name));
        assertNull(sf.asString("foo"));
        assertEquals(val, sf.getValue(name));
        assertNull(sf.getValue("bar"));
        assertTrue(sf.hasValue(name));
        assertFalse(sf.hasValue("glub"));
        StringBuffer buf = new StringBuffer();
        sf.render(buf);
        assertEquals(name + ": <input type=text value='" + val + "' size=6 name='" + name + "'>", buf.toString());
        String newVal = "newValue";
        sf.readString(newVal);
        assertEquals(newVal, sf.asString(name));
    }

    public void testFieldGroups() {
        ExperimentConfig config = new ExperimentConfig(3, "2", "foo");
        config.setValue("priceBettering", "false");
        config.setValue("wholeShareTrading", "true");
        config.setValue(priceBettering, "false");
        config.setValue(wholeShareTrading, "true");
        String bar = "bar";
        ConfigHolder top = config.initTopField(3, "2:00", bar);
        StringBuffer buf = new StringBuffer();
        top.render(buf);
        String tableToken = "<table .*>.*";
//        String expected =
//                tableToken +
//                    "<tr><td>" + tableToken +
//                        textField("title", bar) + intField("rounds", "3") + textField("duration", "2:00") +
//                        textField("saveFile", "bar") + ".*" +
//                    tableToken +
//                        checkboxField(priceBettering) + checkboxField(wholeShareTrading) + ".*" +
//                        intField("maxPrice", "100") + ".*";
        String expected =
                tableToken +
                    "<tr><td>" + tableToken +
                        textField("title", bar) + intField("rounds", "3") + textField("duration", "2:00") +
                        textField("saveFile", "bar") + ".*" +
                    tableToken + priceBettering + wholeShareTrading + ".*" +
                        intField("maxPrice", "100") + ".*";
//        assertREMatches(expected, buf.toString());
    }

    private String intField(String label, String value) {
        return "\n<tr><td>" + HtmlSimpleElement.labelledTextInputField(label, value) + "</tr>";
    }

    private String textField(String label, String value) {
        return ("\n<tr><td>" + label + ": <input type=text value='" + value + "' name='" + label + "'></tr>");
    }

    private String checkboxField(String label) {
        return "\n<tr><td><label><input type=checkbox name='" + label + "' >" + label + "</label></tr>";
    }

    public void testChoiceField() {
        FieldGroup conventionalGroup = new FieldGroup("conventional", new ConfigField[] { } );

        DoubleField judgeFactor = new DoubleField("judge's scoring factor", 1);
        DoubleField manipFactor = new DoubleField("manipulator's scoring factor", 1);
        FieldGroup judgeGroup = new FieldGroup("judging", new ConfigField[] { judgeFactor, manipFactor } );

        String voteRoundsLabel = "voteRounds";
        IntListField voteRounds = new IntListField(voteRoundsLabel, "1,2");
        FieldGroup voteGroup = new FieldGroup("voting", new ConfigField[] { voteRounds } );

        ChoiceField choice = new ChoiceField("main", new FieldGroup[] { conventionalGroup, judgeGroup, voteGroup } );
        assertEquals("main", choice.getTitle());
        assertTrue(choice.hasValue(voteRoundsLabel));
        assertEquals("1.0", choice.getValue(judgeFactor.getTitle()));
        assertEquals(voteRounds, choice.getField(voteRoundsLabel));
        StringBuffer buf = new StringBuffer();
        choice.render(buf);
        String expected = ".*updateVisibility.*conventional.*updateVisibility.*judging.*updateVisibility.*voting.*";
        assertREMatches(expected, buf.toString());
    }

    public void testDoubleField() {
        String judgeFact = "judge's scoring factor";
        DoubleField judgeFactor = new DoubleField(judgeFact, 1);
        assertEquals("1.0", judgeFactor.asString(judgeFact));
        assertEquals(1.0, judgeFactor.getDouble(), .0000001);
        StringBuffer buf = new StringBuffer();
        judgeFactor.render(buf);
        assertEquals(judgeFact + ": <input type=text value='" + 1.0 + "' size=3 name='" + judgeFact + "'>" , buf.toString());
    }

    public void testIntListField() {
        String voteRounds = "voteRounds";
        IntListField intList = new IntListField(voteRounds, "1,2");
        assertEquals("1,2", intList.asString(voteRounds));
        int[] ints = intList.getInts(voteRounds);
        assertEquals(2, ints.length);
        assertEquals(1, ints[0]);
        assertEquals(2, ints[1]);
        StringBuffer buf = new StringBuffer();
        intList.render(buf);
        assertEquals(voteRounds + ": <input type=text value='1,2' name='" + voteRounds + "'>" , buf.toString());
    }

    public void testRoleGroup() {
        String hint = "initial hint";
        String pleaseWait = "Please wait for the session to start.";
        String pricesVary = "prices may vary";
        StringField initialHint = new StringField(hint, pleaseWait);
        String judgeFact = "judge's scoring factor";
        DoubleField judgeFactor = new DoubleField(judgeFact, 1);

        RoleGroup judge = new RoleGroup("judge", "Judging", new ConfigField[] { judgeFactor }, new ConfigField[] { initialHint });
        assertEquals(1, judge.roleCount());
        String judgeA = "judgeA";
        judge.setRoleName(0, judgeA);

        assertFalse(judge.hasRole("judge"));
        assertFalse(judge.hasRole("judgeB"));
        assertREMatches("Please wait .*", judge.getRoleValue(judgeA, hint));
        judge.setRoleValue(judgeA, hint, pricesVary);
        assertREMatches(pricesVary, judge.getRoleValue(judgeA, hint));
        StringBuffer buf = new StringBuffer();
        judge.render(buf);
        String expected = ".*judge.*judgeA.*copy.*remove.*" + hint + ".*" + pricesVary + ".*";
        assertREMatches(expected, buf.toString());
    }
}
