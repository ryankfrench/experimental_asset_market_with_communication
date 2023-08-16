package jjdm.zocalo;

import jjdm.zocalo.data.ZocaloLabels;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.Session;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static jjdm.zocalo.ZocaloTestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Used to testing running experiments.
 *
 * @author Josh Martin
 */
public class ExperimentTest {

	/**
	 * Test some basic trading.  Ensure scores, and resets are good.  Two notes about how Zocalo is currently logging
	 * activity that looks a bit off at first glance:
	 * <p>
	 * NOTE 1 Below: When trying to place an invalid market order on the sell side (selling higher than any current
	 * bid), Zocalo logs this as MAX - PRICE 470 (600 - 130), but the user screen just shows "No Shares". OK for now,
	 * but just watch out about expecting to see 130 when there is an error on the sell side. Zocalo logs out the
	 * inverted position (max - price).  This is a likely a Zocalo bug, but is not likely to actually happen in
	 * practice, since there is no easy way for users to alter what they bid for the market price.
	 * <p>
	 * NOTE 2 Below: Zocalo has a check (Warnable#addWarning) that only adds a single warning per user. This is OK (only
	 * one message sent to screen per request), but when watching the logs in unit testing, you will not see more than
	 * one error per user since everything is running in one request.  In short, Zocalo will swallow errors for a given
	 * user when unit testing.s
	 */
	@Test
	public void basicExperiment() {
		Properties props = loadProperties("ConfigurationTestNoChat.properties");
		int cash = 1200;
		int coupons = 4;
		List<Integer> dividends = Arrays.asList(240, 50, 240, 50, 490);

		Session session = createSession(props);
		startNextRound();
		int currentRound = session.getCurrentRound();
		int currentDividend = dividends.get(currentRound - 1);
		orderBid("a1", 120); // a1 willing to buy at 120
		orderBid("a2", 240); // a2 willing to buy at 240
		orderAsk("a3", 300); // a3 willing to sell at 300
		orderAsk("a3", 290); // a3 willing to sell at 290
		orderAsk("a3", 100); // a3 willing to sell at 100; a3 sells to a2 at 240
		orderBid("a1", 230); // a1 willing to buy at 230
		orderSell("a3", 230); // a3 buys at market 230; a3 sells to a1 at 230
		orderSell("a4", 130); // [NOTE 1] a4 tries to market sell at 130; warning - no one buying that high
		orderBuy("a5", 5); // a5 tries to market buy at 5; warning - no one selling that low
		orderBuy("a4", 10); // [NOTE 2] a4 tries to market buy at 10; warning - no one selling that low
		orderSell("a4", 119); // a4 sells at market 119; a4 sells to a1 at 120

		// a3 sells to a2 at 240
		// a3 sells to a1 at 230
		// a4 sells to a1 at 120

		assertEquals(getBalance("a1"), cash - 230 - 120);
		assertEquals(getCouponCount("a1"), coupons + 1 + 1);
		assertEquals(getBalance("a2"), cash - 240);
		assertEquals(getCouponCount("a2"), coupons + 1);
		assertEquals(getBalance("a3"), cash + 240 + 230);
		assertEquals(getCouponCount("a3"), coupons - 1 - 1);
		assertEquals(getBalance("a4"), cash + 120);
		assertEquals(getCouponCount("a4"), coupons - 1);
		assertEquals(getBalance("a5"), cash);
		assertEquals(getCouponCount("a5"), coupons);

		endCurrentRound();

		assertEquals(getTotalScore(currentRound, "a1"), (cash - 230 - 120) + currentDividend * (coupons + 1 + 1));
		assertEquals(getTotalScore(currentRound, "a2"), (cash - 240) + currentDividend * (coupons + 1));
		assertEquals(getTotalScore(currentRound, "a3"), (cash + 240 + 230) + currentDividend * (coupons - 1 - 1));
		assertEquals(getTotalScore(currentRound, "a4"), (cash + 120) + currentDividend * (coupons - 1));
		assertEquals(getTotalScore(currentRound, "a5"), (cash) + currentDividend * (coupons));

		startNextRound();

		currentRound = session.getCurrentRound();
		currentDividend = dividends.get(currentRound - 1);
		assertTrue(2 == currentRound);
		try {
			assertTrue(currentDividend == session.getDividend(currentRound).asValue().intValue());
		} catch (ScoreException e) {
			throw new RuntimeException(e);
		}
		assertEquals(getTotalScore(currentRound, "a1"), 0);
		assertEquals(getTotalScore(currentRound, "a2"), 0);
		assertEquals(getTotalScore(currentRound, "a3"), 0);
		assertEquals(getTotalScore(currentRound, "a4"), 0);
		assertEquals(getTotalScore(currentRound, "a5"), 0);
		assertEquals(getBalance("a1"), cash);
		assertEquals(getCouponCount("a1"), coupons);
		assertEquals(getBalance("a2"), cash);
		assertEquals(getCouponCount("a2"), coupons);
		assertEquals(getBalance("a3"), cash);
		assertEquals(getCouponCount("a3"), coupons);
		assertEquals(getBalance("a4"), cash);
		assertEquals(getCouponCount("a4"), coupons);
		assertEquals(getBalance("a5"), cash);
		assertEquals(getCouponCount("a5"), coupons);

	}

	@Test
	public void cashCarryOverExperiment() {
		Properties props = loadProperties("ConfigurationTestCashRollover.properties");
		int cash = 1200;
		int coupons = 4;
		List<Integer> dividends = Arrays.asList(240, 50, 240, 50, 490);

		Session session = createSession(props);
		startNextRound();
		int currentRound = session.getCurrentRound();
		int currentDividend = dividends.get(currentRound - 1);
		orderBid("a1", 120); // a1 willing to buy at 120
		orderBid("a2", 240); // a2 willing to buy at 240
		orderAsk("a3", 300); // a3 willing to sell at 300
		orderAsk("a3", 290); // a3 willing to sell at 290
		orderAsk("a3", 100); // a3 willing to sell at 100; a3 sells to a2 at 240
		orderBid("a1", 230); // a1 willing to buy at 230
		orderSell("a3", 230); // a3 buys at market 230; a3 sells to a1 at 230
		orderSell("a4", 130); // [NOTE 1] a4 tries to market sell at 130; warning - no one buying that high
		orderBuy("a5", 5); // a5 tries to market buy at 5; warning - no one selling that low
		orderBuy("a4", 10); // [NOTE 2] a4 tries to market buy at 10; warning - no one selling that low
		orderSell("a4", 119); // a4 sells at market 119; a4 sells to a1 at 120

		// a3 sells to a2 at 240
		// a3 sells to a1 at 230
		// a4 sells to a1 at 120

		assertEquals(getBalance("a1"), cash - 230 - 120);
		assertEquals(getCouponCount("a1"), coupons + 1 + 1);
		assertEquals(getBalance("a2"), cash - 240);
		assertEquals(getCouponCount("a2"), coupons + 1);
		assertEquals(getBalance("a3"), cash + 240 + 230);
		assertEquals(getCouponCount("a3"), coupons - 1 - 1);
		assertEquals(getBalance("a4"), cash + 120);
		assertEquals(getCouponCount("a4"), coupons - 1);
		assertEquals(getBalance("a5"), cash);
		assertEquals(getCouponCount("a5"), coupons);

		endCurrentRound();

		assertEquals(getTotalScore(currentRound, "a1"), (cash - 230 - 120) + currentDividend * (coupons + 1 + 1));
		assertEquals(getTotalScore(currentRound, "a2"), (cash - 240) + currentDividend * (coupons + 1));
		assertEquals(getTotalScore(currentRound, "a3"), (cash + 240 + 230) + currentDividend * (coupons - 1 - 1));
		assertEquals(getTotalScore(currentRound, "a4"), (cash + 120) + currentDividend * (coupons - 1));
		assertEquals(getTotalScore(currentRound, "a5"), (cash) + currentDividend * (coupons));

		startNextRound();

		currentRound = session.getCurrentRound();
		currentDividend = dividends.get(currentRound - 1);
		assertTrue(2 == currentRound);
		try {
			assertTrue(currentDividend == session.getDividend(currentRound).asValue().intValue());
		} catch (ScoreException e) {
			throw new RuntimeException(e);
		}
		assertEquals(getTotalScore(currentRound, "a1"), 0);
		assertEquals(getTotalScore(currentRound, "a2"), 0);
		assertEquals(getTotalScore(currentRound, "a3"), 0);
		assertEquals(getTotalScore(currentRound, "a4"), 0);
		assertEquals(getTotalScore(currentRound, "a5"), 0);
		assertEquals(getBalance("a1"), getTotalScore(currentRound - 1, "a1"));
		assertEquals(getCouponCount("a1"), coupons);
		assertEquals(getBalance("a2"), getTotalScore(currentRound - 1, "a2"));
		assertEquals(getCouponCount("a2"), coupons);
		assertEquals(getBalance("a3"), getTotalScore(currentRound - 1, "a3"));
		assertEquals(getCouponCount("a3"), coupons);
		assertEquals(getBalance("a4"), getTotalScore(currentRound - 1, "a4"));
		assertEquals(getCouponCount("a4"), coupons);
		assertEquals(getBalance("a5"), getTotalScore(currentRound - 1, "a5"));
		assertEquals(getCouponCount("a5"), coupons);

	}

	@Test
	public void fullCarryOverExperiment() {
		Properties props = loadProperties("ConfigurationTestCashRollover.properties");
		props.put("carryForward", "true");
		int cash = 1200;
		int coupons = 4;
		List<Integer> dividends = Arrays.asList(240, 50, 240, 50, 490);

		Session session = createSession(props);
		startNextRound();
		int currentRound = session.getCurrentRound();
		int currentDividend = dividends.get(currentRound - 1);
		orderBid("a1", 120); // a1 willing to buy at 120
		orderBid("a2", 240); // a2 willing to buy at 240
		orderAsk("a3", 300); // a3 willing to sell at 300
		orderAsk("a3", 290); // a3 willing to sell at 290
		orderAsk("a3", 100); // a3 willing to sell at 100; a3 sells to a2 at 240
		orderBid("a1", 230); // a1 willing to buy at 230
		orderSell("a3", 230); // a3 buys at market 230; a3 sells to a1 at 230
		orderSell("a4", 130); // [NOTE 1] a4 tries to market sell at 130; warning - no one buying that high
		orderBuy("a5", 5); // a5 tries to market buy at 5; warning - no one selling that low
		orderBuy("a4", 10); // [NOTE 2] a4 tries to market buy at 10; warning - no one selling that low
		orderSell("a4", 119); // a4 sells at market 119; a4 sells to a1 at 120

		// a3 sells to a2 at 240
		// a3 sells to a1 at 230
		// a4 sells to a1 at 120

		assertEquals(getBalance("a1"), cash - 230 - 120);
		assertEquals(getCouponCount("a1"), coupons + 1 + 1);
		assertEquals(getBalance("a2"), cash - 240);
		assertEquals(getCouponCount("a2"), coupons + 1);
		assertEquals(getBalance("a3"), cash + 240 + 230);
		assertEquals(getCouponCount("a3"), coupons - 1 - 1);
		assertEquals(getBalance("a4"), cash + 120);
		assertEquals(getCouponCount("a4"), coupons - 1);
		assertEquals(getBalance("a5"), cash);
		assertEquals(getCouponCount("a5"), coupons);

		endCurrentRound();

		assertEquals(getTotalScore(currentRound, "a1"), 0);
		assertEquals(getTotalScore(currentRound, "a2"), 0);
		assertEquals(getTotalScore(currentRound, "a3"), 0);
		assertEquals(getTotalScore(currentRound, "a4"), 0);
		assertEquals(getTotalScore(currentRound, "a5"), 0);

		startNextRound();

		currentRound = session.getCurrentRound();
		currentDividend = dividends.get(currentRound - 1);
		int lastRoundDividend = dividends.get(currentRound - 2);
		assertTrue(2 == currentRound);
		try {
			assertTrue(currentDividend == session.getDividend(currentRound).asValue().intValue());
		} catch (ScoreException e) {
			throw new RuntimeException(e);
		}
		assertEquals(getTotalScore(currentRound, "a1"), 0);
		assertEquals(getTotalScore(currentRound, "a2"), 0);
		assertEquals(getTotalScore(currentRound, "a3"), 0);
		assertEquals(getTotalScore(currentRound, "a4"), 0);
		assertEquals(getTotalScore(currentRound, "a5"), 0);
		assertEquals(getBalance("a1"), (cash - 230 - 120) + lastRoundDividend * (coupons + 1 + 1));
		assertEquals(getCouponCount("a1"), coupons + 1 + 1);
		assertEquals(getBalance("a2"), (cash - 240) + lastRoundDividend * (coupons + 1));
		assertEquals(getCouponCount("a2"), coupons + 1);
		assertEquals(getBalance("a3"), (cash + 240 + 230) + lastRoundDividend * (coupons - 1 - 1));
		assertEquals(getCouponCount("a3"), coupons - 1 - 1);
		assertEquals(getBalance("a4"), (cash + 120) + lastRoundDividend * (coupons - 1));
		assertEquals(getCouponCount("a4"), coupons - 1);
		assertEquals(getBalance("a5"), (cash) + lastRoundDividend * (coupons));
		assertEquals(getCouponCount("a5"), coupons);

	}

	@Test
	public void labelTest() {
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		props.put("label.participation.cash", "P Cash");
		Session session = createSession(props);
		assertEquals("P Cash", ZocaloLabels.getParticipationCash());
		props.remove("label.participation.cash");
		session = createSession(props);
		assertEquals("Participation", ZocaloLabels.getParticipationCash());
	}

	@Test
	public void participationTest() {

		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		props.put("participation.clue.cost", "-50, 75 , 0, 0, 150");

		int cash = 1200;
		int coupons = 4;
		List<Integer> dividends = Arrays.asList(240, 50, 240, 50, 490);
		List<Integer> participationCosts = Arrays.asList(200, 0, 0, 500, 600);
		List<Integer> clueCosts = Arrays.asList(-50, 75, 0, 0, 150);

		Session session = createSession(props);
		ZocaloService service = ZocaloService.getInstance();

		service.startParticipation();
		service.addParticipationChoice(1, "a1", true, true);
		service.addParticipationChoice(1, "a2", false, true);
		service.addParticipationChoice(1, "a3", true, false);
		service.addParticipationChoice(1, "a4", false, false);
		service.addParticipationChoice(1, "a5", true, true);

		startNextRound();
		int currentRound = session.getCurrentRound();
		int currentDividend = dividends.get(currentRound - 1);
		assertEquals(getBalance("a1"), cash);
		assertEquals(getBalance("a2"), cash);
		assertEquals(getBalance("a3"), cash);
		assertEquals(getBalance("a4"), cash);
		assertEquals(getBalance("a5"), cash);

		orderBid("a1", 120); // a1 willing to buy at 120
		orderBid("a2", 240); // a2 willing to buy at 240
		orderAsk("a3", 300); // a3 willing to sell at 300
		orderAsk("a3", 290); // a3 willing to sell at 290
		orderAsk("a3", 100); // a3 willing to sell at 100; a3 sells to a2 at 240
		orderBid("a1", 230); // a1 willing to buy at 230
		orderSell("a3", 230); // a3 buys at market 230; a3 sells to a1 at 230
		orderSell("a4", 130); // [NOTE 1] a4 tries to market sell at 130; warning - no one buying that high
		orderBuy("a5", 5); // a5 tries to market buy at 5; warning - no one selling that low
		orderBuy("a4", 10); // [NOTE 2] a4 tries to market buy at 10; warning - no one selling that low
		orderSell("a4", 119); // a4 sells at market 119; a4 sells to a1 at 120

		// a3 sells to a2 at 240
		// a3 sells to a1 at 230
		// a4 sells to a1 at 120

		assertEquals(getBalance("a1"), cash - 230 - 120);
		assertEquals(getBalance("a2"), cash - 240);
		assertEquals(getBalance("a3"), cash + 240 + 230);
		assertEquals(getBalance("a4"), cash + 120);
		assertEquals(getBalance("a5"), cash);

		endCurrentRound();

		assertEquals(getTotalScore(currentRound, "a1"), (cash - 230 - 120) + currentDividend * (coupons + 1 + 1) + service.getParticipationCashTotalForParticipant(currentRound, "a1"));
		assertEquals(getTotalScore(currentRound, "a2"), (cash - 240) + currentDividend * (coupons + 1) + service.getParticipationCashTotalForParticipant(currentRound, "a2"));
		assertEquals(getTotalScore(currentRound, "a3"), (cash + 240 + 230) + currentDividend * (coupons - 1 - 1) + service.getParticipationCashTotalForParticipant(currentRound, "a3"));
		assertEquals(getTotalScore(currentRound, "a4"), (cash + 120) + currentDividend * (coupons - 1) + service.getParticipationCashTotalForParticipant(currentRound, "a4"));
		assertEquals(getTotalScore(currentRound, "a5"), (cash) + currentDividend * (coupons) + service.getParticipationCashTotalForParticipant(currentRound, "a5"));

	}

}
