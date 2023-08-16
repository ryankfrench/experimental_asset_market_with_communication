package jjdm.zocalo;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.market.DuplicateOrderException;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.user.User;

import java.util.Properties;
import java.util.Random;

/**
 * Common testing methods.
 */
public class ZocaloTestHelper {

	/**
	 * Create a new Zocalo session.
	 *
	 * @param properties The properties.
	 * @return The session created.
	 */
	public static Session createSession(Properties properties) {
		SessionSingleton.setSession(properties);
		return SessionSingleton.getSession();
	}

	/**
	 * End the current round in the session.
	 */
	public static void endCurrentRound() {
		Session session = SessionSingleton.getSession();
		session.endTrading(true);
	}

	/**
	 * Return the current balance for the user.
	 *
	 * @param username The user ID (e.g. "a1");
	 * @return The balance.
	 */
	public static int getBalance(String username) {
		TradingSubject trader = getTrader(username);
		return trader.balance().asValue().intValue();
	}

	/**
	 * Return the current coupon count for the user.
	 *
	 * @param username The user ID (e.g. "a1");
	 * @return The coupon count.
	 */
	public static int getCouponCount(String username) {
		TradingSubject trader = getTrader(username);
		Session session = SessionSingleton.getSession();
		BinaryClaim binaryClaim = (BinaryClaim) session.getMarket().getClaim();
		Quantity actual = trader.currentCouponCount(binaryClaim);
		return actual.asValue().intValue();
	}

	/**
	 * Get the score for a user for a given round.
	 *
	 * @param round    The round to retrieve the score.
	 * @param username The user ID (e.g. "a1");
	 * @return The user's score.
	 */
	public static int getTotalScore(int round, String username) {
		TradingSubject trader = getTrader(username);
		return trader.getScore(round).asValue().intValue();
	}

	/**
	 * Return the trader based on the username.
	 *
	 * @param username The name (e.g. "a1").
	 * @return The trader.
	 */
	public static TradingSubject getTrader(String username) {
		Session session = SessionSingleton.getSession();
		AbstractSubject subject = session.getPlayer(username);
		if (subject instanceof TradingSubject) {
			return (TradingSubject) subject;
		} else {
			throw new IllegalArgumentException(username + " is not a trader.");
		}
	}

	/**
	 * Return the user based on the username.
	 *
	 * @param username The name (e.g. "a1").
	 * @return The user.
	 */
	public static User getUser(String username) {
		TradingSubject trader = getTrader(username);
		return trader.getUser();
	}

	/**
	 * Return properties based on test configuration.
	 *
	 * @param filename The filename (in test/resources).
	 * @return The properties.
	 */
	public static Properties loadProperties(String filename) {
		Properties props = new Properties();
		try {
			props.load(ZocaloTestHelper.class.getClassLoader().getResourceAsStream(filename));
			return props;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Print out message to System.out.
	 *
	 * @param message The text.
	 */
	public static void log(Object message) {
		System.out.println(message.toString());
	}

	/**
	 * Get the time (milliseconds since 1970).
	 *
	 * @return System.currentTimeMillis()
	 */
	public static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Seller is willing to sell at this price.
	 *
	 * @param username The user ID (e.g. a1).
	 * @param price    The amount of the ask.
	 */
	public static void orderAsk(String username, int price) {
		Session session = SessionSingleton.getSession();
		try {
			order(session.getMarket(), username, price, false, false);
		} catch (DuplicateOrderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seller is willing to buy at this price.
	 *
	 * @param username The user ID (e.g. a1).
	 * @param price    The price of the bid.
	 */
	public static void orderBid(String username, int price) {
		Session session = SessionSingleton.getSession();
		try {
			order(session.getMarket(), username, price, true, false);
		} catch (DuplicateOrderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seller is willing to sell at this price.
	 *
	 * @param username The user ID (e.g. a1).
	 * @param price    The amount of the ask.
	 */
	public static void orderBuy(String username, int price) {
		Session session = SessionSingleton.getSession();
		try {
			order(session.getMarket(), username, price, true, true);
		} catch (DuplicateOrderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Seller is willing to buy at this price.
	 *
	 * @param username The user ID (e.g. a1).
	 * @param price    The price of the bid.
	 */
	public static void orderSell(String username, int price) {
		Session session = SessionSingleton.getSession();
		try {
			order(session.getMarket(), username, price, false, true);
		} catch (DuplicateOrderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get a random character.
	 *
	 * @return Single, random character in String format.
	 */
	public static String randomString() {
		return String.valueOf((char) (new Random().nextInt(25) + 65));
	}

	/**
	 * Start the next round in the session.
	 */
	public static void startNextRound() {
		Session session = SessionSingleton.getSession();
		session.startNextTimedRound();
	}

	/**
	 * Create a limit order in the market.
	 *
	 * @param market        The running market.
	 * @param username      The user ID (e.g. a1)
	 * @param price         The price for the trade.
	 * @param isBid         True if buy position; false if sell.
	 * @param isMarketOrder Is this a market order? Limit order otherwise.
	 * @throws DuplicateOrderException
	 * @see Market#limitOrder(Position, Price, Quantity, User)
	 * @see Market#marketOrder(Position, Price, Quantity, User)
	 */
	private static void order(Market market, String username, int price, boolean isBid, boolean isMarketOrder) throws DuplicateOrderException {
		User user = getUser(username);
		BinaryClaim claim = (BinaryClaim) market.getClaim();
		Position position;
		Price zocaloPrice;
		if (isBid) {
			position = claim.getYesPosition();
			zocaloPrice = new Price(price, market.maxPrice());
		} else {
			position = claim.getNoPosition();
			zocaloPrice = new Price(price, market.maxPrice()).inverted();
		}
		if (isMarketOrder) {
			market.marketOrder(position, zocaloPrice, Quantity.ONE, user);
		} else {
			market.limitOrder(position, zocaloPrice, Quantity.ONE, user);
		}

	}

}
