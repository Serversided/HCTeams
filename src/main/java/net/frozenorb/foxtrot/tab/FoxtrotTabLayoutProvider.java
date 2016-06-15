package net.frozenorb.foxtrot.tab;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import javafx.util.Pair;
import net.frozenorb.foxtrot.Foxtrot;
import net.frozenorb.foxtrot.koth.KOTH;
import net.frozenorb.foxtrot.koth.KOTHScheduledTime;
import net.frozenorb.foxtrot.listener.BorderListener;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.claims.LandBoard;
import net.frozenorb.foxtrot.team.commands.team.TeamListCommand;
import net.frozenorb.foxtrot.util.PlayerDirection;
import net.frozenorb.qlib.tab.LayoutProvider;
import net.frozenorb.qlib.tab.TabLayout;
import net.frozenorb.qlib.util.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class FoxtrotTabLayoutProvider implements LayoutProvider {

    private LinkedHashMap<Team, Integer> cachedTeamList = Maps.newLinkedHashMap();
    long cacheLastUpdated;

    @Override
    public TabLayout provide(Player player) {
        TabListMode mode = Foxtrot.getInstance().getTabListModeMap().getTabListMode(player.getUniqueId());

        if (mode == TabListMode.VANILLA) {
            return null;
        }

        TabLayout layout = TabLayout.create(player);
        Team team = Foxtrot.getInstance().getTeamHandler().getTeam(player);

        String serverName = Foxtrot.getInstance().getServerHandler().isSquads() ? "HCSquads.com" : "HCTeams.com";

        layout.set(1, 0, ChatColor.GOLD.toString() + ChatColor.BOLD + serverName);

        int y = -1;

        if (team != null) {
            layout.set(0, ++y, ChatColor.DARK_PURPLE + "Home:");

            if (team.getHQ() != null) {
                String homeLocation = ChatColor.YELLOW.toString() + team.getHQ().getBlockX() + ", " + team.getHQ().getBlockY() + ", " + team.getHQ().getBlockZ();
                layout.set(0, ++y, homeLocation);
            } else {
                layout.set(0, ++y, ChatColor.YELLOW + "Not Set");
            }

            ++y; // blank

            int balance = (int) team.getBalance();
            layout.set(0, ++y, ChatColor.DARK_PURPLE + "Team Info:");
            layout.set(0, ++y, ChatColor.YELLOW + "DTR: " + (team.isRaidable() ? ChatColor.DARK_RED : ChatColor.YELLOW) + Team.DTR_FORMAT.format(team.getDTR()));
            layout.set(0, ++y, ChatColor.YELLOW + "Online: " + team.getOnlineMemberAmount() + "/" + team.getMembers().size());
            layout.set(0, ++y, ChatColor.YELLOW + "Balance: $" + balance);

            ++y; // blank
        }

        layout.set(0, ++y, ChatColor.DARK_PURPLE + "Player Info:");
        layout.set(0, ++y, ChatColor.YELLOW + "Kills: " + Foxtrot.getInstance().getKillsMap().getKills(player.getUniqueId()));

//        layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW + "Deaths: " + Foxtrot.getInstance().getDeathsMap().getDeaths(player.getUniqueId())));

        ++y; // blank

        layout.set(0, ++y, ChatColor.DARK_PURPLE + "Your Location:");

        String location;

        Location loc = player.getLocation();
        Team ownerTeam = LandBoard.getInstance().getTeam(loc);

        if (ownerTeam != null) {
            location = ownerTeam.getName(player.getPlayer());
        } else if (!Foxtrot.getInstance().getServerHandler().isWarzone(loc)) {
            location = ChatColor.GRAY + "The Wilderness";
        } else if (LandBoard.getInstance().getTeam(loc) != null && LandBoard.getInstance().getTeam(loc).getName().equalsIgnoreCase("citadel")) {
            location = ChatColor.DARK_PURPLE + "Citadel";
        } else {
            location = ChatColor.RED + "Warzone";
        }

        layout.set(0, ++y, location);

        /* Getting the direction 4 times a second for each player on the server may be intensive.
        We may want to cache the entire location so it is accessed no more than 1 time per second.
        FIXME, WIP */
        String direction = PlayerDirection.getCardinalDirection(player);
        if (direction != null) {
            layout.set(0, ++y, ChatColor.GRAY + "(" + loc.getBlockX() + ", " + loc.getBlockZ() + ") [" + direction + "]");
        } else {
            layout.set(0, ++y, ChatColor.GRAY + "(" + loc.getBlockX() + ", " + loc.getBlockZ() + ")");
        }
        ++y; // blank

        KOTH activeKOTH = null;
        for (KOTH koth : Foxtrot.getInstance().getKOTHHandler().getKOTHs()) {
            if (koth.isActive() && !koth.isHidden()) {
                activeKOTH = koth;
                break;
            }
        }

        if (activeKOTH == null) {
            Date now = new Date();

            Pair<String, Date> nextKOTH = null;

            for (Map.Entry<KOTHScheduledTime, String> entry : Foxtrot.getInstance().getKOTHHandler().getKOTHSchedule().entrySet()) {
                if (entry.getKey().toDate().after(now)) {
                    if (nextKOTH == null || nextKOTH.getValue().getTime() > entry.getKey().toDate().getTime()) {
                        nextKOTH = new Pair<>(entry.getValue(), entry.getKey().toDate());
                    }
                }
            }

            if (nextKOTH != null) {
                layout.set(0, ++y, ChatColor.DARK_PURPLE + "Next KOTH:");
                layout.set(0, ++y, ChatColor.YELLOW + nextKOTH.getKey());

                KOTH koth = Foxtrot.getInstance().getKOTHHandler().getKOTH(nextKOTH.getKey());

                if (koth != null) {
                    layout.set(0, ++y, ChatColor.YELLOW.toString() + koth.getCapLocation().getBlockX() + ", " + koth.getCapLocation().getBlockY() + ", " + koth.getCapLocation().getBlockZ()); // location

                    int seconds = (int) ((nextKOTH.getValue().getTime() - System.currentTimeMillis()) / 1000);
                    layout.set(0, ++y, ChatColor.DARK_PURPLE + "Goes active in:");

                    String time = formatIntoDetailedString(seconds)
                            .replace("minutes", "min").replace("minute", "min")
                            .replace("seconds", "sec").replace("second", "sec");

                    layout.set(0, ++y, ChatColor.YELLOW + time);
                }
            }
        } else {
            layout.set(0, ++y, ChatColor.DARK_PURPLE + activeKOTH.getName());
            layout.set(0, ++y, ChatColor.YELLOW + TimeUtils.formatIntoHHMMSS(activeKOTH.getRemainingCapTime()));
            layout.set(0, ++y, ChatColor.YELLOW.toString() + activeKOTH.getCapLocation().getBlockX() + ", " + activeKOTH.getCapLocation().getBlockY() + ", " + activeKOTH.getCapLocation().getBlockZ()); // location
        }

        if (team != null) {
            layout.set(1, mode == TabListMode.DETAILED_WITH_FACTION_INFO ? 5 : 2, ChatColor.DARK_PURPLE + team.getName());

            String watcherName = ChatColor.DARK_GREEN + player.getName();
            if (team.isOwner(player.getUniqueId())) {
                watcherName += ChatColor.GRAY + "**";
            } else if (team.isCaptain(player.getUniqueId())) {
                watcherName += ChatColor.GRAY + "*";
            }

            layout.set(1, mode == TabListMode.DETAILED_WITH_FACTION_INFO ? 6 : 3, watcherName, ((CraftPlayer) player).getHandle().ping); // the viewer is always first on the list

            Player owner = null;
            List<Player> captains = Lists.newArrayList();
            List<Player> members = Lists.newArrayList();
            for (Player member : team.getOnlineMembers()) {
                if (team.isOwner(member.getUniqueId())) {
                    owner = member;
                } else if (team.isCaptain(member.getUniqueId())) {
                    captains.add(member);
                } else {
                    members.add(member);
                }
            }

            int x = 1;
            y = mode == TabListMode.DETAILED ? 4 : 7;

            // then the owner
            if (owner != null && !owner.equals(player)) {
                layout.set(x, y, ChatColor.DARK_GREEN + owner.getName() + ChatColor.GRAY + "**", ((CraftPlayer) owner).getHandle().ping);

                y++;

                if (y >= 20) {
                    y = 0;
                    x++;
                }
            }

            // then the captains
            for (Player captain : captains) {
                if (captain.equals(player)) continue;

                layout.set(x, y, ChatColor.DARK_GREEN + captain.getName() + ChatColor.GRAY + "*", ((CraftPlayer) captain).getHandle().ping);

                y++;

                if (y >= 20) {
                    y = 0;
                    x++;
                }
            }

            // and only then, normal members.
            for (Player member : members) {
                if (member.equals(player)) continue;

                if (member.equals(player)) {
                    continue;
                }

                layout.set(x, y, ChatColor.DARK_GREEN + member.getName(), ((CraftPlayer) member).getHandle().ping);

                y++;

                if (y >= 20) {
                    y = 0;
                    x++;
                }
            }

            // basically, if we're not on the third column yet, set the y to 0, and go to the third column.
            // if we're already there, just place whatever we got under the last player's name
            if (x < 2) {
                y = 0;
            } else {
                y++; // comment this out if you don't want a space in between the last player and the info below:
            }
        }

        if (team == null) {
            y = 0;
        }

        if (mode == TabListMode.DETAILED) {
            if (!Foxtrot.getInstance().getMapHandler().getEndPortalLocation().equals("N/A")) {
                layout.set(2, y, ChatColor.DARK_PURPLE + "End Portals:");
                layout.set(2, ++y, ChatColor.YELLOW + Foxtrot.getInstance().getMapHandler().getEndPortalLocation());
                layout.set(2, ++y, ChatColor.YELLOW + "in each quadrant");

                ++y;
                layout.set(2, ++y, ChatColor.DARK_PURPLE + "Kit:");
                layout.set(2, ++y, ChatColor.YELLOW + Foxtrot.getInstance().getServerHandler().getEnchants());
            } else {
                layout.set(2, y, ChatColor.DARK_PURPLE + "Kit:");
                layout.set(2, ++y, ChatColor.YELLOW + Foxtrot.getInstance().getServerHandler().getEnchants());
            }

            ++y;
            layout.set(2, ++y, ChatColor.DARK_PURPLE + "Border:");
            layout.set(2, ++y, ChatColor.YELLOW + String.valueOf(BorderListener.BORDER_SIZE));

            ++y;
            layout.set(2, ++y, ChatColor.DARK_PURPLE + "Players Online:");
            layout.set(2, ++y, ChatColor.YELLOW + String.valueOf(Bukkit.getOnlinePlayers().size()));

            Team capper = Foxtrot.getInstance().getTeamHandler().getTeam(Foxtrot.getInstance().getCitadelHandler().getCapper());
            if (capper != null) {
                ++y;
                layout.set(2, ++y, ChatColor.DARK_PURPLE + "Citadel Cappers:");
                layout.set(2, ++y, ChatColor.YELLOW + capper.getName());
            }
        } else {
            layout.set(1, 2, ChatColor.DARK_PURPLE + "Players Online:");
            layout.set(1, 3, ChatColor.YELLOW + String.valueOf(Bukkit.getOnlinePlayers().size()));

            // faction list (10 entries)
            boolean shouldReloadCache = cachedTeamList == null || (System.currentTimeMillis() - cacheLastUpdated > 2000);

            y = 1;

            Map<Team, Integer> teamPlayerCount = new HashMap<>();

            if (shouldReloadCache) {
                // Sort of weird way of getting player counts, but it does it in the least iterations (1), which is what matters!
                for (Player other : Foxtrot.getInstance().getServer().getOnlinePlayers()) {
                    if (other.hasMetadata("invisible")) {
                        continue;
                    }

                    Team playerTeam = Foxtrot.getInstance().getTeamHandler().getTeam(other);

                    if (playerTeam != null) {
                        if (teamPlayerCount.containsKey(playerTeam)) {
                            teamPlayerCount.put(playerTeam, teamPlayerCount.get(playerTeam) + 1);
                        } else {
                            teamPlayerCount.put(playerTeam, 1);
                        }
                    }
                }
            }

            LinkedHashMap<Team, Integer> sortedTeamPlayerCount;

            if (shouldReloadCache) {
                sortedTeamPlayerCount = TeamListCommand.sortByValues(teamPlayerCount);
                cachedTeamList = sortedTeamPlayerCount;
                cacheLastUpdated = System.currentTimeMillis();
            } else {
                sortedTeamPlayerCount = cachedTeamList;
            }

            int index = 0;

            boolean title = false;

            for (Map.Entry<Team, Integer> teamEntry : sortedTeamPlayerCount.entrySet()) {
                index++;

                if (index > 19) {
                    break;
                }

                if (!title) {
                    title = true;
                    layout.set(2, 0, ChatColor.DARK_PURPLE + "Team List:");
                }

                String teamName = teamEntry.getKey().getName();
                ChatColor teamColor = teamEntry.getKey().isMember(player.getUniqueId()) ? ChatColor.GREEN : ChatColor.YELLOW;

                if (teamName.length() > 10) teamName = teamName.substring(0, 10);

                layout.set(2, y++, teamColor + teamName + ChatColor.GRAY + " (" + teamEntry.getValue() + ")");
            }
        }

        /* This works, but it's probably not efficient...
        if (player.hasPermission("basic.staff")) {
            ++y;
            layout.set(2, ++y, ChatColor.DARK_PURPLE + "Online Staff:");
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.hasPermission("basic.staff")) {
                    continue;
                }
                layout.set(2, ++y, ChatColor.YELLOW + onlinePlayer.getName());
            }
        }
        */

        return layout;
    }

    public static String formatIntoDetailedString(int secs) {
        if (secs <= 60) {
            return "1 minute";
        } else {
            int remainder = secs % 86400;
            int days = secs / 86400;
            int hours = remainder / 3600;
            int minutes = remainder / 60 - hours * 60;
            int seconds = remainder % 3600 - minutes * 60;
            String fDays = days > 0 ? " " + days + " day" + (days > 1 ? "s" : "") : "";
            String fHours = hours > 0 ? " " + hours + " hour" + (hours > 1 ? "s" : "") : "";
            String fMinutes = minutes > 0 ? " " + minutes + " minute" + (minutes > 1 ? "s" : "") : "";
            String fSeconds = (seconds > 0 && hours <= 0) ? " " + seconds + " second" + (seconds > 1 ? "s" : "") : "";
            return (fDays + fHours + fMinutes).trim();
        }

    }

}
