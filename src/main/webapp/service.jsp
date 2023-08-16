<%@page import="jjdm.zocalo.*, jjdm.zocalo.data.*, jjdm.zocalo.service.*, net.commerce.zocalo.experiment.*, java.util.*, com.google.gson.*, org.apache.log4j.Logger" %>

<%

Logger logger = Logger.getLogger("jjdm.jsp.service_jsp");
Gson gson = new Gson();
Session zSession = SessionSingleton.getSession();
String userId = request.getParameter("u");
String action = request.getParameter("a");
String json = null;

ZocaloService zocaloService = null;
ZocaloConfig zocaloConfig = null;
ChatState chatState = null;
int currentRound = 0;

if(userId != null && zSession != null) {
	zocaloService = ZocaloService.getInstance();
	zocaloConfig = zocaloService.getConfiguration();
	chatState = zocaloService.getChatState();
	currentRound = chatState.getCurrentRound();
}

if ("addMessage".equals(action)) {
	String message = request.getParameter("m");
	String recipientArray = request.getParameter("r");
	List<String> recipients = Arrays.asList(recipientArray.split(","));
	ChatMessage chatMessage = zocaloService.addMessage(currentRound, userId, recipients, message);
	json = gson.toJson(chatMessage);
} else if ("getMessages".equals(action)) {
	List<ChatMessage> messages = zocaloService.getMessages(currentRound, userId);
	json = gson.toJson(messages);
} else if ("allUsers".equals(action)) {
	TreeMap<String, String> allUsers = zocaloConfig.getDisplayIdsForRound(currentRound);
	json = gson.toJson(allUsers);
} else if ("allMutedUsers".equals(action)) {
	List<String> mutedUsers = zocaloService.getMutedSenders(currentRound, userId);
	json = gson.toJson(mutedUsers);
} else if ("mute".equals(action)) {
	String data = request.getParameter("d");
	List<String> result = zocaloService.muteSender(currentRound, userId, data);
	json = gson.toJson(result);
} else if ("unmute".equals(action)) {
	String data = request.getParameter("d");
	List<String> result = zocaloService.unmuteSender(currentRound, userId, data);
	json = gson.toJson(result);
} else if ("choices".equals(action)) {
	List<String> choices = zocaloConfig.getMessageChoicesForRound(userId, currentRound);
	if(choices == null) {
		choices = new ArrayList<String>();
	}
	json = gson.toJson(choices);
} else if ("maxMessageSize".equals(action)) {
	int maxMessageSize = zocaloConfig.getMaxMessageSize();
	json = gson.toJson(maxMessageSize);
} else if ("chatState".equals(action)) {
	json = gson.toJson(chatState);
} else if ("liveUpdate".equals(action)) {
	Map<String, Object> message = ZocaloHelper.buildLiveUpdateMessage();
	json = gson.toJson(message);
} else if ("addParticipationChoice".equals(action)) {
	boolean willParticipate = "true".equals(request.getParameter("p"));
	boolean receiveClue = "true".equals(request.getParameter("c"));
	ParticipationChoice participationChoice = zocaloService.addParticipationChoice(chatState.getParticipationRound(), userId, willParticipate, receiveClue);
	json = gson.toJson(participationChoice);
} else if ("addParticipationMultiClueChoice".equals(action)) {
	boolean willParticipate = "true".equals(request.getParameter("p"));
	int additionalClues = Integer.valueOf(request.getParameter("c"));
	ParticipationChoice participationChoice = zocaloService.addParticipationMultiClueChoice(chatState.getParticipationRound(), userId, willParticipate, additionalClues);
	json = gson.toJson(participationChoice);
} else if ("participationChoices".equals(action)) {
	List<ParticipationChoice> participationChoices = zocaloService.getParticipationChoicesForRound(chatState.getParticipationRound());
	json = gson.toJson(participationChoices);
} else if ("participationChoiceForUser".equals(action)) {
	ParticipationChoice choice = zocaloService.getParticipationChoiceForRoundAndUser(chatState.getParticipationRound(), userId);
	json = gson.toJson(choice);
} else if ("participationCashForUser".equals(action)) {
	Integer participationCash = zocaloService.getParticipationCashForParticipant(currentRound, userId);
	json = gson.toJson(participationCash);
} else if ("participationCashTotalForUser".equals(action)) {
	Integer participationCash = zocaloService.getParticipationCashTotalForParticipant(currentRound, userId);
	json = gson.toJson(participationCash);
} else if ("participationCashTotals".equals(action)) {
	Map<String,List<Integer>> participationCash = zocaloService.getParticipationCashPerUserForRounds(currentRound);
	json = gson.toJson(participationCash);
} else if ("generateParticipationPage".equals(action)) {
	String templatePage = request.getParameter("p");
	String output = zocaloService.generateParticipationPage(userId, templatePage);
	json = gson.toJson(output);
} else if ("validateUser".equals(action)) {
	List<String> allUsers = zocaloConfig.getParticipants();
	json = allUsers.contains(userId) ? "OK" : "FAIL"; // JJDM Note: Not standard JSON
} else if ("connectedDesktops".equals(action)) {
	if(zocaloService != null) {
		List<String> desktops = zocaloService.getConnectedDesktops();
		json = gson.toJson(desktops);
	} else {
		json = "[]";
	}
} else if ("terminateAllConnectedDesktops".equals(action)) {
	if("EXPERIMENTER_SUB_FRAME".equals(userId)) {
		zocaloService.terminateAllConnectedDesktops();
		json = gson.toJson("Terminated Successfully");
	}
} else if ("completeRefresh".equals(action)) {
	Map<String, Object> data = new HashMap<>();
	TreeMap<String, String> allUsers = zocaloConfig.getDisplayIdsForRound(currentRound);
	List<String> mutedUsers = zocaloService.getMutedSenders(currentRound, userId);
	List<ChatMessage> messages = zocaloService.getMessages(currentRound, userId);
	List<String> choices = zocaloConfig.getMessageChoicesForRound(userId, currentRound);
	if(choices == null) {
		choices = new ArrayList<String>();
	}
	int maxMessageSize = zocaloConfig.getMaxMessageSize();
	List<ChatReputationScore> reputationScores = zocaloService.getReputationScores(currentRound);
	Map<String, String> colorsForChoice = new HashMap<>();
	if(zocaloConfig.isChoiceColorsEnabled()) {
	    colorsForChoice = zocaloConfig.getChoiceColorMap().get(userId);
	}
	data.put("allUsers", allUsers);
	data.put("mutedUsers", mutedUsers);
	data.put("messages", messages);
	data.put("choices", choices);
	data.put("maxMessageSize", maxMessageSize);
	data.put("reputationScores", reputationScores);
	data.put("colorsForChoice", colorsForChoice);
	data.put("colorChartEnabled", zocaloConfig.isColorChartEnabled());
	json = gson.toJson(data);
}


logger.debug("Result: userId=" + userId + "; action=" + action + "; json=" + json);

if(json == null) {
	response.sendError(404);
} else {
	out.println(json);
	response.setContentType("application/json");
}

%>