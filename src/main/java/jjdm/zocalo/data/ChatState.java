package jjdm.zocalo.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about the state of trading and chat.
 */
public class ChatState {

	private boolean betweenRounds;
	private boolean chatActive;
	private boolean chatBlockEnabled;
	private boolean chatBlockShowReputation;
	private boolean chatEnabled;
	private int chatSecondsRemaining;
	private boolean clueDistributionMultiClue = false;
	private int currentRound;
	private boolean experimentEnded;
	private boolean experimentStarted;
	private int maxPrice;
	private int multiClueInitialCount = 0;
	private int multiClueMaximumCount = 0;
	private boolean participationEnabled;
	private int participationRound;
	private boolean participationRunning;
	private Integer roundClueCost;
	private Integer roundParticipationCash;
	private Integer roundParticipationCost;
	private boolean roundRunning;
	private int roundSecondsElapsed;
	private int roundSecondsRemaining;
	private int roundTime;
	private boolean tradingPaused;

	public int getChatSecondsRemaining() {
		return chatSecondsRemaining;
	}

	public int getCurrentRound() {
		return currentRound;
	}

	public int getMaxPrice() {
		return maxPrice;
	}

	public int getMultiClueInitialCount() {
		return multiClueInitialCount;
	}

	public int getMultiClueMaximumCount() {
		return multiClueMaximumCount;
	}

	public int getParticipationRound() {
		return participationRound;
	}

	public Integer getRoundClueCost() {
		return roundClueCost;
	}

	public Integer getRoundParticipationCash() {
		return roundParticipationCash;
	}

	public Integer getRoundParticipationCost() {
		return roundParticipationCost;
	}

	public int getRoundSecondsElapsed() {
		return roundSecondsElapsed;
	}

	public int getRoundSecondsRemaining() {
		return roundSecondsRemaining;
	}

	public int getRoundTime() {
		return roundTime;
	}

	public boolean isBetweenRounds() {
		return betweenRounds;
	}

	public boolean isChatActive() {
		return chatActive;
	}

	public boolean isChatBlockEnabled() {
		return chatBlockEnabled;
	}

	public boolean isChatBlockShowReputation() {
		return chatBlockShowReputation;
	}

	public boolean isChatEnabled() {
		return chatEnabled;
	}

	public boolean isClueDistributionMultiClue() {
		return clueDistributionMultiClue;
	}

	public boolean isExperimentEnded() {
		return experimentEnded;
	}

	public boolean isExperimentStarted() {
		return experimentStarted;
	}

	public boolean isParticipationEnabled() {
		return participationEnabled;
	}

	public boolean isParticipationRunning() {
		return participationRunning;
	}

	public boolean isRoundRunning() {
		return roundRunning;
	}

	public boolean isTradingPaused() {
		return tradingPaused;
	}

	public void setBetweenRounds(boolean betweenRounds) {
		this.betweenRounds = betweenRounds;
	}

	public void setChatActive(boolean chatActive) {
		this.chatActive = chatActive;
	}

	public void setChatBlockEnabled(boolean chatBlockEnabled) {
		this.chatBlockEnabled = chatBlockEnabled;
	}

	public void setChatBlockShowReputation(boolean chatBlockShowReputation) {
		this.chatBlockShowReputation = chatBlockShowReputation;
	}

	public void setChatEnabled(boolean chatEnabled) {
		this.chatEnabled = chatEnabled;
	}

	public void setChatSecondsRemaining(int chatSecondsRemaining) {
		this.chatSecondsRemaining = chatSecondsRemaining;
	}

	public void setClueDistributionMultiClue(boolean clueDistributionMultiClue) {
		this.clueDistributionMultiClue = clueDistributionMultiClue;
	}

	public void setCurrentRound(int currentRound) {
		this.currentRound = currentRound;
	}

	public void setExperimentEnded(boolean experimentEnded) {
		this.experimentEnded = experimentEnded;
	}

	public void setExperimentStarted(boolean experimentStarted) {
		this.experimentStarted = experimentStarted;
	}

	public void setMaxPrice(int maxPrice) {
		this.maxPrice = maxPrice;
	}

	public void setMultiClueInitialCount(int multiClueInitialCount) {
		this.multiClueInitialCount = multiClueInitialCount;
	}

	public void setMultiClueMaximumCount(int multiClueMaximumCount) {
		this.multiClueMaximumCount = multiClueMaximumCount;
	}

	public void setParticipationEnabled(boolean participationEnabled) {
		this.participationEnabled = participationEnabled;
	}

	public void setParticipationRound(int participationRound) {
		this.participationRound = participationRound;
	}

	public void setParticipationRunning(boolean participationRunning) {
		this.participationRunning = participationRunning;
	}

	public void setRoundClueCost(Integer roundClueCost) {
		this.roundClueCost = roundClueCost;
	}

	public void setRoundParticipationCash(Integer roundParticipationCash) {
		this.roundParticipationCash = roundParticipationCash;
	}

	public void setRoundParticipationCost(Integer roundParticipationCost) {
		this.roundParticipationCost = roundParticipationCost;
	}

	public void setRoundRunning(boolean roundRunning) {
		this.roundRunning = roundRunning;
	}

	public void setRoundSecondsElapsed(int roundSecondsElapsed) {
		this.roundSecondsElapsed = roundSecondsElapsed;
	}

	public void setRoundSecondsRemaining(int roundSecondsRemaining) {
		this.roundSecondsRemaining = roundSecondsRemaining;
	}

	public void setRoundTime(int roundTime) {
		this.roundTime = roundTime;
	}

	public void setTradingPaused(boolean tradingPaused) {
		this.tradingPaused = tradingPaused;
	}

	/**
	 * Convert the chat state to a hash map. Useful for sending JSON messages.
	 *
	 * @return The map representation.
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> message = new HashMap<>();
		message.put("roundRunning", this.isRoundRunning());
		message.put("currentRound", this.getCurrentRound());
		message.put("experimentStarted", this.isExperimentStarted());
		message.put("betweenRounds", this.isBetweenRounds());
		message.put("tradingPaused", this.isTradingPaused());
		message.put("chatEnabled", this.isChatEnabled());
		message.put("chatActive", this.isChatActive());
		message.put("chatBlockEnabled", this.isChatBlockEnabled());
		message.put("chatBlockShowReputation", this.isChatBlockShowReputation());
		message.put("roundTime", this.getRoundTime());
		message.put("roundSecondsElapsed", this.getRoundSecondsElapsed());
		message.put("roundSecondsRemaining", this.getRoundSecondsRemaining());
		message.put("chatSecondsRemaining", this.getChatSecondsRemaining());
		message.put("maxPrice", this.getMaxPrice());
		message.put("participationEnabled", this.isParticipationEnabled());
		message.put("participationRunning", this.isParticipationRunning());
		message.put("experimentEnded", this.isExperimentEnded());
		message.put("participationRound", this.getParticipationRound());
		message.put("roundParticipationCost", this.getRoundParticipationCost());
		message.put("roundClueCost", this.getRoundClueCost());
		message.put("clueDistributionMultiClue", this.isClueDistributionMultiClue());
		message.put("multiClueInitialCount", this.getMultiClueInitialCount());
		message.put("multiClueMaximumCount", this.getMultiClueMaximumCount());
		return message;
	}
}
