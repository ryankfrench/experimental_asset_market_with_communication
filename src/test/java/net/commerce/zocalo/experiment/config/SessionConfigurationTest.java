package net.commerce.zocalo.experiment.config;

// Copyright 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import net.commerce.zocalo.JunitHelper;
import net.commerce.zocalo.JspSupport.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

public class SessionConfigurationTest extends JunitHelper {
    public void testBasicConfig() throws TypeMisMatch {
        ExperimentConfig b = new ExperimentConfig(3, "2:00", "short");
        assertEquals(3, b.getInt("rounds"));
        int duration = b.getTime("duration");
        assertEquals(120, duration);
    }

    public void testXMLOutput() throws TypeMisMatch {
        ExperimentConfig config = new ExperimentConfig(5, "2", "longer");
        int rounds = config.getInt("rounds");
        assertEquals(5, rounds);
        assertEquals(120, config.getTime("duration"));
        byte[] bytes = writeObjToXmlBytes(config);
        Object obj = readObjFromBytes(bytes);
        assertTrue(obj instanceof ExperimentConfig);
        ExperimentConfig reborn = (ExperimentConfig)obj;
        int newRounds = reborn.getInt("rounds");
        assertEquals("Should be able to read an int back", 5, newRounds);
        assertEquals(120, reborn.getTime("duration"));
    }

    public void testXMLOutputGroups() throws TypeMisMatch {
        ExperimentConfig exp = new ExperimentConfig(5, "2", "longer");
        FieldGroup config = exp.initTopField(5, "2", "longer");
        String rounds = config.getValue("rounds");
        assertEquals("5", rounds);
        assertEquals("2", config.getValue("duration"));
        byte[] bytes = writeObjToXmlBytes(config);
        Object obj = readObjFromBytes(bytes);
        assertTrue(obj instanceof FieldGroup);
        FieldGroup reborn = (FieldGroup)obj;
        String newRounds = reborn.getValue("rounds");
        assertEquals("Should be able to read an int back", "5", newRounds);
        assertEquals("2", reborn.getValue("duration"));
    }

    public void testConfigFieldXml() {
        BooleanField betteringField = new BooleanField("priceBettering", false);
        BooleanField wholeShares = new BooleanField("WholeShareTrading", true);
        FieldGroup noCarryGroup = new FieldGroup("no Carry Forward", new ConfigField[]{});
        BooleanField displayCarryForwardScores = new BooleanField("DisplayCarryForward", false);
        String CARRY_FORWARD = "Carry Forward";
        FieldGroup carryForwardGroup = new FieldGroup(CARRY_FORWARD, new ConfigField[]{displayCarryForwardScores});
        ChoiceField carryForward = new ChoiceField(CARRY_FORWARD, new FieldGroup[] { carryForwardGroup, noCarryGroup } );
        FieldGroup unary = new FieldGroup("unary assets", new ConfigField[]{});
        FieldGroup binary = new FieldGroup("binary assets", new ConfigField[]{});
        ChoiceField unaryShares = new ChoiceField("unary shares", new FieldGroup[] { unary, binary } );

        IntField divValue = new IntField("dividend value", 0);
        IntField maxPrice = new IntField("maxPrice", 100);

        ConfigField[] marketFields = new ConfigField[]{betteringField, wholeShares, divValue, maxPrice,};
        ValueGroup[] marketGroups = new ValueGroup[]{carryForward, unaryShares,};
        FieldGroup marketGroup = new FieldGroup("market", marketFields, marketGroups, new RoleGroup[]{});
        byte[] bytes = writeObjToXmlBytes(marketGroup);
        Object obj = readObjFromBytes(bytes);
        assertTrue(obj instanceof FieldGroup);
        FieldGroup reborn = (FieldGroup)obj;
        assertEquals("false", reborn.getValue("priceBettering"));
    }

    public void testXMLOutputFields() throws TypeMisMatch {
        StringField s = new StringField("foo", "bar");
        assertEquals("foo", s.getTitle());
        assertEquals("bar", s.getValue("foo"));
        byte[] bytes = writeObjToXmlBytes(s);
        Object obj = readObjFromBytes(bytes);
        assertTrue(obj instanceof StringField);
        StringField reborn = (StringField)obj;
        String newVal = reborn.getValue("foo");
        assertEquals("Should be able to read a String back", "bar", newVal);
        assertEquals("foo", reborn.getTitle());

        String listName = "glub";
        StringListField list = new StringListField(listName, "1,two,tree");
        assertEquals(listName, list.getTitle());
        String[] strings = list.getStrings(listName);
        assertEquals("tree", strings[2]);
        byte[] listBytes = writeObjToXmlBytes(list);
        Object listObj = readObjFromBytes(listBytes);
        assertTrue(listObj instanceof StringListField);
        StringListField rebornList = (StringListField)listObj;
        String[] newList = rebornList.getStrings(listName);
        assertEquals("Should be able to read a StringList back", "two", newList[1]);
        assertEquals(listName, rebornList.getTitle());

        String intsName = "num";
        IntListField ints = new IntListField(intsName, "1,3,0");
        assertEquals(intsName, ints.getTitle());
        int[] intArray = ints.getInts(intsName);
        assertEquals(3, intArray[1]);
        byte[] intsBytes = writeObjToXmlBytes(ints);
        Object intsObj = readObjFromBytes(intsBytes);
        assertTrue(intsObj instanceof IntListField);
        IntListField rebornInts = (IntListField)intsObj;
        int[] newInts = rebornInts.getInts(intsName);
        assertEquals("Should be able to read a StringList back", 1, newInts[0]);
        assertEquals(intsName, rebornInts.getTitle());

        String timeName = "duration";
        TimeField time = new TimeField(timeName, "1:00");
        assertEquals(timeName, time.getTitle());
        int t = time.getTime(timeName);
        assertEquals(60, t);
        byte[] timeBytes = writeObjToXmlBytes(time);
        Object timeObj = readObjFromBytes(timeBytes);
        assertTrue(timeObj instanceof TimeField);
        TimeField rebornTime = (TimeField)timeObj;
        int newTime = rebornTime.getTime(timeName);
        assertEquals("Should be able to read a Time back", 60, newTime);
        assertEquals(timeName, rebornTime.getTitle());
    }

    private byte[] writeObjToXmlBytes(Object config) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream("temp.xml");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        XMLEncoder fenc = new XMLEncoder(fos);
//        fenc.writeObject(config);
//        fenc.close();
        XMLEncoder enc = new XMLEncoder(baos);
        enc.writeObject(config);
        enc.close();
        return baos.toByteArray();
    }

    private Object readObjFromBytes(byte[] bytes) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        XMLDecoder dec = new XMLDecoder(bais);
        return dec.readObject();
    }

    public void testConfigField() {
        ExperimentConfig config = new ExperimentConfig(5, "2", "longer");
        StringBuffer buf = new StringBuffer();
        config.initTopField(5, "2:00", "longer").render(buf);
        String patt = "<table [^>]*>.*<tr><td><table [^>]*>.*" +
                "\n<tr><td>title: .*value='longer' .*" +
                "\n<tr><td>rounds: .*value='5' .*" +
                "\n<tr><td>duration: .*value='2:00' .*" +
                "\n<tr><td><label><input [^>]*name='priceBettering'.*";
        assertREMatches(patt, buf.toString());
    }

    public void xtestReadFromRequestParameters() throws TypeMisMatch {
        HttpServletRequest req = new MockHttpServletRequest();
        Map map = req.getParameterMap();
        map.put("duration", "2");
//        map.put("duration", "2:00");
        map.put("rounds", "3");
        String title = "simpleExperiment";
        map.put("title", title);
        map.put("priceBettering", "true");
        map.put("wholeShareTrading", "false");
        ExperimentConfig config = new ExperimentConfig(req);
        assertEquals("should set duration from request", 120, config.getTime("duration"));
        assertEquals(3, config.getInt("rounds"));
        assertEquals(title, config.getString("title"));
    }

}
