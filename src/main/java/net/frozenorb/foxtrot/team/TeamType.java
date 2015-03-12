package net.frozenorb.foxtrot.team;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.util.UUIDUtils;
import net.frozenorb.qlib.command.ParameterType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TeamType implements ParameterType<Team> {

    public Team transform(CommandSender sender, String source) {
        if (sender instanceof Player && (source.equalsIgnoreCase("self") || source.equals(""))) {
            Team team = FoxtrotPlugin.getInstance().getTeamHandler().getTeam(((Player) sender).getUniqueId());

            if (team == null) {
                sender.sendMessage(ChatColor.GRAY + "You're not on a team!");
                return (null);
            }

            return (team);
        }

        Team byName = FoxtrotPlugin.getInstance().getTeamHandler().getTeam(source);

        if (byName != null) {
            return (byName);
        }

        Team byMember = FoxtrotPlugin.getInstance().getTeamHandler().getTeam(UUIDUtils.uuid(source));

        if (byMember != null) {
            return (byMember);
        }

        sender.sendMessage(ChatColor.RED + "No team or member with the name " + source + " found.");
        return (null);
    }

    public List<String> tabComplete(Player sender, Set<String> flags, String source) {
        List<String> completions = new ArrayList<>();

        for (Team team : FoxtrotPlugin.getInstance().getTeamHandler().getTeams()) {
            if (StringUtils.startsWithIgnoreCase(team.getName(), source)) {
                completions.add(team.getName());
            }
        }

        for (Player player : FoxtrotPlugin.getInstance().getServer().getOnlinePlayers()) {
            if (StringUtils.startsWithIgnoreCase(player.getName(), source)) {
                completions.add(player.getName());
            }
        }

        return (completions);
    }

}