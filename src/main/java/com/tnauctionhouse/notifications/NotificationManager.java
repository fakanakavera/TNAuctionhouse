package com.tnauctionhouse.notifications;

import com.tnauctionhouse.TNAuctionHousePlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NotificationManager implements org.bukkit.event.Listener {

    private final TNAuctionHousePlugin plugin;
    private final File storeFile;
    private final Map<UUID, List<String>> pending = new HashMap<>();

    public NotificationManager(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.storeFile = new File(plugin.getDataFolder(), "notifications.yml");
        load();
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                UUID id = e.getPlayer().getUniqueId();
                List<String> msgs = drain(id);
                for (String msg : msgs) e.getPlayer().sendMessage(msg);
            }
        }, plugin);
    }

    public synchronized void notify(UUID userId, String message) {
        org.bukkit.entity.Player online = Bukkit.getPlayer(userId);
        if (online != null && online.isOnline()) {
            online.sendMessage(message);
            return;
        }
        pending.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
        saveAsync();
    }

    private synchronized List<String> drain(UUID userId) {
        List<String> msgs = pending.remove(userId);
        if (msgs == null) return Collections.emptyList();
        saveAsync();
        return msgs;
    }

    private void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<String>> e : pending.entrySet()) {
            yaml.set("pending." + e.getKey(), e.getValue());
        }
        try { yaml.save(storeFile);} catch (IOException ignored) {}
    }

    private synchronized void load() {
        pending.clear();
        if (!storeFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storeFile);
        org.bukkit.configuration.ConfigurationSection sec = yaml.getConfigurationSection("pending");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                List<String> msgs = sec.getStringList(key);
                if (!msgs.isEmpty()) pending.put(id, new ArrayList<>(msgs));
            } catch (Exception ignored) {}
        }
    }
}


