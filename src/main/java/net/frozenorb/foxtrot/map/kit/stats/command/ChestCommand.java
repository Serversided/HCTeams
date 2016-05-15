package net.frozenorb.foxtrot.map.kit.stats.command;

import lombok.Getter;
import net.frozenorb.foxtrot.team.dtr.DTRBitmask;
import net.frozenorb.qlib.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChestCommand {

    @Getter private static final Set<UUID> BYPASS = new HashSet<>();

    @Command(names = {"chest"}, permission = "")
    public static void chest(Player sender) {
        if (!DTRBitmask.SAFE_ZONE.appliesAt(sender.getLocation())) {
            sender.sendMessage(ChatColor.RED + "You can only do this at spawn.");
            return;
        }

        BYPASS.add(sender.getUniqueId());
        sender.openInventory(sender.getEnderChest());
        BYPASS.remove(sender.getUniqueId());
    }

}
