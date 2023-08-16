package jjdm.zocalo.rules;

import jjdm.zocalo.ZocaloLogger;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.ScoreException;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Properties;

/**
 * Used to calculate scores after each round.
 *
 * @author Josh Martin
 */
public class RoundScorer {

	/**
	 * Singleton instance.
	 */
	private static RoundScorer INSTANCE = new RoundScorer();
	/**
	 * Setting to true makes the score output on the screen include coupons in the total score.  If this is false, then
	 * only the coupon value is shown.
	 *
	 * @see TradingSubject#assetValueTable(Properties, String, boolean, boolean)
	 */
	private static boolean KEEPING_SCORE = true;
	/**
	 * Used for generating score "explanation" below.  There is no effect whether this is true or false since the
	 * "actualValueLabel" is used if it is present in the uploaded properties file.
	 *
	 * @see TradingSubject#recordActualValueExplanations(Properties, boolean)
	 */
	private static boolean IS_CARRY_FORWARD = false;
	/**
	 * Logging utility.
	 */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Singleton constructor.
	 */
	private RoundScorer() {
		super();
	}

	/**
	 * Singleton accessor.
	 *
	 * @return The cash resetter.
	 */
	public static RoundScorer getInstance() {
		return INSTANCE;
	}

	/**
	 * Used to add the participation cash for the round into each players total score. Note that this does NOT score a
	 * round, and that the caller must call  one of the other scoring methods must be called prior. This method simply
	 * updates the total score with the participation cash.
	 *
	 * @param data    The key information about the session.
	 * @param outcome Unsure about this.
	 */
	public void addParticipationCash(SessionData data, String outcome) {
		ZocaloService service = ZocaloService.getInstance();
		for (Iterator iterator = data.getPlayers().values().iterator(); iterator.hasNext(); ) {
			AbstractSubject subject = (AbstractSubject) iterator.next();
			if (!(subject instanceof TradingSubject)) {
				continue;
			}
			TradingSubject trader = (TradingSubject) subject;
			BigDecimal totalScore = trader.getScore(data.getCurrentRound()).asValue();
			String userId = subject.getName();
			BigDecimal participationCash = new BigDecimal(service.getParticipationCashForParticipant(data.getCurrentRound(), userId));
			Quantity newTotalScore = new Quantity(totalScore.add(participationCash));
			trader.addScoreComponent(TradingSubject.ScoreComponent, newTotalScore);
			trader.setScore(data.getCurrentRound(), newTotalScore);
			trader.recordExplanation(data.getProperties(), KEEPING_SCORE, IS_CARRY_FORWARD, outcome);
		}
	}

	/**
	 * Simplification of the logic in defaultScoreRound. Omits any check of isCarryOver.
	 *
	 * @param data    The key information about the session.
	 * @param outcome Unsure about this.
	 * @throws net.commerce.zocalo.experiment.ScoreException
	 * @see this#defaultScoreRound(SessionData, Price, Quantity, Quantity, String)
	 */
	public void cashOnlyScoreRound(SessionData data, String outcome) throws ScoreException {
		for (Iterator iterator = data.getPlayers().values().iterator(); iterator.hasNext(); ) {

			AbstractSubject subject = (AbstractSubject) iterator.next();
			if (!(subject instanceof TradingSubject)) {
				continue;
			}
			TradingSubject trader = (TradingSubject) subject;
			User user = trader.getUser();

			Quantity cash = user.cashOnHand();

			Quantity reserves = user.reserveBalance(data.getClaim().getNoPosition());
			if (reserves != null && !reserves.isZero()) {
				cash = cash.plus(reserves);
			}

			Quantity couponCount = trader.currentCouponCount(data.getClaim());
			Quantity couponValueThisRound = data.getSession().getDividend(subject, data.getCurrentRound());
			Quantity totalCouponValue = couponCount.times(couponValueThisRound);
			Quantity totalScore = cash.plus(totalCouponValue);

			// these score components are needed by the "recordExplanation" generator below.
			trader.addScoreComponent(TradingSubject.BalanceComponent, cash);
			trader.addScoreComponent(TradingSubject.AssetsComponent, couponCount);
			trader.addScoreComponent(TradingSubject.PublicDividendComponent, couponValueThisRound);
			trader.addScoreComponent(TradingSubject.TotalDividendComponent, totalCouponValue);
			trader.addScoreComponent(TradingSubject.ScoreComponent, totalScore);
			trader.setScore(data.getCurrentRound(), totalScore);

			trader.recordExplanation(data.getProperties(), KEEPING_SCORE, IS_CARRY_FORWARD, outcome);
		}
	}

	/**
	 * Default logic used by Zocalo to score participants at the end of each round.
	 *
	 * @param data          The key information about the session.
	 * @param average       Unsure about this.
	 * @param judged        Unsure about this.
	 * @param judgingTarget Unsure about this.
	 * @param outcome       Unsure about this.
	 * @throws net.commerce.zocalo.experiment.ScoreException
	 */
	public void defaultScoreRound(SessionData data, Price average, Quantity judged, Quantity judgingTarget, String outcome) throws ScoreException {
		for (Iterator iterator = data.getPlayers().values().iterator(); iterator.hasNext(); ) {
			AbstractSubject player = (AbstractSubject) iterator.next();
			if (player.isDormant(data.getCurrentRound())) {
				player.recordDormantInfo(judged, outcome, data.getProperties());
				continue;
			}
			Quantity multiplier = data.getSession().getMultiplier(player);
			Quantity total = player.totalDividend(data.getClaim(), data.getSession(), data.getCurrentRound());

			boolean keepingScore = data.getSession().lastRound() || !data.getSession().isCarryForward();
			if (keepingScore) {
				player.rememberHoldings(data.getClaim());
				player.rememberAssets(data.getClaim());
				player.recordBonusInfo(average, judged, judgingTarget, total, data.getCurrentRound(), data.getProperties());
				player.recordScore(data.getCurrentRound(), multiplier, data.getProperties());
			} else { // Carry Forward assets and cash; add dividends to balance
				player.rememberAssets(data.getClaim());
				Quantity bonus = player.recordBonusInfo(average, judged, judgingTarget, total, data.getCurrentRound(), data.getProperties());
				bonus = bonus.times(multiplier);
				if (bonus.isPositive()) {
					player.addBonus(data.getCashBank().makeFunds(bonus));
				}
				if (total.isNegative()) {
					Quantity coupons = player.currentCouponCount(data.getClaim());
					Funds returned = player.payDividend((total.div(coupons)), coupons.negate(), data.getClaim().getNoPosition());
					if (returned != null) {
						Funds sequestered = returned.provide(total);// remove it from the user's purse
						if (sequestered.getBalance().compareTo(total) < 0) {
							ZocaloLogger.logInfo("Player " + player.getName() + " didn't pay a required dividend in round " + data.getCurrentRound());
						}
					}
				} else {
					player.receiveDividend(data.getCashBank().makeFunds(total));
				}
				Quantity perShare = data.getSession().getRemainingDividend(player, data.getCurrentRound() + 1);
				player.reduceReservesTo(perShare, data.getClaim().getNoPosition());
			}
			player.recordExplanation(data.getProperties(), keepingScore, data.getSession().isCarryForward(), outcome);
		}
	}

	/**
	 * This was originally called in Trader recordExplanation, but was moved here to fix a duplication issue since all
	 * other methods in this class call recordExplanation.  It also ensures that all logic is completed before writing
	 * out the score.
	 *
	 * @param data The session data.
	 * @see net.commerce.zocalo.experiment.role.Trader#recordExplanation(java.util.Properties, boolean, boolean, String)
	 * @see jjdm.zocalo.ZocaloLogger#logRoundResult(String, int, int, int, int, Integer, int)
	 */
	public void logRoundResults(SessionData data) {
		ZocaloService service = ZocaloService.getInstance();
		for (Iterator iterator = data.getPlayers().values().iterator(); iterator.hasNext(); ) {
			AbstractSubject subject = (AbstractSubject) iterator.next();
			if (!(subject instanceof TradingSubject)) {
				continue;
			}
			TradingSubject trader = (TradingSubject) subject;
			String userId = subject.getName();
			Integer cash = quantityToInteger(trader.getScoreComponent(TradingSubject.BalanceComponent));
			Integer couponCount = quantityToInteger(trader.getScoreComponent(TradingSubject.AssetsComponent));
			Integer couponValueThisRound = quantityToInteger(trader.getScoreComponent(TradingSubject.PublicDividendComponent));
			Integer totalCouponValue = quantityToInteger(trader.getScoreComponent(TradingSubject.TotalDividendComponent));
			Integer totalScore = quantityToInteger(trader.getScoreComponent(TradingSubject.ScoreComponent));
			Integer participationCash = null;
			if (service.getConfiguration().isParticipationEnabled()) {
				participationCash = service.getParticipationCashForParticipant(data.getCurrentRound(), userId);
			}
			ZocaloLogger.logRoundResult(userId, cash, couponValueThisRound, couponCount, totalCouponValue, participationCash, totalScore);
		}

	}

	/**
	 * Check to ensure quantity is not null, then return the int value.
	 *
	 * @param q The quantity.
	 * @return The int value or null.
	 */
	private Integer quantityToInteger(Quantity q) {
		return q == null ? null : q.asValue().intValue();
	}
}
