package jjdm.zocalo.data;

import java.util.*;

public class ChatRound {

	private int id;
	private Map<String, Set<String>> mutedSenders = new HashMap<>();
	private List<ChatMessage> messages = new ArrayList<>();

	public ChatRound(int id) {
		this.id = id;
	}

	public void addMessage(ChatMessage message) {
		this.messages.add(message);
	}

	public void addMutedSender(String participant, String sender) {
		Set<String> muted = mutedSenders.get(participant);
		if (muted == null) {
			muted = new TreeSet<String>();
		}
		muted.add(sender);
		mutedSenders.put(participant, muted);
	}

	public int getId() {
		return id;
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}

	public Map<String, Set<String>> getMutedSenders() {
		return mutedSenders;
	}

	public void removeMutedSender(String participant, String sender) {
		Set<String> muted = mutedSenders.get(participant);
		if (muted == null) {
			return;
		}
		muted.remove(sender);
		mutedSenders.put(participant, muted);
	}

}
