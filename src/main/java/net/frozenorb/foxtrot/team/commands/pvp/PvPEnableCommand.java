package net.frozenorb.foxtrot.team.commands.pvp;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.qlib.command.annotations.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PvPEnableCommand {

    @Command(names={ "pvptimer enable", "timer enable", "pvp enable", "pvptimer remove", "timer remove", "pvp remove" }, permissionNode="")
    public static void pvpEnable(Player sender) {
        if (FoxtrotPlugin.getInstance().getPvPTimerMap().hasTimer(sender.getUniqueId())) {
            FoxtrotPlugin.getInstance().getPvPTimerMap().removeTimer(sender.getUniqueId());
            sender.sendMessage(ChatColor.RED + "Your PVP Timer has been removed!");
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have a PVP Timer on!");
        }
    }

}