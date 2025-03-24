package dev.nandi0813.practice.Command.Leaderboard;

import dev.nandi0813.practice.Manager.File.ConfigManager;
import dev.nandi0813.practice.Manager.File.LanguageManager;
import dev.nandi0813.practice.Manager.Ladder.Ladder;
import dev.nandi0813.practice.Manager.Profile.Profile;
import dev.nandi0813.practice.Manager.Profile.ProfileStatus;
import dev.nandi0813.practice.Practice;
import dev.nandi0813.practice.Util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class LeaderboardCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Profile profile = Practice.getProfileManager().getProfiles().get(player);

            if (profile != null && !profile.getStatus().equals(ProfileStatus.OFFLINE)) {
                if (args.length == 2 && (args[1].equalsIgnoreCase("elo") || args[1].equalsIgnoreCase("win"))) {
                    Ladder ladder = Practice.getLadderManager().getLadder(args[0]);

                    if (ladder != null) {
                        if (ladder.isEnabled()) {
                            if (args[1].equalsIgnoreCase("elo")) {
                                if (ladder.isRanked()) {
                                    createLeaderboard(ladder, "elo", result -> {
                                        ArrayList<UUID> topPlayers = new ArrayList<>(result.keySet());
                                        int showLimit = Math.min(ConfigManager.getInt("show-player"), topPlayers.size());

                                        player.sendMessage(StringUtil.CC("&7&m------------------------------------"));
                                        player.sendMessage(LanguageManager.getString("leaderboard.elo.title").replace("%ladder%", ladder.getName()));
                                        player.sendMessage(StringUtil.CC("&7&m------------------------------------"));

                                        for (int i = 0; i < showLimit; i++) {
                                            UUID target = topPlayers.get(i);
                                            String targetName = Bukkit.getOfflinePlayer(target).getName();
                                            player.sendMessage(LanguageManager.getString("leaderboard.elo.place-line")
                                                    .replace("%place%", String.valueOf(i + 1))
                                                    .replace("%player%", targetName)
                                                    .replace("%elo%", String.valueOf(result.get(target))));
                                        }
                                        player.sendMessage(StringUtil.CC("&7&m------------------------------------"));
                                    });
                                } else {
                                    player.sendMessage(StringUtil.CC(LanguageManager.getString("leaderboard.ladder-not-ranked")));
                                }
                            } else {
                                createLeaderboard(ladder, "win", result -> {
                                    ArrayList<UUID> topPlayers = new ArrayList<>(result.keySet());
                                    int showLimit = Math.min(ConfigManager.getInt("show-player"), topPlayers.size());

                                    player.sendMessage(StringUtil.CC("&7&m------------------------------------"));
                                    player.sendMessage(LanguageManager.getString("leaderboard.win.title").replace("%ladder%", ladder.getName()));
                                    player.sendMessage(StringUtil.CC("&7&m------------------------------------"));

                                    for (int i = 0; i < showLimit; i++) {
                                        UUID target = topPlayers.get(i);
                                        String targetName = Bukkit.getOfflinePlayer(target).getName();
                                        player.sendMessage(LanguageManager.getString("leaderboard.win.place-line")
                                                .replace("%place%", String.valueOf(i + 1))
                                                .replace("%player%", targetName)
                                                .replace("%wins%", String.valueOf(result.get(target))));
                                    }
                                    player.sendMessage(StringUtil.CC("&7&m------------------------------------"));
                                });
                            }
                        } else {
                            player.sendMessage(LanguageManager.getString("leaderboard.ladder-disabled"));
                        }
                    } else {
                        player.sendMessage(LanguageManager.getString("leaderboard.ladder-not-found"));
                    }
                } else {
                    player.sendMessage(StringUtil.CC("&c/" + label + " <ladder> <elo/win>"));
                }
            }
        }
        return true;
    }

    public static void createLeaderboard(final Ladder ladder, final String engine, final LeaderboardCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(Practice.getInstance(), () -> {
            HashMap<UUID, Integer> leaderboard = new HashMap<>();

            for (Profile profile : Practice.getProfileManager().getProfiles().values()) {
                if (profile.getPlayer() != null) {
                    UUID playerUUID = profile.getPlayer().getUniqueId();

                    if (engine.equalsIgnoreCase("elo")) {
                        int elo = profile.getElo().getOrDefault(ladder, 1000);
                        leaderboard.put(playerUUID, elo);
                    } else {
                        int unrankedWin = profile.getLadderUnRankedWins().getOrDefault(ladder, 0);
                        int rankedWin = ladder.isRanked() ? profile.getLadderRankedWins().getOrDefault(ladder, 0) : 0;
                        leaderboard.put(playerUUID, (unrankedWin + rankedWin));
                    }
                }
            }

            LinkedHashMap<UUID, Integer> sortedLeaderboard = sortByValue(leaderboard);

            Bukkit.getScheduler().runTask(Practice.getInstance(), () -> callback.onQueryDone(sortedLeaderboard));
        });
    }

    public static LinkedHashMap<UUID, Integer> sortByValue(HashMap<UUID, Integer> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }
}