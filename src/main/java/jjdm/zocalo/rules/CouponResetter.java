package jjdm.zocalo.rules;

import jjdm.zocalo.ZocaloLogger;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Coupons;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.Role;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;

import java.util.Iterator;

/**
 * Used to reset coupons after each round.
 *
 * @author Josh Martin
 */
public class CouponResetter {

	/**
	 * Singleton instance.
	 */
	private static CouponResetter INSTANCE = new CouponResetter();

	/**
	 * Logging utility.
	 */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Singleton constructor.
	 */
	private CouponResetter() {
		super();
	}

	/**
	 * Singleton accessor.
	 *
	 * @return The cash resetter.
	 */
	public static CouponResetter getInstance() {
		return INSTANCE;
	}

	/**
	 * Default coupon resetter that resets coupons at the beginning of a round.
	 *
	 * @param data The key information about the session.
	 */
	public void defaultResetCoupons(SessionData data) {
		for (Iterator iterator = data.getPlayers().values().iterator(); iterator.hasNext(); ) {
			AbstractSubject subject = (AbstractSubject) iterator.next();
			Role role = ((Role) data.getRoles().get(subject.roleName()));
			Quantity initialCouponValue = role.getInitialCoupons();
			Quantity current = subject.currentCouponCount(data.getClaim());
			if (!current.eq(initialCouponValue)) {
				TradingSubject holder = null;
				try {
					holder = (TradingSubject) subject;
				} catch (ClassCastException e) {
					ZocaloLogger.logError("Initialization problem: subject has initial account value, but is not eligible to trade.");
				}
				Position pos;
				Quantity quantity;
				if (initialCouponValue.compareTo(current) > 0) {
					pos = data.getClaim().getYesPosition();
					quantity = initialCouponValue.minus(current);
				} else {
					pos = data.getClaim().getNoPosition();
					quantity = current.minus(initialCouponValue);
				}
				Coupons coupons = data.getCouponBank().issueUnpairedCoupons(quantity, pos);
				User user = holder.getUser();
				user.endow(coupons);
				user.settle(data.getMarket());
			}
		}
	}

}