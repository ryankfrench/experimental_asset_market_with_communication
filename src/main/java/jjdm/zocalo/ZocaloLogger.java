package jjdm.zocalo;

import jjdm.zocalo.data.ChatMessage;
import jjdm.zocalo.data.ParticipationChoice;
import net.commerce.zocalo.logging.GID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Used to get entries into the experiment logging file.
 */
public class ZocaloLogger {

	/**
	 * This class logs to the session-based log file, not the normal JJDM.
	 */
	private static Logger logger = Logger.getLogger("ZocaloLogger");

	/**
	 * Log a message that someone was blocked/unblocked.
	 *
	 * @param round     The current round.
	 * @param user      The participant taking the action.
	 * @param affected  The participant the action (block/unblock) is taken upon.
	 * @param isBlocked Is the action to block (false is to unblock).
	 */
	public static void logChatBlock(int round, String user, String affected, boolean isBlocked) {
		StringBuilder builder = new StringBuilder();
		builder.append(isBlocked ? "CHAT_BLOCK:" : "CHAT_UNBLOCK:");
		builder.append(" USER=" + user);
		builder.append(" AFFECTED=" + affected);
		logInfo(builder.toString());
	}

	/**
	 * Log a message that a chat message was sent.
	 *
	 * @param round   The current round.
	 * @param message The message to log.
	 */
	public static void logChatMessage(int round, ChatMessage message) {
		StringBuilder builder = new StringBuilder();
		builder.append("CHAT_MESSAGE:");
		builder.append(" FROM=" + message.getSenderId());
		builder.append(" TO=" + message.getRecipientIds());
		builder.append(" MSG=" + message.getMessage());
		logInfo(builder.toString());
	}

	/**
	 * For each participant, log their choice/color map.
	 *
	 * @param colorMap The [Participant: [Choice: Color]] map.
	 */
	public static void logColorAssignment(Map<String, Map<String, String>> colorMap) {
		if (colorMap == null) {
			return;
		}
		for (String id : colorMap.keySet()) {
			logInfo("CHAT_COLOR_ASSIGNMENT FOR " + id + ": " + colorMap.get(id).toString());
		}

	}

	/**
	 * Shortcut to log an error message.
	 *
	 * @param message The message to log.
	 */
	public static void logError(String message) {
		log(message, Level.ERROR);
	}

	/**
	 * Shortcut to log an info message.  The most common for log file generation.
	 *
	 * @param message The message to log.
	 */
	public static void logInfo(String message) {
		log(message, Level.INFO);
	}

	/**
	 * Explicitly logs the start of a new round.
	 *
	 * @param round
	 */
	public static void logNewRound(int round) {
		StringBuilder builder = new StringBuilder();
		builder.append("NEW_ROUND_STARTED:");
		builder.append(" ROUND=" + round);
		logInfo(builder.toString());
	}

	/**
	 * Log the clues for each participant for the round.
	 *
	 * @param roundId The round the clues were generated.
	 * @param id      The participant's ID.
	 * @param clues   The clues (e.g. X or X,Y) for the participant for the round.
	 */
	public static void logParticipantClues(int roundId, String id, List<String> clues) {
		StringBuilder builder = new StringBuilder();
		builder.append("CLUE_ASSIGNMENT:");
		builder.append(" ROUND=" + roundId);
		builder.append(" ID=" + id);
		builder.append(" CLUE=" + String.join(";", clues));
		logInfo(builder.toString());
	}

	/**
	 * Log the choice of a participant to participate and receive a clue.
	 *
	 * @param choice        The choices.
	 * @param resultingCash The amount of cash remaining after the choice.
	 * @param freeClues     The number of free clues the participant received this round.
	 */
	public static void logParticipationChoice(ParticipationChoice choice, int resultingCash, int freeClues) {
		StringBuilder builder = new StringBuilder();
		builder.append("PARTICIPATION_CHOICE:");
		builder.append(" ROUND=" + choice.getRoundId());
		builder.append(" ID=" + choice.getParticipantId());
		builder.append(" PARTICIPATE=" + choice.isParticipate());
		builder.append(" CLUE=" + choice.isReceiveClue());
		builder.append(" ADDITIONAL_CLUES=" + choice.getAdditionalClues());
		builder.append(" FREE_CLUES=" + freeClues);
		builder.append(" PARTICIPATION_CASH=" + resultingCash);
		logInfo(builder.toString());
	}

	/**
	 * Logs out a message in the following format:
	 * <p>
	 * a5 dividend: 50 dividendPaid: 1400 score: 1700 coupons: 4
	 *
	 * @param userId               The user ID
	 * @param cash                 Cash remaining for the user.
	 * @param couponValueThisRound The value of a coupon/dividend this round.
	 * @param couponCount          The number of coupons the user had.
	 * @param totalCouponValue     The value * the number of coupons.
	 * @param participationCash    Participation cash (or null).
	 * @param totalScore           The total score for the round.
	 */
	public static void logRoundResult(String userId, Integer cash, Integer couponValueThisRound, Integer couponCount, Integer totalCouponValue, Integer participationCash, Integer totalScore) {
		StringBuilder builder = new StringBuilder();
		builder.append("ROUND_SCORE_RESULT:");
		builder.append(" USER=" + userId);
		builder.append(" CASH=" + cash);
		builder.append(" COUPON_VALUE_FOR_ROUND=" + couponValueThisRound);
		builder.append(" COUPON_COUNT=" + couponCount);
		builder.append(" TOTAL_COUPON_VALUE=" + totalCouponValue);
		builder.append(" PARTICIPATION_CASH=" + participationCash);
		builder.append(" TOTAL_SCORE=" + totalScore);
		logInfo(builder.toString());
	}

	/**
	 * Log a message to the Zocalo log file.
	 *
	 * @param message The message to log (will be stripped of s+.
	 * @param level   The level to log at in the file.
	 */
	private static void log(String message, Level level) {
		String cleaned = GID.log() + message.replaceAll("\\s+", " ");
		if (Level.TRACE.equals(level)) {
			logger.trace(cleaned);
		} else if (Level.DEBUG.equals(level)) {
			logger.debug(cleaned);
		} else if (Level.WARN.equals(level)) {
			logger.warn(cleaned);
		} else if (Level.ERROR.equals(level)) {
			logger.error(cleaned);
		} else if (Level.FATAL.equals(level)) {
			logger.fatal(cleaned);
		} else {
			logger.info(cleaned);
		}
	}
}
