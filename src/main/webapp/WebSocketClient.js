var logger = console;

/**
 * Get the userName parameter from the search string.
 */
function getUsername() {
	return getParameter("userName");
}

/**
 * Get a parameter value from the URL.
 */
function getParameter(param) {
	var r = new RegExp(".*" + param + "=(\\w+).*", "g");
	var p = window.location.search.replace(r, "$1");
	if(!p || p.length == window.location.search.length) {
		throw "Parameter for " + param + " was not found.";
	}
	return p;
}

/**
 * Connect to a websocket connection and provide messages.
 */
function createWebSocket(onMessageFunction) {
	var cp = location.pathname;
	var u = getUsername();
	return new WebSocketClient(cp, u, onMessageFunction);
}

/**
 * Connect to a websocket connection and provide messages.
 */
function createWebSocketForUser(u, onMessageFunction) {
	var cp = location.pathname;
	return new WebSocketClient(cp, u, onMessageFunction);
}

/**
 * Convert seconds into MM:SS display.
 */
function secondsToMmSsDisplay(seconds) {
	var pad = "00";
	var minutesLeft = Math.floor(seconds / 60);
	var secondsLeft = "" + (seconds - (minutesLeft * 60));
	secondsLeft = pad.substring(0, pad.length - secondsLeft.length) + secondsLeft;
	return minutesLeft + ":" + secondsLeft;
}

/**
 * Connect to a websocket connection and provide messages.
 */
function WebSocketClient(callingPage, userId, onMessageFunction) {

	this.userId = userId;
	this.callingPage = callingPage;
	this.baseUrl = "ws://" + location.hostname + ":" + location.port + "/ws";
	this.wsUrl = this.baseUrl + "?userName=" + this.userId + "&callingPage=" + this.callingPage;

	function getLoggingId() {
		return "userId=" + userId + "; callingPage=" + callingPage;
	}

	this.sendMessage = function sendMessage(message) {
		ws.send(message);
		logger.debug("Message sent for " + getLoggingId() + ": " + message);
	}

	var ws = new WebSocket(this.wsUrl);

	ws.onmessage = onMessageFunction;

	ws.onopen = function() {
		logger.debug("Opened for " + getLoggingId());
	}

	ws.onclose = function() {
		logger.debug("Closed for " + getLoggingId());
	}

	ws.onerror = function(error) {
		logger.error("Error for " + getLoggingId() + ": " + error);
	}

}