package jjdm.zocalo.rules;

import jjdm.zocalo.data.ChatState;
import jjdm.zocalo.data.ParticipationChoice;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.currency.Accounts;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.experiment.role.AbstractSubject;
import net.commerce.zocalo.experiment.role.Role;
import net.commerce.zocalo.experiment.role.TradingSubject;
import net.commerce.zocalo.user.User;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used to reset cash at the start of each new round.
 *
 * @author Josh Martin
 */
public class CashResetter {

	/**
	 * Singleton instance.
	 */
	private static CashResetter INSTANCE = new CashResetter();

	/**
	 * Logging utility.
	 */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Singleton constructor.
	 */
	private CashResetter() {
		super();
	}

	/**
	 * Singleton accessor.
	 *
	 * @return The cash resetter.
	 */
	public static CashResetter getInstance() {
		return INSTANCE;
	}

	/**
	 * Reset the cash in each players account at the start of each round. This is the default implementation originally
	 * coded in Zocalo.  It resets the cash to whatever is specified in the properties file.
	 *
	 * @param data The key information about the session.
	 */
	public void defaultResetCash(SessionData data) {

		Funds leftover = data.getCashBank().noFunds();
		Map<TradingSubject, Quantity> deserving = new HashMap<>();

		for (AbstractSubject subject : data.getPlayers().values()) {

			if (!(subject instanceof TradingSubject)) {
				continue;
			}

			TradingSubject trader = (TradingSubject) subject;

			trader.getUser().reduceReservesTo(Quantity.ZERO, data.getClaim().getNoPosition()); // resetting reserves

			Role role = data.getRoles().get(trader.roleName());
			Quantity initialCash = role.getInitialCash();
			Quantity balance = trader.balance();

			if (balance.compareTo(initialCash) > 0) { // more cash than they need
				Funds excess = trader.getUser().getAccounts().provideCash(balance.minus(initialCash));
				excess.transfer(leftover);

			} else if (balance.compareTo(initialCash) < 0) { // less cash than they need
				deserving.put(trader, initialCash.minus(balance));
			}

		}

		for (TradingSubject trader : deserving.keySet()) {
			Quantity cashDifference = deserving.get(trader);
			trader.getUser().getAccounts().receiveCash(leftover.provide(cashDifference));
		}

	}

	/**
	 * Deducts cash from participants based on their participation.   Note that this does NOT reset cash before
	 * deducting. The caller must first either: 1) reset cash; or, 2) carry over.  This method simply deducts money
	 * based on participation choices.
	 *
	 * @param data The key information about the session.
	 */
	public void participationResetCash(SessionData data) {

		ZocaloService service = ZocaloService.getInstance();
		ChatState state = service.getChatState();
		int currentRound = state.getCurrentRound(); // participation not turned off yet.  use current round.
		Integer costToParticipate = service.getConfiguration().getParticipationCostForRound(currentRound);
		Integer costForClue = service.getConfiguration().getParticipationClueCostForRound(currentRound);
		List<ParticipationChoice> choices = service.getParticipationChoicesForRound(currentRound);

		for (AbstractSubject subject : data.getPlayers().values()) {

			if (!(subject instanceof TradingSubject)) {
				continue;
			}

			TradingSubject trader = (TradingSubject) subject;
			Accounts account = trader.getUser().getAccounts();

			Optional<ParticipationChoice> optional = choices.stream().filter(c -> c.getParticipantId().equals(trader.getName())).findAny();
			if (optional.isPresent()) {
				ParticipationChoice choice = optional.get();
				if (choice.isParticipate() && costToParticipate != null) {
					handleCashTransfer(data.getCashBank(), account, costToParticipate);
				}
				if (choice.isReceiveClue() && costForClue != null) {
					handleCashTransfer(data.getCashBank(), account, costForClue);
				}
			} else {
				String message = String.format("There is no choice recorded for %s and round %d", trader.getName(), currentRound);
				logger.debug(message);
			}
		}
	}

	/**
	 * Takes the total score of the previous round, and converts it to the starting cash balance.
	 *
	 * @param data The key information about the session.
	 */
	public void rollOverTotalCash(SessionData data) {
		for (AbstractSubject subject : data.getPlayers().values()) {
			if (!(subject instanceof TradingSubject)) {
				continue;
			}
			TradingSubject trader = (TradingSubject) subject;
			User user = trader.getUser();
			Accounts account = user.getAccounts();
			user.reduceReservesTo(Quantity.ZERO, data.getClaim().getNoPosition()); // resetting reserves
			Quantity existingCash = trader.balance();
			Quantity endingScore = trader.getScore(data.getCurrentRound() - 1);
			int required = endingScore.minus(existingCash).asValue().intValue();
			handleCashTransfer(data.getCashBank(), account, required);
		}
	}

	/**
	 * Used to move money to/from a participant based on positive/negative amounts;
	 *
	 * @param bank    The bank to use for cash.
	 * @param account The participants cash amount.
	 * @param amount  The amount (positive or negative) to move.
	 */
	private void handleCashTransfer(CashBank bank, Accounts account, int amount) {
		boolean deductCash = amount < 0;
		Quantity quantity = new Quantity(Math.abs(amount));
		if (deductCash) {
			account.provideCash(quantity);
		} else {
			Funds funds = bank.makeFunds(quantity);
			account.receiveCash(funds);
		}
	}

}
