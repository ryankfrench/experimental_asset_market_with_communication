package jjdm.zocalo.data;

import jjdm.zocalo.ZocaloHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class serves as an in-memory database. If more demand is needed in the future, this could be adapted to use
 * something like HSQLDB or a more traditional DB like MySQL, PostgreSQL, etc. All public methods are synchronized to
 * prevent race conditions on the underlying "database" - currently a Java set.
 *
 * @author Josh Martin
 * @since October, 2014
 */
public class ZocaloDatabase {

	private static ZocaloDatabase INSTANCE = new ZocaloDatabase();
	private ZocaloConfig configuration;
	private List<ChatRound> rounds = new ArrayList<ChatRound>();
	private List<ParticipationChoice> participationChoices = new ArrayList<>();
	private List<Map<String, List<String>>> automaticCluesPerRound = new ArrayList<>();

	/**
	 * Singleton contructor.
	 */
	private ZocaloDatabase() {
		super();
	}

	/**
	 * Constructor to build up configuration.
	 *
	 * @param properties The properties loaded by the experimenter.
	 */
	public static ZocaloDatabase newInstance(Properties properties) {
		resetInstance();
		INSTANCE.configuration = new ZocaloConfig(properties);
		return INSTANCE;
	}

	/**
	 * Used to clear out any remaining database.
	 */
	public static void resetInstance() {
		INSTANCE.configuration = null;
		INSTANCE.rounds = new ArrayList<>();
		INSTANCE.participationChoices = new ArrayList<>();
		INSTANCE.automaticCluesPerRound = new ArrayList<>();
	}

	/**
	 * Add a message in the current round.
	 *
	 * @param roundId      The round of the message.
	 * @param senderId     Who sent id (ID not display).
	 * @param recipientIds Who are the recipients (ID not display).
	 * @param message      The message text.
	 * @return The message.
	 */
	public synchronized ChatMessage addMessage(int roundId, String senderId, List<String> recipientIds, String message) {
		ChatRound round = this.getRound(roundId);
		ChatMessage chatMessage = new ChatMessage(senderId, recipientIds, message);
		round.addMessage(chatMessage);
		this.updateRound(round);
		return chatMessage;
	}

	/**
	 * Add a selection for the given participant to participate and/or receive a clue.
	 *
	 * @param nextRoundId   The upcoming round.
	 * @param participantId The participant.
	 * @param participate   Whether they choose to participate.
	 * @param receiveClue   Whether they choose to receive a clue.
	 * @return The Participation choices.
	 */
	public ParticipationChoice addParticipationChoice(int nextRoundId, String participantId, boolean participate, boolean receiveClue) {
		ParticipationChoice choice = new ParticipationChoice(nextRoundId, participantId, participate, receiveClue);
		if (this.participationChoices.stream().anyMatch(c -> c.getRoundId() == nextRoundId && c.getParticipantId().equals(participantId))) {
			throw new IllegalArgumentException("Choice for participant already exists: " + nextRoundId + " " + participantId);
		}
		this.participationChoices.add(choice);
		return choice;
	}

	/**
	 * Add a selection for the given participant to participate and/or the number of clues they receive.
	 *
	 * @param nextRoundId     The upcoming round.
	 * @param participantId   The participant.
	 * @param participate     Whether they choose to participate.
	 * @param additionalClues The number of additional clues purchased.
	 * @return The Participation choices.
	 */
	public ParticipationChoice addParticipationMultiClueChoice(int nextRoundId, String participantId, boolean participate, int additionalClues) {
		ParticipationChoice choice = new ParticipationChoice(nextRoundId, participantId, participate, additionalClues);
		if (this.participationChoices.stream().anyMatch(c -> c.getRoundId() == nextRoundId && c.getParticipantId().equals(participantId))) {
			throw new IllegalArgumentException("Choice for participant already exists: " + nextRoundId + " " + participantId);
		}
		this.participationChoices.add(choice);
		return choice;
	}

	/**
	 * Start a new round.
	 *
	 * @param roundId The ID (1 to N) of the round.
	 * @return The round.
	 */
	public synchronized ChatRound addRound(int roundId) {
		if (this.rounds.stream().anyMatch(r -> r.getId() == roundId)) {
			throw new IllegalArgumentException("Round ID already exists: " + roundId);
		}
		ChatRound round = new ChatRound(roundId);
		this.rounds.add(round);
		return round;
	}

	/**
	 * Generates clues for everyone who selected to participate.  People choose to participate either explicitly (if
	 * participation is enabled) or passively (if they access the participation page and it is not enabled).  User ID's
	 * that do not do either will not be included in the calculation.  Direct or "auto" clues based on the
	 * configuration.
	 * <p>
	 * Once this method is called for a given round, the random assignments are locked.  If called again for the same
	 * round, a runtime error will be thrown.
	 *
	 * @param roundId The round about to start (or just started).
	 * @return A map of userID to clueId (e.g. "a1:X").
	 * @throws IllegalArgumentException If called more than once for a round.
	 * @see #getAutomaticCluesForRound(int)
	 */
	public Map<String, List<String>> generateParticipantCluesForRound(int roundId) {
		if (this.automaticCluesPerRound.size() >= roundId) {
			throw new IllegalArgumentException("Round " + roundId + " has already been generated.");
		}
		List<ParticipationChoice> playing;
		ZocaloConfig config = this.getConfiguration();
		if (config.isParticipationEnabled()) {
			// only people who receive clues are considered
			List<ParticipationChoice> choices = this.getParticipationChoicesForRound(roundId);
			playing = choices.stream().filter(c -> c.isReceiveClue()).collect(Collectors.toList());
		} else {
			// everyone receives clues
			playing = new ArrayList<>();
			for (String player : config.getParticipants()) {
				ParticipationChoice choice = new ParticipationChoice(roundId, player, true, true);
				playing.add(choice);
			}
		}
		List<String> possibleHintsForRound = config.getHintsPossibleForRound(roundId);
		Map<String, List<String>> cluesForThisRound = new HashMap<>();
		if (config.isClueDistributionMultiClue()) {
			int freeClues = config.getParticipationSelectStartingForRound(roundId);
			boolean allowRepeats = config.isParticipationClueRepeats();
			boolean shuffle = config.isClueDistributionSelectShuffle();
			cluesForThisRound = ZocaloHelper.buildCluesMultipleForRound(playing, possibleHintsForRound, freeClues, allowRepeats, shuffle);
		} else {
			Map<String, String> singleCluesForThisRound = ZocaloHelper.buildCluesForRound(playing, possibleHintsForRound);
			for (String user : singleCluesForThisRound.keySet()) {
				String clueId = singleCluesForThisRound.get(user);
				List<String> oneEntryList = new ArrayList<>();
				oneEntryList.add(clueId);
				cluesForThisRound.put(user, oneEntryList);
			}
		}
		this.automaticCluesPerRound.add(cluesForThisRound);
		return cluesForThisRound;
	}

	/**
	 * Return all of the messages for the round.
	 *
	 * @param roundId The round.
	 * @return The messages for the round.
	 */
	public synchronized List<ChatMessage> getAllMessages(int roundId) {
		ChatRound round = this.getRound(roundId);
		return round.getMessages();
	}

	/**
	 * Returns all of the muted senders per participant ID.
	 *
	 * @param roundId The round.
	 * @return A map of muted senders by participant ID.
	 */
	public synchronized Map<String, Set<String>> getAllMutedSenders(int roundId) {
		ChatRound round = this.getRound(roundId);
		Map<String, Set<String>> mutedSenders = round.getMutedSenders();
		return (mutedSenders == null) ? new HashMap<>() : mutedSenders;
	}

	/**
	 * Returns the clues per participant for the round.
	 *
	 * @param roundId The round to pull the clues for.
	 * @return A map of userID to clueId (e.g. "a1:X").
	 */
	public Map<String, List<String>> getAutomaticCluesForRound(int roundId) {
		return this.automaticCluesPerRound.get(roundId - 1);
	}

	/**
	 * Return the property-based configuration for this session.
	 *
	 * @return The configuration.
	 */
	public synchronized ZocaloConfig getConfiguration() {
		return configuration;
	}

	/**
	 * Return a list of all muted senders for a given round and participant (receiver).
	 *
	 * @param roundId       The round ID.
	 * @param participantId The receiver (who has muted).
	 * @return All participant ID's that have been muted for this receiver.
	 */
	public synchronized List<String> getMutedSenders(int roundId, String participantId) {
		ChatRound round = this.getRound(roundId);
		Map<String, Set<String>> mutedSenders = round.getMutedSenders();
		Set<String> muted = mutedSenders.get(participantId);
		return (muted == null) ? new ArrayList<String>() : new ArrayList<String>(muted);
	}

	/**
	 * Returns the choice for a particular round and user.
	 *
	 * @param roundId The round the participant chose (or did not choose) to play in.
	 * @param userId  The participant ID.
	 * @return The choice, or null if no match was found for the user.
	 * @see #getParticipationChoicesForRound(int)
	 */
	public ParticipationChoice getParticipationChoiceForRoundAndUser(int roundId, String userId) {
		Optional<ParticipationChoice> choice = this.participationChoices.stream().filter(c -> c.getRoundId() == roundId && c.getParticipantId().equalsIgnoreCase(userId)).findAny();
		if (choice.isPresent()) {
			return choice.get();
		} else {
			return null;
		}
	}

	/**
	 * Return all of the participations choices for a given round.
	 *
	 * @param roundId The round.
	 * @return The choices for participants matching the round.
	 */
	public List<ParticipationChoice> getParticipationChoicesForRound(int roundId) {
		return this.participationChoices.stream().filter(c -> c.getRoundId() == roundId).collect(Collectors.toList());
	}

	/**
	 * Return a specific round.
	 *
	 * @param roundId The round ID.
	 * @return The specific round.
	 */
	public synchronized ChatRound getRound(int roundId) {
		Optional<ChatRound> round = this.rounds.stream().filter(r -> r.getId() == roundId).findFirst();
		if (!round.isPresent()) {
			throw new IllegalArgumentException("Cannot find matching round for round ID: " + roundId);
		}
		return round.get();
	}

	/**
	 * Get all rounds - useful for reporting across a session.
	 *
	 * @return All rounds for the session.
	 */
	public synchronized List<ChatRound> getRounds() {
		return rounds;
	}

	/**
	 * Mute a sender (block messages from them).
	 *
	 * @param roundId       The round.
	 * @param participantId The receiver's ID.
	 * @param senderId      The sender ID to mute.
	 * @return The list of muted senders after the sender is added from the list.
	 */
	public synchronized List<String> muteSender(int roundId, String participantId, String senderId) {
		ChatRound round = this.getRound(roundId);
		round.addMutedSender(participantId, senderId);
		this.updateRound(round);
		return this.getMutedSenders(roundId, participantId);
	}

	/**
	 * Unmute a sender (receive messages from them).
	 *
	 * @param roundId       The round.
	 * @param participantId The receiver's ID.
	 * @param senderId      The sender ID to unmute.
	 * @return The list of muted senders after the sender is removed from the list.
	 */
	public synchronized List<String> unmuteSender(int roundId, String participantId, String senderId) {
		ChatRound round = this.getRound(roundId);
		round.removeMutedSender(participantId, senderId);
		this.updateRound(round);
		return this.getMutedSenders(roundId, participantId);
	}

	/**
	 * Used to remove then add a round.
	 *
	 * @param round The round to update.
	 */
	private synchronized void updateRound(ChatRound round) {
		this.rounds.removeIf(r -> r.getId() == round.getId());
		this.rounds.add(round);
	}

}
