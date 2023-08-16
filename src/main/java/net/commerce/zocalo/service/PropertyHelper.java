package net.commerce.zocalo.service;

import java.math.BigDecimal;
import java.util.Properties;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.logging.GID;
import org.apache.log4j.Logger;

import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** some methods for parsing Property files, and converting results to useful values.  */
public class PropertyHelper {
    static public String indirectPropertyForRound(String propName, int round, Properties props) {
        String indirectProperties = props.getProperty(propName);
        if (null == indirectProperties) {
            return "";
        }
        String result;
        String[] rawProperties = indirectProperties.split(COMMA_SPLIT_RE);
        if (round < 1 || round > rawProperties.length) {
            result = "";
        } else {
            String indirectProperty = rawProperties[round - 1];
            String ultimateProperty = props.getProperty(indirectProperty);
            if ("" == ultimateProperty || null == ultimateProperty) {
                result = indirectProperty.trim(); // JJDM 2017NOV01 Fixing trailing spaces on private dividends
            } else {
                result = ultimateProperty.trim();
            }
        }
        return result;
    }

    static public double parseDouble(String propertyName, Properties props) {
        return parseDouble(propertyName, props, 0.0);
    }

    public static double parseDouble(String propertyName, Properties props, double defaultValue) {
        String valueString = props.getProperty(propertyName);
        if ("".equals(valueString) || null == valueString) {
            Logger logger = configLogger();
            logger.warn(GID.log() + "no " + propertyName + " property found in initialization file, defaulting to " + defaultValue + ".");
            return defaultValue;
        }
        return Double.parseDouble(valueString.trim());
    }

    public static Integer parseInteger(String propertyName, Properties props, Integer defaultValue) {
        String valueString = props.getProperty(propertyName);
        if ("".equals(valueString) || null == valueString) {
            Logger logger = configLogger();
            logger.warn(GID.log() + "no " + propertyName + " property found in initialization file, defaulting to " + defaultValue + ".");
            return defaultValue;
        }
        return Integer.parseInt(valueString.trim());
    }

    static public Integer parseInteger(String propertyName, Properties props) {
        return parseInteger(propertyName, props, 0);
    }

    public static Quantity parseDecimalQuantity(String propertyName, Properties props, Quantity defaultValue) {
        BigDecimal n = parseDecimal(propertyName, props, (BigDecimal)null);
        if (n == null) {
            return defaultValue;
        }
        return new Quantity(n);
    }

    public static BigDecimal parseDecimal(String propertyName, Properties props, BigDecimal defaultValue) {
        String valueString = props.getProperty(propertyName);
        if ("".equals(valueString) || null == valueString) {
            Logger logger = configLogger();
            logger.warn(GID.log() + "no " + propertyName + " property found in initialization file, defaulting to " + defaultValue + ".");
            return defaultValue;
        }
        return new BigDecimal(valueString);
    }

    static public BigDecimal parseDecimal(String propertyName, Properties props) {
        return parseDecimal(propertyName, props, BigDecimal.ZERO);
    }

    static public Quantity parseDoubleNoWarn(String propertyName, Properties props) {
        String valueString = props.getProperty(propertyName);
        if ("".equals(valueString) || null == valueString) {
            return Quantity.ZERO;
        }
        ;
        return new Quantity(valueString.trim());
    }

    static public boolean parseBoolean(String propertyName, Properties props, boolean defaultValue) {
        String valueString = props.getProperty(propertyName);
        if ("".equals(valueString) || null == valueString) {
            Logger logger = configLogger();
            logger.warn(GID.log() + "no " + propertyName + " property found in initialization file; defaulting to " + defaultValue);
            return defaultValue;
        }
        return Boolean.valueOf(valueString.trim()).booleanValue();
    }

    static public String dottedWords(String firstPart, String secondPart) {
        return firstPart + DOT_SEPARATOR + secondPart;
    }

    static public boolean getUnaryAssets(Properties props) {
        return parseBoolean(UNARY_ASSETS, props, true);
    }

    static public Quantity getMaxTradingPrice(Properties props) {
        return new Quantity(parseDecimal(MAX_TRADING_PRICE, props, DefaultRange100));
    }

    static public Logger configLogger() {
        return Logger.getLogger("Session Config");
    }

    static public boolean getBetterPriceRequired(Properties props) {
        return parseBoolean(BETTER_PRICE_REQUIRED, props, true);
    }

    static public boolean getWholeShareTradingOnly(Properties props) {
        return parseBoolean(WHOLE_SHARE_TRADING_ONLY, props, true);
    }

    static public int parseTimeStringAsSeconds(String timeString) {
        int seconds;
        String[] timeComponents = timeString.split(":");
        if (timeComponents.length == 0) {
            seconds = 0;
        } else if (timeComponents.length == 1) {
            seconds = Integer.parseInt(timeComponents[0]) * 60;
        } else if (timeComponents.length == 2) {
            int minutes = 0;
            if (! timeComponents[0].equals("")) {
                minutes = Integer.parseInt(timeComponents[0]);
            }
            seconds = Integer.parseInt(timeComponents[1]) + (60 * minutes);
        } else {
            seconds = 0;
        }
        return seconds;
    }

    public static boolean getRequireReserves(Properties props) {
        return parseBoolean(REQUIRE_RESERVES, props, false);
    }

    public static Quantity getMaxDividend(Properties props) {
        String valueString = props.getProperty(MAX_DIVIDEND);
        if ("".equals(valueString) || null == valueString) {
            Logger logger = configLogger();
            logger.warn(GID.log() + "no " + MAX_DIVIDEND + " property found in initialization file, calculating...");
            return Quantity.ONE.negate();
        }
        return new Quantity(valueString.trim());
    }

    public static int getSliderInputStepSize(Properties props) {
        return parseInteger(SLIDER_STEPSIZE_KEYWORD, props, 5);
    }

    public static int getSliderLabelStepSize(Properties props) {
        return parseInteger(SLIDER_LABEL_STEPSIZE_KEYWORD, props, 20);
    }

    static public String privateValueLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, PRIVATE_VALUE_LABEL, DEFAULT_PRIVATE_VALUE_LABEL);
    }

    static public String averagePriceLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, AVERAGE_PRICE_LABEL, DEFAULT_AVERAGE_PRICE_LABEL);
    }

    static public String totalDividendLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, TOTAL_DIVIDEND_LABEL, DEFAULT_TOTAL_DIVIDEND_LABEL);
    }

    static public String dividendsPlusBonusLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, DIV_PLUS_BONUS_LABEL, DEFAULT_DIV_PLUS_BONUS_LABEL);
    }

    static public String assetsPlusBonusLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, ASSETS_PLUS_BONUS_LABEL, DEFAULT_ASSETS_PLUS_BONUS_LABEL);
    }

    static public String dividendAddedToCashLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, DIV_ADDED_TO_CASH_LABEL, DEFAULT_DIV_ADDED_TO_CASH_LABEL);
    }

    static public String divSubtractedLabel(Properties props) {
        return AbstractSubject.labelFromPropertyOrDefault(props, DIV_FROM_RESERVE_LABEL, DEFAULT_DIV_FROM_RESERVE_LABEL);
    }
}
