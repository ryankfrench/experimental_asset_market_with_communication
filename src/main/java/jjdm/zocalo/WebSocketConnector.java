package jjdm.zocalo;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Used to setup and receive WebSocket connections from the client web.
 *
 * @author Josh Martin
 */

@ServerEndpoint(value = "/ws")
public class WebSocketConnector {

	private static final Logger logger = Logger.getLogger(WebSocketConnector.class);
	private static final Set<WebSocketConnector> connections = new CopyOnWriteArraySet<>();
	private static Gson gson = new Gson();
	private static Set<String> allMessages = new HashSet<>();

	private Session webSocketSession;
	private String userId;
	private String page;

	/**
	 * Return a list of users (e.g. a1) who are connected via the desktop client.  The passed page for these clients is
	 * "javaWsClient", and can be found in BrowserData#CLIENT_PAGE_NAME in the wsclient project.
	 *
	 * @return
	 */
	public static List<String> getConnectedDesktopClients() {
		if (connections == null || connections.isEmpty()) {
			return new ArrayList<>();
		}
		return connections.stream().filter(c -> "javaWsClient".equals(c.getPage())).map(WebSocketConnector::getUserId).collect(Collectors.toList());
	}

	/**
	 * Used to reset messages that have already been sent.
	 */
	public static void resetUniqueMessages() {
		allMessages.clear();
	}

	/**
	 * Send a message to specific users.
	 *
	 * @param message    The message.
	 * @param recipients A list of user ID's (user names).
	 */
	public static void sendMessage(String message, List<String> recipients) {
		if (recipients == null || recipients.size() == 0) {
			logger.warn("Attempting to send message to no recipients: " + message);
			return;
		}
		Set<WebSocketConnector> clients = connections.stream().filter(c -> recipients.contains(c.getUserId())).collect(Collectors.toSet());
		sendMessageInternal(message, clients);
	}

	/**
	 * Send a message to all users.
	 *
	 * @param message The message.
	 */
	public static void sendMessageToAll(String message) {
		sendMessageInternal(message, connections);
	}

	/**
	 * Utility to convert Object into a JSON String using Gson.
	 *
	 * @param object The object to convert.
	 * @return The JSON representation.
	 */
	public static String toJson(Object object) {
		return gson.toJson(object);
	}

	/**
	 * Workaround for Zocalo's message spam - events generated based on logging appenders.
	 *
	 * @param message The message to check.
	 * @return True if this message has been seen before.
	 * @see #resetUniqueMessages()
	 */
	private static boolean isDuplicateMessage(String message) {
		return message.contains("timeRemaining") && allMessages.contains(message);
	}

	/**
	 * Plumbing of sending Websocket messages.
	 *
	 * @param message The message.
	 * @param clients The clients to send the message to.
	 */
	private static void sendMessageInternal(String message, Set<WebSocketConnector> clients) {
		if (isDuplicateMessage(message)) {
			return;
		} else {
			logger.debug(message);
			allMessages.add(message);
		}
		for (WebSocketConnector client : clients) {
			try {
				synchronized (client) {
					client.webSocketSession.getBasicRemote().sendText(message);
				}
			} catch (Exception e) {
				logger.error("Chat Error: Failed to send message to client: " + client.getLoggingId(), e);
				connections.remove(client);
				try {
					client.webSocketSession.close();
				} catch (Exception e1) {
					logger.error("Chat Error: Failed to close client session: " + client.getLoggingId(), e);
				}
			}
		}
	}

	public String getPage() {
		return page;
	}

	public String getUserId() {
		return userId;
	}

	@OnClose
	public void onClose() {
		logger.debug("onClose: " + getLoggingId());
		this.connections.remove(this);
	}

	@OnError
	public void onError(Throwable t) {
		logger.error("Error for " + getLoggingId(), t);
	}

	@OnMessage
	public void onMessage(String message) {
		logger.debug("onMessage from " + getLoggingId() + ": " + message);
	}

	@OnOpen
	public void onOpen(Session session) {

		logger.debug("onOpen: " + session.getId());
		this.webSocketSession = session;

		try {
			Map<String, List<String>> params = session.getRequestParameterMap();
			String username = params.get("userName").get(0);
			String callingPage = params.get("callingPage").get(0);
			this.userId = username;
			this.page = callingPage;
			connections.add(this);
		} catch (Exception e) {
			logger.debug("Unable to open session.", e);
		}
	}

	/**
	 * Returns the page/userId String for logging.
	 *
	 * @return The page/userId String.
	 */
	private String getLoggingId() {
		return "page=" + page + ";userId=" + userId + ";sessionId=" + webSocketSession.getId();
	}

}
