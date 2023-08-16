package jjdm.zocalo.data;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jjdm.zocalo.ZocaloHelper;
import net.commerce.zocalo.experiment.Session;
import net.commerce.zocalo.service.PropertyHelper;
import net.commerce.zocalo.service.PropertyKeywords;
import org.apache.log4j.Logger;

/**
 * Contains information related to configuration of chat.
 *
 * @author Josh Martin
 */
public class ZocaloConfig {

	public final static String FREEMARKER_PREFIX = "freemarker.template.";
	private String carryForward = null;
	private boolean chatBlockEnabled;
	private boolean chatBlockShowReputation;
	private Map<Integer, String> chatChoices = new TreeMap<>();
	private boolean chatEnabled;
	private List<Map<Integer, Boolean>> chatOnPerRound = new ArrayList<>();
	private Map<String, Map<String, String>> choiceColorMap = null;
	private boolean choiceColorsEnabled;
	private boolean colorChartEnabled;
	private Map<String, List<String>> displayIds = new HashMap<>();
	private List<TreeMap<String, String>> displayIdsPerRound = new ArrayList<>();
	private Configuration freemarkerConfig;
	private Map<String, String> hintMap = new HashMap<>();
	private List<List<String>> hintsPossiblePerRound = new ArrayList<>();
	private Logger logger = Logger.getLogger(this.getClass());
	private int maxMessageSize;
	private List<String> overriddenFreemarkerTemplates = new ArrayList<>();
	private Map<String, List<List<String>>> participantChoicesPerRound = new HashMap<>();
	private List<String> participants = new ArrayList<>();
	private List<Integer> participationAccountAmounts = new ArrayList<>();
	private List<Integer> participationClueCostPerRound = new ArrayList<>();
	private String participationClueDistribution;
	private boolean participationClueRepeats = false;
	private List<Integer> participationClueSelectMaxPerRound = new ArrayList<>();
	private List<Integer> participationClueSelectStartingPerRound = new ArrayList<>();
	private List<Integer> participationCostPerRound = new ArrayList<>();
	private boolean participationEnabled;
	private Properties properties;
	private int roundTime;
	private int rounds;
	private List<Map<Integer, Boolean>> tradingPausedPerRound = new ArrayList<>();

	/**
	 * Setup the internal configurations.
	 *
	 * @param properties The properties file for the session.
	 */
	public ZocaloConfig(Properties properties) {

		try {

			// setup properties
			this.properties = properties;

			// inject into labels
			ZocaloLabels.setProperties(properties);

			// number of rounds
			this.rounds = PropertyHelper.parseInteger("rounds", properties);

			// establish rounds
			String timeLimitString = get("timeLimit");
			this.roundTime = PropertyHelper.parseTimeStringAsSeconds(timeLimitString);

			// establish max message size
			this.maxMessageSize = PropertyHelper.parseInteger("chat.max.message.size", properties);

			// establish participants
			String participantString = get("players");
			this.participants = commaToStringList(participantString);

			// get display id's
			boolean hasDisplayId = get(participants.get(0) + ".display.id") != null;
			if (hasDisplayId) {
				for (String participant : participants) {
					String displayString = get(participant + ".display.id");
					if (displayString == null) {
						throw new IllegalStateException("No display ID's for " + participant);
					}
					List<String> displaysPerRound = commaToStringList(displayString);
					displayIds.put(participant, displaysPerRound);
				}
				for (int i = 1; i <= rounds; i++) {
					TreeMap<String, String> displayMapping = new TreeMap<>();
					for (String participant : participants) {
						String displayId = this.getDisplayIdForRound(participant, i);
						displayMapping.put(displayId, participant);
					}
					this.displayIdsPerRound.add(displayMapping);
				}

			}

			// setup the on/off timing for trading per round
			this.tradingPausedPerRound = this.readPerRoundTimeRanges("trading.pauses", rounds);

			// is chat enabled
			this.chatEnabled = "true".equalsIgnoreCase(get("chat.enabled"));

			if (this.chatEnabled) {

				// setup the on/off timing for chats per round
				this.chatOnPerRound = this.readPerRoundTimeRanges("chat.timing", rounds);

				// setup the potential chat messages for the session
				Set<String> allProperties = this.properties.stringPropertyNames();
				List<String> choicesString = allProperties.stream().filter(p -> p.startsWith("chat.choices")).collect(Collectors.toList());
				for (String choice : choicesString) {
					String choiceValue = get(choice);
					int choiceId = Integer.valueOf(choice.replace("chat.choices.", ""));
					chatChoices.put(choiceId, choiceValue);
				}

				// setup the per participant chat options (or free) per round
				choicesString = allProperties.stream().filter(p -> p.endsWith("chat.choices")).collect(Collectors.toList());
				for (String choice : choicesString) {
					String choiceValue = get(choice);
					String participant = choice.replace(".chat.choices", "").trim();
					List<List<String>> choicesPerRound = new ArrayList<>();
					List<String> roundChoices = this.semicolonToStringList(choiceValue);
					for (String roundChoice : roundChoices) {
						List<String> choicesForRound = new ArrayList<>();
						if (roundChoice.contains(",")) {
							List<String> choiceIdStringList = this.commaToStringList(roundChoice);
							for (String choiceIdString : choiceIdStringList) {
								int choiceId = Integer.valueOf(choiceIdString.trim());
								String choiceMessage = this.chatChoices.get(choiceId);
								choicesForRound.add(choiceMessage);
							}
						} else {
							choicesForRound = null;
						}
						choicesPerRound.add(choicesForRound);
					}
					this.participantChoicesPerRound.put(participant, choicesPerRound);
				}

				// color assignment for participants
				String colorParam = get("chat.colors.for.choices");
				this.choiceColorsEnabled = (colorParam != null);
				if (choiceColorsEnabled) {
					List<String> colors = this.readStringList("chat.colors.for.choices");
					this.choiceColorMap = ZocaloHelper.buildColorMap(this.participants, colors, this.chatChoices);
					this.colorChartEnabled = "true".equalsIgnoreCase(get("chat.colors.chart.enabled"));
				}

				// chat blocking (true by default)
				this.chatBlockEnabled = "false".equalsIgnoreCase(get("chat.block.enabled")) ? false : true;

				// if you can block chats, do you want to show reputation scores (true by default)
				if (this.chatBlockEnabled) {
					this.chatBlockShowReputation = "false".equalsIgnoreCase(get("chat.block.show.reputation")) ? false : true;
				}

			}

			// get the clues/hints
			this.participationClueDistribution = get("hint.distribution");
			if (this.isClueDistributionDirect()) {
				List<String> allHintProps = this.properties.stringPropertyNames().stream().filter(p -> p.contains(".hint")).collect(Collectors.toList());
				for (String hintProperty : allHintProps) {
					String hintsForParticipant = get(hintProperty);
					List<String> allHintsForParticipant = this.commaToStringList(hintsForParticipant);
					allHintsForParticipant.forEach(clue -> hintMap.put(clue, this.get(clue)));
				}
			} else {
				String rawPossibleClues = get("hint.possible");
				List<String> possibleCluesPerRound = semicolonToStringList(rawPossibleClues);
				for (String possibleCluesString : possibleCluesPerRound) {
					List<String> possibleClues = commaToStringList(possibleCluesString);
					this.hintsPossiblePerRound.add(possibleClues);
					possibleClues.forEach(clue -> hintMap.put(clue, this.get(clue)));
				}
			}

			// is participation enabled
			this.participationEnabled = "true".equalsIgnoreCase(get("participation.enabled"));
			if (this.participationEnabled) {
				this.participationCostPerRound = readIntegerList("participation.participate.cost");
				this.participationClueCostPerRound = readIntegerList("participation.clue.cost");
				this.participationAccountAmounts = readIntegerList("participation.account.amounts");
				if (this.isClueDistributionMultiClue()) {
					participationClueSelectStartingPerRound = readIntegerList("hint.select.starting");
					participationClueSelectMaxPerRound = readIntegerList("hint.select.maximum");
					participationClueRepeats = "true".equalsIgnoreCase(get("hint.select.allow.repeats"));
				}
			}

			// setup freemarker
			List<String> templateNames = this.properties.stringPropertyNames().stream().filter(n -> n.startsWith(FREEMARKER_PREFIX)).collect(Collectors.toList());
			this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_23);
			this.freemarkerConfig.setDefaultEncoding("UTF-8");
			this.freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
			ClassTemplateLoader classLoader = new ClassTemplateLoader(getClass(), "/templates/");
			if (templateNames != null) {
				StringTemplateLoader stringLoader = new StringTemplateLoader();
				for (String templateName : templateNames) {
					String templateValue = this.get(templateName).trim();
					String mapName = templateName.replaceAll(FREEMARKER_PREFIX, "");
					stringLoader.putTemplate(mapName, templateValue);
					this.overriddenFreemarkerTemplates.add(mapName);
				}
				MultiTemplateLoader multiLoader = new MultiTemplateLoader(new TemplateLoader[]{stringLoader, classLoader});
				this.freemarkerConfig.setTemplateLoader(multiLoader);
			} else {
				this.freemarkerConfig.setTemplateLoader(classLoader);
			}

			// carry forward
			this.carryForward = get(PropertyKeywords.CARRY_FORWARD, "false").trim();

		} catch (Exception e) {
			logger.error("Unable to create Zocalo Config.", e);
			throw e;
		}

	}

	/**
	 * Generates freemarker output based on the passed template name (minus freemarker.template) and value map.
	 *
	 * @param templateName
	 * @param values
	 * @return
	 */
	public String freemarker(String templateName, Map<String, Object> values) {
		if (this.freemarkerConfig == null) {
			throw new IllegalStateException("Freemarker is not properly configured.  There is no configuration.");
		}
		try {
			boolean overridden = overriddenFreemarkerTemplates.contains(templateName);
			String templateLookupName = overridden ? templateName : templateName + ".ftl";
			Template t = this.freemarkerConfig.getTemplate(templateLookupName);
			StringWriter writer = new StringWriter();
			t.process(values, writer);
			return writer.toString().trim();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Provides a collection of every second of a round, and if chat should be on for that second.
	 *
	 * @param round The round number.
	 * @return A map of every second of the round, and if chat should be on for that second.
	 */
	public Map<Integer, Boolean> getChatTimesForRound(int round) {
		if (this.isChatEnabled()) {
			return this.chatOnPerRound.get(round - 1);
		} else {
			throw new IllegalStateException("Chat is not enabled for this session");
		}
	}

	/**
	 * The map of [PARTICIPANT: [CHOICE: COLOR]].
	 *
	 * @return The map with colors, or null if not defined.
	 * @see #isChoiceColorsEnabled()
	 * @see jjdm.zocalo.ZocaloHelper#buildColorMap(java.util.List, java.util.List, java.util.Map)
	 */
	public Map<String, Map<String, String>> getChoiceColorMap() {
		return this.choiceColorMap;
	}

	/**
	 * Return the display ID for a participant and round.
	 *
	 * @param id The ID of the participant.
	 * @param round The round for the display.
	 * @return The display ID/name.
	 */
	public String getDisplayIdForRound(String id, int round) {
		return this.getDisplayIdsForParticipant(id).get(round - 1);
	}

	/**
	 * Returns a list of display ID's - one per round.
	 *
	 * @param id The ID of the participant.
	 * @return The list of display ID's across rounds;
	 */
	public List<String> getDisplayIdsForParticipant(String id) {
		return this.displayIds.get(id);
	}

	/**
	 * Returns a sorted (tree) map of all the displayId:participantId for the round.
	 *
	 * @param round The round to get the display ID map.
	 * @return The mapping for the round.
	 */
	public TreeMap<String, String> getDisplayIdsForRound(int round) {
		return this.displayIdsPerRound.get(round - 1);
	}

	/**
	 * Returns a map (hintId:hintValue) of all possible hints.
	 *
	 * @return The hint map.
	 */
	public Map<String, String> getHintMap() {
		return this.hintMap;
	}

	/**
	 * Return the possible clues for the round. Note these are the raw hint IDs. Use hintMap to get the full hint.
	 *
	 * @param round The round to lookup.
	 * @return The possible clues for the round.
	 * @see #getHintMap()
	 */
	public List<String> getHintsPossibleForRound(int round) {
		return hintsPossiblePerRound.get(round - 1);
	}

	/**
	 * Retrieve the maximum size allowed for messages.
	 *
	 * @return The maximum size in characters (HTML text area maxsize).
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}

	/**
	 * Returns the message choices for a given participant and round. If free text is on, then null is return.
	 *
	 * @param id The participant's ID
	 * @param round The round ID
	 * @return The list of message choices for the round, or null if free text is allowed.
	 */
	public List<String> getMessageChoicesForRound(String id, int round) {
		return this.participantChoicesPerRound.get(id).get(round - 1);
	}

	/**
	 * Get the list of participant ID's for the experiment.
	 *
	 * @return The list of participant ID's.
	 */
	public List<String> getParticipants() {
		return this.participants;
	}

	/**
	 * Return the starting account balance for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The participant's account balance for that round.
	 */
	public Integer getParticipationAccountAmountForRound(int round) {
		return participationAccountAmounts.get(round - 1);
	}

	/**
	 * Return the cost of a clue for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The cost to of the clue.
	 */
	public Integer getParticipationClueCostForRound(int round) {
		return participationClueCostPerRound.get(round - 1);
	}

	/**
	 * Return the cost to participate for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The cost to participate.
	 */
	public Integer getParticipationCostForRound(int round) {
		return participationCostPerRound.get(round - 1);
	}

	/**
	 * Return the starting number of clues for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The starting number of clues.
	 * @see #isClueDistributionSelect()
	 * @see #isClueDistributionSelectShuffle()
	 */
	public Integer getParticipationSelectMaximumForRound(int round) {
		return participationClueSelectMaxPerRound.get(round - 1);
	}

	/**
	 * Return the starting number of clues for a given round.
	 *
	 * @param round The round to lookup.
	 * @return The starting number of clues.
	 * @see #isClueDistributionSelect()
	 * @see #isClueDistributionSelectShuffle()
	 */
	public Integer getParticipationSelectStartingForRound(int round) {
		return participationClueSelectStartingPerRound.get(round - 1);
	}

	/**
	 * The pre-defined messages available to chatting.
	 *
	 * @return The messages, ordered by the key in the map.
	 */
	public Map<Integer, String> getPossibleChoices() {
		return this.chatChoices;
	}

	/**
	 * Returns the round time in seconds.
	 *
	 * @return The round time.
	 */
	public int getRoundTime() {
		return roundTime;
	}

	/**
	 * The number of rounds.
	 *
	 * @return The number of rounds.
	 */
	public int getRounds() {
		return rounds;
	}

	/**
	 * Get the number of seconds remaining in this chat period.
	 *
	 * @param round The current round.
	 * @param second The elapsed seconds into the round.
	 * @return The number of seconds until this chat ends, or 0 if not chatting.
	 */
	public int getSecondsRemainingInChat(int round, int second) {
		if (!isChatOn(round, second)) {
			return 0;
		}
		Map<Integer, Boolean> thisRound = this.chatOnPerRound.get(round - 1);
		int smallest = thisRound.size();
		for (Integer sec : thisRound.keySet()) {
			boolean state = thisRound.get(sec);
			if ((sec > second) && (sec < smallest) && (!state)) {
				smallest = sec;
			}

		}
		return smallest - second - 1;
	}

	/**
	 * Provides a collection of every second of a round, and if trading should be paused for that second.
	 *
	 * @param round The round number.
	 * @return A map of every second of the round, and if trading should be paused for that second.
	 */
	public Map<Integer, Boolean> getTradingPausedTimesForRound(int round) {
		if (this.isChatEnabled()) {
			return this.tradingPausedPerRound.get(round - 1);
		} else {
			throw new IllegalStateException("Chat is not enabled for this session");
		}
	}

	/**
	 * Is this the standard carry over?
	 *
	 * @return True if carryOver is set to all or true.
	 * @see Session#isCarryForward()
	 */
	public boolean isCarryForwardEverything() {
		return PropertyKeywords.CARRY_FORWARD_ALL.equalsIgnoreCase(carryForward) || "true".equalsIgnoreCase(carryForward);
	}

	/**
	 * Is this a cash-only carry over (coupons are not carried over)?
	 *
	 * @return True if cash only carry over (coupons reset).
	 */
	public boolean isCarryForwardRolloverCash() {
		return "cash".equalsIgnoreCase(this.carryForward);
	}

	/**
	 * Is blocking other participants enabled.
	 *
	 * @return True if chat block is enabled (true is the default)
	 */
	public boolean isChatBlockEnabled() {
		return chatBlockEnabled;
	}

	public boolean isChatBlockShowReputation() {
		return chatBlockShowReputation;
	}

	/**
	 * Is chat enabled for the current session.
	 *
	 * @return True if chat is enabled for the current session.
	 */
	public boolean isChatEnabled() {
		return this.chatEnabled;
	}

	/**
	 * Is chat enabled for this round and second.
	 *
	 * @param round The round.
	 * @param second The second.
	 * @return True if chat is enabled for this round and second.
	 */
	public boolean isChatOn(int round, int second) {
		if (this.isChatEnabled()) {
			return this.chatOnPerRound.get(round - 1).get(second);
		} else {
			return false;
		}
	}

	/**
	 * Should colors be used in pre-defined choices.
	 *
	 * @return True if there are colors present.
	 */
	public boolean isChoiceColorsEnabled() {
		return this.choiceColorsEnabled;
	}

	/**
	 * Are clues automatically assigned across participants.
	 *
	 * @return True if randomly assigned.
	 */
	public boolean isClueDistributionAutomatic() {
		return "auto".equalsIgnoreCase(this.participationClueDistribution);
	}

	/**
	 * Are clues directly assigned (in the configuration file).
	 *
	 * @return True if directly assigned.
	 */
	public boolean isClueDistributionDirect() {
		return !(isClueDistributionAutomatic() || isClueDistributionMultiClue());
	}

	/**
	 * Returns true if either "select" or "select" shuffle is used.
	 *
	 * @return True if participants can select their clues.
	 */
	public boolean isClueDistributionMultiClue() {
		return isClueDistributionSelect() || isClueDistributionSelectShuffle();
	}

	/**
	 * Are clues assigned across participants based on selection.
	 *
	 * @return True if assigned by selection.
	 */
	public boolean isClueDistributionSelect() {
		return "select".equalsIgnoreCase(this.participationClueDistribution);
	}

	/**
	 * Are clues assigned across participants based on selection, and then shuffled.
	 *
	 * @return True if assigned by selection, and then shuffled.
	 */
	public boolean isClueDistributionSelectShuffle() {
		return "select_shuffle".equalsIgnoreCase(this.participationClueDistribution);
	}

	/**
	 * Should a pie chart be drawn for the chat colors.
	 *
	 * @return True if the chart should be drawn.
	 */
	public boolean isColorChartEnabled() {
		return this.colorChartEnabled;
	}

	/**
	 * If select/assigning clues, are repeats allowed.
	 *
	 * @return True if repeats are allowed.
	 * @see #isClueDistributionSelect()
	 * @see #isClueDistributionSelectShuffle()
	 */
	public boolean isParticipationClueRepeats() {
		return participationClueRepeats;
	}

	/**
	 * Is market participation enabled.
	 *
	 * @return True if enabled.
	 */
	public boolean isParticipationEnabled() {
		return participationEnabled;
	}

	/**
	 * Is trading paused for this round and second.
	 *
	 * @param round The round.
	 * @param second The second.
	 * @return True if trading is paused for this round and second.
	 */
	public boolean isTradingPaused(int round, int second) {
		return this.tradingPausedPerRound.get(round - 1).get(second);
	}

	@Override
	public String toString() {
		return "ZocaloConfig [roundTime=" + roundTime + ", participants=" + participants + "]";
	}

	/**
	 * Used to convert a series of time ranges into a map. Ranges are expected in comma-separated format. All values in
	 * the range(s) will be flipped to true. If no ranges, null, or blanks are passed in, all values will be false.
	 * <pre>
	 * Given: 100 total size
	 * 0: All false.
	 * null/empty: All false.
	 * 0-30, 60-90: 0-30 and 60-90 are all true.  Others false.
	 * 0-30: 0-30 are all true.  Others false.
	 * </pre>
	 *
	 * @param totalSize The total number of seconds in a round.
	 * @param rangeString The range string for the round.
	 * @return A map of every second and whether the second is active.
	 */
	private Map<Integer, Boolean> buildTimingMap(int totalSize, String rangeString) {

		Map<Integer, Integer> ranges = new TreeMap<>();
		List<String> chatRangesString = commaToStringList(rangeString);
		for (String chatRange : chatRangesString) {
			String[] parts = chatRange.split("-");
			if (parts.length == 2) {
				int start = Integer.valueOf(parts[0].trim());
				int end = Integer.valueOf(parts[1].trim());
				ranges.put(start, end);
			}
		}

		Map<Integer, Boolean> map = new TreeMap<>();
		for (int i = 0; i <= totalSize; i++) {
			boolean result = false;
			for (int start : ranges.keySet()) {
				int end = ranges.get(start);
				if (i >= start && i <= end) {
					result = true;
					break;
				}
			}
			map.put(i, result);
		}
		return map;

	}

	/**
	 * Turn a comma separated list into a list of strings.
	 *
	 * @param input The original input.
	 * @return The string list.
	 */
	private List<String> commaToStringList(String input) {
		return Arrays.asList(input.split(PropertyKeywords.COMMA_SPLIT_RE));
	}

	/**
	 * Internal method to trim a property before use.
	 *
	 * @param property The property name.
	 * @return The property.
	 */
	private String get(String property) {
		String result = this.properties.getProperty(property);
		return result == null ? null : result.trim();
	}

	/**
	 * Internal method to trim a property, and use a default value.
	 *
	 * @param property The property name.
	 * @param defaultValue The default value if property is null.
	 * @return The value or defaultValue (if null).
	 */
	private String get(String property, String defaultValue) {
		String result = get(property);
		return result == null ? defaultValue : result;
	}

	/**
	 * Returns a list of integers based on a comma-separated list.
	 *
	 * @param propertyName The property name to read.
	 * @return The list of integers.
	 * @see #readStringList(String)
	 */
	private List<Integer> readIntegerList(String propertyName) {
		List<String> values = this.readStringList(propertyName);
		List<Integer> integers = new ArrayList<>();
		for (String value : values) {
			Integer integer = (value.trim().isEmpty()) ? null : Integer.valueOf(value);
			integers.add(integer);
		}
		return integers;
	}

	/**
	 * Reads and converts a time range property.
	 *
	 * @param property The property name.
	 * @param numberOfRounds The number of rounds in this session.
	 * @return The per-round list of which seconds are "on".
	 * @see #buildTimingMap(int, String)
	 */
	private List<Map<Integer, Boolean>> readPerRoundTimeRanges(String property, int numberOfRounds) {
		List<Map<Integer, Boolean>> onPerRound = new ArrayList<>();
		String allRoundsString = get(property);
		List<String> eachRoundStrings = semicolonToStringList(allRoundsString);
		for (int i = 0; i < numberOfRounds; i++) {
			if (eachRoundStrings.size() > i) {
				String roundString = eachRoundStrings.get(i);
				Map<Integer, Boolean> secondOnMap = buildTimingMap(this.roundTime, roundString);
				onPerRound.add(secondOnMap);
			} else {
				Map<Integer, Boolean> secondOnMap = buildTimingMap(this.roundTime, "");
				onPerRound.add(secondOnMap);
			}
		}
		return onPerRound;
	}

	/**
	 * Returns a list of Strings based on a comma-separated list.
	 *
	 * @param propertyName The property name to read.
	 * @return The list of strings.
	 */
	private List<String> readStringList(String propertyName) {
		List<String> results = new ArrayList<>();
		String value = get(propertyName);
		if (value == null) {
			throw new IllegalArgumentException("No value found for " + propertyName);
		}
		String[] parts = value.split(",", -1);
		if (parts == null) {
			throw new IllegalArgumentException("No comma separated values found for " + propertyName);
		}
		for (String part : parts) {
			results.add(part.trim());
		}
		return results;
	}

	/**
	 * Turn a semi-colon separated list into a list of strings.
	 *
	 * @param input The original input.
	 * @return The string list.
	 */
	private List<String> semicolonToStringList(String input) {
		return (input == null) ? new ArrayList<>() : Arrays.asList(input.split(PropertyKeywords.SEMICOLON_SPLIT_RE));
	}

}
