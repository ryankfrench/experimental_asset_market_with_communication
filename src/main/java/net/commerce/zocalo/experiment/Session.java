package net.commerce.zocalo.experiment;

import jjdm.zocalo.data.ZocaloConfig;
import jjdm.zocalo.rules.SessionData;
import jjdm.zocalo.rules.ZocaloRules;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.ajax.dispatch.MockBayeuxChannel;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;
import net.commerce.zocalo.ajax.dispatch.PrivateEventAppender;
import net.commerce.zocalo.ajax.dispatch.TradeEventDispatcher;
import net.commerce.zocalo.ajax.dispatch.TransitionAppender;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.ajax.events.TimingUpdater;
import net.commerce.zocalo.ajax.events.Transition;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.CouponBank;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.UnaryCouponBank;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.Role;
import net.commerce.zocalo.experiment.role.Trader;
import net.commerce.zocalo.experiment.role.TraderRole;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.experiment.states.NoActionStatusAdaptor;
import net.commerce.zocalo.experiment.states.NoActionTransitionAdaptor;
import net.commerce.zocalo.experiment.states.SessionState;
import net.commerce.zocalo.experiment.states.SessionStatusAdaptor;
import net.commerce.zocalo.experiment.states.StandardStateHolder;
import net.commerce.zocalo.experiment.states.StateHolder;
import net.commerce.zocalo.experiment.states.StatusAdaptor;
import net.commerce.zocalo.experiment.states.TransitionAdaptor;
import net.commerce.zocalo.html.HtmlRow;
import net.commerce.zocalo.html.HtmlTable;
import net.commerce.zocalo.logging.GID;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.BinaryMarketReserveVerifier;
import net.commerce.zocalo.market.ReserveVerifier;
import net.commerce.zocalo.market.UnaryMarket;
import net.commerce.zocalo.market.UnaryMarketReserveVerifier;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import org.antlr.stringtemplate.StringTemplate;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.mortbay.cometd.AbstractBayeux;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

import static net.commerce.zocalo.service.PropertyKeywords.*;

// Copyright 2007-2010 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/**  Manage a session of an experiment.  Handles timing, scoring, and
  initialization from config file. */
public class Session {
    final static public String END_TRADING_TRANSITION = "endTrading";
    final static public String END_SCORING_TRANSITION = "endScoring";
    final static private String START_ROUND_TRANSITION = "startRound";
    final static private String START_NEXT_ROUND_ACTION = "Start next round";
    final static private String STOP_ROUND_ACTION = "Stop round";

    static private TimerTask task;
    private CashBank rootBank;
    private int currentRound = 0;
    protected Properties props;
    private Map<String, AbstractSubject> players = new HashMap<String, AbstractSubject>();
    private Map<String, Role> roles;
    private BinaryMarket market;
    private BinaryClaim claim;
    private CouponBank mint;
    private AbstractBayeux bayeux;
    private long roundFinishTime;
    private FileAppender fileAppender;
    private String lastLogFileName = "";
    private String errorMessage = "";
    private Price averagePricePrevFullRound = null;
    private boolean privateValuesInUse;
    private StateHolder phase;
    public Quantity minorUnit;
    public Quantity majorUnit;
    public Quantity chartScale;

    /**
     * JJDM 20150927: Removed the creation of this - was interferring with tests.  If needed in the future, we need
     * to keep track of each trade as they occur separately from a dispatcher/appender.  Looks like it it used to
     * calculate average trades.  We could mimic this behavior by storing all messages sent via web socket, but
     * probably better to explicitly store trades in a "database" (or in memory).
     */
    private MockBayeuxChannel tradeHistory;

	private ZocaloConfig zocaloConfig;
	private ZocaloRules zocaloRules;

    static public Logger sessionLogger() {
        return Logger.getLogger(Session.class);
    }

    static protected void logEvent(String eventName) {
        Logger sessionLogger = Logger.getLogger(Session.class);
        sessionLogger.info(GID.log() + eventName);
    }

    public Session(Properties props, AbstractBayeux bayeux) {
        this.props = props;
        this.bayeux = bayeux;
        fileAppender = Log4JHelper.getInstance().openLogFile(getSessionTitle(props));
        roles = initializeRoles(props);
        initializeMarket();
        initializePlayers();
        logSessionInitialization();
        initializeTransitionAppender();
        initializeState();
    }

    // Test only
    public Session(Properties props, String logFile, AbstractBayeux bayeux) {
        this.props = props;
        this.bayeux = bayeux;
        fileAppender = Log4JHelper.getInstance().openLogFile(logFile);
        roles = initializeRoles(props);
        initializeMarket();
        initializePlayers();
        logSessionInitialization();
        initializeTransitionAppender();
        initializeState();
    }

    public static Session make(Properties props, AbstractBayeux bayeux, String logFile) {
        if (VotingSession.describesVotingSession(props)) {
            return new VotingSession(props, bayeux);
        } else if (LendingSession.describesM2MSession(props)) {
            return new LendingSession(props, bayeux);
        } else if (JudgingSession.describesJudgingSession(props)) {
            return new JudgingSession(props, bayeux);
        } else {
            return new Session(props, bayeux);
        }
    }

    protected HashMap<String, Role> initializeRoles(Properties props) {
        String rolesProperty = props.getProperty(ROLES_PROPNAME);
        if (rolesProperty == null || rolesProperty.length() == 0) {
            return initializeRoles(new String[] { "trader" }, props);
        } else {
            return initializeRoles(rolesProperty.split(COMMA_SPLIT_RE), props);
        }
    }

    private HashMap<String, Role> initializeRoles(String[] rolesProperty, Properties props) {
        HashMap<String, Role> rolesMap = new HashMap<String, Role>();

        for (int i = 0; i < rolesProperty.length; i++) {
            String role = rolesProperty[i];
            Role subjectType = rolesMap.get(role);

            if (subjectType == null) {
                subjectType = basicRoleForSession(role);
                subjectType.initializeFromProps(props);
                rolesMap.put(role, subjectType);
                String roleDormancy = props.getProperty(PropertyHelper.dottedWords(role, DORMANT_PROPERTY_WORD));
                subjectType.initializeDormancy(buildDormantArray(rounds(), roleDormancy));
            }
        }

        return rolesMap;
    }

    protected TraderRole basicRoleForSession(String role) {
        return new TraderRole(role);
    }

    void initializeState() {
        final Session s = this;
        SessionStatusAdaptor ad = sessionEndTradingAdaptor(s);
        setPhase(new StandardStateHolder(ad));
    }

    protected void setPhase(StateHolder newPhase) {
        phase = newPhase;
    }

    protected SessionStatusAdaptor sessionEndTradingAdaptor(final Session s) {
        return new SessionStatusAdaptor() {
            public void endTradingEvents() {
                errorMessage = "";
                market.close();
                otherEndTradingEvents();
                s.logTransitionEvent(END_TRADING_TRANSITION, END_TRADING_TEXT);
            }

            public void calculateScores() throws ScoreException { s.calculateScores(); }
            public void setErrorMessage(String warning) { errorMessage = warning; }
        };
    }

    void otherEndTradingEvents() {
        // No body here.  For subclasses to override.
    }

    protected void logSessionInitialization() {
        Logger log = PropertyHelper.configLogger();
        log.info(GID.log() + SESSION_TITLE_PROPNAME + ": " + getSessionTitle(props));
        log.info(GID.log() + ROUNDS_PROPNAME + ": " + rounds());
        log.info(GID.log() + TIME_LIMIT_PROPNAME + ": " + timeLimit() + " seconds.");
        logCommonMessages(log);
        logActualValues(log);
        logPrivateValues(log);
        logEndowments(log);
        logRestrictions(log);
        logPlayerValues(log);
        logGeneralSettings(log);
    }

    private void logRestrictions(Logger log) {
        Set<String> roleNames = roles.keySet();
        for (Iterator<String> strings = roleNames.iterator(); strings.hasNext();) {
            String roleName = strings.next();
            Role role = roles.get(roleName);
            boolean canBuy = role.canBuy();
            boolean canSell = role.canSell();
            if (canBuy && canSell) {
                log.info(GID.log() + "Role '" + roleName + "' is unrestricted.");
            } else if (canBuy) {
                log.info(GID.log() + "Role '" + roleName + "' can only Buy.");
            } else if (canSell) {
                log.info(GID.log() + "Role '" + roleName + "' can only Sell.");
            } else {
                log.info(GID.log() + "Role '" + roleName + "' cannot trade.");
            }
            logDormancy(log, "role", roleName, role.dormantPeriods());
        }
    }

    private void logDormancy(Logger log, String label, String name, boolean[] dormantPeriods) {
        if (dormantPeriods == null) {
            return;
        }

        StringBuffer buf = new StringBuffer();
        boolean insertedAlready = false;
        for (int i = 0; i < dormantPeriods.length; i++) {
            boolean period = dormantPeriods[i];
            if (period) {
                if (insertedAlready) {
                    buf.append(", ");
                }
                buf.append(i);
                insertedAlready = true;
            }
        }
        String roundList = buf.toString();
        if (roundList.length() > 0) {
            log.info(GID.log() + label + " " + name + " is dormant in rounds " + roundList);
        }
    }

    private void logGeneralSettings(Logger log) {
        logValue(log, UNARY_ASSETS, PropertyHelper.getUnaryAssets(props));
        logValue(log, BETTER_PRICE_REQUIRED, PropertyHelper.getBetterPriceRequired(props));
        logValue(log, CARRY_FORWARD, isCarryForward());
        logValue(log, SHOW_EARNINGS, getShowEarnings());
        logValue(log, WHOLE_SHARE_TRADING_ONLY, PropertyHelper.getWholeShareTradingOnly(props));
        logValue(log, MAX_TRADING_PRICE, PropertyHelper.getMaxTradingPrice(props));
        Quantity scale = getChartScale();
        if (scale != null) {
            logValue(log, CHART_SCALE, scale);
        }
        logValue(log, REQUIRE_RESERVES, PropertyHelper.getRequireReserves(props));
        Quantity majorUnit = getMajorUnit();
        if (majorUnit != null) {
            logValue(log, MAJOR_UNIT, majorUnit);
        }
        Quantity minorUnit = getMinorUnit();
        if (minorUnit != null) {
            logValue(log, MINOR_UNIT, minorUnit);
        }
    }

    private void logValue(Logger log, String label, boolean value) {
        log.info(GID.log() + label + ": " + value);
    }

    private void logValue(Logger log, String label, Quantity value) {
        log.info(GID.log() + label + ": " + value);
    }

    private void logPlayerValues(Logger log) {
        int rounds = rounds();
        String message = null;
        for (Iterator<AbstractSubject> iterator = players.values().iterator(); iterator.hasNext();) {
            AbstractSubject subject = iterator.next();
            message = subject.logConfigValues(log, props, rounds);
            if (message != null) {
                appendToErrorMessage(message);
            }
            logDormancy(log, "player", subject.getName(), subject.dormantPeriods());
        }
    }

    protected void appendToErrorMessage(String message) {
        if  (errorMessage.length() == 0) {
            errorMessage = message;
        } else {
            errorMessage = errorMessage + "\n" + message;
        }
    }

    private void logEndowments(Logger log) {
        String[] endowments = new String[] { ENDOWMENT_PROPERTY_WORD, TICKETS_PROPERTY_WORD };
        String[] endowedRoles = endowedRoles();
        logParameterCombinations(endowments, endowedRoles, log, false);
    }

    protected String[] endowedRoles() {
        String[] endowedRoles;
        endowedRoles = new String[roles.size()];
        int i = 0;
        for (Iterator<String> iterator = roles.keySet().iterator(); iterator.hasNext();) {
            String roleName = iterator.next();
            Role role = roles.get(roleName);
            if (role.needsUser()) {
                endowedRoles[i++] = (String)roleName;
            }
        }
        return endowedRoles;
    }

    private void logPrivateValues(Logger log) {
        String propertySuffixes[] = new String[] { PAY_COMMON_DIVIDEND_PROPERTY_WORD, PRIVATE_DIVIDENDS_PROPERTY_WORD };
        String[] playerNames = players.keySet().toArray(new String[players.size()]);

        privateValuesInUse = checkPrivateValuesInUse(playerNames, propertySuffixes);
        if (privateValuesInUse) {
            logParameterCombinations(playerNames, propertySuffixes, log, true);
        }
    }

    private boolean checkPrivateValuesInUse(String[] names, String[] suffixes) {
        for (int i = 0; i < names.length; i++) {
            String namePart = names[i];
            for (int j = 0; j < suffixes.length; j++) {
                String privateValueParameter = PropertyHelper.dottedWords(namePart, suffixes[j]);
                String value = props.getProperty(privateValueParameter);
                if (null != value && value.trim().length() != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void logParameterCombinations(String[] first, String[] second, Logger log, boolean checkByRound) {
        for (int i = 0; i < first.length; i++) {
            String firstPart = first[i];
            for (int j = 0; j < second.length; j++) {
                if (second[j] == null) continue;
                String parameter = PropertyHelper.dottedWords(firstPart, second[j]);
                String value = props.getProperty(parameter);
                log.info(GID.log() + parameter + ": " + value);
                if (null == value || value.trim().length() == 0) {
                    String nullWarning = "'" + parameter + "' is null.";
                    appendToErrorMessage(nullWarning);
                    log.warn(GID.log() + nullWarning);
                } else if (j == 1 && checkByRound && value.split(COMMA_SPLIT_RE).length < rounds()) {
                    String shortWarning = "'" + parameter + "' has less than " + rounds() + " entries.";
                    appendToErrorMessage(shortWarning);
                    log.warn(GID.log() + shortWarning);
                }
            }
        }
    }

    protected void logCommonMessages(Logger log) {
        if (thresholdMessagesDefined()) {
            log.info(GID.log() + BELOW_THRESHOLD_WORD + ": " + belowThresholdMessage());
            log.info(GID.log() + ABOVE_THRESHOLD_WORD + ": " + aboveThresholdMessage());
            log.info(GID.log() + THRESHOLD_VALUE + ": " + props.getProperty(THRESHOLD_VALUE));
        } else {
            for (int round = 0 ; round < rounds() ; round++) {
                log.info(GID.log() + COMMON_MESSAGE_PROPNAME + "." + (round + 1) + ": " + message(round));
            }
        }
    }

    private void logActualValues(Logger log) {
        String dividendValue = props.getProperty(DIVIDEND_VALUE_PROPNAME);
        String actualValue = props.getProperty(ACTUAL_VALUE_PROPNAME);
        if ((null != dividendValue) && ! "".equals(dividendValue)) {
            log.info(GID.log() + DIVIDEND_VALUE_PROPNAME + ": " + simplifyList(DIVIDEND_VALUE_PROPNAME).toString());
        } else if ((null != actualValue) && ! "".equals(actualValue)) {
            log.info(GID.log() + ACTUAL_VALUE_PROPNAME + ": " + simplifyList(ACTUAL_VALUE_PROPNAME).toString());
        } else {
            String newMsg = "Neither " + ACTUAL_VALUE_PROPNAME + " nor " + DIVIDEND_VALUE_PROPNAME +
                    " property was found in configuration file.  Zero will be used for the value.";
            appendToErrorMessage(newMsg);
            log.warn(GID.log() + errorMessage);
        }
    }

    private StringBuffer simplifyList(String propname) {
        StringBuffer buff = new StringBuffer();
        for (int round = 1 ; round <= rounds() ; round++) {
            buff.append(PropertyHelper.indirectPropertyForRound(propname, round, props));
            if (round < rounds()) {
                buff.append(", ");
            }
        }
        return buff;
    }

//////  INITIALIZE SESSION ///////////////////////////////////////////////////////////////

    private void initializeTransitionAppender() {
        TransitionAppender.registerNewAppender(bayeux);
    }

    private void initializeMarket() {
        rootBank = new CashBank("cash");
        String title = getSessionTitle(props);
        SecureUser owner = new SecureUser("marketOwner", rootBank.makeFunds(Quantity.Q100), "", "");
        claim = BinaryClaim.makeClaim(title, owner, "default description");
        Quantity bookFunding = bookFundingRequired(props);
        Funds initialBookFunds = rootBank.makeFunds(bookFunding);
        ReserveVerifier verifier = null;
        if (PropertyHelper.getUnaryAssets(props)) {
            if (PropertyHelper.getRequireReserves(props)) {
                verifier = new UnaryMarketReserveVerifier(this);
            }
            mint = UnaryCouponBank.makeUnaryBank(claim, initialBookFunds);
            market = UnaryMarket.make(claim, mint, owner, props, verifier);
        } else {
            if (PropertyHelper.getRequireReserves(props)) {
                verifier = new BinaryMarketReserveVerifier(this);
            }
            mint = CouponBank.makeBank(claim, initialBookFunds);
            market = BinaryMarket.make(claim, mint, owner, props, verifier, 2);
        }

        market.close();

        TimingUpdater updater = new TimingUpdater() {
            public void addTimingInfo(Map e) {
                e.put("round", Integer.toString(getCurrentRound()));
                e.put("timeRemaining", timeRemaining());
            }
        };
        PriceActionAppender.registerNewAppender(bayeux, market.getBook(), updater);
        PrivateEventAppender.registerNewAppender(bayeux);

        ZocaloService service = ZocaloService.startNewSession(this.props);
        this.zocaloConfig = service.getConfiguration();
	    this.zocaloRules = new ZocaloRules(zocaloConfig);

    }

    public TradingSubject getTrader(User user) {
        AbstractSubject subject = getPlayer(user.getName());
        if (!(subject instanceof TradingSubject)) {
            return null;
        }
        return (TradingSubject) subject;
    }

    /* It would be better to retrieve the coupons and currency from the players
    and re-issue them, in order to be sure that money isn't being lost.  For now,
    we'll just start the bank with enough funds to buy back all the issued coupons. */
    protected Quantity bookFundingRequired(Properties props) {
        String playersProperty = props.getProperty(PLAYERS_PROPNAME);
        int playerCount = playersProperty.split(COMMA_SPLIT_RE).length;
        return initialTraderTickets().times(new Quantity(playerCount * rounds()));
    }

    private void initializePlayers() {
        String initialHint = props.getProperty("initialHint");
        if (initialHint == null) {
            initialHint = "Trading has not yet begun.";
        } else {
            initialHint = initialHint.trim();
        }
        String playersProperty = props.getProperty(PLAYERS_PROPNAME);
        String[] playerNames = playersProperty.split(COMMA_SPLIT_RE);
        for (int i = 0; i < playerNames.length; i++) {
            initializePlayer(playerNames[i], initialHint);
        }
    }

    private void initializePlayer(String playerName, String initialHint) {
        AbstractSubject subject;
        int rounds = rounds();
        String roleName = props.getProperty(PropertyHelper.dottedWords(playerName, ROLE_PROPERTY_WORD));
        Role role = roles.get(roleName);
        if (role == null) {
            Logger logger = PropertyHelper.configLogger();
            String roleWarning = "player '" + playerName + "' has no assigned Role.";
            logger.error(GID.log() + roleWarning);
            appendToErrorMessage(roleWarning);
            return;
        }

        if (role.needsUser()) {
            Quantity initialCash = role.getInitialCash();
            User user = rootBank.makeEndowedUser(playerName, initialCash);
            subject = role.createSubject(user, rounds, playerName);
        } else {
            subject = role.createSubject(null, rounds, playerName);
        }

        String playerDormancy = props.getProperty(PropertyHelper.dottedWords(playerName, DORMANT_PROPERTY_WORD));
        subject.initializeDormancy(buildDormantArray(rounds, playerDormancy));
        subject.setHint(initialHint);
        players.put(playerName, subject);
    }

    static public boolean[] buildDormantArray(int rounds, String playerDormancy) {
        boolean[] dormant = new boolean[rounds + 1];
        Arrays.fill(dormant, false);
        if (playerDormancy != null) {
            String[] dormantRounds = playerDormancy.split(COMMA_SPLIT_RE);
            for (int i = 0; i < dormantRounds.length; i++) {
                int dormantRound = Integer.parseInt(dormantRounds[i]);
                dormant[dormantRound] = true;
            }
        }
        return dormant;
    }

    private void initializePlayerAccounts() {
        SessionData data = this.inspect();
        this.zocaloRules.resetCoupons(data);
        this.zocaloRules.resetCash(data);
        players.values().forEach(player -> player.resetOutstandingOrders());
    }

    //////  STATE TRANSITIONS ///////////////////////////////////////////////////////////////

    protected void logTransitionEvent(String transitionLabel, String displayText) {
        new Transition(transitionLabel, replaceRoundString(displayText), currentRound, timeRemaining());
    }

    // testing only
    public void startSession() {
        currentRound = 0;
        startNextTimedRound();
    }

    // testing only
    public void startSession(long duration) {
        currentRound = 0;
        startNextRound(duration);
    }

    public void startNextRound() {
        startNextRound(0);
    }

    public void startNextTimedRound() {
        startNextRound(timeLimit() * 1000);
    }

    public void startNextRound(final long duration) {
        TransitionAdaptor start = new NoActionTransitionAdaptor() {
            public void startRound() {
                if (lastRound()) {
                    errorMessage = SESSION_OVER;
                    return;
                }

                if (duration != 0) {
                    setUpEndTimer(duration);
                }
                startRoundActions();
            }
            public void warn(String warning) { errorMessage = warning; }
        };

        phase.startNextRound(start);
    }

    private void setUpEndTimer(long duration) {
        cancelTask();
        final int roundAtStart = currentRound;
        task = new TimerTask() {
            public void run() {
                if (currentRound == roundAtStart + 1) {
                    NoActionStatusAdaptor endTradingAdaptor = new NoActionStatusAdaptor() {
                        public void trading() { endTrading(false); }
                    };
                    phase.informTrading(endTradingAdaptor);
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(task, duration);
        roundFinishTime = new Date().getTime() + duration;
    }

    private void startRoundActions() {
        errorMessage = "";
        currentRound++;
        ZocaloService zocaloService = ZocaloService.getInstance();
        zocaloService.stopParticipation(); // JJDM Need to do this for last round (right when current round changes).
        logEvent(START_NEXT_ROUND_ACTION);
        initializePlayerAccounts();
        resetScoring();
        market.resetBook();
        otherStartActions();
        market.open();
        zocaloService.startRound(this.getCurrentRound()); // JJDM Depends on items before this
        setHintsForRound();
        logTransitionEvent(START_ROUND_TRANSITION, START_ROUND_TEXT);
    }

    void otherStartActions() {
        // override if needed
    }

    private void resetScoring() {
        Iterator<String> iter = playerNameIterator();
        while (iter.hasNext()) {
            AbstractSubject subject = players.get(iter.next());
            subject.resetScoreInfo();
        }
    }

    public void endTrading(boolean manual) {
        phase.endTrading(phase.endTradingAdaptor(manual));
        ZocaloService.getInstance().endRound();
    }

    public void endSession() {
        cancelTask();
        saveLogFileLinkableName();
        closeSessionAppenders();
		currentRound = 0; // JJDM reset the current round - errors on new property upload without
    }

    private void cancelTask() {
        if (task != null) {
// TODO JJDM - 20150913 Was getting double logging of dividends.  Not sure why this is required here.
//            endTrading(false);
            task.cancel();
        }
    }

    public void closeSessionAppenders() {
        Logger.getRootLogger().removeAppender(fileAppender);
        if (null != fileAppender) {
            fileAppender.close();
        }
        PriceAction.getActionLogger().removeAllAppenders();
        Transition.getActionLogger().removeAllAppenders();
    }

    private void setHintsForRound() {
        for (Iterator iterator = playerNameIterator(); iterator.hasNext();) {
            String playerName = (String) iterator.next();
            AbstractSubject subject = players.get(playerName);
            subject.setHintsForRound(currentRound, this);
        }
    }

    private Price eventTradePrice(Map event) {
        Price price;
        Object o = event.get("traded");
        if (o == null) {
            price = market.asPrice(Quantity.ZERO);
        } else {
            price = market.asPrice(new BigDecimal(String.valueOf(o)));
        }
        return price;
    }

    protected Quantity lastTradeValue() {
        List events = tradeHistory.getEvents(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        int lastIndex = events.size() - 1;
        if (lastIndex < 0) {
            return Quantity.ZERO;
        }
        Map event = (Map) events.get(lastIndex);
        return eventTradePrice(event);
    }

    protected void calculateScores() throws ScoreException {
        if (displayAveragePrices() || thresholdMessagesDefined()) {
            averagePricePrevFullRound = calculateAveragePrice();
        } else {
            averagePricePrevFullRound = market.asPrice(Quantity.ZERO);
        }
        calculateScores(averagePricePrevFullRound);
    }

    private boolean displayAveragePrices() {
        String displayAverages = props.getProperty(DISPLAY_AVERAGES);
        if (displayAverages == null || "".equals(displayAverages) || "false".equalsIgnoreCase(displayAverages.trim())) {
            return false;
        } else if ("true".equalsIgnoreCase(displayAverages.trim())) {
            return true;
        }
        return false;
    }

    private Price calculateAveragePrice() {
        Price zeroPrice = market.asPrice(Quantity.ZERO);
        List events = tradeHistory.getEvents(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX);
        Quantity sum = Quantity.ZERO;
        double count = 0;
        for (Iterator iterator = events.iterator(); iterator.hasNext();) {
            Map event = (Map) iterator.next();
            Price price = eventTradePrice(event);
            if (! price.isZero() && (eventRound(event) == currentRound)) {
                sum = sum.plus(price);
                count ++;
            }
        }
        return count == 0
                ? zeroPrice
                : zeroPrice.newValue(sum.div(new Quantity(count)));
    }

    private int eventRound(Map event) {
        int round = 0;
        Object o = event.get("round");
        if (null == o) {
            round = 0;
        } else {
            round = Integer.parseInt(String.valueOf(o));
        }
        return round;
    }

    // override to fill in JUDGED argument
    protected void calculateScores(Price average) throws ScoreException {
        accrueDividendsAndBonuses(average, Quantity.ZERO, Quantity.ZERO);
    }

	protected void accrueDividendsAndBonuses(Price average, Quantity judged, Quantity judgingTarget) throws ScoreException {
		SessionData data = this.inspect();
		String outcome = PropertyHelper.indirectPropertyForRound(EVENT_OUTCOME_WORD, this.currentRound, this.props);
		zocaloRules.scoreRound(data, average, judged, judgingTarget, outcome);
	}

/*

TODO JJDM Remove this after testing calculate scores.

    protected void accrueDividendsAndBonuses(Price average, Quantity judged, Quantity judgingTarget) throws ScoreException {
        String outcome = eventOutcome(props);
        for (Iterator playerNames = playerNameIterator(); playerNames.hasNext();) {
            AbstractSubject player = getPlayer((String)playerNames.next());
            if (player.isDormant(currentRound)) {
                player.recordDormantInfo(judged, outcome, props);
                continue;
            }
            Quantity multiplier = getMultiplier(player);
            Quantity total = player.totalDividend(claim, this, currentRound);

            boolean keepingScore = lastRound() || ! isCarryForward();
            if (keepingScore) {
                player.rememberHoldings(claim);
                player.rememberAssets(claim);
                player.recordBonusInfo(average, judged, judgingTarget, total, currentRound, props);
                player.recordScore(currentRound, multiplier, props);
            } else { // Carry Forward assets and cash; add dividends to balance
                player.rememberAssets(claim);
                Quantity bonus = player.recordBonusInfo(average, judged, judgingTarget, total, currentRound, props);
                settleBonusAndDividendAsCash(player, total, bonus.times(multiplier));
            }
            recordScoreExplanation(player, keepingScore, outcome);
        }
    }

    protected String eventOutcome(Properties props) {
        return PropertyHelper.indirectPropertyForRound(EVENT_OUTCOME_WORD, currentRound, props);
    }

    protected void recordScoreExplanation(AbstractSubject player, boolean keepingScore, String outcome) {
        player.recordExplanation(props, keepingScore, isCarryForward(), outcome);
    }

    private void settleBonusAndDividendAsCash(AbstractSubject player, Quantity dividend, Quantity bonus) throws ScoreException {
        if (bonus.isPositive()) {
            player.addBonus(rootBank.makeFunds(bonus));
        }
        if (dividend.isNegative()) {
            Quantity coupons = player.currentCouponCount(claim);
            Funds returned = player.payDividend((dividend.div(coupons)), coupons.negate(), claim.getNoPosition());
            if (returned != null) {
                Funds sequestered = returned.provide(dividend);// remove it from the user's purse
                if (sequestered.getBalance().compareTo(dividend) < 0) {
                    logEvent("Player " + player.getName() + " didn't pay a required dividend in round " + currentRound);
                }
            }
        } else {
            player.receiveDividend(rootBank.makeFunds(dividend));
        }
        Quantity perShare = getRemainingDividend(player, currentRound + 1);
        player.reduceReservesTo(perShare, claim.getNoPosition());
    }
*/

    static private String getSessionTitle(Properties props) {
        return props.getProperty(SESSION_TITLE_PROPNAME);
    }

//////  PROPERTY ACCESS ///////////////////////////////////////////////////////////////
    public int rounds() {
        return Integer.parseInt(props.getProperty(ROUNDS_PROPNAME));
    }

	public String getPriceHint(String playerName, int round) {
		if (this.zocaloConfig.isClueDistributionDirect()) {
			String propName = PropertyHelper.dottedWords(playerName, HINT_PROPERTY_WORD);
			return PropertyHelper.indirectPropertyForRound(propName, round, props);
		} else {
			List<String> clues = ZocaloService.getInstance().getAutomaticCluesForRound(round).get(playerName);
			if (clues == null || clues.size() == 0) {
				return null;
			}
			if (clues.size() == 1) {
				return this.zocaloConfig.getHintMap().get(clues.get(0));
			} else {
				StringBuffer buffer = new StringBuffer();
				buffer.append("<ul id='clueList'>");
				for (String clueId : clues) {
					String fullClue = this.zocaloConfig.getHintMap().get(clueId);
					buffer.append("<li>" + fullClue + "</li>");
				}
				buffer.append("</ul>");
				return buffer.toString();
			}
		}
	}

    public Quantity getMultiplier(AbstractSubject player) {
        String playerProp = PropertyHelper.dottedWords(PropertyHelper.dottedWords(MULTIPLIER_PROPERTY_WORD, player.getName()), "" + currentRound);
        Quantity playerVal = PropertyHelper.parseDoubleNoWarn(playerProp, props);
        if (! playerVal.isZero()) {
            return playerVal;
        }
        String roleProp = PropertyHelper.dottedWords(PropertyHelper.dottedWords(MULTIPLIER_PROPERTY_WORD, player.roleName()), "" + currentRound);
        Quantity roleVal = PropertyHelper.parseDoubleNoWarn(roleProp, props);
        if (! roleVal.isZero()) {
            return roleVal;
        }
        return Quantity.ONE;
    }

    public Quantity getDividend(int round) throws ScoreException {
        String dividendValue = PropertyHelper.indirectPropertyForRound(DIVIDEND_VALUE_PROPNAME, round, props);
        String actualValue = PropertyHelper.indirectPropertyForRound(ACTUAL_VALUE_PROPNAME, round, props);
        String commonDividendValue = PropertyHelper.indirectPropertyForRound(COMMON_DIVIDEND_VALUE_PROPNAME, round, props);

        try {
            if (! "".equals(dividendValue)) {
                if (! "".equals(commonDividendValue)) {
                    appendToErrorMessage("Both " + DIVIDEND_VALUE_PROPNAME + " and " + COMMON_DIVIDEND_VALUE_PROPNAME + " were specified, using " + DIVIDEND_VALUE_PROPNAME);
                }
                if (! "".equals(actualValue)) {
                    appendToErrorMessage("Both " + DIVIDEND_VALUE_PROPNAME + " and " + ACTUAL_VALUE_PROPNAME + " were specified, using " + DIVIDEND_VALUE_PROPNAME);
                }
                return new Quantity(dividendValue);
            }

            if (! "".equals(actualValue)) {
                if (! "".equals(commonDividendValue)) {
                    appendToErrorMessage("Both " + COMMON_DIVIDEND_VALUE_PROPNAME + " and " + ACTUAL_VALUE_PROPNAME + " were specified, using " + ACTUAL_VALUE_LABEL);
                }
                return new Quantity(actualValue);
            }

            if (! "".equals(commonDividendValue)) {
                return new Quantity(commonDividendValue);
            }
        } catch (NumberFormatException e) {
            throw new ScoreException(e);
        }
        return Quantity.ZERO;
    }

    public Quantity getDividend(AbstractSubject subject, int round) throws ScoreException {
        boolean payDividend = getPayDividend(subject, round);
        Quantity privateDividend = getPrivateDividend(subject, round);
        if (payDividend) {
            Quantity commonDividend = getDividend(round);
            return privateDividend.plus(commonDividend);
        } else {
            return privateDividend;
        }
    }

    public Quantity getPrivateDividend(AbstractSubject subject, int round) {
        String propName = PropertyHelper.dottedWords(subject.getName(), PRIVATE_DIVIDENDS_PROPERTY_WORD);
        String privateDividendString = PropertyHelper.indirectPropertyForRound(propName, round, props);
        if ("".equals(privateDividendString)) {
            return Quantity.ZERO;
        }
        return new Quantity(privateDividendString);
    }

    // payCommonDividend can have 0, 1, or exactly as many entries as there are rounds.
    // If it's missing (A), we always pay common dividends.  If there's only one entry (B),
    // that value covers all rounds.  Otherwise (C), each round must be specified.
    public boolean getPayDividend(AbstractSubject subject, int round) {
        Pattern yesPattern = Pattern.compile(YES_TRUE_RE, Pattern.DOTALL);
        String propName = PropertyHelper.dottedWords(subject.getName(), PAY_COMMON_DIVIDEND_PROPERTY_WORD);
        String round1String = PropertyHelper.indirectPropertyForRound(propName, 1, props);
        String round2String = PropertyHelper.indirectPropertyForRound(propName, 2, props);
        String roundNString = PropertyHelper.indirectPropertyForRound(propName, round, props);
        if ("".equals(round1String)) { // case A
            return true;
        } else if ("".equals(round2String)) {  // case B
            return yesPattern.matcher(round1String).matches();
        } else {                        // case C
            return yesPattern.matcher(roundNString).matches();
        }
    }

    public int getShareLimit(TradingSubject trader) {
        String limitPropString = props.getProperty(PropertyHelper.dottedWords(trader.getName(), SHARE_LIMIT_PROPERTY_WORD));
        if (limitPropString == null || limitPropString.length() == 0) {
            return 0;
        }
        return Integer.parseInt(limitPropString);
    }

    public String getEarningsHint(String name, int round) {
        String propName = PropertyHelper.dottedWords(name, EARNINGS_HINT_PROPERTY_WORD);
        return PropertyHelper.indirectPropertyForRound(propName, round, props);
    }

    public String message(int round) {
        if (thresholdMessagesDefined()) {
            return thresholdMessage(round);
        }
        return PropertyHelper.indirectPropertyForRound(COMMON_MESSAGE_PROPNAME, round, props);
    }

    private boolean thresholdMessagesDefined() {
        return props.containsKey(THRESHOLD_VALUE)
                && aboveThresholdMessage().length() != 0 && belowThresholdMessage().length() != 0;
    }

    private Quantity thresholdValue(int round) throws ScoreException {
        String label = props.getProperty(THRESHOLD_VALUE);
        if (label == null) {
            return null;
        }
        if (label.equals(DIVIDEND_PROPERTY_WORD)) {
            return getDividend(round);
        } else if (label.equals(REMAINING_DIVIDEND_PROPERTY_WORD)) {
            Quantity remainingDividend = Quantity.ZERO;
            for (int i = round ; i <= rounds() ; i++) {
                remainingDividend = remainingDividend.plus(getDividend(i));
            }
            return remainingDividend;
        } else {
            String value = PropertyHelper.indirectPropertyForRound(THRESHOLD_VALUE, round, props);
            try {
                return new Quantity(value) ;
            } catch (NumberFormatException e) {
                throw new ScoreException(e);
            }
        }
    }

    private String thresholdMessage(final int round) {
        if (round < 2) {
            return "";
        }

        final StringBuffer buff = new StringBuffer();
        NoActionStatusAdaptor ad = new NoActionStatusAdaptor() {
            public void trading() {
                composeThresholdMessage(buff, round);
            }
        };
        phase.informTrading(ad);
        return buff.toString();
    }

    private void composeThresholdMessage(StringBuffer buff, int round) {
        Quantity threshold = Quantity.ZERO;
        try {
            threshold = thresholdValue(round - 1);
        } catch (ScoreException e) {
            e.printStackTrace();
            buff.append("unable to calculate value; problem parsing configuration values.");
            return;
        }
        if (threshold.compareTo(averagePricePrevFullRound) == 0) {
            return;
        }
        String thresholdMsg =
                threshold.compareTo(averagePricePrevFullRound) >= 0
                        ? belowThresholdMessage()
                        : aboveThresholdMessage();

        StringTemplate template = new StringTemplate(thresholdMsg);
        Quantity difference = threshold.minus(averagePricePrevFullRound).abs().roundFloor();
        template.setAttribute("difference", difference.printAsIntegerQuantity());

        String percent;
        if (threshold.isZero() || difference.isZero()) {
            percent = "100";
        } else {
            Quantity rawPercent = Quantity.Q100.times(difference.div(threshold));
            percent = scaleToInteger(rawPercent).toString();
        }
        template.setAttribute("percent", percent);
        buff.append(template.toString());
    }

    private Quantity scaleToInteger(Quantity value) {
        return value.newScale(0);
    }

    public String message() {
        if (null == props) {
            return "";
        }
        return message(currentRound);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int timeLimit() {
        String timeString = props.getProperty(TIME_LIMIT_PROPNAME);
        return PropertyHelper.parseTimeStringAsSeconds(timeString);
    }

    public Quantity initialTraderTickets() {
        Quantity maxTix = Quantity.ZERO;
        for (Iterator iterator = roles.values().iterator(); iterator.hasNext();) {
            Role role = (Role) iterator.next();
            maxTix = maxTix.max(role.getInitialCoupons());
        }
        return maxTix;
    }

/////// ACCESSING PLAYERS  //////////////////////////////

    public AbstractSubject getPlayer(String name) {
        return players.get(name);
    }

    public User getUserOrNull(String userName) {
        AbstractSubject player = getPlayer(userName);
        if (player instanceof TradingSubject) {
            return ((TradingSubject)player).getUser();
        } else {
            return null;
        }
    }

    public Iterator<String> playerNameIterator() {
        return players.keySet().iterator();
    }

    public Iterator playerNameSortedIterator() {
        SortedSet<String> sortedNames = new TreeSet<String>();
        sortedNames.addAll(players.keySet());
        return sortedNames.iterator();
    }

    protected Collection<User> getTraders() {
        Collection<User> traders = new HashSet<User>();
        Iterator<String> it = playerNameIterator();
        while (it.hasNext()) {
            String name = it.next();
            User trader = getUserOrNull(name);
            if (trader != null) {
                traders.add(trader);
            }
        }
        return traders;
    }

////// ACCESSING SESSION STATE /////////////////////////////////////////////////////
    public int getCurrentRound() {
        return currentRound;
    }

    public boolean marketIsActive() {
        return market.isOpen();
    }

    public BinaryClaim getClaim() {
        return market.getBinaryClaim();
    }

    public BinaryMarket getMarket() {
        return market;
    }

    //////  PRINTING ///////////////////////////////////////////////////////////////

    public void logFileLinks(final StringBuffer buff, final PrintStringAdaptor printer) {
        if (fileAppender == null) {
            buff.append("no active log.");
        }
        final String linkableName = saveLogFileLinkableName();

        StatusAdaptor ad = new NoActionStatusAdaptor() {
            public void showingScores() {
                printer.printString(buff, linkableName);
            }
        };

        if (phase == null) {
            return;
        }
        if (lastRound()) {
            phase.informShowingScores(ad);
        }

        if (buff.length() == 0) {
            buff.append("Logging to ").append(linkableName);
        }
    }

    public boolean lastRound() {
        return currentRound == rounds();
    }

    private String saveLogFileLinkableName() {
        if (null == fileAppender) {
            return lastLogFileName;
        }
        String fullLogFileName = fileAppender.getFile();
        if (null == fullLogFileName || "".equals(fullLogFileName)) {
            return lastLogFileName;
        }

        lastLogFileName = Log4JHelper.getInstance().getFilenameFromAppender(fileAppender);
        return lastLogFileName;
    }

    public String timeRemaining() {
        if (market.isOpen()) {
            long remaining = rawTimeRemaining();
            long mins = remaining / (60 * 1000);
            String minutes = Long.toString(mins);
            String seconds = Long.toString((remaining - (mins * 60 * 1000)) / 1000);
            if (seconds.length() == 1) {
                seconds = "0" + seconds;
            }
            return minutes + ":" + seconds;
        } else {
            return "closed";
        }
    }

    public long rawTimeRemaining() {
        Date date = new Date();
        long now = date.getTime();
        return Math.max(0, roundFinishTime - now);
    }

    public Quantity calculateEarnings(String userName) {
        Quantity total = Quantity.ZERO;
        for (int round = 1; round <= currentRound ; round++) {
            total = total.plus(getPlayer(userName).getScore(round));
        }
        return total;
    }

    public Quantity getChartScale() {
        if (chartScale == null) {
            chartScale = PropertyHelper.parseDecimalQuantity(CHART_SCALE, props, Quantity.ZERO);
        }
        return chartScale;
    }

    public Quantity getMajorUnit() {
        if (majorUnit == null) {
            majorUnit = PropertyHelper.parseDecimalQuantity(MAJOR_UNIT, props, Quantity.ZERO);
        }
        return majorUnit;
    }

    public Quantity getMinorUnit() {
        if (minorUnit == null) {
            minorUnit = PropertyHelper.parseDecimalQuantity(MINOR_UNIT, props, Quantity.ZERO);
        }
        return minorUnit;
    }

    public boolean isCarryForward() {
        String carryForward = props.getProperty(CARRY_FORWARD, "false").trim();
        return CARRY_FORWARD_ALL.equalsIgnoreCase(carryForward) || "true".equalsIgnoreCase(carryForward);
    }

    public boolean getShowEarnings() {
        String showEarnings = props.getProperty(SHOW_EARNINGS);
        if (showEarnings == null || showEarnings.matches("[ \t]*") || "true".equalsIgnoreCase(showEarnings.trim())) {
            return true;
        } else {
            return false;
        }
    }

    public boolean showCumulativeProfits() {
        String showCP = props.getProperty(SHOW_CUMULATIVE_PROFITS);
        return (showCP == null || showCP.matches("[ \t]*") || "true".equalsIgnoreCase(showCP.trim()));
    }

    public String getCommonMessageLabel() {
        String label = props.getProperty(COMMON_MESSAGE_LABEL);
        if (label == null || "".equals(label)) {
            return DEFAULT_COMMON_MESSAGE_LABEL;
        }
        return label.trim();
    }

    public String getMessageLabel() {
        String label = props.getProperty(MESSAGE_LABEL);
        if (label == null || "".equals(label)) {
            return DEFAULT_MESSAGE_LABEL;
        }
        return label.trim();
    }

    public String getRoundLabel() {
        String label = props.getProperty(ROUND_LABEL);
        if (label == null || "".equals(label)) {
            return DEFAULT_ROUND_LABEL;
        }
        return label.trim();
    }

    static public String getRoundLabelOrDefault() {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return DEFAULT_ROUND_LABEL;
        } else {
            return session.getRoundLabel();
        }
    }

    public String getSharesLabel() {
        String label = props.getProperty(SHARES_LABEL);
        if (label == null || "".equals(label)) {
            return DEFAULT_SHARES_LABEL;
        }
        return label.trim();
    }

    public String aboveThresholdMessage() {
        String label = props.getProperty(ABOVE_THRESHOLD_WORD);
        if (label == null) {
            return "";
        }
        return label.trim();
    }

    public String belowThresholdMessage() {
        String label = props.getProperty(BELOW_THRESHOLD_WORD);
        if (label == null) {
            return "";
        }
        return label.trim();
    }

    public Price maxPrice() {
        return market.maxPrice();
    }

    public Price marketPrice(Quantity q) {
        return new Price(q, maxPrice());
    }

    public String getLogoPath() {
        String label = props.getProperty(LOGO_PATH_PROP_NAME);
        if (label == null || "".equals(label)) {
            return LOGO_DEFAULT_PATH;
        } else if (label.trim().equals(LOGO_URL_NONE)) {
            return "";
        } else {
            return label.trim();
        }
    }

    public boolean privateDividendsInUse() {
        return privateValuesInUse;
    }

    public void ifScoring(StatusAdaptor ifScoring) {
        phase.informShowingScores(ifScoring);
    }

    public void ifTrading(StatusAdaptor adaptor) {
        phase.informTrading(adaptor);
    }

    protected StateHolder getPhase() {
        return phase;
    }

    public String[] experimenterButtons() {
        String[] buttons;
        if (getCurrentRound() == 0) {
            buttons = new String[] { startRoundActionLabel() };
        } else {
            buttons = new String[] {
                    startRoundActionLabel(),
                    stopRoundActionLabel() + " " + getCurrentRound()
            };
        }
        return buttons;
    }

    static public String startRoundActionLabel() {
        return findSessionReplaceRoundString(START_NEXT_ROUND_ACTION);
    }

    static public String startRoundTransitionLabel() {
        return findSessionReplaceRoundString(START_ROUND_TRANSITION);
    }

    static public String startRoundText() {
        return findSessionReplaceRoundString(START_ROUND_TEXT);
    }

    static public String cannotStartRoundMessage() {
        return findSessionReplaceRoundString(SessionState.CANNOT_START_ROUND);
    }

    static public String stopRoundActionLabel() {
        return findSessionReplaceRoundString(STOP_ROUND_ACTION);
    }

    static public String endTradingLabel() {
        return findSessionReplaceRoundString(END_TRADING_TEXT);
    }

    public static String findSessionReplaceRoundString(String label) {
        Session session = SessionSingleton.getSession();
        if (session == null) {
            return label;
        } else {
            return session.replaceRoundString(label);
        }
    }

    public String replaceRoundString(String label) {
        return label.replaceAll("round", getRoundLabel());
    }

    public String stateSpecificTraderHtml(String claimName, String userName) {
        return "";
    }

    public String[] stateSpecificTraderButtons() {
        return new String[0];
    }

    public String stateSpecificDisplay() {
        return "";
    }

    protected MockBayeuxChannel getTradeHistory() {
        return tradeHistory;
    }

    public void webAction(String userName, String parameter) {
        return;  // override as necessary
    }

    public String showEarningsSummary(final String userName) {
        final StringBuffer buff = new StringBuffer();
        StatusAdaptor ad = new NoActionStatusAdaptor() {
            public void trading() { renderScore(userName, buff); }
            public void showingScores() { renderScoreAndExplanation(userName, buff); }
        };
        ifScoring(ad);
        ifTrading(ad);
        return buff.toString();
    }

    void renderScore(String userName, StringBuffer buff) {
        if (getPlayer(userName) == null) {
            return;
        }

        if (!isCarryForward()) {
            renderScore(buff, calculateEarnings(userName).printAsScore());
        }
    }

    void renderScoreAndExplanation(String userName, StringBuffer buff) {
        AbstractSubject subject = getPlayer(userName);
        if (subject == null) {
            return;
        }

        if ((lastRound()) || !isCarryForward()) {
            renderScore(buff, calculateEarnings(userName).printAsScore());
            buff.append("<p>");
        }
        buff.append(subject.getScoreExplanation());
    }

    protected void renderScore(StringBuffer buff, final String score) {
        if(showCumulativeProfits()) {
            new HtmlTable().render(buff);
            buff.append("\n    ");
            HtmlRow.twoCells(buff, "Cumulative Profits", score);
            buff.append("\n</table>\n");
        }
    }
    public Quantity getRemainingDividend(AbstractSubject trader, int round) throws ScoreException {
        Quantity propMax = PropertyHelper.getMaxDividend(props);
        Quantity max;
        if (! (propMax.equals(Quantity.ONE.negate()))) {
            max = propMax;
        } else {
            max = Quantity.ZERO;
            for (int i = 1; i <= rounds(); i++) {
                max = max.max(getDividend(trader, i));
            }
        }
        if (isCarryForward()) {
            return max.times(new Quantity((rounds() + 1 - round)));
        } else {
            return max;
        }
    }

    public boolean reservesAreRequired() {
        return market.requireReserves();
    }

    protected void provideCash(Trader trader, Quantity amount) {
        trader.getUser().receiveCash(rootBank.makeFunds(amount));
    }

	/**
	 * Exposes key information about the session.
	 *
	 * @return The session data.
	 */
	public SessionData inspect() {
		SessionData data = new SessionData(this);
		data.setProperties(this.props);
		data.setCashBank(this.rootBank);
		data.setCouponBank(this.mint);
		data.setMarket(this.market);
		data.setRoles(this.roles);
		data.setClaim(this.claim);
		data.setPlayers(this.players);
		data.setCurrentRound(this.currentRound);
		return data;
	}

}
