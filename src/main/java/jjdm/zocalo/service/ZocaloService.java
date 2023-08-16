package jjdm.zocalo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jjdm.zocalo.CometHelper;
import jjdm.zocalo.WebSocketConnector;
import jjdm.zocalo.ZocaloLogger;
import jjdm.zocalo.data.ChatMessage;
import jjdm.zocalo.data.ChatReputationScore;
import jjdm.zocalo.data.ChatRound;
import jjdm.zocalo.data.ChatState;
import jjdm.zocalo.data.ParticipationChoice;
import jjdm.zocalo.data.ZocaloConfig;
import jjdm.zocalo.data.ZocaloDatabase;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.experiment.role.TradingSubject;
import org.apache.log4j.Logger;

/**
 * This service is the client-facing interface to the ZocaloDatabase, ZocaloConfig, and Comet Helper. Any business-logic
 * should reside in this service, rather than in lower level classes.
 *
 * @author Josh Martin
 * @see jjdm.zocalo.data.ZocaloDatabase
 * @see jjdm.zocalo.data.ZocaloConfig
 * @see CometHelper
 */
public class ZocaloService {

	private static ZocaloService INSTANCE = new ZocaloService();

	/**
	 * Singleton accessor.
	 *
	 * @return The chat service.
	 */
	public static ZocaloService getInstance() {
		return INSTANCE;
	}

	/**
	 * Resets database, clears the participation flag, and clears "unique" web socket messages.
	 *
	 * @see jjdm.zocalo.data.ZocaloDatabase#newInstance(Properties) (java.util.Properties)
	 */
	public static void resetService() {

		// clear out the database
		INSTANCE.database = null;
		ZocaloDatabase.resetInstance();

		// reset participation
		INSTANCE.stopParticipation();

		// reset "spam" prevention
		WebSocketConnector.resetUniqueMessages();
	}

	/**
	 * Resets internal counters and logs out session-based configuration.
	 *
	 * @param properties The properties (from the uploaded file) for the session.
	 * @see #resetService() For the method that clears out configuration.
	 * @see jjdm.zocalo.data.ZocaloDatabase#newInstance(Properties) (java.util.Properties)
	 */
	public static ZocaloService startNewSession(Properties properties) {

		// clear out the service
		resetService();

		// reset the database and get the new configuration
		INSTANCE.database = ZocaloDatabase.newInstance(properties);
		ZocaloConfig config = INSTANCE.getConfiguration();

		// log configuration colors if present
		if (config.isChoiceColorsEnabled()) {
			ZocaloLogger.logColorAssignment(INSTANCE.getConfiguration().getChoiceColorMap());
		}

		CometHelper.publishChatActiveChange(false, false);
		CometHelper.publishTradingActiveChange(false, false);
		CometHelper.publishParticipationChange(INSTANCE.getChatState());

		return INSTANCE;
	}
	private ZocaloDatabase database;
	private Logger logger = Logger.getLogger(this.getClass());
	private boolean participationRunning = false;

	/**
	 * Singleton constructor.
	 */
	private ZocaloService() {
		super();
	}

	/**
	 * Add a message in the current round, and publishes the message out to the screens.
	 *
	 * @param roundId The round of the message.
	 * @param senderId Who sent id (ID not display).
	 * @param recipientIds Who are the recipients (ID not display).
	 * @param message The message text.
	 * @return The message.
	 * @see jjdm.zocalo.data.ZocaloDatabase#addMessage(int, String, List, String)
	 * @see CometHelper#publishChat(ChatMessage)
	 */
	public ChatMessage addMessage(int roundId, String senderId, List<String> recipientIds, String message) {
		ChatMessage chatMessage = this.database.addMessage(roundId, senderId, recipientIds, message);
		CometHelper.publishChat(chatMessage);
		ZocaloLogger.logChatMessage(roundId, chatMessage);
		return chatMessage;
	}

	/**
	 * Add a selection for the given participant to participate and/or receive a clue. Publishes and logs.
	 *
	 * @param nextRoundId The upcoming round.
	 * @param participantId The participant.
	 * @param participate Whether they choose to participate.
	 * @param receiveClue Whether they choose to receive a clue.
	 * @return The Participation choices.
	 */
	public ParticipationChoice addParticipationChoice(int nextRoundId, String participantId, boolean participate, boolean receiveClue) {
		ParticipationChoice choice = this.database.addParticipationChoice(nextRoundId, participantId, participate, receiveClue);
		CometHelper.publishParticipationChoice(choice);
		int resultingCash = this.getParticipationCashForParticipant(nextRoundId, participantId);
		ZocaloLogger.logParticipationChoice(choice, resultingCash, 0);
		return choice;
	}

	/**
	 * Add a selection for the given participant to participate and/or the number of clues they receive.
	 *
	 * @param nextRoundId The upcoming round.
	 * @param participantId The participant.
	 * @param participate Whether they choose to participate.
	 * @param additionalClues The number of additional clues purchased.
	 * @return The Participation choices.
	 */
	public ParticipationChoice addParticipationMultiClueChoice(int nextRoundId, String participantId, boolean participate, int additionalClues) {
		ParticipationChoice choice = this.database.addParticipationMultiClueChoice(nextRoundId, participantId, participate, additionalClues);
		CometHelper.publishParticipationChoice(choice);
		int resultingCash = this.getParticipationCashForParticipant(nextRoundId, participantId);
		int freeClues = this.getConfiguration().getParticipationSelectStartingForRound(nextRoundId);
		ZocaloLogger.logParticipationChoice(choice, resultingCash, freeClues);
		return choice;
	}

	/**
	 * Used to fire off events stating that the round has ended.
	 */
	public void endRound() {
		CometHelper.publishChatActiveChange(false, false);
		CometHelper.publishTradingActiveChange(false, false);
		this.stopParticipation(); // should be off, but just in case
		if (this.getConfiguration().isParticipationEnabled()) {
			CometHelper.publishParticipationChange(this.getChatState());
			CometHelper.publishParticipationBrowserShutdown();
		}
		logger.debug("Round ended.");
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#generateParticipantCluesForRound(int)}
	 */
	public Map<String, List<String>> generateParticipantCluesForRound(int roundId) {
		Map<String, List<String>> participantCluesForRound = this.database.generateParticipantCluesForRound(roundId);
		for (String id : participantCluesForRound.keySet()) {
			List<String> clues = participantCluesForRound.get(id);
			ZocaloLogger.logParticipantClues(roundId, id, clues);
		}
		return participantCluesForRound;
	}

	/**
	 * Generates a Freemarker-based page, passing in certain parameters.
	 *
	 * @param userId The user calling the page.
	 * @param templateName The name of the template.
	 * @return
	 */
	public String generateParticipationPage(String userId, String templateName) {
		Map<String, Object> values = new HashMap<>();
		Session session = SessionSingleton.getSession();
		TradingSubject trader = (TradingSubject) session.getPlayer(userId);
		ChatState chatState = this.getChatState();
		int round = chatState.getParticipationRound();
		// TODO JJDM The participation choice will not be set yet - will always be the starting balance.
		int participationBalance = this.getParticipationCashForParticipant(round, userId);
		values.put("chatState", chatState);
		values.put("trader", trader);
		values.put("balance", participationBalance);
		values.put("config", this.getConfiguration());
		return this.getConfiguration().freemarker(templateName, values);
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getAllMessages(int)}
	 */
	public List<ChatMessage> getAllMessages(int roundId) {
		return this.database.getAllMessages(roundId);
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getAutomaticCluesForRound(int)}
	 */
	public Map<String, List<String>> getAutomaticCluesForRound(int roundId) {
		return this.database.getAutomaticCluesForRound(roundId);
	}

	/**
	 * Returns the current state of trading and chat.
	 *
	 * @return The chat state.
	 */
	public ChatState getChatState() {

		ChatState state = new ChatState();
		Session session = SessionSingleton.getSession();
		ZocaloConfig zocaloConfig = this.getConfiguration();

		if (session == null) {
			return state;
		}

		int roundTimeInSeconds = session.timeLimit();
		int roundTimeRemainingInSeconds = (int) session.rawTimeRemaining() / 1000;
		int roundTimeElapsedInSeconds = roundTimeInSeconds - roundTimeRemainingInSeconds;

		boolean isChatEnabled = zocaloConfig.isChatEnabled();
		int currentRound = session.getCurrentRound();
		boolean isRoundActive = session.getMarket().isOpen();

		state.setCurrentRound(currentRound);
		state.setExperimentStarted(currentRound != 0);
		state.setRoundRunning(isRoundActive);
		state.setBetweenRounds(state.isExperimentStarted() && !state.isRoundRunning());
		state.setRoundTime(roundTimeInSeconds);
		state.setRoundSecondsRemaining(roundTimeRemainingInSeconds);
		state.setRoundSecondsElapsed(roundTimeElapsedInSeconds);
		state.setChatEnabled(isChatEnabled);
		state.setChatBlockEnabled(zocaloConfig.isChatBlockEnabled());
		state.setChatBlockShowReputation(zocaloConfig.isChatBlockShowReputation());
		state.setMaxPrice(Integer.valueOf(session.getMarket().maxPrice().toString()));
		state.setParticipationEnabled(zocaloConfig.isParticipationEnabled());
		state.setParticipationRunning(this.isParticipationRunning());

		if (state.isRoundRunning()) {
			state.setTradingPaused(zocaloConfig.isTradingPaused(currentRound, roundTimeElapsedInSeconds));
		}

		if (state.isChatEnabled() && state.isExperimentStarted()) {
			state.setChatActive(zocaloConfig.isChatOn(currentRound, roundTimeElapsedInSeconds));
		}

		if (state.isRoundRunning() && state.isChatActive()) {
			state.setChatSecondsRemaining(zocaloConfig.getSecondsRemainingInChat(currentRound, roundTimeElapsedInSeconds));
		}

		if (state.getChatSecondsRemaining() <= 0) {
			state.setChatActive(false);
		}

		if ((state.getCurrentRound() == zocaloConfig.getRounds()) && !state.isRoundRunning()) {
			state.setExperimentEnded(true);
		}

		if (state.isParticipationEnabled()) {
			int participationRound = state.isParticipationRunning() ? (currentRound + 1) : currentRound;
			if (participationRound > 0) { // nothing there at the start
				Integer participationCost = zocaloConfig.getParticipationCostForRound(participationRound);
				Integer clueCost = zocaloConfig.getParticipationClueCostForRound(participationRound);
				Integer participationCash = zocaloConfig.getParticipationAccountAmountForRound(participationRound);
				state.setParticipationRound(participationRound);
				state.setRoundParticipationCost(participationCost);
				state.setRoundClueCost(clueCost);
				state.setRoundParticipationCash(participationCash);
				state.setClueDistributionMultiClue(zocaloConfig.isClueDistributionMultiClue());
				if (zocaloConfig.isClueDistributionMultiClue()) {
					state.setMultiClueInitialCount(zocaloConfig.getParticipationSelectStartingForRound(participationRound));
					state.setMultiClueMaximumCount(zocaloConfig.getParticipationSelectMaximumForRound(participationRound));
				}
			}
		}

		return state;
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getConfiguration()}
	 */
	public ZocaloConfig getConfiguration() {
		return this.database.getConfiguration();
	}

	/**
	 * Return a list of connected desktops by user ID (e.g. a1, a2).
	 *
	 * @return The list of connected desktops or an empty list if none open.
	 * @see WebSocketConnector#getConnectedDesktopClients()
	 */
	public List<String> getConnectedDesktops() {
		List<String> desktops = WebSocketConnector.getConnectedDesktopClients();
		return (desktops == null || desktops.isEmpty()) ? new ArrayList<>() : desktops;
	}

	/**
	 * Filters all messages for the particular round and recipient ID.
	 *
	 * @param roundId The round of the message.
	 * @param userId The person who receives the message.
	 * @return All messages sent to this recipient for this round.
	 * @see jjdm.zocalo.data.ZocaloDatabase#getAllMessages(int)
	 */
	public List<ChatMessage> getMessages(int roundId, String userId) {
		List<ChatMessage> allMessages = this.getAllMessages(roundId);
		return allMessages.stream()
				.filter(m -> (m.getRecipientIds().contains(userId) || m.getSenderId().equals(userId)))
				.collect(Collectors.toList());
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getMutedSenders(int, String)}
	 */
	public List<String> getMutedSenders(int roundId, String participantId) {
		return this.database.getMutedSenders(roundId, participantId);
	}

	/**
	 * Calculates the ending participation cash balance based on the participants choices for a given round.
	 *
	 * @param round The round to lookup.
	 * @param userId The participant.
	 * @return The resulting participation balance based on the participants choices.
	 */
	public int getParticipationCashForParticipant(int round, String userId) {
		Integer startingBalance = this.getParticipationCashForRound(round);
		int result = startingBalance == null ? 0 : startingBalance.intValue();
		ParticipationChoice choice = this.getParticipationChoiceForRoundAndUser(round, userId);
		ZocaloConfig config = this.getConfiguration();
		if (choice == null) {
			return result;
		}
		if (choice.isParticipate()) {
			Integer participationCost = config.getParticipationCostForRound(round);
			if (participationCost != null) {
				result += participationCost;
			}
		}
		if (choice.isReceiveClue()) {
			Integer clueCost = this.getConfiguration().getParticipationClueCostForRound(round);
			if (clueCost != null) {
				if (config.isClueDistributionMultiClue()) {
					result += choice.getAdditionalClues() * clueCost; // JJDM Per Mark on 20160122 Initial clues are free.
				} else {
					result += clueCost;
				}
			}
		}
		return result;
	}

	/**
	 * Retrieve the starting participation cash balance for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The starting participation account balance.
	 */
	public Integer getParticipationCashForRound(int round) {
		return this.getConfiguration().getParticipationAccountAmountForRound(round);
	}

	/**
	 * Returns the total participation cash received up to and including each round. This method is meant to be used on
	 * the experimenter page where the rolling total is displayed per round. This method will allow experimenters to see
	 * how much of that cumulative total comes from participation cash.
	 * <p>
	 * Note the the list per user is zero-based (List.get(0) would be cash for Round 1).
	 *
	 * @param round The maximum round to return (should be current round - 1).
	 * @return Map of User ID : Total Cash per Round
	 */
	public Map<String, List<Integer>> getParticipationCashPerUserForRounds(int round) {
		Map<String, List<Integer>> results = new HashMap<>();
		List<String> users = this.getConfiguration().getParticipants();
		if (users == null) {
			throw new IllegalStateException("There are no configured participants.");
		}
		for (String user : users) {
			List<Integer> cashTotalPerRound = new ArrayList<>();
			for (int r = 1; r <= round; r++) {
				int roundCash = this.getParticipationCashForParticipant(r, user);
				cashTotalPerRound.add(roundCash);
			}
			results.put(user, cashTotalPerRound);
		}
		return results;
	}

	/**
	 * Calculate the cumulative earnings for participation for all rounds up to, and including, the passed round.
	 *
	 * @param round The maximum round to lookup (inclusive stopping point).
	 * @param userId The participant.
	 * @return The total cash received up until, and including, the given round.
	 */
	public int getParticipationCashTotalForParticipant(int round, String userId) {
		int result = 0;
		for (int r = 1; r <= round; r++) {
			result += this.getParticipationCashForParticipant(r, userId);
		}
		return result;
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getParticipationChoiceForRoundAndUser(int, String)}
	 */
	public ParticipationChoice getParticipationChoiceForRoundAndUser(int roundId, String userId) {
		return this.database.getParticipationChoiceForRoundAndUser(roundId, userId);
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getParticipationChoicesForRound(int)} }
	 */
	public List<ParticipationChoice> getParticipationChoicesForRound(int roundId) {
		return this.database.getParticipationChoicesForRound(roundId);
	}

	/**
	 * Returns a reputation score for each participant in the current round.
	 *
	 * @param roundId The current round ID.
	 * @return A list of reputation scores.
	 */
	public List<ChatReputationScore> getReputationScores(int roundId) {

		ArrayList scores = new ArrayList<ChatReputationScore>();
		TreeMap<String, String> displayIds = this.getConfiguration().getDisplayIdsForRound(roundId);
		Map<String, Set<String>> muted = this.database.getAllMutedSenders(roundId);
		BigDecimal perfectScore = BigDecimal.valueOf(displayIds.size() - 1);

		for (String id : displayIds.values()) {
			ChatReputationScore score = new ChatReputationScore();
			score.setId(id);
			score.setDisplayId(displayIds.get(id));
			long mutedCount = muted.values().stream().filter(set -> (set.contains(id))).count();
			BigDecimal mutedTotal = BigDecimal.valueOf(mutedCount);
			BigDecimal numerator = perfectScore.subtract(mutedTotal);
			BigDecimal result = numerator.divide(perfectScore, 2, BigDecimal.ROUND_HALF_UP);
			result = result.multiply(BigDecimal.valueOf(100));
			score.setScore(result.intValue());
			scores.add(score);
		}

		return scores;

	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#getRound(int)}
	 */
	public ChatRound getRound(int roundId) {
		return this.database.getRound(roundId);
	}

	/**
	 * Is Participation currently running.
	 *
	 * @return True if running.
	 */
	public boolean isParticipationRunning() {
		return participationRunning;
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#muteSender(int, String, String)}
	 */
	public List<String> muteSender(int roundId, String participantId, String senderId) {
		List<String> muted = this.database.muteSender(roundId, participantId, senderId);
		ZocaloLogger.logChatBlock(roundId, participantId, senderId, true);
		CometHelper.publishReputationScores(this.getReputationScores(roundId));
		return muted;
	}

	/**
	 * Starts participation, and alerts users.
	 */
	public void startParticipation() {
		this.participationRunning = true;
		CometHelper.publishParticipationChange(this.getChatState());
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#addRound(int)}
	 */
	public ChatRound startRound(int roundId) {
		ChatRound round = this.database.addRound(roundId);
		boolean isChatOn = this.getConfiguration().isChatOn(roundId, 0);
		CometHelper.publishChatActiveChange(isChatOn, true);
		CometHelper.publishTradingActiveChange(true, true);
		if (!this.getConfiguration().isClueDistributionDirect()) {
			this.generateParticipantCluesForRound(roundId);
		}
		if (this.getConfiguration().isParticipationEnabled()) {
			CometHelper.publishParticipationChange(this.getChatState());
			List<ParticipationChoice> choices = this.database.getParticipationChoicesForRound(roundId);
			List<String> notParticipating = choices.stream().filter(c -> !c.isParticipate()).map(ParticipationChoice::getParticipantId).collect(Collectors.toList());
			CometHelper.publishParticipationBrowserStart(notParticipating);
		}
		logger.debug("Round added: " + 1);
		ZocaloLogger.logNewRound(roundId);
		return round;
	}

	/**
	 * Turn off participation.
	 */
	public void stopParticipation() {
		this.participationRunning = false;
	}

	/**
	 * To be used carefully. This will terminate all connected Java clients.
	 */
	public void terminateAllConnectedDesktops() {
		CometHelper.publishParticipationBrowserTerminate();
	}

	/**
	 * Refer to: {@link jjdm.zocalo.data.ZocaloDatabase#unmuteSender(int, String, String)}
	 */
	public List<String> unmuteSender(int roundId, String participantId, String senderId) {
		List<String> muted = this.database.unmuteSender(roundId, participantId, senderId);
		ZocaloLogger.logChatBlock(roundId, participantId, senderId, false);
		CometHelper.publishReputationScores(this.getReputationScores(roundId));
		return muted;
	}

}
