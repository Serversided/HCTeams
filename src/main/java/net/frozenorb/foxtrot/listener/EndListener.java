package net.frozenorb.foxtrot.listener;

import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.jedis.persist.JoinTimerMap;
import net.frozenorb.foxtrot.server.SpawnTag;
import net.frozenorb.foxtrot.team.Team;
import org.bukkit.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class EndListener implements Listener {

    public static boolean endActive = true;
    private HashMap<String, Long> msgCooldown = new HashMap<>();

    // Display a message and give the killer the egg (when the dragon is killed)
	@EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            Team team = FoxtrotPlugin.getInstance().getTeamManager().getPlayerTeam(event.getEntity().getKiller().getName());
            String teamName = ChatColor.GOLD + "[" + ChatColor.YELLOW + "-" + ChatColor.GOLD + "]";

            if (team != null) {
                teamName = ChatColor.GOLD + "[" + ChatColor.YELLOW + team.getFriendlyName() + ChatColor.GOLD + "]";
            }

            Bukkit.broadcastMessage(ChatColor.BLACK + "████████");
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████");
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████" + ChatColor.GOLD + " [Enderdragon]");
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████" + ChatColor.YELLOW + " killed by");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "█" + ChatColor.DARK_PURPLE + "█" + ChatColor.LIGHT_PURPLE + "█" + ChatColor.BLACK + "██" + ChatColor.LIGHT_PURPLE + "█" + ChatColor.DARK_PURPLE + "█" + ChatColor.LIGHT_PURPLE + "█" + " " + teamName);
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████" + " " + event.getEntity().getKiller().getDisplayName());
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████");
            Bukkit.broadcastMessage(ChatColor.BLACK + "████████");

            ItemStack dragonEgg = new ItemStack(Material.DRAGON_EGG);
            ItemMeta itemMeta = dragonEgg.getItemMeta();
            DateFormat sdf = new SimpleDateFormat("M/d HH:mm:ss");

            itemMeta.setLore(Arrays.asList
                    (ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "Enderdragon " + ChatColor.WHITE + "slain by " + ChatColor.YELLOW + event.getEntity().getKiller().getName(),
                            ChatColor.WHITE + sdf.format(new Date()).replace(" AM", "").replace(" PM", "")));

            dragonEgg.setItemMeta(itemMeta);

            // Should we drop the item or directly add it to their inventory?

            event.getEntity().getKiller().getInventory().addItem(dragonEgg);

            if (!event.getEntity().getKiller().getInventory().contains(Material.DRAGON_EGG)) {
                event.getDrops().add(dragonEgg);
            }
        }
    }

    // Display the enderdragon's health on the bar at the top of the screen (with a percentage)
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderDragon && event.getEntity().getWorld().getEnvironment() == World.Environment.THE_END) {
            ((EnderDragon) event.getEntity()).setCustomName("Ender Dragon " + ChatColor.YELLOW.toString() + ChatColor.BOLD + Math.round((((EnderDragon) event.getEntity()).getHealth() / ((EnderDragon) event.getEntity()).getMaxHealth()) * 100) + "% Health");
        }
    }

    // Cancel the exit portal being spawned when the dragon is killed.
    @EventHandler
    public void onCreatePortal(EntityCreatePortalEvent event) {
        switch (event.getEntityType()) {
            case ENDER_DRAGON:
                event.setCancelled(true);
        }
    }

    // Whenever a player enters/leaves the end
    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }

        Player player = event.getPlayer();

        // Don't let players enter the end while it's not activated (and they're not in gamemode)
        if (!endActive && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            // Let them leave even if they're not in gamemode.
            if (event.getTo().getWorld().getEnvironment() != World.Environment.THE_END) {
                return;
            }

            event.setCancelled(true);

            if (!(msgCooldown.containsKey(player.getName())) || msgCooldown.get(player.getName()) < System.currentTimeMillis()) {
                event.getPlayer().sendMessage(ChatColor.RED + "The End is currently in beta and will be publicly released soon.");
                msgCooldown.put(player.getName(), System.currentTimeMillis() + 3000L);
            }
        }

        // Don't let players enter the end while they have their PvP timer (or haven't activated it)
        if (FoxtrotPlugin.getInstance().getJoinTimerMap().hasTimer(player) || FoxtrotPlugin.getInstance().getJoinTimerMap().getValue(player.getName()) == JoinTimerMap.PENDING_USE) {
            event.setCancelled(true);

            if (!(msgCooldown.containsKey(player.getName())) || msgCooldown.get(player.getName()) < System.currentTimeMillis()) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot enter the end while you have pvp protection.");
                msgCooldown.put(player.getName(), System.currentTimeMillis() + 3000L);
            }
        }

        // Don't let players leave the end while the dragon is still alive.
        // Don't let players leave the end while combat tagged
        if (event.getTo().getWorld().getEnvironment() == World.Environment.NORMAL) {
            if (event.getFrom().getWorld().getEntitiesByClass(EnderDragon.class).size() != 0) {
                event.setCancelled(true);

                if (!(msgCooldown.containsKey(player.getName())) || msgCooldown.get(player.getName()) < System.currentTimeMillis()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the end before the dragon is killed.");
                    msgCooldown.put(player.getName(), System.currentTimeMillis() + 3000L);
                }
            }

            if (SpawnTag.isTagged(event.getPlayer())) {
                event.setCancelled(true);

                if (!(msgCooldown.containsKey(player.getName())) || msgCooldown.get(player.getName()) < System.currentTimeMillis()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot leave the end while spawn tagged.");
                    msgCooldown.put(player.getName(), System.currentTimeMillis() + 3000L);
                }
            }
        }
    }

    // Always deny enderdragons using portals.
    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            event.setCancelled(true);
        }
    }

}