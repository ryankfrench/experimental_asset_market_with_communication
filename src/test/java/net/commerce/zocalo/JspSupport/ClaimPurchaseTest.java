package net.commerce.zocalo.JspSupport;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.freechart.ChartScheduler;
//JJDM import net.commerce.zocalo.service.AllMarkets;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Map;
import java.util.Date;

public class ClaimPurchaseTest extends PersistentTestHelper {
    private final String dbFilePath = "data/ClaimPurchaseTest";
    private final String marketName = "rain";
    private final String userName = "joe";
    private final String userPassword = "joeSecret";
    private final String ownerName = "owner";
    private final String ownerPassword = "masterSecret";
    private String claimDescription = "will it rain tomorrow?";

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate(dbFilePath);
        // JJDM new AllMarkets(dbFilePath, false);
        Config.initPasswdGen();

        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = new CashBank(MarketOwner.ROOT_CASH_BANK_NAME);
        storeObject(bank);
        SecureUser user = new SecureUser(userName, bank.makeFunds(30000), userPassword, "user@example.com");
        storeObject(user);
        SecureUser owner = new SecureUser(ownerName, bank.makeFunds(30000), ownerPassword, "someone@example.com");
        storeObject(owner);
        BinaryClaim weather = BinaryClaim.makeClaim(marketName, owner, claimDescription);
        storeObject(weather);
        Market market = BinaryMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testSubmitOrders() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        assertTrue(getBestPrice(marketName, 0).isZero());
        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();

        buy(marketName, "23", "10", userName, getUserWrapper(userName, userPassword));
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        Market market = HibernateTestUtil.getMarketByName(marketName);
        BinaryClaim claim = (BinaryClaim)market.getClaim();
        Price bestYesPrice = market.getBook().getOffers(claim.getYesPosition()).bestPrice();
        assertQEquals(new Quantity("23"), bestYesPrice);
        Price bestNoPrice = market.getBook().getOffers(claim.getNoPosition()).bestPrice();
        assertTrue(bestNoPrice.isZero());
    }

    public void testPriceDisplay() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        assertTrue(getBestPrice(marketName, 0).isZero());
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);

        ClaimPurchase screen = setUpPurchaseScreen(marketName, "3", "20", userName, TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("20 @ 3", screen.displayBestOrdersHtml());

        HttpServletRequest servletRequest = getUserWrapper(ownerName, ownerPassword);
        String goodDesc = "foo the bar";
        MockHttpServletResponse mockServletResponse = new MockHttpServletResponse();

        screen.setQuantity("");
        screen.setPrice("`");
        screen.setDescription(goodDesc);

        screen.processRequest(servletRequest, mockServletResponse);
        assertMatches("<td></td>", screen.displayBestOrdersHtml());
    }

    public void testIllegalOrders() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);
        SecureUser user = HibernateUtil.getUserByName(userName);

        ClaimPurchase screen = setUpPurchaseScreen(marketName, "-3", "20", userName, TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("prices and quantities must be positive: -3<br>", user.getWarningsHTML());

        screen = setUpPurchaseScreen(marketName, "3", "-20", userName, TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("prices and quantities must be positive: -20<br>", user.getWarningsHTML());
    }

    private Price getBestPrice(String marketName, int index) {
        Market market = HibernateTestUtil.getMarketByName(marketName);
        Book book = market.getBook();
        return book.getOffers(market.getClaim().positions()[index]).bestPrice();
    }

    public void testSelling() throws Exception {
        manualSetUpForUpdate(dbFilePath);

        assertTrue(getBestPrice(marketName, 1).isZero());
        sell(marketName, "73", "10", userName, getUserWrapper(userName, userPassword));

        assertQEquals(27, getBestPrice(marketName, 1));

        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String buyerPwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(30000), buyerPwd, "buyer@example.com");
        storeObject(buyer);
        buy(marketName, "80", "10", buyerName, getUserWrapper(buyerName, buyerPwd));
        assertEquals("Bought 10.<br>", buyer.getWarningsHTML());
        assertBinaryTradesBalance((BinaryMarket) HibernateTestUtil.getMarketByName(marketName), buyer, 30000);
    }

    public void testDeleteOrder() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        String buyPrice = "23";
        String buyQuantity = "10";
        assertTrue(getBestPrice(marketName, 0).isZero());
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);
        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        buy(marketName, buyPrice, buyQuantity, userName, userWrapper);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        assertTrue(getBestPrice(marketName, 0).isPositive());

        ClaimPurchase screen = setUpPurchaseScreen(marketName, "", "", userName, "");
        screen.setDeleteOrderPosition("buy");
        screen.setDeleteOrderPrice(buyPrice);
        screen.processRequest(userWrapper, new MockHttpServletResponse());

        assertTrue(getBestPrice(marketName, 0).isZero());
    }

    private void buy(String marketName, String price, String quantity, String userName, HttpServletRequest request) {
        enterOrder(marketName, price, quantity, userName, TradeSupport.BUY_ACTION, request);
    }

    private void sell(String marketName, String price, String quantity, String userName, HttpServletRequest request) {
        enterOrder(marketName, price, quantity, userName, TradeSupport.SELL_ACTION, request);
    }

    private void enterOrder(String marketName, String price, String quantity, String userName, String action, HttpServletRequest request) {
        ClaimPurchase screen = setUpPurchaseScreen(marketName, price, quantity, userName, action);
        screen.processRequest(request, new MockHttpServletResponse());
    }

    private ClaimPurchase setUpPurchaseScreen(String marketName, String price, String quantity, String userName, String action) {
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPrice(price);
        screen.setQuantity(quantity);
        screen.setUserName(userName);
        screen.setBuySell(action);
        return screen;
    }

    public void testClaimPurchaseText() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        ClaimPurchase screen = setUpPurchaseScreen(marketName, "3", "100", userName, TradeSupport.BUY_ACTION);
        screen.processRequest(getUserWrapper(userName, userPassword), new MockHttpServletResponse());
        assertTrue(screen.buyOrEditClaimHtml().lastIndexOf("Edit the Claim Description for") < 0 );

        screen.processRequest(getUserWrapper(ownerName, ownerPassword), new MockHttpServletResponse());
        assertMatches("Edit the Claim Description for", screen.buyOrEditClaimHtml());
        Market market = HibernateTestUtil.getMarketByName(marketName);
        assertEquals(claimDescription, market.getClaim().getDescription());

        screen.setQuantity("");
        screen.setPrice("`");
        screen.setDescription("foo<b>the bar</b>");
        screen.processRequest(getUserWrapper(ownerName, ownerPassword), new MockHttpServletResponse());
        SecureUser owner = HibernateTestUtil.getUserByName(ownerName);
        assertEquals(claimDescription, market.getClaim().getDescription());
        assertREMatches("HTML special.*", owner.getWarningsHTML());

        String foobar = "foo the bar";
        screen.setDescription(foobar);
        screen.processRequest(getUserWrapper(ownerName, ownerPassword), new MockHttpServletResponse());
        assertEquals(foobar, market.getClaim().getDescription());
        assertREMatches("", owner.getWarningsHTML());
    }

    public void testEditingClaim() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        ClaimPurchase screen = setUpPurchaseScreen(marketName, "3", "100", userName, TradeSupport.BUY_ACTION);
        MockHttpServletResponse mockServletResponse = new MockHttpServletResponse();
        screen.processRequest(getUserWrapper(userName, userPassword), mockServletResponse);
        assertTrue(screen.buyOrEditClaimHtml().lastIndexOf("Edit the Claim Description for") < 0 );

        Market market = HibernateTestUtil.getMarketByName(marketName);
        HttpServletRequest servletRequest = getUserWrapper(ownerName, ownerPassword);
        SecureUser owner = HibernateTestUtil.getUserByName(ownerName);
        String badDesc = "foo<b>the bar</b>";
        String goodDesc = "foo the bar";

        screen.setQuantity("");
        screen.setPrice("`");
        screen.setDescription(badDesc);
        screen.processRequest(servletRequest, mockServletResponse);
        assertTrue(screen.buyOrEditClaimHtml().lastIndexOf("Edit the Claim Description for") > 0 );
        assertEquals(claimDescription, market.getClaim().getDescription());
        assertREMatches("HTML special.*", owner.getWarningsHTML());

        screen.setDescription(goodDesc);
        screen.processRequest(servletRequest, mockServletResponse);
        assertEquals(goodDesc, market.getClaim().getDescription());
        assertREMatches("", owner.getWarningsHTML());
    }

    public void testDecidingClaim() throws Exception {
        final String chooseOutcome = "Choose the outcome that will pay";

        manualSetUpForUpdate(dbFilePath);
        ClaimPurchase screen = setUpPurchaseScreen(marketName, "", "", userName, TradeSupport.BUY_ACTION);
        screen.processRequest(getUserWrapper(userName, userPassword), new MockHttpServletResponse());
        assertRENoMatch(chooseOutcome, screen.buyOrEditClaimHtml());
        screen.processRequest(getUserWrapper(ownerName, ownerPassword), new MockHttpServletResponse());
        assertMatches(chooseOutcome, screen.buyOrEditClaimHtml());
    }

    public void testMultiClaimsInForms() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser owner = HibernateUtil.getUserByName(ownerName);
        String marketName = "weather";
        MultiClaim weather = MultiClaim.makeClaim(marketName, owner, marketName, new String[] { "sun", "rain", "fog" } );
        storeObject(weather);
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        MultiMarket market = MultiMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        MultiMarketMaker mmm = market.makeMarketMaker(q(300), owner);
        storeObject(mmm);
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);
        SecureUser user = HibernateUtil.getUserByName(userName);

        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setUserName(userName);
        StringBuffer buf = new StringBuffer();
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("name=description>weather", screen.allowEditingDescription(buf));
        assertMatches("weather", screen.allowEditingDescription(buf));
        assertMatches("<td><span id='fog-price'>33.333</span></td>", screen.displayBestOrdersHtml());
        assertREMatches(".*Specify position, target price, and quantity:<input.*value='weather'.*", screen.buyOrEditClaimHtml());
        assertEquals(new File("charts/weather-pv.png"), new File(screen.historyChartNameForJsp()));
        assertMatches("<p><b><h3>User has no Holdings</h3>", screen.displayHoldingsHtml());
        assertMatches("<h3>User has no Outstanding Orders</h3>", screen.displayOrdersHtml(market));

        screen.setPositionName("rain");
        screen.setPrice("40");
        screen.setQuantity("1");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertTrue(user.couponCount(weather.lookupPosition("rain")).isPositive());

        screen.setPrice("20");
        screen.setAction(TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());

        assertMatches("34.1", screen.displayBestOrdersHtml());
        assertTrue(user.couponCount(weather.lookupPosition("rain")).isZero());
        assertBalancedTrades(user, market, 30000);
    }

    public void testClosedMarkets() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser user = HibernateUtil.getUserByName(userName);
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);

        // Set up an offer
        assertTrue(getBestPrice(marketName, 1).isZero());
        sell(marketName, "73", "10", userName, getUserWrapper(userName, userPassword));
        assertQEquals(27, getBestPrice(marketName, 1));

        // Set up purchase screen, without purchasing
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String buyerPwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(300), buyerPwd, "buyer@example.com");
        storeObject(buyer);
        HttpServletRequest buyerWrapper = getUserWrapper(buyerName, buyerPwd);

        ClaimPurchase buyScreen = new ClaimPurchase();
        buyScreen.setClaimName(marketName);
        buyScreen.setPositionName("yes");
        buyScreen.setUserName(buyerName);
// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "Trade");
//        map.put("selectedRow", "yes");
//        map.put("selectedClaim", "yes");
//        map.put("yesprice", "30");
//        map.put("yescost", "5");
        buyScreen.setAction(TradeSupport.BUY_ACTION);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertMatches("", user.getWarningsHTML());
        assertMatches("", buyer.getWarningsHTML());

        // Close market
        Market market = HibernateTestUtil.getMarketByName(marketName);
        market.close();

        // Can't buy with pre-existing screen
        buyScreen.processRequest(buyerWrapper, response);
        assertMatches("", user.getWarningsHTML());
        assertMatches("market is closed", buyer.getWarningsHTML());
        assertQEquals(300.0, buyer.cashOnHand());
    }

    public void testCostLimitBuy() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser user = HibernateUtil.getUserByName(userName);
        assertTrue(getBestPrice(marketName, 1).isZero());
        sell(marketName, "73", "10", userName, getUserWrapper(userName, userPassword));

        assertQEquals(27, getBestPrice(marketName, 1));

        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String buyerPwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(30000), buyerPwd, "buyer@example.com");
        storeObject(buyer);
        HttpServletRequest userWrapper = getUserWrapper(buyerName, buyerPwd);

        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("yes");
        screen.setUserName(buyerName);
// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "Trade");
//        map.put("selectedRow", "yes");
//        map.put("selectedClaim", "yes");
//        map.put("yesprice", "30");
//        map.put("yescost", "5");
        screen.setAction(TradeSupport.BUY_ACTION);
        MockHttpServletResponse response = new MockHttpServletResponse();
        screen.processRequest(userWrapper, response);
        assertMatches("", user.getWarningsHTML());

        assertQEquals(30000 - 500.0, buyer.cashOnHand());
        assertBinaryTradesBalance((BinaryMarket) HibernateTestUtil.getMarketByName(marketName), buyer, 30000.0);        
   }

    public void testCostLimitBuyPriceLimit() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser user = HibernateUtil.getUserByName(userName);
        assertTrue(getBestPrice(marketName, 1).isZero());
        sell(marketName, "80", "8", userName, getUserWrapper(userName, userPassword));
        sell(marketName, "73", "5", userName, getUserWrapper(userName, userPassword));

        assertQEquals(27, getBestPrice(marketName, 1));

        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String buyerPwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(30000), buyerPwd, "buyer@example.com");
        storeObject(buyer);
        HttpServletRequest userWrapper = getUserWrapper(buyerName, buyerPwd);

        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("yes");
        screen.setUserName(buyerName);
// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("selectedRow", "yes");
//        map.put("selectedClaim", "yes");
//        map.put("action", "Trade");
//        map.put("yesprice", "30");
//        map.put("yescost", "5");
        screen.setAction(TradeSupport.BUY_ACTION);
        MockHttpServletResponse response = new MockHttpServletResponse();
        screen.processRequest(userWrapper, response);
        assertMatches("", user.getWarningsHTML());

        assertTrue(buyer.cashOnHand().compareTo(q(30000 - 500)) >= 0);
        assertQEquals(20, getBestPrice(marketName,1));
        Market market = HibernateTestUtil.getMarketByName(marketName);
        Book book = market.getBook();
        assertTrue(new Quantity(8.0).compareTo(book.getOffers(market.getClaim().positions()[1]).getQuantityAtPrice(Price.dollarPrice("20"))) > 0);
        assertBinaryTradesBalance((BinaryMarket) HibernateTestUtil.getMarketByName(marketName), buyer, 30000);
   }

    public void testCostLimitSell() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser user = HibernateUtil.getUserByName(userName);
        assertTrue(getBestPrice(marketName, 1).isZero());
        buy(marketName, "12", "15", userName, getUserWrapper(userName, userPassword));

        assertQEquals(12, getBestPrice(marketName, 0));

        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String buyerPwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(30000), buyerPwd, "buyer@example.com");
        storeObject(buyer);
        HttpServletRequest userWrapper = getUserWrapper(buyerName, buyerPwd);

// JJDM  Map<String, String> map = userWrapper.getParameterMap();
//	  map.put("selectedRow", "no");
//	  map.put("selectedClaim", "no");
//	  map.put("action", "Trade");
//	  map.put("noprice", "10");
//	  map.put("nocost", "5");
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("no");
        screen.setUserName(buyerName);
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        screen.processRequest(userWrapper, response2);
        assertMatches("", user.getWarningsHTML());

        assertQEquals(30000 - 500, buyer.cashOnHand());
        assertBinaryTradesBalance((BinaryMarket)HibernateTestUtil.getMarketByName(marketName), buyer, 30000.0);
    }

    public void testMultiCostLimitBuySell() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser owner = HibernateUtil.getUserByName(ownerName);
        String marketName = "weather";
        MultiClaim weather = MultiClaim.makeClaim(marketName, owner, marketName, new String[] { "sun", "rain", "fog" } );
        storeObject(weather);
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        MultiMarket market = MultiMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        MultiMarketMaker mmm = market.makeMarketMaker(q(30000), owner);
        storeObject(mmm);

        SecureUser user = HibernateUtil.getUserByName(userName);
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);

// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "Trade");
//        map.put("selectedRow", "rain");
//        map.put("selectedClaim", "rain");
//        map.put("rainprice", "25");
//        map.put("raincost", "5");
//        map.put("raindisplay", "33");
//        map.put("rainReference", "33");
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("rain");
        screen.setUserName(userName);
        screen.setAction(TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("", user.getWarningsHTML());
        assertQEquals(30000 - 500, user.cashOnHand());
        Position rainPosition = market.getClaim().lookupPosition("rain");
        Probability rainProb = market.currentProbability(rainPosition);
        assertTrue("Probability should be lower after selling", q(.33).compareTo(rainProb) >= 0);

        Position sunPosition = market.getClaim().lookupPosition("sun");
        Probability sunProb = market.currentProbability(sunPosition);

        screen.setPositionName(null);
        screen.setUserName(userName);
// JJDM   map.put("action", "Trade");
//        map.put("selectedRow", "sun");
//        map.put("selectedClaim", "sun");
//        map.put("sunprice", "45");
//        map.put("suncost", "5");
//        map.put("sundisplay", "33");
//        map.put("sunReference", "33");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("", user.getWarningsHTML());

        assertQEquals(30000 - 1000, user.cashOnHand());
        assertTrue("Probability should be higher after buying", market.currentProbability(sunPosition).compareTo(sunProb) > 0);
        assertBalancedTrades(user, market, 30000);
    }

    public void testMultiCostLimitScenario1107() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        storeObject(bank);
        SecureUser owner = new SecureUser("McDuck", bank.makeFunds(15000), "oneTime", "mcduck@example.com");
        storeObject(owner);
        MultiClaim weather = MultiClaim.makeClaim("football", owner, "football", new String[] { "RUN", "PASS" } );
        storeObject(weather);
        MultiMarket market = MultiMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        MultiMarketMaker mmm = market.makeMarketMaker(q(10000), owner);
        storeObject(mmm);
        String pwd = "shared";

        quickCreateUser("z13", bank);
        quickCreateUser("z11", bank);
        quickCreateUser("z10", bank);
        quickCreateUser("z5", bank);
        quickCreateUser("z12", bank);
        quickCreateUser("z14", bank);
        quickCreateUser("z9", bank);
        quickCreateUser("z7", bank);
        quickCreateUser("z15", bank);
        quickCreateUser("z3", bank);
        quickCreateUser("z6", bank);
        quickCreateUser("z2", bank);
        quickCreateUser("z1", bank);
        quickCreateUser("z8", bank);
        quickCreateUser("z4", bank);

        processCostLimitRequest("z13", "PASS", "500", "70", "1", pwd, "football");
        processCostLimitRequest("z11", "RUN", "750", "65", "1", pwd, "football");
        processCostLimitRequest("z10", "PASS", "100", "60", "1", pwd, "football");
        processCostLimitRequest("z5", "PASS", "100", "52", "1", pwd, "football");
        processCostLimitRequest("z13", "PASS", "500", "60", "1", pwd, "football");
        processCostLimitRequest("z12", "RUN", "1100", "75", "1", pwd, "football");
        processCostLimitRequest("z14", "PASS", "300", "75", "1", pwd, "football");
        processCostLimitRequest("z9", "PASS", "1500", "65", "1", pwd, "football");
        processCostLimitRequest("z7", "RUN", "800", "55", "1", pwd, "football");
        processCostLimitRequest("z10", "PASS", "500", "60", "1", pwd, "football");
        processCostLimitRequest("z15", "PASS", "7000", "80", "1", pwd, "football");
        processCostLimitRequest("z14", "PASS", "300", "75", "1", pwd, "football");
        processCostLimitRequest("z12", "RUN", "1100", "75", "1", pwd, "football");
        processLiquidateRequest("z12", "RUN", pwd, "football");
        processCostLimitRequest("z13", "PASS", "900", "77", "1", pwd, "football");
        processLiquidateRequest("z10", "PASS", pwd, "football");
        processLiquidateRequest("z15", "PASS", pwd, "football");
        processCostLimitRequest("z3", "RUN", "500", "85", "1", pwd, "football");
        processCostLimitRequest("z9", "PASS", "2000", "70", "1", pwd, "football");
        processCostLimitRequest("z13", "PASS", "900", "55", "99", pwd, "football");
        processCostLimitRequest("z6", "PASS", "100", "90", "1", pwd, "football");
        processCostLimitRequest("z15", "RUN", "7000", "80", "1", pwd, "football");
        processCostLimitRequest("z2", "PASS", "200", "75", "1", pwd, "football");
        processCostLimitRequest("z10", "PASS", "100", "80", "1", pwd, "football");
        processCostLimitRequest("z11", "RUN", "750", "50", "99", pwd, "football");
        processLiquidateRequest("z7", "RUN", pwd, "football");
}

    private void quickCreateUser(String name, CashBank bank) {
        storeObject(new SecureUser(name, bank.makeFunds(10000), "shared", "everyone@example.com"));
    }

    private SecureUser processCostLimitRequest(String loginName, String pos, String cost, String price, String displayedPrice, String pwd, String marketName) {
        SecureUser user = HibernateUtil.getUserByName(loginName);
        HttpServletRequest userWrapper = getUserWrapper(loginName, pwd);
// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "Trade");
//        map.put("selectedRow", pos);
//        map.put("selectedClaim", pos);
//        map.put(pos + "price", price);
//        map.put(pos + "cost", cost);
//        map.put(pos + "display", displayedPrice);        
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName(pos);
        screen.setUserName(loginName);
        screen.setAction(TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("", user.getWarningsHTML());
        return user;
    }

    private SecureUser processLiquidateRequest(String loginName, String pos, String pwd, String marketName) {
        SecureUser user = HibernateUtil.getUserByName(loginName);
        HttpServletRequest userWrapper = getUserWrapper(loginName, pwd);
// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "sell holdings");
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName(pos);
        screen.setUserName(loginName);
        screen.setAction(TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        return user;
    }

    public void testEmptyMultiCostLimitTrade() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        SecureUser owner = HibernateUtil.getUserByName(ownerName);
        String marketName = "weather";
        MultiClaim weather = MultiClaim.makeClaim(marketName, owner, marketName, new String[] { "sun", "rain", "fog" } );
        storeObject(weather);
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        MultiMarket market = MultiMarket.make(owner, weather, bank.noFunds());
        storeObject(market);
        MultiMarketMaker mmm = market.makeMarketMaker(q(30000), owner);
        storeObject(mmm);

        SecureUser user = HibernateUtil.getUserByName(userName);
        HttpServletRequest userWrapper = getUserWrapper(userName, userPassword);

// JJDM   Map<String, String> map = userWrapper.getParameterMap();
//        map.put("action", "Trade");
//        map.put("selectedRow", "rain");
//        map.put("selectedClaim", "rain");
//        map.put("rainprice", "");
//        map.put("raincost", "");
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("rain");
        screen.setUserName(userName);
        screen.setAction(TradeSupport.SELL_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("price must be greater than 0 and less than 100.<br>", user.getWarningsHTML());
        assertQEquals(30000, user.cashOnHand());
        Position rainPosition = market.getClaim().lookupPosition("rain");
        Probability rainProb = market.currentProbability(rainPosition);
        assertQEquals("Probability should be 1/3", q(1.0/3.0), rainProb);

        screen.setPositionName(null);
        screen.setUserName(userName);
// JJDM   map.put("action", "Trade");
//        map.put("selectedRow", "sun");
//        map.put("selectedClaim", "sun");
//        map.put("sunprice", "");
//        map.put("suncost", "5");
        screen.setAction(TradeSupport.BUY_ACTION);
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        assertMatches("price must be greater than 0 and less than 100.<br>", user.getWarningsHTML());
        assertQEquals(30000, user.cashOnHand());
        assertQEquals("Probability should be 1:3", q(1.0/3.0), rainProb);
        assertBalancedTrades(user, market, 30000);
    }

    public void testChartUpdating() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        Market market = HibernateTestUtil.getMarketByName(marketName);
        Date firstDate = market.getLastTrade();
        ClaimPurchase screen = new ClaimPurchase();
        screen.setClaimName(marketName);
        screen.setPositionName("rain");
        screen.setUserName(userName);
        String chartName = screen.historyChartNameForJsp();
        ChartScheduler sched = ChartScheduler.find(market.getName());
        int i;
        for (i = 0; i < 20 ; i++) {
            Thread.sleep(100);
            if (! sched.isBusy()) {
                break;
            }
        }
        assertTrue(i < 15);
        Logger logger = Logger.getLogger("info");
        logger.warn("slept for " + i / 10.0 + " secs.");
        long lastChartGen = new File("webpages/" + chartName).lastModified();
        assertTrue(" " + lastChartGen + " ! > " + firstDate.getTime(), 1000 + lastChartGen > firstDate.getTime());

        CashBank bank = HibernateUtil.getOrMakePersistentRootBank(MarketOwner.ROOT_CASH_BANK_NAME);
        String buyerName = "buyer";
        String passwd = "bar";
        SecureUser buyer = new SecureUser(buyerName, bank.makeFunds(300), passwd, "buyer@example.com");
        storeObject(buyer);

        sell(marketName, "73", "10", userName, getUserWrapper(userName, userPassword));
        assertEquals(lastChartGen, new File("webpages/" + screen.historyChartNameForJsp()).lastModified());
        buy(marketName, "75", "5", buyerName, getUserWrapper(buyerName, passwd));

        String name = screen.historyChartNameForJsp();
        int j;
        for (j = 0; j < 20 ; j++) {
            Thread.sleep(100);
            if (! sched.isBusy()) {
                break;
            }
        }
        assertTrue(j < 15);
        logger.warn("slept for " + j / 10.0 + " secs.");
        long lastModified = new File("webpages/" + name).lastModified();
        assertTrue(lastChartGen + " ! <= " +  lastModified, lastChartGen <= 1000 + lastModified);
    }
}
