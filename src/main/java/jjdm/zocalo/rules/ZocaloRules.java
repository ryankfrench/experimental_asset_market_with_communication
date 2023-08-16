package jjdm.zocalo.rules;

import jjdm.zocalo.data.ZocaloConfig;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.ScoreException;
import org.apache.log4j.Logger;

/**
 * Extracts processing logic from Zocalo and allows subclassing based on configuration.
 */
public class ZocaloRules {

	private ZocaloConfig configuration;
	private Logger logger = Logger.getLogger(this.getClass());
	private CashResetter cashResetter;
	private CouponResetter couponResetter;
	private RoundScorer roundScorer;

	// JJDM - Keep this in case the option is used in the future.
	private final boolean useMainCashForParticipation = false;

	/**
	 * Hidden constructor.
	 */
	public ZocaloRules(ZocaloConfig configuration) {
		super();
		this.configuration = configuration;
		this.cashResetter = CashResetter.getInstance();
		this.couponResetter = CouponResetter.getInstance();
		this.roundScorer = RoundScorer.getInstance();
	}

	/**
	 * Reset the cash at the beginning of a round, based on configuration.
	 *
	 * @param data The exposed session data.
	 */
	public void resetCash(SessionData data) {

		if (data.getCurrentRound() <= 1) {
			this.cashResetter.defaultResetCash(data);
		} else if (this.configuration.isCarryForwardEverything()) {
			logger.debug("Not resetting cash for round: " + data.getCurrentRound());
		} else if (this.configuration.isCarryForwardRolloverCash()) {
			this.cashResetter.rollOverTotalCash(data);
		} else {
			this.cashResetter.defaultResetCash(data);
		}

		if (this.configuration.isParticipationEnabled() && useMainCashForParticipation) {
			this.cashResetter.participationResetCash(data);
		}
	}

	/**
	 * Reset the coupons at the beginning of a round, based on configuration.
	 *
	 * @param data The exposed session data.
	 */
	public void resetCoupons(SessionData data) {
		if (data.getCurrentRound() <= 1) {
			this.couponResetter.defaultResetCoupons(data);
		} else if (this.configuration.isCarryForwardEverything()) {
			logger.debug("Not resetting coupons for round: " + data.getCurrentRound());
		} else {
			this.couponResetter.defaultResetCoupons(data);
		}
	}

	/**
	 * Score the round for each participant, based on configuration.
	 *
	 * @param data          The key information about the session.
	 * @param average       Unsure about this.
	 * @param judged        Unsure about this.
	 * @param judgingTarget Unsure about this.
	 * @param outcome       Unsure about this.
	 * @throws net.commerce.zocalo.experiment.ScoreException
	 */
	public void scoreRound(SessionData data, Price average, Quantity judged, Quantity judgingTarget, String outcome) throws ScoreException {
		if (this.configuration.isCarryForwardRolloverCash()) {
			this.roundScorer.cashOnlyScoreRound(data, outcome);
		} else {
			this.roundScorer.defaultScoreRound(data, average, judged, judgingTarget, outcome);
		}
		if (this.configuration.isParticipationEnabled() && !useMainCashForParticipation) {
			this.roundScorer.addParticipationCash(data, outcome);
		}
		this.roundScorer.logRoundResults(data);
	}

}
