package jjdm.zocalo;

import com.google.common.collect.Collections2;
import jjdm.zocalo.data.ChatState;
import jjdm.zocalo.data.ParticipationChoice;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.ajax.dispatch.BidUpdateDispatcher;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.market.Book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Used to correct things in Zocalo.
 */
public class ZocaloHelper {

	/**
	 * Builds up a map of participant ID's to clues.
	 *
	 * @param players The people playing/participating in the round.
	 * @param hints   The possible hints for the round.
	 * @return A map of "evening" distributed clues to the players.
	 */
	public static Map<String, String> buildCluesForRound(List<ParticipationChoice> players, List<String> hints) {

		Map<String, String> playerHintMap = new HashMap<>();

		if (players == null) {
			return playerHintMap; // possible if no one participates
		}
		if (hints == null || hints.isEmpty()) {
			throw new IllegalArgumentException("There are hints available to assign.");
		}

		List<ParticipationChoice> randomPlayers = new ArrayList<>(players);
		Collections.copy(randomPlayers, players);
		Collections.shuffle(randomPlayers);

		int currentHint = 0;
		for (ParticipationChoice player : randomPlayers) {
			if (currentHint >= hints.size()) {
				currentHint = 0;
			}
			String hint = hints.get(currentHint++);
			playerHintMap.put(player.getParticipantId(), hint);
		}

		return playerHintMap;
	}

	/**
	 * Used when multiple clues are allowed (participants can select the number of clues they receive).
	 *
	 * @param players      This list of choices for the given round.
	 * @param hints        The possible hints for the round.
	 * @param freeClues    Number of initial/free clues for each participant.
	 * @param allowRepeats Can the same clue be assigned to the same participant.
	 * @param shuffle      Should the clues be shuffled before assignment.
	 * @return The clues per participant (clue names, not values - i.e. X, not "The true value..."
	 */
	public static Map<String, List<String>> buildCluesMultipleForRound(List<ParticipationChoice> players, List<String> hints, int freeClues, boolean allowRepeats, boolean shuffle) {

		Map<String, List<String>> playerHintMap = new HashMap<>();

		if (players == null) {
			return playerHintMap; // possible if no one participates
		}
		if (hints == null || hints.isEmpty()) {
			throw new IllegalArgumentException("There are hints available to assign.");
		}

		// shuffle the clues if needed

		// sort based on the number of additional clues purchased - only those participating
		List<ParticipationChoice> sortedParticipants = players.stream().sorted((p1, p2) -> Integer.compare(p2.getAdditionalClues(), p1.getAdditionalClues())).filter(p -> p.isParticipate()).collect(Collectors.toList());

		int clueIndex = 0;

		// no repeats
		List<String> cluesToUse = new ArrayList<>(hints);
		Collections.copy(cluesToUse, hints);
		if (shuffle) {
			Collections.shuffle(cluesToUse);
		}

		// repeats
		int totalCluesToAssign = players.stream().filter(p -> p.isParticipate()).map(ParticipationChoice::getAdditionalClues).reduce(0, (additional, total) -> (additional + total + freeClues));
		List<String> repeatsCluesToUse = new ArrayList<>();
		for (int i = 0; i < totalCluesToAssign; i++) {
			if (clueIndex >= cluesToUse.size()) {
				clueIndex = 0;
			}
			repeatsCluesToUse.add(cluesToUse.get(clueIndex++));
		}
		Collections.shuffle(repeatsCluesToUse);

		// reset clue index to start at beginning
		clueIndex = 0;

		for (ParticipationChoice player : sortedParticipants) {
			List<String> hintsForPlayer = new ArrayList<>();
			int requiredClues = freeClues + player.getAdditionalClues();
			while (hintsForPlayer.size() < requiredClues) {
				String hint;
				if (allowRepeats) {
					hint = repeatsCluesToUse.get(clueIndex++); // no need to reset index - same size as total hints
				} else {
					if (clueIndex >= cluesToUse.size()) {
						clueIndex = 0;
					}
					hint = cluesToUse.get(clueIndex++);
				}
				hintsForPlayer.add(hint);
			}
			playerHintMap.put(player.getParticipantId(), hintsForPlayer);
		}

		return playerHintMap;
	}

	/**
	 * Builds up a map of colors to be used per participant to highlight pre-defined messages.
	 *
	 * @param participants The list of participants (e.g. a1, a2).
	 * @param colors       The list of possible colors (e.g. red, blue, green or HEX values).
	 * @param choices      The map of with their order choices.
	 * @return A map of the colors per choice for each participant [PARTICIPANT: [CHOICE: COLOR]]
	 */
	public static Map<String, Map<String, String>> buildColorMap(List<String> participants, List<String> colors, Map<Integer, String> choices) {

		Map<String, Map<String, String>> colorMap = new TreeMap<>();
		List<List<String>> colorPermutations = new ArrayList(Collections2.permutations(colors));
		List<String> randomParticipants = new ArrayList<>(participants);
		Collections.copy(randomParticipants, participants);
		Collections.shuffle(randomParticipants);

		int choiceCount = choices.size();
		List<String> choiceStrings = new ArrayList<>();

		for (int i = 0; i < choices.size(); i++) {
			choiceStrings.add(choices.get(i + 1)); // choices start with 1
		}

		int permutationsSize = colorPermutations.size();
		int currentPermutation = 0;

		for (String participant : randomParticipants) {
			Map<String, String> choiceColors = new HashMap<>();
			if (currentPermutation >= permutationsSize) {
				currentPermutation = 0;
			}
			List<String> permutationsForParticipant = colorPermutations.get(currentPermutation++);
			for (int i = 0; i < choiceCount; i++) {
				choiceColors.put(choiceStrings.get(i), permutationsForParticipant.get(i));
			}
			colorMap.put(participant, choiceColors);
		}

		return colorMap;
	}

	/**
	 * Provides a way to generate an updated list of current bids/asks.
	 *
	 * @return A map ready to be formatted into JSON.
	 */
	public static Map<String, Object> buildLiveUpdateMessage() {

		Session session = SessionSingleton.getSession();
		BinaryMarket market = session.getMarket();
		Book book = market.getBook();
		BinaryClaim claim = (BinaryClaim) book.getClaim();
		Position buyPosition = claim.getYesPosition();
		Position sellPosition = claim.getNoPosition();
		String buys = book.buildPriceList(buyPosition);
		String sells = book.buildPriceList(sellPosition);

		ZocaloService service = ZocaloService.getInstance();
		ChatState state = service.getChatState();

		Map<String, Object> message = new HashMap<>();
		message.put("channel", BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI);
		message.put("id", "");
		Map<String, Object> data = new HashMap<>();
		data.put("round", state.getCurrentRound());
		String timeRemaining = "closed";
		int secondsRemaining = state.getRoundSecondsRemaining();
		if (session.getMarket().isOpen() && secondsRemaining != 0) {
			timeRemaining = secondsToMmSsDisplay(secondsRemaining);
		}
		data.put("timeRemaining", timeRemaining);
		if ("closed".equals(timeRemaining)) {
			data.put("buy", "");
			data.put("sell", "");
		} else {
			data.put("buy", buys);
			data.put("sell", sells);
		}
		message.put("data", data);

		return message;

	}

	/**
	 * Turn seconds in integer into a MM:SS string display.
	 *
	 * @param seconds The seconds.
	 * @return The MM:SS display.
	 */
	public static String secondsToMmSsDisplay(int seconds) {
		String pad = "00";
		int minutesLeft = (int) Math.floor(seconds / 60);
		String secondsLeft = "" + (seconds - (minutesLeft * 60));
		secondsLeft = pad.substring(0, pad.length() - secondsLeft.length()) + secondsLeft;
		return minutesLeft + ":" + secondsLeft;
	}
}
