package jjdm.zocalo;

import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DeleteMe {

	public static void main(String[] args) {

		DeleteMe me = new DeleteMe();
		List<String> colors = me.list("red", "yellow", "blue");
		List<String> colors2 = new ArrayList<>(colors);
		colors2.add("purple");
		me.permutation(colors);
//		me.permutation(colors2);
		List<String> participants = me.list("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10", "a11", "a12");
		me.participantColors(colors, participants);

	}

	private List<String> list(String... args) {
		return Arrays.asList(args);
	}

	private void log(Object message) {
		System.out.println(message.toString());
	}

	private void participantColors(List<String> colors, List<String> participants) {

		List<List<String>> options = new ArrayList(Collections2.permutations(colors));
		List<String> randomParticipants = new ArrayList<>(participants);
		Collections.copy(randomParticipants, participants);
		Collections.shuffle(randomParticipants);
		log(participants);
		log(randomParticipants);

		int size = options.size();
		int current = 0;
		Map<String, List<String>> colorMap = new TreeMap<>();

		for (String participant : randomParticipants) {
			colorMap.put(participant, options.get(current++));
			if (current >= size) {
				current = 0;
			}
		}

		randomParticipants.forEach(p -> log(p + ": " + colorMap.get(p)));

	}

	private void permutation(List<String> list) {
		log("INPUT LIST: " + list);
		Collection<List<String>> options = Collections2.permutations(list);
		options.forEach(option -> log(option));
	}

}
