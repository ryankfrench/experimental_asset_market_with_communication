package jjdm.zocalo;

import com.google.common.collect.Collections2;
import jjdm.zocalo.data.ChatReputationScore;
import jjdm.zocalo.data.ZocaloConfig;
import jjdm.zocalo.service.ZocaloService;
import org.junit.Test;

import java.util.*;

import static jjdm.zocalo.ZocaloTestHelper.loadProperties;
import static jjdm.zocalo.ZocaloTestHelper.log;
import static jjdm.zocalo.ZocaloTestHelper.now;
import static jjdm.zocalo.ZocaloTestHelper.randomString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChatTest {

	@Test
	public void chatMessage() {
		int roundId = 1;
		String message = "This is the message!";
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloService service = ZocaloService.startNewSession(props);
		ZocaloConfig config = service.getConfiguration();
		log(config);
		service.startRound(roundId);
		service.addMessage(roundId, "A", Arrays.asList("B", "C", "D", "E"), message);
		service.addMessage(roundId, "A", Arrays.asList("B", "D", "E"), message);
		assertTrue(service.getMessages(roundId, "C").get(0).getMessage().equals(message));
		assertTrue(service.getMessages(roundId, "C").size() == 1);
		assertTrue(service.getMessages(roundId, "D").size() == 2);
	}

	@Test
	public void colorChoices() {

		Properties props = setupProperties();
		props.put("chat.enabled", "true");
		props.put("chat.choices.1", "The dividend is X.");
		props.put("chat.choices.2", "The dividend is Y.");
		props.put("chat.choices.3", "The dividend is Z.");
		props.put("chat.colors.for.choices", "red, yellow, green");

		ZocaloConfig config = new ZocaloConfig(props);
		Map<String, Map<String, String>> colorChoices = config.getChoiceColorMap();
		assertTrue(config.isChoiceColorsEnabled());
		assertTrue(colorChoices.size() == 12);

		Map<String, Map<String, Integer>> colorsPerChoice = new HashMap<>();
		Map<String, Map<String, Integer>> choicesPerColor = new HashMap<>();

		for (String id : colorChoices.keySet()) {

			Set<String> choicesForParticipant = new HashSet<>();
			Set<String> colorsForParticipant = new HashSet<>();
			Map<String, String> choiceColor = colorChoices.get(id);

			for (String choice : choiceColor.keySet()) {

				String color = choiceColor.get(choice);
				choicesForParticipant.add(choice);
				colorsForParticipant.add(color);

				Map<String, Integer> colorCounts;
				Map<String, Integer> choiceCounts;

				if (colorsPerChoice.containsKey(choice)) {
					colorCounts = colorsPerChoice.get(choice);
					if (colorCounts.containsKey(color)) {
						colorCounts.put(color, colorCounts.get(color) + 1);
					} else {
						colorCounts.put(color, 1);
					}
					colorsPerChoice.put(choice, colorCounts);
				} else {
					colorCounts = new HashMap<>();
					colorCounts.put(color, 1);
					colorsPerChoice.put(choice, colorCounts);
				}

				if (choicesPerColor.containsKey(color)) {
					choiceCounts = choicesPerColor.get(color);
					if (choiceCounts.containsKey(choice)) {
						choiceCounts.put(choice, choiceCounts.get(choice) + 1);
					} else {
						choiceCounts.put(choice, 1);
					}
					choicesPerColor.put(color, choiceCounts);
				} else {
					choiceCounts = new HashMap<>();
					choiceCounts.put(choice, 1);
					choicesPerColor.put(color, choiceCounts);
				}

			}

			assertTrue(choicesForParticipant.size() == 3);
			assertTrue(colorsForParticipant.size() == 3);

		}

		log(colorsPerChoice);
		assertTrue(colorsPerChoice.size() == 3);
		assertTrue(colorsPerChoice.get("The dividend is X.").get("red") == 4);
		assertTrue(colorsPerChoice.get("The dividend is X.").get("yellow") == 4);
		assertTrue(colorsPerChoice.get("The dividend is X.").get("green") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Y.").get("red") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Y.").get("yellow") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Y.").get("green") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Z.").get("red") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Z.").get("yellow") == 4);
		assertTrue(colorsPerChoice.get("The dividend is Z.").get("green") == 4);

		log(choicesPerColor);
		assertTrue(choicesPerColor.size() == 3);
		assertTrue(choicesPerColor.get("green").get("The dividend is X.") == 4);
		assertTrue(choicesPerColor.get("green").get("The dividend is Y.") == 4);
		assertTrue(choicesPerColor.get("green").get("The dividend is Z.") == 4);
		assertTrue(choicesPerColor.get("yellow").get("The dividend is X.") == 4);
		assertTrue(choicesPerColor.get("yellow").get("The dividend is Y.") == 4);
		assertTrue(choicesPerColor.get("yellow").get("The dividend is Z.") == 4);
		assertTrue(choicesPerColor.get("red").get("The dividend is X.") == 4);
		assertTrue(choicesPerColor.get("red").get("The dividend is Y.") == 4);
		assertTrue(choicesPerColor.get("red").get("The dividend is Z.") == 4);

		assertFalse(config.isColorChartEnabled());
		props.put("chat.colors.chart.enabled", " true ");
		config = new ZocaloConfig(props);
		assertTrue(config.isColorChartEnabled());

		props.remove("chat.colors.for.choices");
		config = new ZocaloConfig(props);
		assertNull(config.getChoiceColorMap());
		assertFalse(config.isChoiceColorsEnabled());
		assertFalse(config.isColorChartEnabled());

	}

	@Test
	public void configurationTest() throws Exception {

		// load configuration
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloConfig config = new ZocaloConfig(props);

		// test display id
		assertTrue("C".equals(config.getDisplayIdForRound("a1", 3)));
		assertFalse("C".equals(config.getDisplayIdForRound("a5", 5)));

		// test chat enabled
		assertTrue(config.isChatEnabled());

		// test chat enabled times
		assertTrue(config.getChatTimesForRound(1).get(0));
		assertTrue(config.getChatTimesForRound(1).get(17));
		assertTrue(config.getChatTimesForRound(1).get(30));
		assertFalse(config.getChatTimesForRound(1).get(31));
		Map<Integer, Boolean> timesForRound3 = config.getChatTimesForRound(3);
		for (int second : timesForRound3.keySet()) {
			assertFalse(timesForRound3.get(second));
		}
		assertFalse(config.getChatTimesForRound(5).get(0));

		// test trading paused times
		assertTrue(config.getTradingPausedTimesForRound(4).get(0));
		assertTrue(config.getTradingPausedTimesForRound(4).get(60));
		assertTrue(config.getTradingPausedTimesForRound(4).get(90));
		assertTrue(config.getTradingPausedTimesForRound(4).get(95));
		assertFalse(config.getTradingPausedTimesForRound(4).get(121));
		Map<Integer, Boolean> pausesForRound3 = config.getTradingPausedTimesForRound(3);
		for (int second : pausesForRound3.keySet()) {
			assertFalse(pausesForRound3.get(second));
		}
		assertFalse(config.getTradingPausedTimesForRound(5).get(0));
		assertTrue(config.getTradingPausedTimesForRound(5).get(299));
		assertNull(config.getTradingPausedTimesForRound(5).get(301));

		// test main chat messages
		Map<Integer, String> choices = config.getPossibleChoices();
		assertTrue("The dividend is Y.".equals(choices.get(2)));
		assertTrue("Some other message.".equals(choices.get(4)));
		assertNull(choices.get(50));

		// test messages per round and participant
		assertTrue("The dividend is Y.".equals(config.getMessageChoicesForRound("a1", 3).get(1)));
		assertTrue(config.getMessageChoicesForRound("a1", 1) == null);
		assertTrue(config.getMessageChoicesForRound("a1", 4) == null);
		assertTrue(config.getMessageChoicesForRound("a2", 4) == null);

		log(config);
	}

	@Test
	public void getDisplayIdsForRound() {
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		assertTrue("a2".equals(config.getDisplayIdsForRound(2).get("A")));
		assertTrue("a5".equals(config.getDisplayIdsForRound(1).get("B")));
		assertTrue("a1".equals(config.getDisplayIdsForRound(5).get("E")));

	}

	@Test
	public void getSecondsRemainingInChat() {
		// chat.timing: 0-30,120-150;0;;90-120,120-130;0
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		List<Integer[]> tests = new ArrayList<>();
		tests.add(new Integer[]{5, 1, 25});
		tests.add(new Integer[]{30, 1, 0});
		tests.add(new Integer[]{29, 1, 121});
		tests.add(new Integer[]{0, 2, 15});
		tests.add(new Integer[]{0, 3, 15});
		tests.add(new Integer[]{1, 4, 129});
		tests.add(new Integer[]{0, 4, 130});
		for (Integer[] test : tests) {
			int expected = test[0];
			int round = test[1];
			int current = test[2];
			int actual = config.getSecondsRemainingInChat(round, current);
			String m = String.format("Expected %d for Round %d and Current %d - Actual was %d", expected, round, current, actual);
			assertTrue(m, expected == actual);
		}
	}

	@Test
	public void listPerformance() {
		Map<String, List<String>> displayIdsPerParticipants = new HashMap<String, List<String>>();
		for (int i = 0; i < 24; i++) {
			String id = randomString();
			List<String> displayIds = new ArrayList<String>();
			for (int d = 0; d < 17; d++) {
				String displayId = randomString();
				displayIds.add(displayId);
			}
			displayIdsPerParticipants.put(id, displayIds);
		}
		log(displayIdsPerParticipants);
		int iterations = 250000;
		int hits = 0;
		long matches = 0L;
		long start = now();
		for (int i = 0; i < iterations; i++) {
			String lookup = randomString();
			List<String> displayIds = displayIdsPerParticipants.get(lookup);
			if (displayIds != null) {
				if (displayIds.contains(lookup)) {
					hits++;
				}
			}
			matches += displayIdsPerParticipants.values().stream().filter(list -> list.contains(lookup)).count();
		}
		long end = now();
		long elapsed = end - start;
		double average = (double) elapsed / (double) iterations;
		log("Elapsed: " + elapsed + "ms for " + iterations + " iterations (" + average + "ms avg)");
		log("Total Hits: " + hits);
		log("Hit Rate: " + hits / ((double) iterations * 2) * 100 + "%");
		log("Total Matches: " + matches);

	}

	@Test
	public void maxMessageSizeTest() {
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		assertTrue(config.getMaxMessageSize() == 150);
	}

	@Test
	public void noZocaloConfiguration() {
		Properties props = loadProperties("ConfigurationTestNoChat.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		assertFalse(config.isChatEnabled());
	}

	@Test
	public void participantColors() {

		List<String> colors = Arrays.asList("red", "yellow", "blue");
		List<String> participants = Arrays.asList("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10", "a11", "a12");

		List<List<String>> options = new ArrayList(Collections2.permutations(colors));
		List<String> randomParticipants = new ArrayList<>(participants);
		Collections.copy(randomParticipants, participants);
		Collections.shuffle(randomParticipants);
		log(participants);
		log(randomParticipants);

		int size = options.size();
		int current = 0;
		Map<String, List<String>> colorMap = new TreeMap<>();

		for (String participant : randomParticipants) {
			colorMap.put(participant, options.get(current++));
			if (current >= size) {
				current = 0;
			}
		}

		assertTrue(12 == colorMap.size());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("red", "yellow", "blue"))).count());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("red", "blue", "yellow"))).count());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("yellow", "red", "blue"))).count());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("yellow", "blue", "red"))).count());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("blue", "yellow", "red"))).count());
		assertTrue(2 == colorMap.values().stream().filter(c -> c.equals(Arrays.asList("blue", "red", "yellow"))).count());

	}

	@Test
	public void permutation() {
		Collection<List<String>> options = Collections2.permutations(Arrays.asList("red", "yellow", "blue"));
		assertTrue(options.size() == 6);
		assertTrue(options.contains(Arrays.asList("red", "yellow", "blue")));
		assertTrue(options.contains(Arrays.asList("red", "blue", "yellow")));
		assertTrue(options.contains(Arrays.asList("yellow", "red", "blue")));
		assertTrue(options.contains(Arrays.asList("yellow", "blue", "red")));
		assertTrue(options.contains(Arrays.asList("blue", "yellow", "red")));
		assertTrue(options.contains(Arrays.asList("blue", "red", "yellow")));
	}

	@Test
	public void reputationScore() {
		int roundId = 1;
		String message = "This is the message!";
		Properties props = loadProperties("ConfigurationTest.properties");
		ZocaloService service = ZocaloService.startNewSession(props);

		// a1=100, a2=75, a3=50, a4=0, a5=100
		service.startRound(roundId);
		service.muteSender(roundId, "a1", "a2");
		service.muteSender(roundId, "a1", "a2"); // intentional duplication

		service.muteSender(roundId, "a1", "a3");
		service.unmuteSender(roundId, "a1", "a3"); // unmute

		service.muteSender(roundId, "a2", "a3");
		service.muteSender(roundId, "a4", "a3");

		service.muteSender(roundId, "a1", "a4");
		service.muteSender(roundId, "a2", "a4");
		service.muteSender(roundId, "a3", "a4");
		service.muteSender(roundId, "a5", "a4");

		List<ChatReputationScore> scores = service.getReputationScores(roundId);

		log(scores);

		ChatReputationScore a1 = scores.stream().filter(s -> s.getId().equals("a1")).findFirst().get();
		ChatReputationScore a2 = scores.stream().filter(s -> s.getId().equals("a2")).findFirst().get();
		ChatReputationScore a3 = scores.stream().filter(s -> s.getId().equals("a3")).findFirst().get();
		ChatReputationScore a4 = scores.stream().filter(s -> s.getId().equals("a4")).findFirst().get();
		ChatReputationScore a5 = scores.stream().filter(s -> s.getId().equals("a5")).findFirst().get();
		assertTrue(a1.getScore() == 100);
		assertTrue(a2.getScore() == 75);
		assertTrue(a3.getScore() == 50);
		assertTrue(a4.getScore() == 0);
		assertTrue(a5.getScore() == 100);

	}

	@Test
	public void secondsAgo() {
		long now = System.currentTimeMillis();
		Date sent = new Date();
		sent.setTime(now - 4321L);
		long millisSinceSent = now - sent.getTime();
		int secondsSince = (int) Math.floorDiv(millisSinceSent, 1000L);
		assertTrue(secondsSince == 4);
	}

	private Properties setupProperties() {
		Properties props = new Properties();
		props.put("sessionTitle", "Chat Testing");
		props.put("rounds", "5");
		props.put("players", "a1,  a2,  a3,  a4,  a5, a6, a7, a8, a9, a10, a11, a12");
		props.put("timeLimit", "5:00");
		return props;
	}

}
