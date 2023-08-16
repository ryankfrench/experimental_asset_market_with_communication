package net.commerce.zocalo.service;

import java.math.BigDecimal;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Interface that holds many strings useful for reading Zocalo configuration files.  */
public interface PropertyKeywords {

////////////////////////////// DELIMITERS & Regular Expressions
    final public String DELIMITER                 = "+";
    final public String DOT_SEPARATOR             = ".";
    final public String COLON_DELIMITER           = ":";
    final public String COMMA_SPLIT_RE            = "[ \t\n]*,[ \t\n]*";
    final public String SEMICOLON_SPLIT_RE            = "[ \t\n]*;[ \t\n]*";
    final public String YES_TRUE_RE               = "(yes|true|YES|TRUE)";

////////////////////////////// SESSION CONFIGURATION
    final public String SESSION_TITLE_PROPNAME    = "sessionTitle";
    final public String LOGO_PATH_PROP_NAME       = "logoPath";
    final public String LOGO_DEFAULT_PATH         = "images/logo.zocalo.jpg";
    final public String LOGO_URL_NONE             = "none";
    final public String SERVER_PROPERTY_WORD      = "server";
    final public String HOST_PROPERTY_WORD        = "host";
    final public String PORT_PROPERTY_WORD        = "port";

////////////////////////////// EXPERIMENT CONFIGURATION
    final public String PLAYERS_PROPNAME          = "players";
    final public String TIME_LIMIT_PROPNAME       = "timeLimit";
    final public String ROUNDS_PROPNAME           = "rounds";
    final public String ENDOWMENT_PROPERTY_WORD   = "endowment";
    final public String TICKETS_PROPERTY_WORD     = "tickets";
    final public String WHOLE_SHARE_TRADING_ONLY  = "WholeShareTradingOnly";
    final public String DISPLAY_AVERAGES          = "displayAverages";
    final public String BETTER_PRICE_REQUIRED     = "betterPriceRequired";
    final public String UNARY_ASSETS              = "useUnaryAssets";
    final public String REQUIRE_RESERVES          = "requireReserves";
    final public String SHARE_LIMIT_PROPERTY_WORD = "shareHoldingLimit";
    final public String CAPITAL_GAINS             = "capitalGains";
    final public String HISTORIC_COST_PROPERTY_WORD     = "historicCost";
    final public String FUNDAMENTAL_VALUE_PROPERTY_WORD = "fundamentalValue";
    final public String MARK_TO_MARKET_PROPERTY_WORD    = "markToMarket";

    final public String CARRY_FORWARD             = "carryForward";
    final public String DISPLAY_CARRY_FORWARD_SCORES = "displayCarryForwardScores";
    final public String CARRY_FORWARD_ALL         = "all";
    final public String SHOW_EARNINGS             = "showEarnings";
    final public String SHOW_CUMULATIVE_PROFITS   = "showCumulativeProfits";

////////////////////////////// COUPON VALUES & DIVIDENDS
    final public String ACTUAL_VALUE_PROPNAME             = "actualValue";
    final public String DIVIDEND_VALUE_PROPNAME           = "dividendValue";
    final public String COMMON_DIVIDEND_VALUE_PROPNAME    = "commonDividendValue";
    final public String DIVIDEND_PROPERTY_WORD            = "dividend";
    final public String REMAINING_DIVIDEND_PROPERTY_WORD  = "remainingDividend";
    final public String PRIVATE_DIVIDENDS_PROPERTY_WORD   = "privateDividends";
    final public String PAY_COMMON_DIVIDEND_PROPERTY_WORD = "payCommonDividend";
    final public String MAX_DIVIDEND                      = "maxDividend";

////////////////////////////// HINTS & MESSAGES
    final public String COMMON_MESSAGE_PROPNAME = "commonMessage";
    final public String HINT_PROPERTY_WORD        = "hint";
    final public String EARNINGS_HINT_PROPERTY_WORD    = "earningsHint";
    final public String END_SCORING_TEXT = "Scores have been computed.";
    final public String START_ROUND_TEXT = "A new round has started.";
    final public String END_TRADING_TEXT = "Trading is finished for this round.";
    final public String DEFAULT_ROUND_LABEL = "round";
    final public String SESSION_OVER = "No more rounds.";

////////////////////////////// CHART SCALE
    final public String MAX_TRADING_PRICE         = "maxPrice";
    final public String CHART_SCALE               = "chartScale";
    final public String MAJOR_UNIT                = "majorUnit";
    final public String MINOR_UNIT                = "minorUnit";

////////////////////////////// TRADING RESTRICTIONS
    final public String RESTRICTION               = "restriction";
    final public String SELL_ONLY                 = "sellOnly";
    final public String BUY_ONLY                  = "buyOnly";
    final public BigDecimal DefaultRange100       = new BigDecimal("100");
    final public BigDecimal DefaultRangeOne       = new BigDecimal("1");
    final public String DORMANT_PROPERTY_WORD     = "dormantRounds";

///////////////////////////// ROLES
    final public String ROLES_PROPNAME            = "roles";
    final public String TRADER_PROPERTY_WORD      = "trader";
    final public String MANIPULATOR_PROPERTY_WORD = "manipulator";
    final public String JUDGE_PROPERTY_WORD       = "judge";
    final public String ROLE_PROPERTY_WORD        = "role";
    final public String TARGET_PROPERTY_WORD      = "target";

    final public String TARGET_VALUE_PROPNAME     = "judge.target";
    final public String SCORING_FACTOR_PROPERTY_WORD   = "scoringFactor";
    final public String SCORING_CONSTANT_PROPERTY_WORD = "scoringConstant";

    final public String ABOVE_THRESHOLD_WORD      = "aboveThresholdMessage";
    final public String BELOW_THRESHOLD_WORD      = "belowThresholdMessage";
    final public String THRESHOLD_VALUE           = "thresholdValue";

///////////////////////////////// scoring labels
    final public String ACTUAL_VALUE_LABEL             = "actualValueLabel";
    final public String DEFAULT_ACTUAL_TICKET_VALUE_LABEL = "Actual Ticket Value";
    final public String ASSETS_PLUS_BONUS_LABEL        = "assetsPlusBonusLabel";
    final public String DEFAULT_ASSETS_PLUS_BONUS_LABEL  = "Total<br><font size='-2'>(assets + bonus)</font>";
    final public String AVERAGE_PRICE_LABEL            = "averagePriceLabel";
    final public String DEFAULT_AVERAGE_PRICE_LABEL      = "Average Price";
    final public String DIV_FROM_RESERVE_LABEL         = "dividendsubtractedFromReservesLabel";
    final public String DEFAULT_DIV_FROM_RESERVE_LABEL   = "Total Dividend subtracted from Reserves";
    final public String DIV_PLUS_BONUS_LABEL           = "dividendsPlusBonusLabel";
    final public String DEFAULT_DIV_PLUS_BONUS_LABEL     = "Total (Added to Cash)<br><font size='-2'>(dividends + bonus)</font>";
    final public String DIV_ADDED_TO_CASH_LABEL        = "dividendsAddedToCashLabel";
    final public String DEFAULT_DIV_ADDED_TO_CASH_LABEL  = "Total Dividend added to Cash";
    final public String PRIVATE_VALUE_LABEL            = "privateValueLabel";
    final public String DEFAULT_PRIVATE_VALUE_LABEL      = "Private Dividend Value";
    final public String PUBLIC_VALUE_LABEL             = "publicValueLabel";
    final public String DEFAULT_PUBLIC_VALUE_LABEL       = "Common Dividend Value";

    final public String COMMON_MESSAGE_LABEL      = "commonMessageLabel";
    final public String DEFAULT_COMMON_MESSAGE_LABEL = "Shared Message";
    final public String DIVIDEND_VALUE_LABEL      = "dividendValueLabel";
    final public String DEFAULT_DIV_VALUE_LABEL   = "Dividend Value";
    final public String TOTAL_DIVIDEND_LABEL      = "totalDividendLabel";
    final public String DEFAULT_TOTAL_DIVIDEND_LABEL = "Total Dividend";
    final public String MESSAGE_LABEL             = "messageLabel";
    final public String DEFAULT_MESSAGE_LABEL     = "Your clue";
    final public String ROUND_LABEL               = "roundLabel";
    final public String TOTAL_ASSETS_LABEL        = "totalAssetsLabel";
    final public String DEFAULT_TOTAL_ASSETS_LABEL = "Total Asset Value<br><font size='-2'>(cash + tickets)</font>";
    final public String MULTIPLIER_PROPERTY_WORD  = "multiplyScore";
    final public String EVENT_OUTCOME_WORD        = "eventOutcome";
    final public String SHARES_LABEL              = "sharesLabel";
    final public String DEFAULT_SHARES_LABEL      = "Shares";

///////////////////////////////// scoring labels during JUDGING

    final public String JUDGE_INVESTMENT_LABEL            = "judgeInvestmentLabel";
    final public String DEFAULT_JUDGE_INVESTMENT_LABEL       = "Investment in Black";
    final public String JUDGE_EARNINGS_LABEL              = "judgeEarningsLabel";
    final public String DEFAULT_JUDGE_EARNINGS_LABEL         = "Earnings";
    final public String MANIPULATOR_EARNINGS_LABEL        = "manipulatorEarningsLabel";
    final public String DEFAULT_MANIPULATOR_EARNINGS_LABEL   = "Earnings<br><small>(Multiplier x Average Investment)</small>";
    final public String MANIPULATOR_INVESTMENT_LABEL      = "manipulatorInvestmentLabel";
    final public String DEFAULT_MANIPULATOR_INVESTMENT_LABEL = "Average Investment";
    final public String TRADER_EARNINGS_LABEL             = "traderEarningsLabel";
    final public String DEFAULT_TRADER_EARNINGS_LABEL        = "Earnings<br><small>(Cash + Reserves + Shares x Value)</small>";
    final public String TRADER_INVESTMENT_LABEL           = "traderInvestmentLabel";
    final public String DEFAULT_TRADER_INVESTMENT_LABEL      = "Value";

    final public String JUDGE_REWARDS              = "judgeRewards";
    final public String EARLIEST_JUDGE_CUTOFF_WORD = "earliestJudgeCutoff";

    final public String LATEST_JUDGE_CUTOFF_WORD   = "latestJudgeCutoff";
    final public String SIMULTANEOUS_JUDGE_CUTOFF_WORD = "simultaneousJudgeCutoff";

    final public String JUDGE_INPUT_CHOICE_KEYWORD = "judgeInputChoices";    //  Using COMMA_SPLIT_RE
    final public String SLIDER_STEPSIZE_KEYWORD    = "judgeSliderStepsize";    // minimum slider step
    final public String SLIDER_LABEL_STEPSIZE_KEYWORD = "judgeSliderLabelStepsize";  // separation between slider step labels
    final public String ESTIMATE_LABEL_WORD        = "judgeSliderLabel";
    final public String DEFAULT_ESTIMATE_SLIDER_LABEL = "Amount to invest in Black";
    final public String FEEDBACK_LABEL_WORD        = "judgeSliderFeedbackLabel";
    final public String DEFAULT_JUDGE_SLIDER_FEEDBACK_LABEL = "Black";

////////////////////////////// MARK to MARKET

    final public String MARK_TO_MARKET_RATIO        = "markToMarket.loan.ratio";
    final public String COUPON_BASIS                = "coupon.basis";

    final public String BALANCE_LABEL               = "cashBalanceLabel";
    final public String DEFAULT_BALANCE_LABEL       = "Cash Balance";
    final public String SHARE_VALUE_LABEL           = "shareValueLabel";
    final public String DEFAULT_SHARE_VALUE_LABEL   = "Value of shares held";
    final public String TOTAL_VALUE_LABEL           = "totalValueLabel";
    final public String DEFAULT_TOTAL_VALUE_LABEL   = "Total <font size='-2'>(Cash + Shares)</font>";
    final public String LOAN_VALUE_LABEL            = "loansOutstandingLabel";
    final public String DEFAULT_LOAN_VALUE_LABEL    = "Loans Outstanding";
    final public String NET_WORTH_LABEL             = "netWorthLabel";
    final public String DEFAULT_NET_WORTH_LABEL     = "Net Worth<font size='-2'>(Cash + Shares - Loan)</font>";
    final public String CAPITAL_GAINS_LABEL         = "capitalGainsLabel";
    final public String DEFAULT_CAPITAL_GAINS_LABEL = "Gain/Loss on Shares";
    final public String NET_EARNINGS_LABEL          = "netEarningsLabel";
    final public String DEFAULT_NET_EARNINGS_LABEL  = "Net Earnings";
    final public String LAST_TRADE_PRICE_LABEL      = "lastTradePriceLabel";
    final public String DEFAULT_LAST_TRADE_PRICE_LABEL = "Last Trade Price";
    final public String LOAN_CHANGE_LABEL           = "loanChangeLabel";
    final public String DEFAULT_LOAN_CHANGE_LABEL   = "Change in Loan Amount";
    final public String LOAN_UNCHANGED_LABEL        = "loanUnchangedLabel";
    final public String DEFAULT_LOAN_REDUCED_LABEL  = "Loan Amount Reduced.<font size='-2'>(repayment deducted from cash)</font>";
    final public String LOAN_REDUCED_LABEL          = "loanReducedLabel";
    final public String DEFAULT_LOAN_UNCHANGED_LABEL = "Loan Amount Unchanged";
    final public String LOANS_DUE_WORD              = "loansDueInRound";

    final public String SUPPRESS_ACCOUNTING_DETAILS = "suppressAccountingDetails";

    ////////////////////////////// VOTING

    final public String END_VOTING_TEXT             = "Voting concluded";
    final public String CHOSEN                      = "The following outcome was chosen: ";
    final public String VOTED_FOR                   = " voted for outcome ";
    final public String DEFAULT_VOTE_PROMPT         = "Please vote for a message to be displayed:<br>";
    final public String VOTING_ALTERNATIVES         = "voteAlternatives";
    final public String VOTE_TEXT                   = "voteText";
    final public String VOTING_ROUNDS               = "voteBeforeRounds";
    final public String VOTE_PROMPT                 = "votePrompt";
}
