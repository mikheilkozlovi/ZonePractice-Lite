package dev.nandi0813.practice.Command.Leaderboard;

import java.util.HashMap;
import java.util.UUID;

public interface LeaderboardCallback {
    void onQueryDone(HashMap<UUID, Integer> result);
}
