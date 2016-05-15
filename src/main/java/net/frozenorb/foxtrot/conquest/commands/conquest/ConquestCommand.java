package net.frozenorb.foxtrot.conquest.commands.conquest;

import net.frozenorb.foxtrot.Foxtrot;
import net.frozenorb.foxtrot.conquest.ConquestHandler;
import net.frozenorb.foxtrot.conquest.game.ConquestGame;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.qlib.command.Command;
import org.bson.types.ObjectId;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;

public class ConquestCommand {

    @Command(names={ "conquest" }, permission="")
    public static void conquest(Player sender) {
        ConquestGame game = Foxtrot.getInstance().getConquestHandler().getGame();

        if (game == null) {
            sender.sendMessage(ChatColor.RED + "Conquest is not active.");
            return;
        }

        Map<ObjectId, Integer> caps = game.getTeamPoints();

        sender.sendMessage(ChatColor.YELLOW + "Conquest Scores:");
        boolean sent = false;

        for (Map.Entry<ObjectId, Integer> capEntry : caps.entrySet()) {
            Team resolved = Foxtrot.getInstance().getTeamHandler().getTeam(capEntry.getKey());

            if (resolved != null) {
                sender.sendMessage(resolved.getName(sender) + ": " + ChatColor.WHITE + capEntry.getValue() + " point" + (capEntry.getValue() == 1 ? "" : "s"));
                sent = true;
            }
        }

        if (!sent) {
            sender.sendMessage(ChatColor.GRAY + "No points have been scored!");
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW.toString() + ConquestHandler.POINTS_TO_WIN + " points are required to win.");
    }

}