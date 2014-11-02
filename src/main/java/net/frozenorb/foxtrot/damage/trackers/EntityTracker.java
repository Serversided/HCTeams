package net.frozenorb.foxtrot.damage.trackers;

import net.frozenorb.foxtrot.damage.event.CustomPlayerDamageEvent;
import net.frozenorb.foxtrot.damage.objects.MobDamage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Tracker to detect when an entity damages a player.
 */
public class EntityTracker implements Listener {

    //***************************//

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onCustomPlayerDamage(CustomPlayerDamageEvent event) {
        if (event.getCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event.getCause();

            if (!(e.getDamager() instanceof Player) && !(e.getDamager() instanceof Arrow)) {
                event.setTrackerDamage(new EntityDamage(event.getPlayer().getName(), event.getDamage(), e.getDamager()));
            }
        }
    }

    //***************************//

    public class EntityDamage extends MobDamage {

        //***************************//

        public EntityDamage(String damaged, double damage, Entity entity) {
            super(damaged, damage, entity.getType());
        }

        //***************************//

        public String getDescription() {
            return (getMobType().getName());
        }

        public String getDeathMessage() {
            return (ChatColor.GOLD + getDamaged() + ChatColor.RED + " was killed by a " + ChatColor.GOLD + getMobType().getName() + ChatColor.RED + ".");
        }

        //***************************//

    }

    //***************************//

}