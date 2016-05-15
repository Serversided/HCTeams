package net.frozenorb.foxtrot.commands;

import net.frozenorb.foxtrot.team.claims.LandBoard;
import net.frozenorb.qlib.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ToggleClaimsCommand {

    @Command(names={ "ToggleClaims" }, permission="op")
    public static void toggleClaims(Player sender) {
        LandBoard.getInstance().setClaimsEnabled(!LandBoard.getInstance().isClaimsEnabled());
        sender.sendMessage(ChatColor.YELLOW + "Claims enabled? " + ChatColor.LIGHT_PURPLE + (LandBoard.getInstance().isClaimsEnabled() ? "Yes" : "No"));
    }

}