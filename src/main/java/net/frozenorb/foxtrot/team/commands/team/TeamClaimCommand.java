package net.frozenorb.foxtrot.team.commands.team;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.claims.VisualClaim;
import net.frozenorb.foxtrot.team.claims.VisualClaimType;
import net.frozenorb.foxtrot.team.commands.team.subclaim.TeamSubclaimCommand;
import net.frozenorb.qlib.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class TeamClaimCommand implements Listener {

    public static final ItemStack SELECTION_WAND = new ItemStack(Material.WOOD_HOE);

    static {
        ItemMeta meta = SELECTION_WAND.getItemMeta();

        meta.setDisplayName("§a§oClaiming Wand");
        meta.setLore(Arrays.asList(

                "",
                "§eRight/Left Click§6 Block",
                "§b- §fSelect claim's corners",
                "",
                "§eRight Click §6Air",
                "§b- §fCancel current claim",
                "",
                "§9Shift §eLeft Click §6Block/Air",
                "§b- §fPurchase current claim"

        ));

        SELECTION_WAND.setItemMeta(meta);
    }

    @Command(names={ "team claim", "t claim", "f claim", "faction claim", "fac claim" }, permissionNode="")
    public static void teamClaim(final Player sender) {
        Team team = FoxtrotPlugin.getInstance().getTeamHandler().getTeam(sender);

        if (team == null) {
            sender.sendMessage(ChatColor.GRAY + "You are not on a team!");
            return;
        }

        if (team.isOwner(sender.getUniqueId()) || team.isCaptain(sender.getUniqueId())) {
            sender.getInventory().remove(SELECTION_WAND);

            if (team.isRaidable()) {
                sender.sendMessage(ChatColor.RED + "You may not claim land while your faction is raidable!");
                return;
            }

            new BukkitRunnable() {

                public void run() {
                    sender.getInventory().addItem(SELECTION_WAND.clone());
                }

            }.runTaskLater(FoxtrotPlugin.getInstance(), 1L);

            new VisualClaim(sender, VisualClaimType.CREATE, false).draw(false);

            if (!VisualClaim.getCurrentMaps().containsKey(sender.getName())) {
                new VisualClaim(sender, VisualClaimType.MAP, false).draw(true);
            }
        } else {
            sender.sendMessage(ChatColor.DARK_AQUA + "Only team captains can do this.");
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().equals(TeamClaimCommand.SELECTION_WAND) || event.getItemDrop().getItemStack().equals(TeamResizeCommand.SELECTION_WAND)) {
            VisualClaim visualClaim = VisualClaim.getVisualClaim(event.getPlayer().getName());

            if (visualClaim != null) {
                event.setCancelled(true);
                visualClaim.cancel();
            }

            event.getItemDrop().remove();
        } else if (event.getItemDrop().getItemStack().equals(TeamSubclaimCommand.SELECTION_WAND)) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().getInventory().remove(TeamSubclaimCommand.SELECTION_WAND);
        event.getPlayer().getInventory().remove(TeamClaimCommand.SELECTION_WAND);
        event.getPlayer().getInventory().remove(TeamResizeCommand.SELECTION_WAND);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        event.getPlayer().getInventory().remove(TeamSubclaimCommand.SELECTION_WAND);
        event.getPlayer().getInventory().remove(TeamClaimCommand.SELECTION_WAND);
        event.getPlayer().getInventory().remove(TeamResizeCommand.SELECTION_WAND);
    }

}