package jjdm.zocalo.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to hold summary information for the experimenter screen.
 */
public class ParticipationCash {

	Map<String, Integer> totalCashPerParticipant = new HashMap<>();
	List<Map<String, Integer>> perRoundCashPerParticipant = new ArrayList<>();

}
