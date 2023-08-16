package jjdm.zocalo;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import jjdm.zocalo.data.ParticipationChoice;
import jjdm.zocalo.data.ZocaloConfig;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.experiment.Session;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static jjdm.zocalo.ZocaloTestHelper.createSession;
import static jjdm.zocalo.ZocaloTestHelper.endCurrentRound;
import static jjdm.zocalo.ZocaloTestHelper.loadProperties;
import static jjdm.zocalo.ZocaloTestHelper.log;
import static jjdm.zocalo.ZocaloTestHelper.startNextRound;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParticipationTest {

	private Random random = new Random();

	@Test
	public void configurationTest() throws Exception {

		// load configuration
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		ZocaloConfig config = new ZocaloConfig(props);

		assertTrue(config.isParticipationEnabled());
		assertFalse(config.isClueDistributionDirect());
		assertTrue(config.isClueDistributionAutomatic());
		assertTrue(Arrays.asList("X", "Z").equals(config.getHintsPossibleForRound(3)));
		assertTrue(Arrays.asList("X", "Y").equals(config.getHintsPossibleForRound(5)));

		props.remove("hint.distribution");
		config = new ZocaloConfig(props);
		assertTrue(config.isClueDistributionDirect());
		props.put("hint.distribution", " AUTO");
		config = new ZocaloConfig(props);
		assertTrue(config.isClueDistributionAutomatic());
		assertFalse(config.isClueDistributionDirect());

		assertTrue(config.getParticipationCostForRound(1) == 200);
		assertTrue(config.getParticipationCostForRound(3) == 0);
		assertTrue(config.getParticipationCostForRound(5) == 600);
		assertTrue(config.getParticipationClueCostForRound(1) == 50);
		assertTrue(config.getParticipationClueCostForRound(2) == 75);
		assertTrue(config.getParticipationClueCostForRound(4) == 0);
		assertTrue(config.getParticipationClueCostForRound(5) == 150);
		assertEquals(Integer.valueOf(400), config.getParticipationAccountAmountForRound(1));
		assertEquals(Integer.valueOf(-200), config.getParticipationAccountAmountForRound(3));
		assertEquals(Integer.valueOf(100), config.getParticipationAccountAmountForRound(4));

		try {
			config.getParticipationClueCostForRound(6);
			fail("Should not have 6 rounds.");
		} catch (Exception e) {
			log("Expected exception caught.");
		}

		log(config);
	}

	@Test
	public void freemarkerBasicTest() {
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		Map<String, Object> values = new HashMap<>();
		values.put("name", "Josh");
		values.put("food", "pizza");
		values.put("isCost", true);
		String page1 = config.freemarker("participationPage1", values);
		assertTrue("Hello Josh.".equals(page1));
		String page2 = config.freemarker("participationPage2", values);
		assertTrue("Page 2 pizza YES.".equals(page2));
		values.put("writeIt", true);
		String page3 = config.freemarker("participationPage3", values); // from classpath
		assertEquals("OK, I will write it.", page3);
		values.put("writeIt", false);
		page3 = config.freemarker("participationPage3", values);
		assertEquals("I will not write it.", page3);
	}

	@Test
	public void freemarkerConfigurationTest() throws Exception {
		Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_23);
		freemarkerConfig.setDefaultEncoding("UTF-8");
		freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);

		StringTemplateLoader stringLoader = new StringTemplateLoader();
		stringLoader.putTemplate("joshTest", "Hello ${name}.");

		ClassTemplateLoader classLoader = new ClassTemplateLoader(getClass(), "/templates/");

		TemplateLoader[] allLoaders = new TemplateLoader[]{stringLoader, classLoader};
		MultiTemplateLoader multiLoader = new MultiTemplateLoader(allLoaders);
		freemarkerConfig.setTemplateLoader(multiLoader);

		Template t = freemarkerConfig.getTemplate("joshTest");
		Map<String, Object> values = new HashMap<>();
		values.put("name", "Josh");
		StringWriter writer = new StringWriter();
		t.process(values, writer);
		String output = writer.toString();
		assertTrue("Hello Josh.".equals(output));

		t = freemarkerConfig.getTemplate("participationCluePage.ftl");
		assertTrue(t != null);
	}

	@Test
	public void freemarkerMulticluePage() throws Exception {
		Properties props = loadProperties("ConfigurationTestMultiClue.properties");
		props.put("hint.select.starting", "2, 1, 1, 2, 0");
		props.put("hint.select.maximum", "7, 2, 1, 2, 0");
		createSession(props);
		ZocaloService service = ZocaloService.getInstance();
		service.startParticipation();
		String html = service.generateParticipationPage("a1", "participationClueSelectPage");
		Path path = Paths.get("out/participationClueSelectPage.html");
		try (BufferedWriter writer = Files.newBufferedWriter(path)) {
			writer.write(html);
		}
		assertTrue(html.contains("For this round, you will start with 2 clues for the round, and can buy up to 5 additional clues for a total of 7 clues."));

	}

	@Test
	public void hintMultiClueTest() {

		List<String> possibleHints = Arrays.asList("X", "Y", "Z");
		int freeHints = 1;
		boolean allowRepeats = false;
		boolean shuffle = false;

		ParticipationChoice a1 = new ParticipationChoice(1, "a1", true, 2);
		ParticipationChoice a2 = new ParticipationChoice(1, "a2", false, 0);
		ParticipationChoice a3 = new ParticipationChoice(1, "a3", true, 0);
		ParticipationChoice a4 = new ParticipationChoice(1, "a4", true, 1);
		ParticipationChoice a5 = new ParticipationChoice(1, "a5", false, 2);
		List<ParticipationChoice> players = Arrays.asList(a1, a2, a3, a4, a5);

		Map<String, List<String>> hintMap = ZocaloHelper.buildCluesMultipleForRound(players, possibleHints, freeHints, allowRepeats, shuffle);
		assertEquals(Arrays.asList("X", "Y", "Z"), hintMap.get("a1"));
		assertEquals(Arrays.asList("X", "Y"), hintMap.get("a4"));
		assertEquals(Arrays.asList("Z"), hintMap.get("a3"));

		shuffle = true;
		hintMap = ZocaloHelper.buildCluesMultipleForRound(players, possibleHints, freeHints, allowRepeats, shuffle);
		assertEquals(3, hintMap.get("a1").size());
		assertEquals(3, new HashSet<>(hintMap.get("a1")).size());
		assertEquals(2, hintMap.get("a4").size());
		assertEquals(2, new HashSet<>(hintMap.get("a4")).size());
		assertEquals(1, hintMap.get("a3").size());
		assertEquals(1, new HashSet<>(hintMap.get("a3")).size());

		allowRepeats = true;
		hintMap = ZocaloHelper.buildCluesMultipleForRound(players, possibleHints, freeHints, allowRepeats, shuffle);
		List<String> allRepeatedHints = new ArrayList<>();
		assertEquals(3, hintMap.get("a1").size());
		assertEquals(2, hintMap.get("a4").size());
		assertEquals(1, hintMap.get("a3").size());
		allRepeatedHints.addAll(hintMap.get("a1"));
		allRepeatedHints.addAll(hintMap.get("a4"));
		allRepeatedHints.addAll(hintMap.get("a3"));
		// should be equally distributed
		assertEquals(2L, allRepeatedHints.stream().filter(s -> "X".equals(s)).count());
		assertEquals(2L, allRepeatedHints.stream().filter(s -> "Y".equals(s)).count());
		assertEquals(2L, allRepeatedHints.stream().filter(s -> "Z".equals(s)).count());
		log(hintMap);

	}

	@Test
	public void hintTest() {

		// base case - set by hint.distribution
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		ZocaloConfig config = new ZocaloConfig(props);
		Map<String, String> hintMap = config.getHintMap();
		assertTrue(3 == hintMap.size());
		assertTrue(hintMap.get("X").equals("The certificate is not worth 50 Francs (Not X-Dividend)"));
		assertTrue(hintMap.get("Y").equals("The certificate is not worth 240 Francs (Not Y-Dividend)"));
		assertTrue(hintMap.get("Z").equals("The certificate is not worth 490 Francs (Not Z-Dividend)"));

		// now set by a1.hint, etc.
		props.remove("hint.distribution");
		config = new ZocaloConfig(props);
		hintMap = config.getHintMap();
		assertTrue(3 == hintMap.size());
		assertTrue(hintMap.get("X").equals("The certificate is not worth 50 Francs (Not X-Dividend)"));
		assertTrue(hintMap.get("Y").equals("The certificate is not worth 240 Francs (Not Y-Dividend)"));
		assertTrue(hintMap.get("Z").equals("The certificate is not worth 490 Francs (Not Z-Dividend)"));

		// remove all but a1.hint (only X and Y)
		props.remove("a2.hint");
		props.remove("a3.hint");
		props.remove("a4.hint");
		props.remove("a5.hint");
		config = new ZocaloConfig(props);
		hintMap = config.getHintMap();
		assertTrue(2 == hintMap.size());
		assertTrue(hintMap.get("X").equals("The certificate is not worth 50 Francs (Not X-Dividend)"));
		assertTrue(hintMap.get("Y").equals("The certificate is not worth 240 Francs (Not Y-Dividend)"));
	}

	@Test
	public void joinTest() {
		List<String> clues = new ArrayList<>();
		clues.add("X");
		assertEquals("X", String.join(",", clues));
		clues.add("Y");
		assertEquals("X,Y", String.join(",", clues));
	}

	@Test
	public void multiClueTest() {
		Properties props = loadProperties("ConfigurationTestMultiClue.properties");
		Session session = createSession(props);
		ZocaloService service = ZocaloService.getInstance();
		ZocaloConfig config = service.getConfiguration();
		assertEquals(0, config.getParticipationSelectStartingForRound(1).intValue());
		assertEquals(0, config.getParticipationSelectStartingForRound(5).intValue());
		assertEquals(1, config.getParticipationSelectMaximumForRound(3).intValue());
		assertTrue(config.isClueDistributionSelect());
		assertTrue(config.isParticipationClueRepeats());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiClueTestException() {
		Properties props = loadProperties("ConfigurationTestMultiClue.properties");
		props.remove("hint.select.maximum");
		Session session = createSession(props);
	}

	@Test
	public void multiClueTestNoException() {
		Properties props = loadProperties("ConfigurationTestMultiClue.properties");
		props.remove("hint.select.maximum");
		props.remove("participation.enabled");
		Session session = createSession(props);
	}

	@Test
	public void sessionTest() {

		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		ZocaloService service = ZocaloService.startNewSession(props);
		ZocaloConfig config = service.getConfiguration();

		ParticipationChoice choice = service.addParticipationChoice(1, "a1", true, true);
		assertTrue(choice.isParticipate());
		assertTrue(choice.isReceiveClue());
		choice = service.addParticipationChoice(1, "a2", false, true);
		assertFalse(choice.isParticipate());
		assertTrue(choice.isReceiveClue());
		choice = service.addParticipationChoice(1, "a3", true, false);
		assertTrue(choice.isParticipate());
		assertFalse(choice.isReceiveClue());
		choice = service.addParticipationChoice(1, "a4", false, false);
		assertFalse(choice.isParticipate());
		assertFalse(choice.isReceiveClue());

		choice = service.addParticipationChoice(1, "a5", true, true);
		Map<String, List<String>> cluesForParticipants = service.generateParticipantCluesForRound(1);
		assertTrue(3 == cluesForParticipants.size());

		choice = service.addParticipationChoice(2, "a3", false, false);
		choice = service.addParticipationChoice(2, "a4", false, false);
		choice = service.addParticipationChoice(3, "a4", false, false);

		assertTrue(choice.getParticipantId().equals("a4"));
		assertTrue(3 == choice.getRoundId());

		cluesForParticipants = service.generateParticipantCluesForRound(2);
		assertTrue(0 == cluesForParticipants.size());

		try {
			service.generateParticipantCluesForRound(2);
			fail("Should not be able to generate clues again for round 2.");
		} catch (IllegalArgumentException e) {
			log("Successfully caught error.");
		}

		service.addParticipationChoice(4, "a1", true, true);
		service.addParticipationChoice(4, "a2", true, true);
		service.addParticipationChoice(4, "a3", true, true);
		service.addParticipationChoice(4, "a4", true, true);
		service.addParticipationChoice(4, "a5", true, false);
		cluesForParticipants = service.generateParticipantCluesForRound(4);
		assertTrue(4 == cluesForParticipants.size());
		log("cluesForParticipants: " + cluesForParticipants);

		assertTrue(5 == service.getParticipationChoicesForRound(1).size());
		assertTrue(2 == service.getParticipationChoicesForRound(2).size());
		assertTrue(1 == service.getParticipationChoicesForRound(3).size());
		assertTrue(0 == service.getParticipationChoicesForRound(5).size());

		assertTrue(null == service.getParticipationChoiceForRoundAndUser(4, "a6"));
		assertTrue(null != service.getParticipationChoiceForRoundAndUser(4, "a5"));
		assertTrue(service.getParticipationChoiceForRoundAndUser(4, "a5").isParticipate());
		assertFalse(service.getParticipationChoiceForRoundAndUser(4, "a5").isReceiveClue());

		try {
			choice = service.addParticipationChoice(1, "a1", false, false);
			fail("Should not have 6 rounds.");
		} catch (Exception e) {
			log("Expected exception caught.");
		}
	}

	@Test
	public void splitTest() {
		String test = ", , , , ,";
		assertEquals(5, test.split(",").length);
		assertEquals(5, test.split(",", 0).length);
		assertEquals(6, test.split(",", -1).length);
	}

	@Test
	public void testMulticlueParticipationCash() {
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		props.put("participation.participate.cost", "-10, -20, -30, , 50");
		props.put("participation.clue.cost", "-1, -2 , 3, -4, -5");
		props.put("participation.account.amounts", "400, 300, -200, 100, 400");
		props.put("hint.distribution", "select");
		props.put("hint.select.starting", "0, 0, 1, 1, 0");
		props.put("hint.select.maximum", "0, 1, 2, 3, 3");
		props.put("hint.select.allow.repeats", "false");

		Session session = createSession(props);
		ZocaloService service = ZocaloService.getInstance();

		// round 1
		int round = 1;
		service.startParticipation();
		service.addParticipationMultiClueChoice(round, "a1", true, 0);
		service.addParticipationMultiClueChoice(round, "a2", false, 0);
		service.addParticipationMultiClueChoice(round, "a3", true, 0);
		service.addParticipationMultiClueChoice(round, "a4", false, 0);
		service.addParticipationMultiClueChoice(round, "a5", true, 0);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(400 - 10, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(400, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(400 - 10, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(400, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(400 - 10, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 2
		round = 2;
		service.startParticipation();
		service.addParticipationMultiClueChoice(round, "a1", true, 0);
		service.addParticipationMultiClueChoice(round, "a2", false, 0);
		service.addParticipationMultiClueChoice(round, "a3", true, 1);
		service.addParticipationMultiClueChoice(round, "a4", false, 0);
		service.addParticipationMultiClueChoice(round, "a5", true, 1);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(300 - 20, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(300, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(300 - 20 - 2 * 1, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(300, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(300 - 20 - 2 * 1, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 3
		round = 3;
		service.startParticipation();
		service.addParticipationMultiClueChoice(round, "a1", true, 0);
		service.addParticipationMultiClueChoice(round, "a2", false, 0);
		service.addParticipationMultiClueChoice(round, "a3", true, 1);
		service.addParticipationMultiClueChoice(round, "a4", false, 0);
		service.addParticipationMultiClueChoice(round, "a5", true, 1);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(-200 - 30, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(-200, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(-200 - 30 + 3 * 1, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(-200, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(-200 - 30 + 3 * 1, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 4
		round = 4;
		service.startParticipation();
		service.addParticipationMultiClueChoice(round, "a1", true, 0);
		service.addParticipationMultiClueChoice(round, "a2", false, 0);
		service.addParticipationMultiClueChoice(round, "a3", true, 1);
		service.addParticipationMultiClueChoice(round, "a4", false, 0);
		service.addParticipationMultiClueChoice(round, "a5", true, 2);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(100, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(100, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(100 - 4 * 1, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(100, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(100 - 4 * 2, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 5
		round = 5;
		service.startParticipation();
		service.addParticipationMultiClueChoice(round, "a1", true, 0);
		service.addParticipationMultiClueChoice(round, "a2", false, 0);
		service.addParticipationMultiClueChoice(round, "a3", true, 2);
		service.addParticipationMultiClueChoice(round, "a4", false, 0);
		service.addParticipationMultiClueChoice(round, "a5", true, 3);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(400 + 50, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(400, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(400 + 50 - 5 * 2, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(400, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(400 + 50 - 5 * 3, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		int expected = (400 - 10) + (300 - 20) + (-200 - 30) + (100) + (400 + 50);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a1"));
		expected = (400) + (300) + (-200) + (100) + (400);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a2"));
		expected = (400 - 10) + (300 - 20 - 2 * 1) + (-200 - 30 + 3 * 1) + (100 - 4 * 1) + (400 + 50 - 5 * 2);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a3"));
		expected = (400) + (300) + (-200) + (100) + (400);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a4"));
		expected = (400 - 10) + (300 - 20 - 2 * 1) + (-200 - 30 + 3 * 1) + (100 - 4 * 2) + (400 + 50 - 5 * 3);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a5"));

		Map<String, List<Integer>> results = service.getParticipationCashPerUserForRounds(round);
		log(results);
		for (String participant : service.getConfiguration().getParticipants()) {
			for (int r = 1; r <= 5; r++) {
				int expectedCash = service.getParticipationCashForParticipant(r, participant);
				assertEquals(expectedCash, results.get(participant).get(r - 1).intValue());
			}
		}

	}

	@Test
	public void testNoCostForParticipation() {
		// load configuration
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		props.put("participation.participate.cost", "0,25,,-75,100");
		ZocaloConfig config = new ZocaloConfig(props);
		assertTrue(0 == config.getParticipationCostForRound(1));
		assertTrue(25 == config.getParticipationCostForRound(2));
		assertTrue(null == config.getParticipationCostForRound(3));
		assertTrue(-75 == config.getParticipationCostForRound(4));
		assertTrue(100 == config.getParticipationCostForRound(5));
	}

	@Test
	public void testParticipationCash() {
		Properties props = loadProperties("ConfigurationTestParticipation.properties");
		props.put("participation.participate.cost", "-10, -20, -30, 40, 50");
		props.put("participation.clue.cost", "-1, -2 , 3, 4, 5");
		props.put("participation.account.amounts", "400, 300, -200, 100, 400");

		Session session = createSession(props);
		ZocaloService service = ZocaloService.getInstance();

		// round 1
		int round = 1;
		service.startParticipation();
		service.addParticipationChoice(round, "a1", true, true);
		service.addParticipationChoice(round, "a2", false, true);
		service.addParticipationChoice(round, "a3", true, false);
		service.addParticipationChoice(round, "a4", false, false);
		service.addParticipationChoice(round, "a5", true, true);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(400 - 10 - 1, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(400 - 1, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(400 - 10, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(400, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(400 - 10 - 1, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 2
		service.startParticipation();
		startNextRound();
		endCurrentRound();

		// round 3
		round = 3;
		service.startParticipation();
		service.addParticipationChoice(round, "a1", true, true);
		service.addParticipationChoice(round, "a2", false, true);
		service.addParticipationChoice(round, "a3", true, false);
		service.addParticipationChoice(round, "a4", false, false);
		service.addParticipationChoice(round, "a5", true, true);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(-200 - 30 + 3, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(-200 + 3, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(-200 - 30, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(-200, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(-200 - 30 + 3, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		// round 4
		round = 4;
		service.startParticipation();
		service.addParticipationChoice(round, "a1", true, true);
		service.addParticipationChoice(round, "a2", false, true);
		service.addParticipationChoice(round, "a3", true, false);
		service.addParticipationChoice(round, "a4", false, false);
		service.addParticipationChoice(round, "a5", true, true);
		startNextRound();
		assertEquals(round, session.getCurrentRound());
		assertEquals(100 + 40 + 4, service.getParticipationCashForParticipant(round, "a1"));
		assertEquals(100 + 4, service.getParticipationCashForParticipant(round, "a2"));
		assertEquals(100 + 40, service.getParticipationCashForParticipant(round, "a3"));
		assertEquals(100, service.getParticipationCashForParticipant(round, "a4"));
		assertEquals(100 + 40 + 4, service.getParticipationCashForParticipant(round, "a5"));
		endCurrentRound();

		int expected = (400 - 10 - 1) + (300) + (-200 - 30 + 3) + (100 + 40 + 4);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a1"));
		expected = (400 - 1) + (300) + (-200 + 3) + (100 + 4);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a2"));
		expected = (400 - 10) + (300) + (-200 - 30) + (100 + 40);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a3"));
		expected = (400) + (300) + (-200) + (100);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a4"));
		expected = (400 - 10 - 1) + (300) + (-200 - 30 + 3) + (100 + 40 + 4);
		assertEquals(expected, service.getParticipationCashTotalForParticipant(round, "a5"));

		Map<String, List<Integer>> results = service.getParticipationCashPerUserForRounds(round);
		log(results);
		for (String participant : service.getConfiguration().getParticipants()) {
			for (int r = 1; r <= 4; r++) {
				int expectedCash = service.getParticipationCashForParticipant(r, participant);
				assertEquals(expectedCash, results.get(participant).get(r - 1).intValue());
			}
		}

	}

	@Test
	public void testRandom() {
		Random r = new Random();
		List<String> values = Arrays.asList("A", "B", "C", "D", "E");
		for (int i = 0; i < 20; i++) {
			int nextInt = r.nextInt(values.size());
			log(nextInt + ": " + values.get(nextInt));
		}
	}

}
