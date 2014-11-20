package net.frozenorb.foxtrot.koth;

import com.mysql.jdbc.StringUtils;
import net.frozenorb.foxtrot.FoxtrotPlugin;
import net.frozenorb.foxtrot.command.CommandHandler;
import net.frozenorb.foxtrot.command.objects.ParamTabCompleter;
import net.frozenorb.foxtrot.command.objects.ParamTransformer;
import net.frozenorb.foxtrot.koth.tasks.KOTHSchedulerTask;
import net.minecraft.util.com.google.gson.Gson;
import net.minecraft.util.org.apache.commons.io.IOUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by macguy8 on 10/31/2014.
 */
public class KOTHHandler {

    private static Set<KOTH> KOTHs = new HashSet<KOTH>();

    public static void init() {
        loadKOTHs();

        Calendar date = Calendar.getInstance();

        date.set(Calendar.MINUTE, 60);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        (new Timer("KOTH Scheduler")).schedule(new KOTHSchedulerTask(), date.getTime(), TimeUnit.HOURS.toMillis(1));

        CommandHandler.registerTransformer(KOTH.class, new ParamTransformer() {

            @Override
            public Object transform(Player sender, String source) {
                KOTH koth = getKOTH(source);

                if (koth == null) {
                    sender.sendMessage(ChatColor.RED + "No KOTH with the name " + source + " found.");
                    return (null);
                }

                return (koth);
            }

        });

        CommandHandler.registerTabCompleter(KOTH.class, new ParamTabCompleter() {

            public List<String> tabComplete(Player sender, String source) {
                List<String> completions = new ArrayList<String>();

                for (KOTH koth : getKOTHs()) {
                    if (StringUtils.startsWithIgnoreCase(koth.getName(), source)) {
                        completions.add(koth.getName());
                    }
                }

                return (completions);
            }

        });

        new BukkitRunnable() {

            public void run() {
                for (KOTH koth : KOTHs) {
                    if (koth.isActive()) {
                        koth.tick();
                    }
                }
            }

        }.runTaskTimer(FoxtrotPlugin.getInstance(), 20L, 20L);
    }

    public static void loadKOTHs() {
        try {
            File kothsBase = new File("KOTHs");

            if (!kothsBase.exists()) {
                kothsBase.mkdir();
            }

            for (File kothFile : kothsBase.listFiles()) {
                if (kothFile.getName().endsWith(".json")) {
                    BufferedInputStream e = new BufferedInputStream(new FileInputStream(kothFile));

                    StringWriter writer = new StringWriter();
                    IOUtils.copy(e, writer, "utf-8");

                    KOTHs.add((new Gson()).fromJson(writer.toString(), KOTH.class));

                    e.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveKOTHs() {
        try {
            File kothsBase = new File("KOTHs");

            if (!kothsBase.exists()) {
                kothsBase.mkdir();
            }

            for (File kothFile : kothsBase.listFiles()) {
                kothFile.delete();
            }

            for (KOTH koth : KOTHs) {
                File kothFile = new File(kothsBase, koth.getName() + ".json");
                FileWriter e = new FileWriter(kothFile);

                e.write((new Gson()).toJson(koth));

                e.flush();
                e.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<KOTH> getKOTHs() {
        return (KOTHs);
    }

    public static KOTH getKOTH(String name) {
        for (KOTH koth : KOTHs) {
            if (koth.getName().equalsIgnoreCase(name)) {
                return (koth);
            }
        }

        return (null);
    }

    public static void onSchedulerTick() {
        for (Player player : FoxtrotPlugin.getInstance().getServer().getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(ChatColor.DARK_AQUA + "Running KOTH scheduler task...");
            }
        }

        // Don't start a KOTH if another one is active.
        for (KOTH koth : KOTHHandler.getKOTHs()) {
            if (koth.isActive()) {
                return;
            }
        }

        Calendar date = Calendar.getInstance();
        int hour = date.get(Calendar.HOUR_OF_DAY);
        List<KOTH> koths = Arrays.asList(KOTHHandler.getKOTHs().toArray(new KOTH[KOTHHandler.getKOTHs().size()]));

        if (/*koth == 16 || koth ==  18 || koth == 20 || koth == 22 || koth == 24*/true) {
            koths.get(FoxtrotPlugin.RANDOM.nextInt(koths.size())).activate();
        }
    }

}