package com.tnauctionhouse.logging;

import com.tnauctionhouse.TNAuctionHousePlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class OrderLogger {

    private final TNAuctionHousePlugin plugin;
    private final File logFile;

    public OrderLogger(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
        File data = plugin.getDataFolder();
        if (!data.exists()) {
            // noinspection ResultOfMethodCallIgnored
            data.mkdirs();
        }
        this.logFile = new File(data, "completed-orders.yml");
    }

	public synchronized void logCompletedOrder(UUID orderId,
						  String orderType,
						  UUID sellerId,
						  UUID buyerId,
						  ItemStack item,
						  int amount,
						  int pricePerUnit,
						  long timestampMs) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            String isoTime = iso.format(new Date(timestampMs));

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(logFile);
            String key = String.format(Locale.ROOT, "orders.%d_%s_%s", timestampMs, orderType.toLowerCase(Locale.ROOT), orderId);
            yaml.set(key + ".orderId", orderId.toString());
            yaml.set(key + ".type", orderType);
            yaml.set(key + ".seller", sellerId != null ? sellerId.toString() : null);
            yaml.set(key + ".buyer", buyerId != null ? buyerId.toString() : null);
            yaml.set(key + ".amount", amount);
			yaml.set(key + ".pricePerUnit", pricePerUnit);
			yaml.set(key + ".totalPrice", pricePerUnit * amount);
            yaml.set(key + ".timestamp", timestampMs);
            yaml.set(key + ".datetime", isoTime);
            yaml.set(key + ".item", item);
            yaml.save(logFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write completed order log: " + ex.getMessage());
        }
    }
}


