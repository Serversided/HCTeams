package net.frozenorb.foxtrot.chat.listeners;

import mkremins.fanciful.FancyMessage;
import net.frozenorb.foxtrot.FoxConstants;
import net.frozenorb.foxtrot.Foxtrot;
import net.frozenorb.foxtrot.chat.enums.ChatMode;
import net.frozenorb.foxtrot.team.Team;
import net.frozenorb.foxtrot.team.commands.team.TeamMuteCommand;
import net.frozenorb.foxtrot.team.commands.team.TeamShadowMuteCommand;
import net.frozenorb.foxtrot.teamactiontracker.TeamActionTracker;
import net.frozenorb.foxtrot.teamactiontracker.enums.TeamActionType;
import org.bson.types.ObjectId;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler(priority=EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Team team = Foxtrot.getInstance().getTeamHandler().getTeam(event.getPlayer());
        String highRollerString = Foxtrot.getInstance().getServerHandler().getHighRollers().contains(event.getPlayer().getName()) ? FoxConstants.highRollerPrefix() : "";
        String customPrefixString = Foxtrot.getInstance().getServerHandler().getCustomPrefixes().containsKey(event.getPlayer().getName()) ? Foxtrot.getInstance().getServerHandler().getCustomPrefixes().get(event.getPlayer().getName()) : "";
        ChatMode chatMode = Foxtrot.getInstance().getChatModeMap().getChatMode(event.getPlayer().getUniqueId());

        // Determine if we're using one of our custom
        // chat channel prefixes.
        // @ for team chat,
        // # for ally chat,
        // ! for global chat
        boolean doTeamChat = event.getMessage().startsWith("@");
        boolean doAllyChat = event.getMessage().startsWith("#");
        boolean doGlobalChat = event.getMessage().startsWith("!");

        // and take off the character 'starting' it
        if (doTeamChat || doAllyChat || doGlobalChat) {
            event.setMessage(event.getMessage().substring(1));
        }

        // Determine our final chat mode.
        // We check if they used a custom prefix / aren't in a team,
        // otherwise, we fall back on their configuration
        if (doGlobalChat) {
            chatMode = ChatMode.PUBLIC;
        } else if (doTeamChat) {
            chatMode = ChatMode.TEAM;
        } else if (doAllyChat && team != null && team.getAllies().size() != 0) {
            chatMode = ChatMode.ALLIANCE;
        }

        // If another plugin cancelled this event before it got to us (we are on MONITOR, so it'll happen)
        if (event.isCancelled() && chatMode == ChatMode.PUBLIC) { // Only respect cancelled events if this is public chat. Who cares what their team says.
            return;
        }

        // Any route we go down will cancel the event eventually.
        // Let's just do it here.
        event.setCancelled(true);

        // If someone's not in a team, instead of forcing their 'channel' to public,
        // we just tell them they can't.
        if (chatMode != ChatMode.PUBLIC && team == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "You can't speak in non-public chat if you're not in a team!");
            return;
        }

        // and here starts the big logic switch
        switch (chatMode) {
            case PUBLIC:
                if (TeamMuteCommand.getTeamMutes().containsKey(event.getPlayer().getUniqueId())) {
                    event.getPlayer().sendMessage(ChatColor.RED.toString() + ChatColor.BOLD + "Your team is muted!");
                    return;
                }

                String publicChatFormat = FoxConstants.publicChatFormat(team, highRollerString, customPrefixString);
                String finalMessage = String.format(publicChatFormat, event.getPlayer().getDisplayName(), event.getMessage());

                // Loop those who are to receive the message (which they won't if they have the sender /ignore'd or something),
                // not online players
                for (Player player : event.getRecipients()) {
                    if (team == null) {
                        // If the player sending the message is shadowmuted (if their team was and they left it)
                        // then we don't allow them to. We probably could move this check "higher up", but oh well.
                        if (TeamShadowMuteCommand.getTeamShadowMutes().containsKey(event.getPlayer().getUniqueId())) {
                            continue;
                        }

                        // If their chat is enabled (which it is by default) or the sender is op, send them the message
                        // The isOp() fragment is so OP messages are sent regardless of if the player's chat is toggled
                        if (event.getPlayer().isOp() || Foxtrot.getInstance().getToggleGlobalChatMap().isGlobalChatToggled(player.getUniqueId())) {
                            player.sendMessage(finalMessage);
                        }
                    } else {
                        if (team.isMember(player.getUniqueId())) {
                            // Gypsie way to get a custom color if they're allies/teammates
                            player.sendMessage(finalMessage.replace(ChatColor.GOLD + "[" + ChatColor.YELLOW, ChatColor.GOLD + "[" + ChatColor.DARK_GREEN));
                        } else if (team.isAlly(player.getUniqueId())) {
                            // Gypsie way to get a custom color if they're allies/teammates
                            player.sendMessage(finalMessage.replace(ChatColor.GOLD + "[" + ChatColor.YELLOW, ChatColor.GOLD + "[" + Team.ALLY_COLOR));
                        } else {
                            // We only check this here as...
                            // Team members always see their team's messages
                            // Allies always see their allies' messages, 'cause they'll probably be in a TS or something
                            // and they could figure out this code even exists
                            if (TeamShadowMuteCommand.getTeamShadowMutes().containsKey(event.getPlayer().getUniqueId())) {
                                continue;
                            }

                            // If their chat is enabled (which it is by default) or the sender is op, send them the message
                            // The isOp() fragment is so OP messages are sent regardless of if the player's chat is toggled
                            if (event.getPlayer().isOp() || Foxtrot.getInstance().getToggleGlobalChatMap().isGlobalChatToggled(player.getUniqueId())) {
                                player.sendMessage(finalMessage);
                            }
                        }
                    }
                }

                Foxtrot.getInstance().getServer().getConsoleSender().sendMessage(finalMessage);
                break;
            case ALLIANCE:
                String allyChatFormat = FoxConstants.allyChatFormat(event.getPlayer(), event.getMessage());
                String allyChatSpyFormat = FoxConstants.allyChatSpyFormat(team, event.getPlayer(), event.getMessage());

                // Loop online players and not recipients just in case you're weird and
                // /ignore your teammates/allies
                for (Player player : Foxtrot.getInstance().getServer().getOnlinePlayers()) {
                    if (team.isMember(player.getUniqueId()) || team.isAlly(player.getUniqueId())) {
                        player.sendMessage(allyChatFormat);
                    } else if (Foxtrot.getInstance().getChatSpyMap().getChatSpy(player.getUniqueId()).contains(team.getUniqueId())) {
                        player.sendMessage(allyChatSpyFormat);
                    }
                }

                // Log to ally's allychat log.
                for (ObjectId allyId : team.getAllies()) {
                    Team ally = Foxtrot.getInstance().getTeamHandler().getTeam(allyId);

                    if (ally != null) {
                        TeamActionTracker.logAction(ally, TeamActionType.ALLY_CHAT, "[" + team.getName() + "] " + event.getPlayer().getName() + ": " + event.getMessage());
                    }
                }

                // Log to our own allychat log.
                TeamActionTracker.logAction(team, TeamActionType.ALLY_CHAT, "[" + team.getName() + "] " + event.getPlayer().getName() + ": " + event.getMessage());
                Foxtrot.getInstance().getServer().getLogger().info("[Ally Chat] [" + team.getName() + "] " + event.getPlayer().getName() + ": " + event.getMessage());
                break;
            case TEAM:
                String teamChatFormat = FoxConstants.teamChatFormat(event.getPlayer(), event.getMessage());
                String teamChatSpyFormat = FoxConstants.teamChatSpyFormat(team, event.getPlayer(), event.getMessage());

                // Loop online players and not recipients just in case you're weird and
                // /ignore your teammates
                for (Player player : Foxtrot.getInstance().getServer().getOnlinePlayers()) {
                    if (team.isMember(player.getUniqueId())) {
                        player.sendMessage(teamChatFormat);
                    } else if (Foxtrot.getInstance().getChatSpyMap().getChatSpy(player.getUniqueId()).contains(team.getUniqueId())) {
                        player.sendMessage(teamChatSpyFormat);
                    }
                }

                // Log to our teamchat log.
                TeamActionTracker.logAction(team, TeamActionType.TEAM_CHAT, event.getPlayer().getName() + ": " + event.getMessage());
                Foxtrot.getInstance().getServer().getLogger().info("[Team Chat] [" + team.getName() + "] " + event.getPlayer().getName() + ": " + event.getMessage());
                break;
        }
    }

}