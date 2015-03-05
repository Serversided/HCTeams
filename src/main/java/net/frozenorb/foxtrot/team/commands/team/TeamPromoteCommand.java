package net.frozenorb.foxtrot.team.commands.team;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.qlib.command.annotations.Command;
import net.frozenorb.qlib.command.annotations.Parameter;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TeamPromoteCommand {

    @Command(names={ "team promote", "t promote", "f promote", "faction promote", "fac promote", "team captain", "t captain", "f captain", "faction captain", "fac captain" }, permissionNode="")
    public static void teamPromote(Player sender, @Parameter(name="Player") OfflinePlayer target) {
        Team team = FoxtrotPlugin.getInstance().getTeamHandler().getTeam(sender);

        if (team == null) {
            sender.sendMessage(ChatColor.GRAY + "You are not on a team!");
            return;
        }

        if (!team.isOwner(sender.getUniqueId())) {
            sender.sendMessage(ChatColor.DARK_AQUA + "Only team owners can do this.");
            return;
        }

        if (!team.isMember(target.getUniqueId())) {
            sender.sendMessage(ChatColor.DARK_AQUA + target.getName() + " is not on your team.");
            return;
        }

        if (team.isCaptain(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already a captain!");
            return;
        }

        for (Player player : team.getOnlineMembers()) {
            player.sendMessage(ChatColor.DARK_AQUA + target.getName() + " has been promoted to Captain!");
        }

        team.addCaptain(target.getUniqueId());
    }

}