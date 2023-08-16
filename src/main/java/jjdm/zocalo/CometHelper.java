package jjdm.zocalo;

import jjdm.zocalo.data.ChatMessage;
import jjdm.zocalo.data.ChatReputationScore;
import jjdm.zocalo.data.ChatState;
import jjdm.zocalo.data.ParticipationChoice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to interact with CometD.
 *
 * @author Josh Martin
 */
public class CometHelper {

	private static final String CHAT_TOPIC = "/chat";
	private static final String GLOBAL_TOPIC = "/global";

	/**
	 * Publish a list of connected desktops to the experimenter (EXPERIMENTER_SUB_FRAME) page.
	 *
	 * @param connectedDesktops A list of connected users (e.g. a1, a2).
	 */
	public static void publishActiveDesktopClients(List<String> connectedDesktops) {
		if (connectedDesktops == null || connectedDesktops.isEmpty()) {
			connectedDesktops = new ArrayList<>();
		}
		Map<String, Object> message = new HashMap<>();
		message.put("connectedDesktops", connectedDesktops);
		publish(GLOBAL_TOPIC, "CONNECTED_DESKTOPS", message, Arrays.asList("EXPERIMENTER_SUB_FRAME"));
	}

	/**
	 * Publish a chat message.
	 *
	 * @param chat The chat.
	 */
	public static void publishChat(ChatMessage chat) {
		Map<String, Object> message = new HashMap<>();
		message.put("senderId", chat.getSenderId());
		message.put("recipientIds", chat.getRecipientIds());
		message.put("message", chat.getMessage());
		message.put("timestamp", chat.getTimestamp());
		publish(CHAT_TOPIC, "NEW_CHAT_MESSAGE", message);
	}

	/**
	 * Used to change the state of the chat window for participants.
	 *
	 * @param turnedOn    Has the chat been turned on?
	 * @param roundActive Is the round active (still running)?
	 */
	public static void publishChatActiveChange(boolean turnedOn, boolean roundActive) {
		Map<String, Object> message = new HashMap<>();
		message.put("newState", turnedOn);
		message.put("roundActive", roundActive);
		publish(GLOBAL_TOPIC, "CHAT_STATE_CHANGE", message);
	}

	/**
	 * Used to push out the current chat state, regardless of action.
	 *
	 * @param chatState The current chat state.
	 */
	public static void publishChatState(ChatState chatState) {
		publish(GLOBAL_TOPIC, "EXPERIMENTER_ACTION", chatState.toMap());
	}

	/**
	 * Used to inform the all machines to shutdown the IE browser (even if not running).
	 */
	public static void publishParticipationBrowserShutdown() {
		Map<String, Object> message = new HashMap<>();
		message.put("action", "SHUTDOWN");
		publish(GLOBAL_TOPIC, "PARTICIPATION_BROWSER", message);
	}

	/**
	 * Used to inform the non-participant machines to start the browser.
	 *
	 * @param notParticipating The list of people not participating.
	 */
	public static void publishParticipationBrowserStart(List<String> notParticipating) {
		Map<String, Object> message = new HashMap<>();
		message.put("action", "STARTUP");
		publish(GLOBAL_TOPIC, "PARTICIPATION_BROWSER", message, notParticipating);
	}

	/**
	 * Used to inform the all machines terminate the client.
	 */
	public static void publishParticipationBrowserTerminate() {
		Map<String, Object> message = new HashMap<>();
		message.put("action", "TERMINATE");
		publish(GLOBAL_TOPIC, "PARTICIPATION_BROWSER", message);
	}

	/**
	 * Send a message to indicated that participation is running.
	 *
	 * @param chatState The current chat state.
	 */
	public static void publishParticipationChange(ChatState chatState) {
		publish(GLOBAL_TOPIC, "PARTICIPATION_STATE_CHANGE", chatState.toMap());
	}

	/**
	 * Used to inform the experimenter that a choice was made (progress on screen).
	 *
	 * @param choice The choice by the participant for the upcoming round.
	 */
	public static void publishParticipationChoice(ParticipationChoice choice) {
		Map<String, Object> message = new HashMap<>();
		message.put("roundId", choice.getRoundId());
		message.put("participantId", choice.getParticipantId());
		message.put("participate", choice.isParticipate());
		message.put("receiveClue", choice.isReceiveClue());
		message.put("additionalClues", choice.getAdditionalClues());
		publish(GLOBAL_TOPIC, "NEW_PARTICIPATION_CHOICE", message);
	}

	/**
	 * Push the most recent reputation scores out to clients.
	 *
	 * @param reputationScores The current reputation scores.
	 */
	public static void publishReputationScores(List<ChatReputationScore> reputationScores) {
		Map<String, Object> message = new HashMap<>();
		message.put("reputationScores", reputationScores);
		publish(CHAT_TOPIC, "REPUTATION_SCORES", message);
	}

	/**
	 * Push the current time remaining in the round and chat.
	 *
	 * @param roundTimeRemaining The time (in seconds) remaining in the round.
	 * @param chatTimeRemaining  The time (in seconds) remaining to chat.
	 */
	public static void publishTimeSync(int roundTimeRemaining, int chatTimeRemaining) {
		Map<String, Object> message = new HashMap<>();
		message.put("roundTimeRemaining", roundTimeRemaining);
		message.put("chatTimeRemaining", chatTimeRemaining);
		publish(GLOBAL_TOPIC, "TIME_SYNC", message);
	}

	/**
	 * Used to change the state of the trading window for participants.
	 *
	 * @param turnedOn    Has trading been turned on?
	 * @param roundActive Is the round active (still running)?
	 */
	public static void publishTradingActiveChange(boolean turnedOn, boolean roundActive) {
		Map<String, Object> message = new HashMap<>();
		message.put("newState", turnedOn);
		message.put("roundActive", roundActive);
		publish(GLOBAL_TOPIC, "TRADING_STATE_CHANGE", message);
	}

	/**
	 * Publish a topic.
	 *
	 * @param topic   The topic to publish.
	 * @param message The message.
	 */
	private static void publish(String topic, String id, Map<String, Object> message) {
		message.put("topic", topic);
		message.put("id", id);
		String jsonMessage = WebSocketConnector.toJson(message);
		WebSocketConnector.sendMessageToAll(jsonMessage);
	}

	/**
	 * Publish a topic to a subset of participants.
	 *
	 * @param id         The ID of the message.
	 * @param topic      The topic to publish.
	 * @param message    The message.
	 * @param recipients The recipients (e.g. [a1, a3]) who should receive the message.
	 */
	private static void publish(String topic, String id, Map<String, Object> message, List<String> recipients) {
		message.put("topic", topic);
		message.put("id", id);
		String jsonMessage = WebSocketConnector.toJson(message);
		WebSocketConnector.sendMessage(jsonMessage, recipients);
	}
}
