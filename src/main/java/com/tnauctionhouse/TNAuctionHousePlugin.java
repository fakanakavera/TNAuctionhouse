package com.tnauctionhouse;

import com.tnauctionhouse.commands.BuyOrderCreateCommand;
import com.tnauctionhouse.commands.OpenBuyOrdersCommand;
import com.tnauctionhouse.commands.OpenSellOrdersCommand;
import com.tnauctionhouse.commands.SellOrderCreateCommand;
import com.tnauctionhouse.orders.OrderManager;
import com.tnauctionhouse.logging.OrderLogger;
import com.tnauctionhouse.notifications.NotificationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class TNAuctionHousePlugin extends JavaPlugin {

    private static TNAuctionHousePlugin instance;
    private Economy economy;
    private OrderManager orderManager;
    private OrderLogger orderLogger;
    private NotificationManager notificationManager;

    public static TNAuctionHousePlugin getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }

    public OrderLogger getOrderLogger() {
        return orderLogger;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found. Disabling TNauctionhouse.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        orderManager = new OrderManager();
        try {
            orderManager.load(new java.io.File(getDataFolder(), "orders.yml"));
            getLogger().info("Loaded orders and deliveries.");
        } catch (Exception ex) {
            getLogger().warning("Failed to load orders: " + ex.getMessage());
        }

		// Schedule periodic cleanup for expired auctions -> return items to seller deliveries
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			try {
				long now = System.currentTimeMillis();
				java.util.List<com.tnauctionhouse.orders.Auction> toRemove = new java.util.ArrayList<>();
				for (com.tnauctionhouse.orders.Auction auc : orderManager.getAuctions()) {
					if (auc.getEndAt() <= now) {
						// enqueue back to seller
						org.bukkit.inventory.ItemStack back = auc.getItem().clone();
						back.setAmount(auc.getAmount());
						orderManager.enqueueDelivery(auc.getSellerId(), back);
						toRemove.add(auc);
					}
				}
				for (com.tnauctionhouse.orders.Auction auc : toRemove) orderManager.removeAuction(auc);
			} catch (Throwable ignored) {}
		}, 20L * 60L, 20L * 60L);

        orderLogger = new OrderLogger(this);
        notificationManager = new NotificationManager(this);

        // Register commands
        getCommand("sellorder").setExecutor(new SellOrderCreateCommand(this));
        getCommand("buyorder").setExecutor(new BuyOrderCreateCommand(this));
        getCommand("sellorders").setExecutor(new OpenSellOrdersCommand(this));
        getCommand("buyorders").setExecutor(new OpenBuyOrdersCommand(this));
		if (getCommand("auction") != null) {
			getCommand("auction").setExecutor(new com.tnauctionhouse.commands.AuctionCreateCommand(this));
		}
		if (getCommand("auctions") != null) {
			getCommand("auctions").setExecutor(new com.tnauctionhouse.commands.OpenAuctionsCommand(this));
		}
        if (getCommand("auctionhouse") != null) {
            getCommand("auctionhouse").setExecutor(new com.tnauctionhouse.commands.AuctionHouseCommand(this));
        }
        if (getCommand("withdrawitems") != null) {
            getCommand("withdrawitems").setExecutor(new com.tnauctionhouse.commands.WithdrawOrderCommand(this));
        }
        // '/ah' alias provided via plugin.yml
        if (getCommand("myorders") != null) {
            getCommand("myorders").setExecutor(new com.tnauctionhouse.commands.OpenMyOrdersCommand(this));
        }
    }

    @Override
    public void onDisable() {
        if (orderManager != null) {
            try {
                orderManager.save(new java.io.File(getDataFolder(), "orders.yml"));
                getLogger().info("Saved orders and deliveries.");
            } catch (Exception ex) {
                getLogger().severe("Failed to save orders: " + ex.getMessage());
            }
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    
}


