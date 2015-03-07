package net.frozenorb.foxtrot.team.commands.pvp;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.qlib.command.Command;
import net.frozenorb.qlib.command.Parameter;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class PvPAddLivesCommand {

    @Command(names={ "pvptimer addlives", "timer addlives", "pvp addlives", "pvptimer addlives", "timer addlives", "pvp addlives" }, permissionNode="op")
    public static void pvpSetLives(CommandSender sender, @Parameter(name="player") OfflinePlayer target, @Parameter(name="Life Type") String lifeType, @Parameter(name="Amount") int amount) {
        if (lifeType.equalsIgnoreCase("soulbound")) {
            FoxtrotPlugin.getInstance().getSoulboundLivesMap().setLives(target.getUniqueId(), FoxtrotPlugin.getInstance().getSoulboundLivesMap().getLives(target.getUniqueId()) + amount);
            sender.sendMessage(ChatColor.YELLOW + "Gave " + ChatColor.GREEN + target.getName() + ChatColor.YELLOW + " " + amount + " soulbound lives.");
        } else if (lifeType.equalsIgnoreCase("friend")) {
            FoxtrotPlugin.getInstance().getFriendLivesMap().setLives(target.getUniqueId(), FoxtrotPlugin.getInstance().getFriendLivesMap().getLives(target.getUniqueId()) + amount);
            sender.sendMessage(ChatColor.YELLOW + "Gave " + ChatColor.GREEN + target.getName() + ChatColor.YELLOW + " " + amount + " friend lives.");
        } else if (lifeType.equalsIgnoreCase("transferable")) {
            FoxtrotPlugin.getInstance().getTransferableLivesMap().setLives(target.getUniqueId(), FoxtrotPlugin.getInstance().getTransferableLivesMap().getLives(target.getUniqueId()) + amount);
            sender.sendMessage(ChatColor.YELLOW + "Gave " + ChatColor.GREEN + target.getName() + ChatColor.YELLOW + " " + amount + " transferable lives.");
        } else {
            sender.sendMessage(ChatColor.RED + "Not a valid life type: Options are soulbound, friend, or transferable.");
        }
    }

}