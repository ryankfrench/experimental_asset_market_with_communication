package jjdm.zocalo.data;

public class ParticipationChoice {

	private int roundId;
	private String participantId;
	private boolean participate = true;
	private boolean receiveClue = true;
	private int additionalClues = 0;

	/**
	 * Used when participants can select whether or not to receive clues.
	 *
	 * @param roundId       The participation round (upcoming round)
	 * @param participantId User ID
	 * @param participate   Is the participant participating.
	 * @param receiveClue   Is the participant receiving a clue.
	 */
	public ParticipationChoice(int roundId, String participantId, boolean participate, boolean receiveClue) {
		this.roundId = roundId;
		this.participantId = participantId;
		this.participate = participate;
		this.receiveClue = receiveClue;
	}

	/**
	 * Used when participants can select the number of clues they receive.
	 *
	 * @param roundId         The participation round (upcoming round)
	 * @param participantId   User ID
	 * @param participate     Is the participant participating.
	 * @param additionalClues The number of additional clues purchased.
	 */
	public ParticipationChoice(int roundId, String participantId, boolean participate, int additionalClues) {
		this.roundId = roundId;
		this.participantId = participantId;
		this.participate = participate;
		this.additionalClues = additionalClues;
	}

	public int getAdditionalClues() {
		return additionalClues;
	}
	
	public String getParticipantId() {
		return participantId;
	}

	public int getRoundId() {
		return roundId;
	}

	public boolean isParticipate() {
		return participate;
	}

	public boolean isReceiveClue() {
		return receiveClue;
	}
}
