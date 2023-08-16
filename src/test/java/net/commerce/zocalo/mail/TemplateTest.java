package net.commerce.zocalo.mail;
// Copyright 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import junit.framework.TestCase;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.commerce.zocalo.user.UnconfirmedUser;

public class TemplateTest extends TestCase {
    public void testPatternTemplate() {
        String template = "foo{bar}foo";
        Pattern bar = Pattern.compile("\\{bar\\}");
        Matcher matcher = bar.matcher(template);
        String result = matcher.replaceAll("baz");
        assertEquals("foobazfoo", result);
    }

    public void testPatternTemplateTwice() {
        String template = "foo{bar}foo{bar}";
        Pattern bar = Pattern.compile("\\{bar\\}");
        Matcher matcher = bar.matcher(template);
        String result = matcher.replaceAll("baz");
        assertEquals("foobazfoobaz", result);
    }

    public void testTwoPatternTemplates() {
        String template = "{foo}{bar}{foo}{bar}";
        Pattern bar = Pattern.compile("\\{bar\\}");
        Pattern foo = Pattern.compile("\\{foo\\}");
        Matcher fooMatcher = foo.matcher(template);
        String fooResult = fooMatcher.replaceAll("baz");
        Matcher barMatcher = bar.matcher(fooResult);
        String result = barMatcher.replaceAll("glub");
        assertEquals("bazglubbazglub", result);
    }

    public void testStringTemplate() {
        StringTemplate fooTemplate = new StringTemplate("$foo$ $bar$ $baz$ $foo$");
        fooTemplate.setAttribute("foo", "joe");
        fooTemplate.setAttribute("bar", "sue");
        fooTemplate.setAttribute("baz", "sam");
        assertEquals("joe sue sam joe", fooTemplate.toString());
    }

    public void testStringTemplateProps() {
        StringTemplate fooTemplate = new StringTemplate("$foo$ $bar$ $baz$ $foo$");
        Properties props = new Properties();
        props.put("foo", "joe");
        props.put("bar", "sue");
        props.put("baz", "sam");
        fooTemplate.setAttributes(props);
        assertEquals("joe sue sam joe", fooTemplate.toString());
    }

    public void testStringTemplateFromFile() throws IOException {
        String token = "foo19321nansd";
        String name = "uh, Clem";
        String expected = "We received a request to register a new account on zocalo.sf.net with " +
                "your email address under the name \"" + name + "\".  " +
                "If you submitted this request, please confirm your registration by copying the confirmation " +
                "code into the blank on the confirmation page or by visiting the location below.\n\n" +
                "The confirmation code is:   " + token + "\n\n" +
                "The web address to confirm directly is:\n" +
                "\n  http://example.com/default?userName=" + name + "&confirmation=" + token + "\n\n" +
                "If you didn't attempt to create an account, please excuse the interruption.\n" +
                "\nSincerely,\nzocalo.sf.net Administrator\n";
        StringTemplateGroup group = new StringTemplateGroup("emailTest", MailUtil.TemplateDir);
        StringTemplate tpl = group.getInstanceOf("ConfirmRegistration");
        Properties props = new Properties();
        props.put("site", "zocalo.sf.net");
        props.put("userName", name);
        props.put(UnconfirmedUser.CONFIRMATION_URL, "http://example.com/default?userName=" + name + "&confirmation=" + token);
        props.put(UnconfirmedUser.CONFIRMATION_CODE, token);

        tpl.setAttributes(props);
        assertEquals(expected, tpl.toString());
    }
}
