package net.commerce.zocalo.html;

import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** support for generating simple HTML.  Includes fragments as well as complete
    eleements.  */
public class HtmlSimpleElement implements HtmlElement {
    private String content;
    public static final String CENTERED = "align=center";
    private static final String[] CENTER_ONLY = new String[] { CENTERED };

    private HtmlSimpleElement(String content) {
        this.content = content;
    }

    public void render(StringBuffer buf) {
        buf.append(content);
    }

    static public HtmlSimpleElement centeredCell(String content) {
        return cell(nbspIfBlank(content), CENTER_ONLY);
    }

    static public HtmlSimpleElement centeredCellWithWidth(String content, String width) {
        String[] attributes = new String[]{CENTERED, "width=\"" + width + "%\""};
        return cell(nbspIfBlank(content), attributes);
    }

    static public HtmlSimpleElement centeredCellWithWidth(String content, int width) {
        String[] attributes = new String[]{CENTERED, "width=\"" + width + "%\""};
        return cell(nbspIfBlank(content), attributes);
    }

    static public HtmlSimpleElement cell(String text, String[] attributes) {
        String start = "<td";
        for (int i = 0; i < attributes.length; i++) {
            String attributePair = attributes[i];
            start = start + " " + attributePair;
        }
        return new HtmlSimpleElement(start + ">" + text + "</td>");
    }

    static public HtmlSimpleElement centeredPriceCell(Price price) {
        return cell(price.printHighPrecision(), CENTER_ONLY);
    }


    static public HtmlSimpleElement cell(String content) {
        return new HtmlSimpleElement("<td>" + nbspIfBlank(content) + "</td>");
    }

    static public String printTableCell(String s) {
        return "<td>" + nbspIfBlank(s) + "</td>";
    }

    static public String printTableCell(String s, String id) {
        return "<td><span id='" + id + "'>" + nbspIfBlank(s) + "</span></td>";
    }

    static private String nbspIfBlank(String s) {
        return "".equals(s) ? "&nbsp;" : s;
    }

    static public String labelledLine(String label, String text) {
        return "<p>" + label + ": <b>" + text + "</b>";
    }

    static public String labelledPreText(String label, String text) {
        return "<p>" + label + ": <br><table border=1><pre>" + text + "</pre></table>";
    }

    static private String radioButton(StringBuffer buff, String radioName, String button, boolean checked) {
        return radioButton(buff, radioName, button, button, checked, new HashMap<String, String>());
    }

    static public String radioButton(StringBuffer buf, String radioName, String buttonLabel, String buttonValue, boolean check, Map<String, String> extras) {
        buf.append("<label><input type=radio value='").append(buttonValue);
        buf.append("' name='").append(radioName).append("'");
        for (Iterator<String> stringIterator = extras.keySet().iterator(); stringIterator.hasNext();) {
            String key = stringIterator.next();
            buf.append(" ").append(key).append("='").append(extras.get(key)).append("'");
        }
        String checkString = check
                ? " checked"
                : "";
        buf.append(checkString).append(">");
        buf.append(buttonLabel);
        buf.append("</label>");
        return buf.toString();
    }

    static public String radioButtons(StringBuffer buff, String name, String selectedButton, String button2, boolean stacked) {
        String separator = stacked
                                ? "\n    <br>"
                                : " ";
        return radioButton(buff, name, selectedButton, true) + separator + radioButton(buff, name, button2, false);
    }

    static public String radioButtons(StringBuffer buf, String label, Position[] positions) {
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            radioButton(buf, label, position.getName(), false);
        }
        return buf.toString();
    }

    static public String visibleRadioButtons(StringBuffer buf, String group, String checkedButton, String[] allButtons) {
        for (int i = 0; i < allButtons.length; i++) {
            String button = allButtons[i];
            if (checkedButton.equals(button)) {
                radioButton(buf, group, button, true, allButtons);
            } else {
                radioButton(buf, group, button, false, allButtons);
            }
        }

        return buf.toString();
    }

    static public void radioButton(StringBuffer buf, String group, String label, boolean check, String[] allButtons) {
        buf.append("<label><input type=radio ");

        String buttonListWithout = copyWithout(allButtons, label);
        String onClickString = "onclick='updateVisibility(\"" + label + "\", \"" + buttonListWithout + "\")'";
        buf.append(onClickString);

        buf.append(" value='").append(label).append("' name='").append(group).append("'");

        if (check) {
            buf.append(" checked");
        }

        buf.append(">").append(label).append("</label>\n");
    }

    private static String copyWithout(String[] allButtons, String label) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < allButtons.length; i++) {
            String button = allButtons[i];
            if (! button.equals(label)) {
                buf.append(button).append(","); // leave excess commas; javascript ignores empty labels
            }
        }
        return buf.toString();
    }

    static public void printEmptyOrders(StringBuffer buf, String label) {
        buf.append("<p><b><h3>User has no ");
        buf.append(label);
        buf.append("</h3>\n");
    }

    static public void printToggledCloseClaimForm(StringBuffer buf, Market market, String pageName) {
        buf.append(formHeaderWithPost(pageName));
        checkBox(buf, "Close the claim", "close", "onclick", "\"toggleVisibility('closeDetails');\"");
        buf.append(hiddenInputField("claimName", market.getClaim().getName()))
                .append("    <div id='closeDetails' style='display:none;background:lightgray'>\n")
                .append("       <fieldset> Choose the outcome that will pay $1: <br>\n");
        radioButtons(buf, "positionName", market.getClaim().positions());
        buf.append("       </fieldset>\n")
                .append("       <center>\n")
                .append(submitInputField("Submit", ""))
                .append("       </center>\n")
                .append("    </div>\n</form>");
    }

    public static void checkBox(StringBuffer buf, String label, String name, String att, String attValue) {
        buf.append("<label><input type=checkbox name='").append(name).append("' ")
                .append(att).append("=").append(attValue).append(">")
                .append(label).append("</label>");
    }

    public static void checkBox(StringBuffer buf, String name, String display) {
        buf.append("<label><input type=checkbox name='").append(name).append("' >")
                .append(display).append("</label>");
    }

    public static void checkBox(StringBuffer buf, String name, boolean checked) {
        String check = checked ? " checked" : " ";
        buf.append("<label><input type=checkbox name='").append(name).append("'").append(check).append(">")
                .append(name).append("</label>");
    }

    static public void printHeader(StringBuffer buf, int headerLevel, String title) {
        if (! title.equals("")) {
            buf.append("<center><h" + headerLevel + ">" + title + "</h" + headerLevel + "></center>\n");
        }
    }

    static public String textInputField(String name) {
        return "<input type=text autocomplete=off size=3 name='" + name + "'>";
    }

    static public String labelledTextInputField(String name) {
        return name + ": " + textInputField(name);
    }

    public static String textInputField(String name, int size, int maxLength, String value, String trigger) {
        return "<input type=text size=" + size + " maxLength=" + maxLength + " name='" + name + "'" + value + trigger + ">";
    }

    static public String labelledQuantInputField(String name, int defaultValue) {
        return name + ": " + textInputField(name, Integer.toString(defaultValue), "3");
    }

    static public String labelledTextInputField(String name, String defaultValue) {
        return name + ": " + textInputField(name, defaultValue, "3");
    }

    static public String labelledWideTextInputField(String name, String defaultValue) {
        return name + ": <input type=text value='" + defaultValue + "' name='" + name + "'>";
    }

    static public String labelledMediumTextInputField(String name, String defaultValue) {
        return name + ": " + textInputField(name, defaultValue, "6");
    }

    static public String flaggedInputField(String name, int size, int value, String flag) {
        return "<input type='text' size='" + size + "' value='" + value + "' " + flag + " id='" + name + "' name='" + name + "'>";
    }

    private static String textInputField(String name, String defaultValue, String size) {
        return "<input type=text value='" + defaultValue + "' size=" + size + " name='" + name + "'>";
    }

    static public HtmlSimpleElement claimLink(String claimName, String pageName) {
        String link = "<a href='" + pageName + "?claimName=" + claimName + "'>" + claimName + "</a>";
        return centeredCell(link);
    }

    public static String link(String text, String name) {
        return ("<a href='" + name + "' target='_blank'>" + text + "</a>");
    }

    static public String simplePostFormHeader(String submitAction, String actionLabel,
                                              String hiddenName1, String hiddenValue1,
                                              String hiddenName2, String hiddenValue2) {
        return formHeaderWithPost(submitAction) + actionLabel +
                hiddenInputField(hiddenName1, hiddenValue1) +
                hiddenInputField(hiddenName2, hiddenValue2);
    }

    static public String simplePostFormHeader(String targetPage, String actionLabel,
                                              String hiddenName1, String hiddenValue1) {
        return formHeaderWithPost(targetPage) + actionLabel +
                hiddenInputField(hiddenName1, hiddenValue1);
    }

    static public String postFormHeaderWithClass(String submitAction, String className,
                                    String hiddenName1, String hiddenValue1,
                                    String hiddenName2, String hiddenValue2) {
        return postFormHeader(submitAction, className) +
                hiddenInputField(hiddenName1, hiddenValue1) +
                hiddenInputField(hiddenName2, hiddenValue2);
    }

    static public String postFormHeaderWithClass(String submitAction, String className,
                                    String hiddenName1, String hiddenValue1) {
        return postFormHeader(submitAction, className) +
                hiddenInputField(hiddenName1, hiddenValue1);
    }

    public static String postFormHeader(String submitAction, String className) {
        return MessageFormat.format("<form method=POST style=''margin-bottom:0;'' action={0} class=''{1}'' id=''{1}''>", submitAction, className);
    }

    public static String formHeaderWithPost(String targetPage) {
        return "<form method=POST action='" + targetPage + "' autocomplete=\"off\">";
    }

    public static String formHeaderWithGet(String submitAction) {
        return "<form method=GET style='margin-bottom:0;' action=" + submitAction + ">";
    }

    static public String hiddenInputField(String hiddenName, String value) {
        return "<input type=hidden name='" + hiddenName + "' value='" + value + "'>";
    }

    static public String disabledTextField(String name, String value) {
        return "<input type=text readonly style=\"background-color:lightGray;\" size=3 name='" + name + "' value='" + value + "'>";
    }

    static public void printAccountsTableRow(StringBuffer buf, Order order, Market market) {
        String quantAtPrice = order.quantity().printAsQuantity()
                + " @ " + order.naturalPrice().printAsWholeCents();
        String detailedPosName = order.position().getClaim().getName() + ":" + order.position().getName();
        HtmlElement[] rowCells = new HtmlElement[] { centeredCell(detailedPosName), centeredCell(quantAtPrice) };
        new HtmlRow(rowCells).render(buf);
    }

    static public void printScoreCell(StringBuffer buff, Quantity score) {
        String text;
        if (! score.isZero()) {
            text = score.printAsScore();
        } else {
            text = "";
        }
        buff.append(printTableCell(text));
    }

    static public HtmlSimpleElement priceCell(Order order, boolean buying, String targetPage, Market market) {
        if (order == null) {
            return centeredCell("&nbsp;");
        }
        String ownerName = order.ownerName();
        String priceDisplay = order.naturalPrice().toString();
        String style = "style=\"background-color:" + (buying ? "limegreen" : "orange") + ";\"";
        String priceCell = simplePostFormHeader(targetPage + "?userName=" + ownerName, "",
            "deleteOrderPrice", priceDisplay,
            "deleteOrderPosition", (buying ? "buy" : "sell")) +
                hiddenInputField("userName", ownerName) +
                hiddenInputField("claimName", order.position().getClaim().getName()) +
                submitInputField(priceDisplay, style) + "\n</form>\n";
        return centeredCell(priceCell);
    }

    static public String submitInputField(String value) {
        return submitInputField(value, "");
    }

    static public String disabledSubmitInputField(String value) {
        return submitInputField(value, "disabled");
    }

    static public String submitInputField(String value, String extraAttribute) {
        return "<input type=submit class='smallFontButton' " + extraAttribute + " name=action value='" + value + "'>";
    }

    static public String bold(String text) {
        return "<b>" + text + "</b>";
    }

    public static String coloredText(String text, String color, String className) {
        return "<span style='color:" + color + ";' class='" + className + "'>" + text + "</span>";
    }

    public static String redSpan(String text) {
        return "<span style='background:red;'>" + text + "</span>";
    }

    static public String newPageForm(String market, String label, boolean disableFlag) {
        return formHeaderWithPost(market) + "\n    " + 
                submitInputField(label, disableFlag ? "disabled" : "") +
                "\n    </form>\n";
    }

    public static String navButtonTable(String[] labels, String currentPage) {
        StringBuffer buf = new StringBuffer();
        buf.append("<a href=\"http://zocalo.sourceforge.net\">");
        buf.append("<img src=\"images/logo.zocalo-trim.PNG\" alt=\"http://zocalo.sourceforge.net\" align=\"right\" border=\"0\" height=\"50\">");
        buf.append("</a>");
        HtmlTable invisibleTable = new HtmlTable();
        invisibleTable.add("cellpadding", 0);
        invisibleTable.add("border", 0);
        invisibleTable.render(buf);
        buf.append("\n    ");
        HtmlRow.startTag(buf);
        for (int i = 0; i < labels.length; i = i + 2) {
            String page = labels[i];
            String label = labels[i + 1];
            buf.append("    <td>\n    "
                    + HtmlSimpleElement.newPageForm(page, label, page.equals(currentPage))
                    + "\n    </td>");
        }
        HtmlRow.endTag(buf);
        buf.append("\n<tr><td colspan='" + labels.length / 2 + "'><span id='belowNavButtons'>&nbsp;</span></td></tr>\n");
        HtmlTable.endTag(buf);
        return buf.toString();
    }

    public static void headerRow(StringBuffer buf, String[] labels) {
        if (labels.length > 0) {
            buf.append("<tr>");
            for (int i = 0; i < labels.length; i++) {
                String label = labels[i];
                addHeaderCell(buf, label);
            }
            buf.append("</tr>\n");
        }
    }

    private static void addHeaderCell(StringBuffer buf, String label) {
        buf.append("<th>");
        buf.append(label);
        buf.append("</th>");
    }

    public static String selectList(String name, String[] labels, String chosen) {
        StringBuffer buf = new StringBuffer();
        buf.append("<select name='" + name + "'>");
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i].trim();
            if (chosen != "" && label.equals(chosen)) {
                buf.append("<option SELECTED>" + label);
            } else {
                buf.append("<option>" + label);
            }
        }
        buf.append("</select>");
        return buf.toString();
    }

    public static boolean detectHtmlLinkSpecial(String input) {
        return input.matches(".*[<>&'\\?\"+].*");
    }

    public static boolean detectHtmlTextSpecial(String input) {
        return input.matches(".*[<>&'\"].*");
    }

    public static void slider(StringBuffer buff, String name, int start, int min, int max, int stepSize, int labelStepSize, String disabled, String feedbackLabel) {
        String viewerName = "slider" + name + "box";
        int steps = ((max - min) / stepSize) + 1;
        int labelStep = ((max - min) / labelStepSize) + 1;
        buff.append("\t\t<div dojoType='dijit.form.HorizontalSlider' name='" + name + "' onChange='dojo.byId(\"" + viewerName + "\").value=arguments[0];' " + disabled + " class='tundra'\n")
                .append("\t\t\tvalue='" + start + "' maximum='" + max + "' minimum='" + min + "' discreteValues='" + steps + "' style='width:80%;' id='" + name + "'>\n")
                .append("\t\t\t\t<ol dojoType='dijit.form.HorizontalRuleLabels' container='topDecoration' style='height:1.2em;font-size:75%;' count='" + labelStep + "' \n")
                .append("\t\t\t\t\tmaximum='" + max + "' minimum='" + min + "' constraints=\"{pattern:'#'}\"></ol>\n")
                .append("\t\t\t\t\t<div dojoType='dijit.form.HorizontalRule' container='topDecoration' count=" + labelStep + " style='height:5px;'></div>\n")
                .append("\t\t\t" + feedbackLabel + "&nbsp;<input readonly id='" + viewerName + "' size='2' value='" + start + "'>\n")
                .append("\t\t</div>");
    }
}
