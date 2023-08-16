package jjdm.zocalo.data;

/**
 * The calculated reputation score of each participant based on how often they are muted.
 */
public class ChatReputationScore {

	private String id;
	private String displayId;
	private int score;

	public String getDisplayId() {
		return displayId;
	}

	public void setDisplayId(String displayId) {
		this.displayId = displayId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "ChatReputationScore{" +
				"id='" + id + '\'' +
				", displayId='" + displayId + '\'' +
				", score=" + score +
				'}';
	}
}
