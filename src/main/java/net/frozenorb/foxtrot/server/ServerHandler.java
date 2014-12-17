package net.frozenorb.foxtrot.server;

import com.google.common.collect.Sets;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;
import lombok.Getter;
import lombok.Setter;
import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.command.commands.FreezeCommand;
import net.frozenorb.foxtrot.ctf.game.CTFFlag;
import net.frozenorb.foxtrot.factionactiontracker.FactionActionTracker;
import net.frozenorb.foxtrot.jedis.persist.PlaytimeMap;
import net.frozenorb.foxtrot.jedis.persist.PvPTimerMap;
import net.frozenorb.foxtrot.listener.EnderpearlListener;
import net.frozenorb.foxtrot.raffle.enums.RaffleAchievement;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.TeamHandler;
import net.frozenorb.foxtrot.team.claims.LandBoard;
import net.frozenorb.foxtrot.team.dtr.bitmask.DTRBitmaskType;
import net.frozenorb.foxtrot.util.InvUtils;
import net.frozenorb.mBasic.Basic;
import net.minecraft.util.org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonParser;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("deprecation")
public class ServerHandler {

    public static int WARZONE_RADIUS = 1000;

    // NEXT MAP //
    // http://minecraft.gamepedia.com/Potion#Data_value_table
    public static final Set<Integer> DISALLOWED_POTIONS = Sets.newHashSet(
        8193, 8225, 8257, 16385, 16417, 16449, // Regeneration Potions
        8200, 8232, 8264, 16392, 16424, 16456, // Weakness Potions
        8201, 8233, 8265, 16393, 16425, 16457, // Strength Potions
        8204, 8236, 8268, 16396, 16428, 16460, // Harming Potions
        8238, 8270, 16430, 16462, 16398, 8238, // Invisibility Potions
        8228, 8260, 16420, 16452, // Poison Potions
        8234, 8266, 16426, 16458 // Slowness Potions
    );

    @Getter private static Map<String, Integer> tasks = new HashMap<String, Integer>();

    @Getter private Set<String> usedNames = new HashSet<String>();
    @Getter private Set<String> highRollers = new HashSet<String>();

    @Getter @Setter private boolean EOTW = false;
    @Getter @Setter private boolean PreEOTW = false;

    public ServerHandler() {
        try {
            File f = new File("usedNames.json");

            if (!f.exists()) {
                f.createNewFile();
            }

            BasicDBObject dbo = (BasicDBObject) JSON.parse(FileUtils.readFileToString(f));

            if (dbo != null) {
                for (Object o : (BasicDBList) dbo.get("names")) {
                    usedNames.add((String) o);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File f = new File("highRollers.json");

            if (!f.exists()) {
                f.createNewFile();
            }

            BasicDBObject dbo = (BasicDBObject) JSON.parse(FileUtils.readFileToString(f));

            if (dbo != null) {
                for (Object o : (BasicDBList) dbo.get("names")) {
                    highRollers.add((String) o);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        new BukkitRunnable() {

            public void run() {
                StringBuilder highRollers = new StringBuilder();

                for (String highRoller : FoxtrotPlugin.getInstance().getServerHandler().getHighRollers()) {
                    highRollers.append(ChatColor.DARK_PURPLE).append(highRoller).append(ChatColor.GOLD).append(", ");
                }

                if (highRollers.length() > 2) {
                    highRollers.setLength(highRollers.length() - 2);
                    FoxtrotPlugin.getInstance().getServer().broadcastMessage(ChatColor.GOLD + "HCTeams HighRollers: " + highRollers.toString());
                }
            }

        }.runTaskTimer(FoxtrotPlugin.getInstance(), 20L, 20L * 60 * 5);
    }

    public void save() {
        try {
            File f = new File("usedNames.json");

            if (!f.exists()) {
                f.createNewFile();
            }

            BasicDBObject dbo = new BasicDBObject();
            BasicDBList list = new BasicDBList();

            for (String n : usedNames) {
                list.add(n);
            }

            dbo.put("names", list);
            FileUtils.write(f, new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(dbo.toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            File f = new File("highRollers.json");

            if (!f.exists()) {
                f.createNewFile();
            }

            BasicDBObject dbo = new BasicDBObject();
            BasicDBList list = new BasicDBList();

            for (String n : highRollers) {
                list.add(n);
            }

            dbo.put("names", list);
            FileUtils.write(f, new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(dbo.toString())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isBannedPotion(int value) {
        for (int i : DISALLOWED_POTIONS) {
            if (i == value) {
                return (true);
            }
        }

        return (false);
    }

    public boolean isWarzone(Location loc) {
        if (loc.getWorld().getEnvironment() != Environment.NORMAL) {
            return (false);
        }

        return (Math.abs(loc.getBlockX()) <= WARZONE_RADIUS && Math.abs(loc.getBlockZ()) <= WARZONE_RADIUS);
    }

    public void startLogoutSequence(final Player player) {
        player.sendMessage(ChatColor.YELLOW + "§lLogging out... §ePlease wait§c 30§e seconds.");
        final AtomicInteger seconds = new AtomicInteger(30);

        BukkitTask taskid = new BukkitRunnable() {

            @Override
            public void run() {
                seconds.set(seconds.get() - 1);
                player.sendMessage(ChatColor.RED + "" + seconds.get() + "§e seconds...");

                if (seconds.get() == 0) {
                    if (tasks.containsKey(player.getName())) {
                        tasks.remove(player.getName());
                        player.setMetadata("loggedout", new FixedMetadataValue(FoxtrotPlugin.getInstance(), true));
                        player.kickPlayer("§cYou have been safely logged out of the server!");
                        cancel();
                    }
                }

            }
        }.runTaskTimer(FoxtrotPlugin.getInstance(), 20L, 20L);

        if (tasks.containsKey(player.getName())) {
            Bukkit.getScheduler().cancelTask(tasks.remove(player.getName()));
        }

        tasks.put(player.getName(), taskid.getTaskId());
    }

    public RegionData getRegion(Location location) {
        return (getRegion(LandBoard.getInstance().getTeam(location), location));
    }

    public RegionData getRegion(Team ownerTo, Location location) {
        if (ownerTo != null && ownerTo.getOwner() == null) {
            if (ownerTo.hasDTRBitmask(DTRBitmaskType.SAFE_ZONE)) {
                return (new RegionData(RegionType.SPAWN, ownerTo));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.ROAD)) {
                return (new RegionData(RegionType.ROAD, ownerTo));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.KOTH)) {
                return (new RegionData(RegionType.KOTH, ownerTo));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.CITADEL_COURTYARD)) {
                return (new RegionData(RegionType.CITADEL_COURTYARD, ownerTo));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.CITADEL_TOWN)) {
                return (new RegionData(RegionType.CITADEL_TOWN, ownerTo));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.CITADEL_KEEP)) {
                return (new RegionData(RegionType.CITADEL_KEEP, ownerTo));
            }
        }

        if (ownerTo != null) {
            return (new RegionData(RegionType.CLAIMED_LAND, ownerTo));
        } else if (isWarzone(location)) {
            return (new RegionData(RegionType.WARZONE, null));
        }

        return (new RegionData(RegionType.WILDNERNESS, null));
    }

    public void beginWarp(final Player player, final Team team, int price) {
        boolean enemyCheckBypass = player.getGameMode() == GameMode.CREATIVE || player.hasMetadata("invisible") || (!FoxtrotPlugin.getInstance().getServerHandler().isEOTW() && DTRBitmaskType.SAFE_ZONE.appliesAt(player.getLocation()));

        if (FreezeCommand.isFrozen(player)) {
            player.sendMessage(ChatColor.RED + "You cannot teleport while frozen!");
            return;
        }

        TeamHandler tm = FoxtrotPlugin.getInstance().getTeamHandler();
        double bal = tm.getPlayerTeam(player.getName()).getBalance();

        if (bal < price) {
            player.sendMessage(ChatColor.RED + "This costs §e$" + price + "§c while your team has only §e$" + bal + "§c!");
            return;
        }

        if (!enemyCheckBypass) {
            // Disallow warping while on enderpearl cooldown.
            if (!enemyCheckBypass && EnderpearlListener.getEnderpearlCooldown().containsKey(player.getName()) && EnderpearlListener.getEnderpearlCooldown().get(player.getName()) > System.currentTimeMillis()) {
                player.sendMessage(ChatColor.RED + "You cannot warp while your enderpearl cooldown is active!");
                return;
            }

            boolean enemyWithinRange = false;

            for (Entity e : player.getNearbyEntities(30, 256, 30)) {
                if (e instanceof Player) {
                    Player other = (Player) e;

                    if (other.hasMetadata("invisible") || FoxtrotPlugin.getInstance().getPvPTimerMap().hasTimer(other.getName())) {
                        continue;
                    }

                    if (tm.getPlayerTeam(other.getName()) != tm.getPlayerTeam(player.getName())) {
                        enemyWithinRange = true;
                        break;
                    }
                }
            }

            if (enemyWithinRange) {
                player.sendMessage(ChatColor.RED + "You cannot warp because an enemy is nearby!");
                return;
            }

            if (player.getHealth() <= player.getMaxHealth() - 1D) {
                player.sendMessage(ChatColor.RED + "You cannot warp because you do not have full health!");
                return;
            }

            if (player.getFoodLevel() != 20) {
                player.sendMessage(ChatColor.RED + "You cannot warp because you do not have full hunger!");
                return;
            }

            Team inClaim = LandBoard.getInstance().getTeam(player.getLocation());

            if (inClaim != null) {
                if (inClaim.getOwner() != null && !inClaim.isMember(player.getName())) {
                    player.sendMessage(ChatColor.RED + "You may not go to your team headquarters from an enemy's claim!");
                    return;
                }

                if (inClaim.getOwner() == null && (inClaim.hasDTRBitmask(DTRBitmaskType.KOTH) || inClaim.hasDTRBitmask(DTRBitmaskType.CITADEL_COURTYARD) || inClaim.hasDTRBitmask(DTRBitmaskType.CITADEL_KEEP) || inClaim.hasDTRBitmask(DTRBitmaskType.CITADEL_TOWN))) {
                    player.sendMessage(ChatColor.RED + "You may not go to your team headquarters from inside of events!");
                    return;
                }
            }
        }

        // Remove their PvP timer.
        if (FoxtrotPlugin.getInstance().getPvPTimerMap().hasTimer(player.getName()) || FoxtrotPlugin.getInstance().getPvPTimerMap().getTimer(player.getName()) == PvPTimerMap.PENDING_USE) {
            FoxtrotPlugin.getInstance().getPvPTimerMap().removeTimer(player.getName());
        }

        player.sendMessage(ChatColor.YELLOW + "§d$" + price + " §ehas been deducted from your team balance.");
        tm.getPlayerTeam(player.getName()).setBalance(tm.getPlayerTeam(player.getName()).getBalance() - price);

        // Raffle
        FoxtrotPlugin.getInstance().getRaffleHandler().giveRaffleAchievementProgress(player, RaffleAchievement.BIG_SPENDER, 1);

        FactionActionTracker.logAction(team, "actions", "HQ Teleport: " + player.getName());
        player.teleport(team.getHQ());
    }

    public boolean isUnclaimed(Location loc) {
        return (LandBoard.getInstance().getClaim(loc) == null && !isWarzone(loc));
    }

    public boolean isAdminOverride(Player player) {
        return (player.getGameMode() == GameMode.CREATIVE);
    }

    public Location getSpawnLocation() {
        return (Bukkit.getWorld("world").getSpawnLocation().add(new Vector(0.5, 1, 0.5)));
    }

    public boolean isUnclaimedOrRaidable(Location loc) {
        Team owner = LandBoard.getInstance().getTeam(loc);
        return (owner == null || owner.isRaidable());
    }

    public float getDTRLoss(Player player) {
        if (FoxtrotPlugin.getInstance().getCTFHandler().getGame() != null) {
            for (CTFFlag flag : FoxtrotPlugin.getInstance().getCTFHandler().getGame().getFlags().values()) {
                if (flag.getFlagHolder() != null && flag.getFlagHolder() == player) {
                    return (0F);
                }
            }
        }

        return (getDTRLoss(player.getLocation()));
    }

    public float getDTRLoss(Location location) {
        Team ownerTo = LandBoard.getInstance().getTeam(location);

        if (ownerTo != null) {
            if (ownerTo.hasDTRBitmask(DTRBitmaskType.HALF_DTR_LOSS)) {
                return (0.5F);
            }
        }

        return (1F);
    }

    public int getDeathban(Player player) {
        if (FoxtrotPlugin.getInstance().getCTFHandler().getGame() != null) {
            for (CTFFlag flag : FoxtrotPlugin.getInstance().getCTFHandler().getGame().getFlags().values()) {
                if (flag.getFlagHolder() != null && flag.getFlagHolder() == player) {
                    return ((int) TimeUnit.SECONDS.toSeconds(10));
                }
            }
        }

        return (getDeathban(player.getName(), player.getLocation()));
    }

    public int getDeathban(String playerName, Location location) {
        if (isPreEOTW()) {
            return ((int) TimeUnit.DAYS.toSeconds(1000));
        }

        Team ownerTo = LandBoard.getInstance().getTeam(location);

        if (ownerTo != null && ownerTo.getOwner() == null) {
            if (ownerTo.hasDTRBitmask(DTRBitmaskType.FIVE_MINUTE_DEATHBAN)) {
                return ((int) TimeUnit.MINUTES.toSeconds(5));
            } else if (ownerTo.hasDTRBitmask(DTRBitmaskType.FIFTEEN_MINUTE_DEATHBAN)) {
                return ((int) TimeUnit.MINUTES.toSeconds(15));
            }
        }

        PlaytimeMap playtime = FoxtrotPlugin.getInstance().getPlaytimeMap();
        long max = TimeUnit.HOURS.toSeconds(24);
        long ban;

        if (FoxtrotPlugin.getInstance().getServer().getPlayerExact(playerName) != null && playtime.contains(playerName)){
            ban = playtime.getPlaytime(playerName) + (playtime.getCurrentSession(playerName) / 1000L);
        } else {
            ban = playtime.getCurrentSession(playerName) / 1000L;
        }

        return ((int) Math.min(max, ban));
    }

    public void disablePlayerAttacking(final Player p, int seconds) {
        if (seconds == 10) {
            p.sendMessage(ChatColor.GRAY + "You cannot attack for " + seconds + " seconds.");
        }

        final Listener l = new Listener() {
            @EventHandler
            public void onPlayerDamage(EntityDamageByEntityEvent e) {
                if (e.getDamager() instanceof Player && (e.getEntity() instanceof Player || e.getEntity() instanceof Cow)) {
                    if (((Player) e.getDamager()).getName().equals(p.getName())) {
                        e.setCancelled(true);
                    }
                }

            }
        };

        Bukkit.getPluginManager().registerEvents(l, FoxtrotPlugin.getInstance());
        Bukkit.getScheduler().runTaskLater(FoxtrotPlugin.getInstance(), new Runnable() {
            public void run() {
                HandlerList.unregisterAll(l);
            }
        }, seconds * 20);
    }

    public boolean isSpawnBufferZone(Location loc) {
        if (loc.getWorld().getEnvironment() != Environment.NORMAL){
            return (false);
        }

        int radius = 300;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return ((x < radius && x > -radius) && (z < radius && z > -radius));
    }

    public boolean isNetherBufferZone(Location loc) {
        if (loc.getWorld().getEnvironment() != Environment.NETHER){
            return (false);
        }

        int radius = 150;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        return ((x < radius && x > -radius) && (z < radius && z > -radius));
    }

    public void handleShopSign(Sign sign, Player player) {
        ItemStack itemStack = (sign.getLine(2).contains("Crowbar") ? InvUtils.CROWBAR : Basic.get().getItemDb().get(sign.getLine(2).toLowerCase().replace(" ", "")));

        if (itemStack == null) {
            System.err.println(sign.getLine(2).toLowerCase().replace(" ", ""));
            return;
        }

        if (sign.getLine(0).toLowerCase().contains("buy")) {
            int price = 0;
            int amount = 0;

            try {
                price = Integer.parseInt(sign.getLine(3).replace("$", "").replace(",", ""));
                amount = Integer.parseInt(sign.getLine(1));
            } catch (NumberFormatException e) {
                return;
            }

            if (Basic.get().getEconomyManager().getBalance(player.getName()) >= price) {
                if (player.getInventory().firstEmpty() != -1) {
                    Basic.get().getEconomyManager().withdrawPlayer(player.getName(), price);

                    itemStack.setAmount(amount);
                    player.getInventory().addItem(itemStack);
                    player.updateInventory();

                    showSignPacket(player, sign,
                            "§aBOUGHT§r " + amount,
                            "for §a$" + NumberFormat.getNumberInstance(Locale.US).format(price),
                            "New Balance:",
                            "§a$" + NumberFormat.getNumberInstance(Locale.US).format((int) Basic.get().getEconomyManager().getBalance(player.getName()))
                    );
                } else {
                    showSignPacket(player, sign,
                            "§c§lError!",
                            "",
                            "§cNo space",
                            "§cin inventory!"
                    );
                }
            } else {
                showSignPacket(player, sign,
                        "§cInsufficient",
                        "§cfunds for",
                        sign.getLine(2),
                        sign.getLine(3)
                );
            }
        } else if (sign.getLine(0).toLowerCase().contains("sell")) {
            double pricePerItem = 0D;
            int amount = 0;

            try {
                int price = Integer.parseInt(sign.getLine(3).replace("$", "").replace(",", ""));
                amount = Integer.parseInt(sign.getLine(1));

                pricePerItem = price / amount;
            } catch (NumberFormatException e) {
                return;
            }

            int amountInInventory = Math.min(amount, countItems(player, itemStack.getType(), (int) itemStack.getDurability()));

            if (amountInInventory == 0) {
                showSignPacket(player, sign,
                        "§cYou do not",
                        "§chave any",
                        sign.getLine(2),
                        "§con you!"
                );
            } else {
                int totalPrice = (int) (amountInInventory * pricePerItem);
                removeItem(player, itemStack, amountInInventory);
                player.updateInventory();

                // Raffle
                FoxtrotPlugin.getInstance().getRaffleHandler().giveRaffleAchievementProgress(player, RaffleAchievement.TRUMP, totalPrice);
                Basic.get().getEconomyManager().depositPlayer(player.getName(), totalPrice);

                showSignPacket(player, sign,
                        "§aSOLD§r " + amountInInventory,
                        "for §a$" + NumberFormat.getNumberInstance(Locale.US).format(totalPrice),
                        "New Balance:",
                        "§a$" + NumberFormat.getNumberInstance(Locale.US).format((int) Basic.get().getEconomyManager().getBalance(player.getName()))
                );
            }
        }
    }

    public void handleDTRRegenSign(Sign sign, Player player) {
        float mult = Float.valueOf(sign.getLine(1).toLowerCase().replace("x", ""));
        int price = Integer.parseInt(sign.getLine(3).replace("$", "").replace(",", ""));
        Team playerTeam = FoxtrotPlugin.getInstance().getTeamHandler().getPlayerTeam(player.getName());

        if (playerTeam == null) {
            showSignPacket(player, sign,
                    "§c§lError",
                    "",
                    "Not on",
                    "a team"
            );

            return;
        }

        if (playerTeam.getBalance() >= price) {
            if (playerTeam.isRaidable()) {
                showSignPacket(player, sign,
                        "§c§lError",
                        "",
                        "Team is",
                        "raidable"
                );

                return;
            }

            if (playerTeam.getDTR() == playerTeam.getMaxDTR()) {
                showSignPacket(player, sign,
                        "§c§lError",
                        "",
                        "Team is",
                        "at max DTR"
                );

                return;
            }

            if (playerTeam.getDtrRegenMultiplier() != 1F) {
                showSignPacket(player, sign,
                        "§c§lError",
                        "",
                        "Team already",
                        "has multiplier"
                );

                return;
            }

            playerTeam.setBalance(playerTeam.getBalance() - price);
            playerTeam.setDtrRegenMultiplier(mult);
            playerTeam.setDTRCooldown(System.currentTimeMillis());

            showSignPacket(player, sign,
                    "§aStarted DTR",
                    "regen (" + mult + "x)",
                    "New Balance:",
                    "§a$" + NumberFormat.getNumberInstance(Locale.US).format((int) Basic.get().getEconomyManager().getBalance(player.getName()))
            );

            FoxtrotPlugin.getInstance().getServer().broadcastMessage(ChatColor.BLUE + playerTeam.getName() + ChatColor.YELLOW + " has spent " + ChatColor.GOLD + "$" + price + ChatColor.YELLOW + " and started DTR regeneration at a rate of " + ChatColor.GOLD + mult + "x" + ChatColor.YELLOW + "!");
        } else {
            showSignPacket(player, sign,
                    "§cInsufficient",
                    "§cfunds for",
                    sign.getLine(1) + " DTR",
                    "regen mult."
            );
        }
    }

    public void handleKitSign(Sign sign, Player player) {
        String kit = ChatColor.stripColor(sign.getLine(1));

        if (kit.equalsIgnoreCase("Fishing")){
            int uses = FoxtrotPlugin.getInstance().getFishingKitMap().getUses(player.getName());

            if (uses == 3){
                showSignPacket(player, sign, new String[]{ "§aFishing Kit:", "", "§cAlready used", "§c3/3 times!"});
            } else {
                ItemStack rod = new ItemStack(Material.FISHING_ROD);

                rod.addEnchantment(Enchantment.LURE, 2);
                player.getInventory().addItem(rod);
                player.updateInventory();
                player.sendMessage(ChatColor.GOLD + "Equipped the " + ChatColor.WHITE + "Fishing" + ChatColor.GOLD + " kit!");
                FoxtrotPlugin.getInstance().getFishingKitMap().setUses(player.getName(), uses + 1);
                showSignPacket(player, sign, new String[]{"§aFishing Kit:", "§bEquipped!", "", "§dUses: §e" + (uses) + "/3"});
            }
        }
    }

    public void removeItem(Player p, ItemStack it, int amount) {
        boolean specialDamage = it.getType().getMaxDurability() == (short) 0;

        for (int a = 0; a < amount; a++) {
            for (ItemStack i : p.getInventory()) {
                if (i != null) {
                    if (i.getType() == it.getType() && (!specialDamage || it.getDurability() == i.getDurability())) {
                        if (i.getAmount() == 1) {
                            p.getInventory().clear(p.getInventory().first(i));
                            break;
                        } else {
                            i.setAmount(i.getAmount() - 1);
                            break;
                        }
                    }
                }
            }
        }

    }

    public ItemStack generateDeathSign(String killed, String killer) {
        ItemStack deathsign = new ItemStack(Material.SIGN);
        ItemMeta meta = deathsign.getItemMeta();

        ArrayList<String> lore = new ArrayList<String>();

        lore.add("§4" + killed);
        lore.add("§eSlain By:");
        lore.add("§a" + killer);

        DateFormat sdf = new SimpleDateFormat("M/d HH:mm:ss");

        lore.add(sdf.format(new Date()).replace(" AM", "").replace(" PM", ""));

        meta.setLore(lore);
        meta.setDisplayName("§dDeath Sign");
        deathsign.setItemMeta(meta);

        return (deathsign);
    }

    public ItemStack generateKOTHSign(String koth, String capper) {
        ItemStack kothsign = new ItemStack(Material.SIGN);
        ItemMeta meta = kothsign.getItemMeta();

        ArrayList<String> lore = new ArrayList<String>();

        lore.add("§9" + koth);
        lore.add("§eCaptured By:");
        lore.add("§a" + capper);

        DateFormat sdf = new SimpleDateFormat("M/d HH:mm:ss");

        lore.add(sdf.format(new Date()).replace(" AM", "").replace(" PM", ""));

        meta.setLore(lore);
        meta.setDisplayName("§dKOTH Capture Sign");
        kothsign.setItemMeta(meta);

        return (kothsign);
    }

    private HashMap<Sign, BukkitRunnable> showSignTasks = new HashMap<>();

    public void showSignPacket(Player player, final Sign sign, String... lines) {
        player.sendSignChange(sign.getLocation(), lines);

        if (showSignTasks.containsKey(sign)) {
            showSignTasks.remove(sign).cancel();
        }

        BukkitRunnable br = new BukkitRunnable() {

            @Override
            public void run(){
                sign.update();
                showSignTasks.remove(sign);
            }

        };

        showSignTasks.put(sign, br);
        br.runTaskLater(FoxtrotPlugin.getInstance(), 90L);
    }

    public int countItems(Player player, Material material, int damageValue) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] items = inventory.getContents();
        int amount = 0;

        for (ItemStack item : items) {
            if (item != null) {
                boolean specialDamage = material.getMaxDurability() == (short) 0;

                if (item.getType() != null && item.getType() == material && (!specialDamage || item.getDurability() == (short) damageValue)) {
                    amount += item.getAmount();
                }
            }
        }

        return (amount);
    }

}