package net.commerce.zocalo.experiment.config;

import net.commerce.zocalo.html.HtmlSimpleElement;

import javax.servlet.http.HttpServletRequest;
import java.beans.XMLEncoder;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** The root of an object structure representing the specification of an experiment.  */
public class ExperimentConfig {
    private FieldGroup topField;
    public static final String CONFIG_PAGE = "config";
    static public final String PRICE_BETTERING = "priceBettering";
    static public final String WHOLE_SHARE_TRADING = "wholeShareTrading";
    public final String SAVE_FILE = "saveFile";
    public final String MAIN_CHOICE_TITLE = "main";
    private String UNARY_SHARES = "unary";
    private String CARRY_FORWARD = "carryForward";
    private String DISPLAY_CARRY_FORWARD_SCORES = "display Carry Forward scores";
    public final String UNARY_ASSETS = "unary assets";
    public final String BINARY_ASSETS = "binary assets";

    public ExperimentConfig(int rounds, String time, String title) {
        topField = initTopField(rounds, time, title);
    }

    public ExperimentConfig(HttpServletRequest req) {
        topField = initTopField(req);
    }

    /** @deprecated */
    public ExperimentConfig() {
    }

    public FieldGroup initTopField(int rounds, String time, String title) {
        FieldGroup basicGroup = initBasicGroup(rounds, time, title);
        return initTopFieldFromBasicGroup(basicGroup);
    }

    private FieldGroup initTopField(HttpServletRequest req) {
        FieldGroup basicGroup = initBasicGroup(0, "0", "");
        FieldGroup allFields = initTopFieldFromBasicGroup(basicGroup);
        allFields.setAllValues(req);
        return allFields;
    }

    private FieldGroup initTopFieldFromBasicGroup(FieldGroup basicGroup) {
        ValueGroup[] allFields =
            new ValueGroup[] { basicGroup, createMarketGroup(), createConfigChoices() };
        return new FieldGroup("top", allFields);
    }

    private FieldGroup initBasicGroup(int rounds, String time, String title) {
        TimeField duration = new TimeField("duration", time);
        IntField r = new IntField("rounds", rounds);
        StringField t = new StringField("title", title);
        StringField f = new StringField(SAVE_FILE, title);
        ConfigField[] basicFields = new ConfigField[] { t, r, duration, f };
        return new FieldGroup("basic", basicFields);
    }

    private ChoiceField createConfigChoices() {
        FieldGroup voteGroup = createVoteChoiceGroup();
        FieldGroup judgeGroup = createJudgeChoiceGroup();
        FieldGroup conventionalGroup = createEmptyConventionalGroup();
        return new ChoiceField(MAIN_CHOICE_TITLE, new ValueGroup[] { conventionalGroup, judgeGroup, voteGroup } );
    }

    private FieldGroup createEmptyConventionalGroup() {
        String conventional = "Conventional";
        RoleGroup[] traderRole = {basicTraderRole(conventional)};
        return new FieldGroup(conventional, new ConfigField[] { }, new ValueGroup[] { }, traderRole);
    }

    private RoleGroup basicTraderRole(String baseName) {
        StringField hint = new StringListField("hint", "hint1,hint2");
        String roleName = "Trader";
        StringListField names = roleUsernameList(roleName);
        return new RoleGroup(roleName, baseName, new ConfigField[] { }, new ConfigField[] { hint, names } );
    }

    private StringListField roleUsernameList(String roleName) {
        return new StringListField("login names", roleName + "A1");
    }

    private RoleGroup manipulatorRole(String baseName) {
        StringField hint = new StringListField("hint", "hint1,hint2");
        IntListField target = new IntListField("target", "1,2");
        String roleName = "Manipulator";
        StringListField names = roleUsernameList(roleName);
        return new RoleGroup(roleName, baseName, new ConfigField[] { }, new ConfigField[] { hint, target, names } );
    }

    private RoleGroup judgeRole(String baseName) {
        String roleName = "Judge";
        StringListField names = roleUsernameList(roleName);
        return new RoleGroup(baseName, baseName, new ConfigField[0], new ConfigField[] { names} );
    }

    private FieldGroup createJudgeChoiceGroup() {
        DoubleField judgeFactor = new DoubleField("judge scoring factor", 1);
        DoubleField manipFactor = new DoubleField("manipulator scoring factor", 1);
        DoubleField judgeConstant = new DoubleField("judge scoring constant", 0);
        DoubleField manipConstant = new DoubleField("manipulator scoring constant", 0);
        String baseName = "Judge";
        RoleGroup[] roles = {basicTraderRole(baseName), manipulatorRole(baseName), judgeRole(baseName)};
        return new FieldGroup("Judging", new ConfigField[]{judgeFactor, judgeConstant, manipFactor, manipConstant}, new ValueGroup[0], roles);
    }

    private FieldGroup createVoteChoiceGroup() {
        String baseName = "Voting";
        String voteRoundsLabel = "voteRounds";
        IntListField voteRounds = new IntListField(voteRoundsLabel, "1,2");
        RoleGroup[] traderRole = {basicTraderRole(baseName)};
        ConfigField[] roundsField = {voteRounds};
        return new FieldGroup(baseName, roundsField, new ValueGroup[] { }, traderRole);
    }

    private FieldGroup createMarketGroup() {
        BooleanField betteringField = new BooleanField(PRICE_BETTERING, false);
        BooleanField wholeShares = new BooleanField(WHOLE_SHARE_TRADING, true);
        ChoiceField carryForward = createCarryForwardChoice();
        ChoiceField unaryShares = createUnarySharesChoice();
        IntField divValue = new IntField("dividend value", 0);
        IntField maxPrice = new IntField("maxPrice", 100);

        ConfigField[] marketFields = new ConfigField[] {betteringField, wholeShares, divValue, maxPrice, };
        ValueGroup[] marketGroups = new ValueGroup[] { carryForward, unaryShares, };
        return new FieldGroup("market", marketFields, marketGroups, new RoleGroup[] { } );
    }

    private ChoiceField createUnarySharesChoice() {
        FieldGroup unary = new FieldGroup(UNARY_ASSETS, new ConfigField[]{});
        FieldGroup binary = new FieldGroup(BINARY_ASSETS, new ConfigField[]{});
        return new ChoiceField(UNARY_SHARES, new FieldGroup[] { unary, binary } );
    }

    private ChoiceField createCarryForwardChoice() {
        FieldGroup noCarryGroup = new FieldGroup("no Carry Forward", new ConfigField[]{});
        BooleanField displayCarryForwardScores = new BooleanField(DISPLAY_CARRY_FORWARD_SCORES, false);
        FieldGroup carryForwardGroup = new FieldGroup("Carry Forward", new ConfigField[]{displayCarryForwardScores});
        return new ChoiceField(CARRY_FORWARD, new FieldGroup[] { carryForwardGroup, noCarryGroup } );
    }

    public String getString(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof StringField) {
            return ((StringField)field).stringValue();
        }

        throw new TypeMisMatch("String", fieldName);
    }

    public int getInt(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof IntField) {
            return ((IntField)field).intValue();
        }

        throw new TypeMisMatch("int", fieldName);
    }

    public int[] getInts(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof IntListField) {
            return ((IntListField)field).getInts(fieldName);
        }

        throw new TypeMisMatch("int", fieldName);
    }

    public boolean getBoolean(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof BooleanField) {
            return ((BooleanField)field).booleanValue();
        }

        throw new TypeMisMatch("boolean", fieldName);
    }

    public int getTime(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof TimeField) {
            return ((TimeField)field).intValue();
        }

        throw new TypeMisMatch("time", fieldName);
    }

    public String getChoice(String fieldName) throws TypeMisMatch {
        ConfigHolder field = topField.getField(fieldName);
        if (field instanceof ChoiceField) {
            return ((ChoiceField)field).getChoice();
        }

        throw new TypeMisMatch("choice", fieldName);
    }

    public void setValue(String fieldName, String newValue) {
        topField.setValue(fieldName, newValue);
    }

    /** @deprecated */
    public FieldGroup getTopField() {
        return topField;
    }

    /** @deprecated */
    public void setTopField(FieldGroup topField) {
        this.topField = topField;
    }

    public void renderHtml(StringBuffer buf) {
        buf.append(HtmlSimpleElement.formHeaderWithPost(CONFIG_PAGE + ".jsp"));
        topField.render(buf);
        buf.append(HtmlSimpleElement.submitInputField(ConfigEditor.UPDATE_COMMAND));
        buf.append(" &nbsp; ");
        buf.append(HtmlSimpleElement.submitInputField(ConfigEditor.SAVE_COMMAND));
        buf.append("\n\t\t</form>\n");

    }

    public boolean validate() {
        return true;
    }

    public void save() {
        String fileName = topField.getValue(SAVE_FILE);
        if (! fileName.matches(".*\\.xml")) {
            fileName = fileName + ".xml";
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        XMLEncoder fenc = new XMLEncoder(fos);
        fenc.writeObject(topField);
        fenc.close();
    }
}
