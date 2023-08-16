package jjdm.zocalo.data;

import java.util.Date;
import java.util.List;

public class ChatMessage {

	private String senderId;
	private List<String> recipientIds;
	private String message;
	private Date timestamp;

	public ChatMessage(String senderId, List<String> recipientIds, String message) {
		super();
		this.senderId = senderId;
		this.recipientIds = recipientIds;
		this.message = message;
		this.timestamp = new Date();
	}

	public String getMessage() {
		return message;
	}

	public List<String> getRecipientIds() {
		return recipientIds;
	}

	public String getSenderId() {
		return senderId;
	}

	public Date getTimestamp() {
		return timestamp;
	}
}
