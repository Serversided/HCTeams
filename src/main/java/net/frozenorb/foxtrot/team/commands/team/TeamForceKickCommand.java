package net.frozenorb.foxtrot.team.commands.team;

import net.frozenorb.foxtrot.Foxtrot;
import net.frozenorb.foxtrot.server.SpawnTagHandler;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.teamactiontracker.TeamActionTracker;
import net.frozenorb.foxtrot.teamactiontracker.enums.TeamActionType;
import net.frozenorb.qlib.command.Command;
import net.frozenorb.qlib.command.Param;
import net.frozenorb.qlib.nametag.FrozenNametagHandler;
import net.frozenorb.qlib.util.UUIDUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class TeamForceKickCommand {

    @Command(names = {"team forcekick", "t forcekick", "f forcekick", "faction forcekick", "fac forcekick"}, permission = "")
    public static void teamForceKick(Player sender, @Param(name = "player") UUID player) {
        Team team = Foxtrot.getInstance().getTeamHandler().getTeam(sender);

        if (team == null) {
            sender.sendMessage(ChatColor.GRAY + "You are not on a team!");
            return;
        }

        if (!(team.isOwner(sender.getUniqueId()) || team.isCaptain(sender.getUniqueId()))) {
            sender.sendMessage(ChatColor.DARK_AQUA + "Only team captains can do this.");
            return;
        }

        if (!team.isMember(player)) {
            sender.sendMessage(ChatColor.RED + UUIDUtils.name(player) + " isn't on your team!");
            return;
        }

        if (team.isOwner(player)) {
            sender.sendMessage(ChatColor.RED + "You cannot kick the team leader!");
            return;
        }

        if (team.isCaptain(player) && !team.isOwner(sender.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "Only the owner can kick other captains!");
            return;
        }

        TeamActionTracker.logActionAsync(team, TeamActionType.GENERAL, "Member Kicked: " + UUIDUtils.name(player) + " [Force Kicked by: " + sender.getName() + "]");

        if (team.removeMember(player)) {
            team.disband();
        } else {
            team.flagForSave();
        }

        Foxtrot.getInstance().getTeamHandler().setTeam(player, null);
        Player bukkitPlayer = Foxtrot.getInstance().getServer().getPlayer(player);

        if (SpawnTagHandler.isTagged(bukkitPlayer)) {
            team.setDTR(team.getDTR() - 1);
            team.sendMessage(ChatColor.RED + UUIDUtils.name(player) + " was force kicked by " + sender.getName() + " and your team lost 1 DTR!");
        } else {
            team.sendMessage(ChatColor.RED + UUIDUtils.name(player) + " was force kicked by " + sender.getName() + "!");
        }

        if (bukkitPlayer != null) {
            FrozenNametagHandler.reloadPlayer(bukkitPlayer);
            FrozenNametagHandler.reloadOthersFor(bukkitPlayer);
        }
    }

}