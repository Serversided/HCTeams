package net.frozenorb.foxtrot.command.commands.subcommands.teamsubcommands;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.command.annotations.Command;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.claims.VisualClaim;
import net.frozenorb.foxtrot.team.claims.VisualClaim.VisualType;
import net.frozenorb.foxtrot.util.ListUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Claim implements Listener {

	public static final ItemStack SELECTION_WAND = new ItemStack(Material.WOOD_HOE);

	static {
		ItemMeta meta = SELECTION_WAND.getItemMeta();

		meta.setDisplayName("§a§oClaiming Wand");
		meta.setLore(ListUtils.wrap(" | §eRight/Left Click§6 Block   §b- §fSelect claim's corners" + " | §eRight Click §6Air  |  §b- §fCancel current claim" + " | §9Shift §eLeft Click §6Block/Air   §b- §fPurchase current claim", ""));
		SELECTION_WAND.setItemMeta(meta);
	}

    @Command(names={ "team claim", "t claim", "f claim", "faction claim", "fac claim" }, permissionNode="")
    public static void teamClaim(Player sender) {
		Team team = FoxtrotPlugin.getInstance().getTeamHandler().getPlayerTeam(sender.getName());

		if (team == null) {
			sender.sendMessage(ChatColor.GRAY + "You are not on a team!");
			return;
		}

		if (team.isOwner(sender.getName()) || team.isCaptain(sender.getName())) {
            sender.getInventory().remove(SELECTION_WAND);

            if (team.isRaidable()) {
                sender.sendMessage(ChatColor.RED + "You may not claim land while your faction is raidable!");
                return;
            }

			FoxtrotPlugin.getInstance().getServer().getScheduler().runTaskLater(FoxtrotPlugin.getInstance(), () -> sender.getInventory().addItem(SELECTION_WAND.clone()), 1L);

			new VisualClaim(sender, VisualType.CREATE, false).draw(false);

			if (!VisualClaim.getCurrentMaps().containsKey(sender.getName())) {
				new VisualClaim(sender, VisualType.MAP, false).draw(false);
			}
		} else {
            sender.sendMessage(ChatColor.DARK_AQUA + "Only team captains can do this.");
        }
	}

    @Command(names={ "team opclaim", "t opclaim", "f opclaim", "faction opclaim", "fac opclaim" }, permissionNode="op")
    public static void teamOpClaim(Player sender) {
        Team team = FoxtrotPlugin.getInstance().getTeamHandler().getPlayerTeam(sender.getName());

        if (team == null) {
            sender.sendMessage(ChatColor.GRAY + "You are not on a team!");
            return;
        }

        sender.getInventory().remove(SELECTION_WAND);
        FoxtrotPlugin.getInstance().getServer().getScheduler().runTaskLater(FoxtrotPlugin.getInstance(), () -> sender.getInventory().addItem(SELECTION_WAND.clone()), 1L);

        new VisualClaim(sender, VisualType.CREATE, true).draw(false);

        if (!VisualClaim.getCurrentMaps().containsKey(sender.getName())) {
            new VisualClaim(sender, VisualType.MAP, true).draw(false);
        }
    }

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent e) {
		if (e.getItemDrop().getItemStack().equals(SELECTION_WAND)) {
			VisualClaim vc = VisualClaim.getVisualClaim(e.getPlayer().getName());

			if (vc != null) {
				e.setCancelled(true);
				vc.cancel(false);
			}

			FoxtrotPlugin.getInstance().getServer().getScheduler().runTaskLater(FoxtrotPlugin.getInstance(), () -> e.getItemDrop().remove(), 1L);
		}
	}

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        e.getPlayer().getInventory().remove(net.frozenorb.foxtrot.command.commands.subcommands.teamsubcommands.Subclaim.SELECTION_WAND);
        e.getPlayer().getInventory().remove(net.frozenorb.foxtrot.command.commands.subcommands.teamsubcommands.Claim.SELECTION_WAND);
    }

}