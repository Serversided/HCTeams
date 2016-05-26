package net.frozenorb.foxtrot.tab;

import com.google.common.collect.Lists;
import javafx.util.Pair;
import net.frozenorb.foxtrot.Foxtrot;
import net.frozenorb.foxtrot.koth.KOTH;
import net.frozenorb.foxtrot.koth.KOTHScheduledTime;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.claims.LandBoard;
import net.frozenorb.qlib.tab.LayoutProvider;
import net.frozenorb.qlib.tab.TabEntry;
import net.frozenorb.qlib.tab.TabLayout;
import net.frozenorb.qlib.util.TimeUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class FoxtrotTabLayoutProvider implements LayoutProvider {

    @Override
    public TabLayout provide(Player player) {
        TabLayout layout = TabLayout.create(player);
        Team team = Foxtrot.getInstance().getTeamHandler().getTeam(player);

        String serverName = Foxtrot.getInstance().getServerHandler().isSquads() ? "HCSquads.com" : "HCTeams.com";

        layout.set(1, 0, TabEntry.of(ChatColor.GOLD.toString() + ChatColor.BOLD + serverName));

        int y = -1;

        if (team != null) {
            layout.set(0, ++y, TabEntry.of(ChatColor.RED + "Home:"));

            if (team.getHQ() != null) {
                String homeLocation = ChatColor.BLUE.toString() + team.getHQ().getBlockX() + ", " + team.getHQ().getBlockY() + ", " + team.getHQ().getBlockZ();
                layout.set(0, ++y, TabEntry.of(homeLocation));
            } else {
                layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Not Set"));
            }

            ++y; // blank

            layout.set(0, ++y, TabEntry.of(ChatColor.RED + team.getName()));
            layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "DTR: " + (team.isRaidable() ? ChatColor.DARK_RED : ChatColor.BLUE) + Team.DTR_FORMAT.format(team.getDTR())));
            layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Online: " + team.getOnlineMemberAmount() + "/" + team.getMembers().size()));

            ++y; // blank
        }

        layout.set(0, ++y, TabEntry.of(ChatColor.RED + "Player Info:"));
        layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Kills: " + Foxtrot.getInstance().getKillsMap().getKills(player.getUniqueId())));
//        layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Deaths: " + Foxtrot.getInstance().getDeathsMap().getDeaths(player.getUniqueId())));

        ++y; // blank

        layout.set(0, ++y, TabEntry.of(ChatColor.RED + "Your Location:"));

        String location;

        Location loc = player.getLocation();
        Team ownerTeam = LandBoard.getInstance().getTeam(loc);

        if (ownerTeam != null) {
            location = ownerTeam.getName(player.getPlayer());
        } else if (!Foxtrot.getInstance().getServerHandler().isWarzone(loc)) {
            location = ChatColor.GRAY + "The Wilderness";
        } else {
            location = ChatColor.RED + "Warzone";
        }

        layout.set(0, ++y, TabEntry.of(location));

        ++y; // blank

        KOTH activeKOTH = null;
        for (KOTH koth : Foxtrot.getInstance().getKOTHHandler().getKOTHs()) {
            if (koth.isActive()) {
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
                layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Next KOTH:"));
                layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW + nextKOTH.getKey()));

                KOTH koth = Foxtrot.getInstance().getKOTHHandler().getKOTH(nextKOTH.getKey());

                layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW.toString() + koth.getCapLocation().getBlockX() + ", " + koth.getCapLocation().getBlockY() + ", " + koth.getCapLocation().getBlockZ())); // location

                int seconds = (int) ((nextKOTH.getValue().getTime() - System.currentTimeMillis()) / 1000);
                layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "Goes active in:"));

                String time = formatIntoDetailedString(seconds)
                        .replace("minutes", "min").replace("minute", "min")
                        .replace("seconds", "sec").replace("second", "sec");

                layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW + time));
            } else {
                layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + "N/A"));
            }
        } else {
            layout.set(0, ++y, TabEntry.of(ChatColor.BLUE + activeKOTH.getName()));
            layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW.toString() + activeKOTH.getCapLocation().getBlockX() + ", " + activeKOTH.getCapLocation().getBlockY() + ", " + activeKOTH.getCapLocation().getBlockZ())); // location
            layout.set(0, ++y, TabEntry.of(ChatColor.YELLOW + TimeUtils.formatIntoHHMMSS(activeKOTH.getRemainingCapTime())));
        }

        if (team != null) {
            layout.set(1, 2, TabEntry.of(ChatColor.GREEN + "Teammates Online"));

            String watcherName = ChatColor.DARK_GREEN + player.getName();
            if (team.isOwner(player.getUniqueId())) {
                watcherName += ChatColor.GRAY + "**";
            } else if (team.isCaptain(player.getUniqueId())) {
                watcherName += ChatColor.GRAY + "*";
            }

            layout.set(1, 3, TabEntry.of(watcherName, ((CraftPlayer) player).getHandle().ping)); // the viewer is always first on the list

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
            y = 4;

            // then the owner
            if (owner != null && !owner.equals(player)) {
                layout.set(x, y, TabEntry.of(ChatColor.DARK_GREEN + owner.getName() + ChatColor.GRAY + "**", ((CraftPlayer) owner).getHandle().ping));

                y++;

                if (y >= 20) {
                    y = 0;
                    x++;
                }
            }

            // then the captains
            for (Player captain : captains) {
                if (captain.equals(player)) continue;

                layout.set(x, y, TabEntry.of(ChatColor.DARK_GREEN + captain.getName() + ChatColor.GRAY + "*", ((CraftPlayer) captain).getHandle().ping));

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

                layout.set(x, y, TabEntry.of(ChatColor.DARK_GREEN + member.getName(), ((CraftPlayer) member).getHandle().ping));

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

        layout.set(2, y, TabEntry.of(ChatColor.DARK_PURPLE + "End Portal:"));
        layout.set(2, ++y, TabEntry.of(ChatColor.YELLOW + "2500, 2500 each quadrant"));

        ++y; // blank line

        layout.set(2, ++y, TabEntry.of(ChatColor.BLUE + "Kit:"));
        layout.set(2, ++y, TabEntry.of(ChatColor.RED + "Sharp 30912, FF 1"));

        return layout;
    }

    public static String formatIntoDetailedString(int secs) {
        if(secs == 0) {
            return "0 seconds";
        } else {
            int remainder = secs % 86400;
            int days = secs / 86400;
            int hours = remainder / 3600;
            int minutes = remainder / 60 - hours * 60;
            int seconds = remainder % 3600 - minutes * 60;
            String fDays = days > 0 ? " " + days + " day" + (days > 1?"s":""):"";
            String fHours = hours > 0 ? " " + hours + " hour" + (hours > 1?"s":""):"";
            String fMinutes = minutes > 0 ? " " + minutes + " minute" + (minutes > 1?"s":""):"";
            String fSeconds = (seconds > 0 && hours <= 0) ? " " + seconds + " second" + (seconds > 1?"s":""):"";
            return (fDays + fHours + fMinutes + fSeconds).trim();
        }

    }

}
