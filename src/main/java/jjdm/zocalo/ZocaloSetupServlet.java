package jjdm.zocalo;

import dojox.cometd.Bayeux;
import dojox.cometd.Client;
import dojox.cometd.RemoveListener;
import jjdm.zocalo.data.ChatState;
import jjdm.zocalo.service.ZocaloService;
import net.commerce.zocalo.ajax.dispatch.PrivateEventDispatcher;
import net.commerce.zocalo.experiment.SessionSingleton;
import net.commerce.zocalo.service.BayeuxSingleton;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used to setup Zocalo resources (logs, etc.). Replaces the following hierarchy:
 * <p>
 * <pre>
 * ServerUtil
 *   - ServletUtil
 *   	-- CometServer
 *   		--- ExperimentServer
 * </pre>
 *
 * @author Josh Martin
 * @since 2014-OCT
 */
public class ZocaloSetupServlet extends ContinuationCometdServlet {

	// JJDM - Not a good practice, but needed a way to feed the MockBayeux now that we've moved off Jetty.
	// This should work for now, though any tests will likely break without Tomcat running.
	public static ServletContext SERVLET_CONTEXT;
	private static final long serialVersionUID = 4905184226081804689L;
	public static final Object SERVLET_LOCK = new Object();

	private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private boolean isTradingPaused = false;
	private boolean isChatActive = false;
	private List<String> connectedDesktopClients = new ArrayList<>();
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Clean up anything in the Servlet.
	 */
	@Override
	public void destroy() {
		super.destroy();
		this.destroySessionWatcher();
		logger.debug("ZocaloSetupServlet Destroyed.");
	}

	@Override
	public void init() throws ServletException {
		super.init();
		this.setupBayeux();
		this.setupLog4j();
		SERVLET_CONTEXT = this.getServletContext();
		logger.debug("ZocaloSetupServlet Running.");
	}

	/**
	 * Shutdown the timer.
	 */
	private void destroySessionWatcher() {
		this.scheduler.shutdownNow();
	}

	/**
	 * Push the current round and chat time remaining.
	 */
	private void notifyCurrentTime() {
		if (SessionSingleton.getSession() == null) {
			return;
		}
		try {
			ChatState state = ZocaloService.getInstance().getChatState();
			if (state.isExperimentStarted()) {
				boolean roundActive = state.isRoundRunning();
				boolean chatActive = state.isChatActive();
				if (roundActive) {
					int roundTimeRemaining = state.getRoundSecondsRemaining();
					int chatTimeRemaining = state.getChatSecondsRemaining();
					CometHelper.publishTimeSync(roundTimeRemaining, chatTimeRemaining);
				}
			}
		} catch (Exception e) {
			logger.error("Error in notifyCurrentTime", e);
		}
	}

	/**
	 * Send notification to experimenter page about connected desktops.
	 */
	private void notifyDesktopClients() {
		if (SessionSingleton.getSession() == null) {
			return;
		}
		try {
			ZocaloService service = ZocaloService.getInstance();
			ChatState state = service.getChatState();
			if (state.isParticipationEnabled()) {
				List<String> connectedDesktops = service.getConnectedDesktops();
				if (!connectedDesktops.equals(this.connectedDesktopClients)) {
					this.connectedDesktopClients = connectedDesktops;
					CometHelper.publishActiveDesktopClients(this.connectedDesktopClients);
				}
			}
		} catch (Exception e) {
			logger.error("Error in notifyDesktopClients", e);
		}
	}

	/**
	 * Watch for changes in state for trading or chatting.
	 */
	private void notifyOnChange() {
		if (SessionSingleton.getSession() == null) {
			return;
		}
		try {
			ChatState state = ZocaloService.getInstance().getChatState();
			if (state.isExperimentStarted()) {
				boolean roundActive = state.isRoundRunning();
				boolean chatActive = state.isChatActive();
				boolean tradingPaused = state.isTradingPaused();
				if (chatActive != this.isChatActive) {
					CometHelper.publishChatActiveChange(chatActive, roundActive);
					logger.info(String.format("Chat State Change to %s: %tD %<tT", chatActive, new Date()));
				}
				if (tradingPaused != this.isTradingPaused) {
					boolean tradingActive = !tradingPaused; // note the terminology switch
					CometHelper.publishTradingActiveChange(tradingActive, roundActive);
					logger.info(String.format("Trading State Change to %s: %tD %<tT", tradingActive, new Date()));
				}
				this.isChatActive = chatActive;
				this.isTradingPaused = tradingPaused;
			}
		} catch (Exception e) {
			logger.error("Error in notifyOnChange", e);
		}
	}

	/**
	 * Used to setup Cometd Bayeux. Copied from ExperimentServer.
	 */
	private void setupBayeux() {
		AbstractBayeux bayeux = super.getBayeux();
		bayeux.setJSONCommented(true);
		BayeuxSingleton.getInstance().setBayeux(bayeux);
		new ExperimentBayeuxService(BayeuxSingleton.getInstance().getBayeux());
		this.setupSessionWatcher();
	}

	/**
	 * Used to setup initial log4j.
	 */
	private void setupLog4j() {
		URL config = this.getClass().getClassLoader().getResource("log4j.properties");
		PropertyConfigurator.configure(config);
		logger.info("log4j Successfully initialized.");
	}

	/**
	 * Startup the timer.
	 */
	private void setupSessionWatcher() {
		scheduler.scheduleAtFixedRate(this::notifyOnChange, 0, 1, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(this::notifyCurrentTime, 0, 5, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(this::notifyDesktopClients, 0, 10, TimeUnit.SECONDS);
	}

	public static class ExperimentBayeuxService extends BayeuxService {

		private static final Map<String, String> CLIENT_MAP = new HashMap<>();

		public ExperimentBayeuxService(Bayeux bayeux) {
			super(bayeux, "experiments");
			subscribe(PrivateEventDispatcher.PRIVATE_EVENT_TOPIC_SUFFIX, "sendPrivateUpdate");
		}

		public static String getClientId(String name) {
			return CLIENT_MAP.get(name);
		}

		@SuppressWarnings("unchecked")
		public Object sendPrivateUpdate(Client client, Object obj) {
			Map<String, Object> data = (Map<String, Object>) obj;
			if (Boolean.TRUE.equals(data.get("join"))) {
				String s = (String) data.get("user");
				CLIENT_MAP.put(s, client.getId());

				client.addListener(new RemoveListener() {
					@Override
					public void removed(String clientId, boolean timeout) {
						CLIENT_MAP.values().remove(clientId);
					}
				});
			}
			return obj;
		}
	}

}
