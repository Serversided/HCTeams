package net.frozenorb.foxtrot.jedis.persist;

import net.frozenorb.foxtrot.jedis.RedisPersistMap;
import net.frozenorb.foxtrot.team.chat.ChatMode;

public class ChatModeMap extends RedisPersistMap<ChatMode> {

    public ChatModeMap() {
        super("ChatModes", "ChatMode");
    }

    @Override
    public String getRedisValue(ChatMode chatMode) {
        return (chatMode.name());
    }

    @Override
    public ChatMode getJavaObject(String str) {
        return (ChatMode.valueOf(str));
    }

    @Override
    public Object getMongoValue(ChatMode chatMode) {
        return (chatMode.name());
    }

    public ChatMode getChatMode(String player) {
        return (contains(player) ? getValue(player) : ChatMode.PUBLIC);
    }

    public void setChatMode(String player, ChatMode chatMode) {
        updateValueAsync(player, chatMode);
    }

}