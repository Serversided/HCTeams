package net.frozenorb.foxtrot.command.subcommand.subcommands.kothsubcommands;

import net.frozenorb.foxtrot.command.subcommand.Subcommand;
import net.frozenorb.foxtrot.koth.KOTH;
import net.frozenorb.foxtrot.koth.KOTHHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by macguy8 on 10/31/2014.
 */
public class KOTHSetCapLocation extends Subcommand {

    public KOTHSetCapLocation(String name, String errorMessage, String... aliases) {
        super(name, errorMessage, aliases);
    }

    @Override
    public void syncExecute() {
        if (sender.hasPermission("foxtrot.koth.admin")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Error: " + getErrorMessage());
                return;
            }

            KOTH koth = KOTHHandler.getKOTH(args[1]);

            if (koth == null) {
                sender.sendMessage(ChatColor.RED + "No KOTH named " + args[1] + " found.");
                return;
            }

            koth.setLocation(((Player) sender).getLocation());
            sender.sendMessage(ChatColor.GRAY + "Set cap location for the " + koth.getName() + " KOTH.");
        }
    }

}