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

        orderLogger = new OrderLogger(this);
        notificationManager = new NotificationManager(this);

        // Schedule periodic auction settlement (every minute)
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                settleEndedAuctions();
            } catch (Throwable t) {
                getLogger().warning("Auction settlement error: " + t.getMessage());
            }
        }, 20L * 30L, 20L * 60L); // initial delay 30s, then every 60s

        // Register commands
        getCommand("sellorder").setExecutor(new SellOrderCreateCommand(this));
        getCommand("buyorder").setExecutor(new BuyOrderCreateCommand(this));
        getCommand("sellorders").setExecutor(new OpenSellOrdersCommand(this));
        getCommand("buyorders").setExecutor(new OpenBuyOrdersCommand(this));
        if (getCommand("auction") != null) {
            getCommand("auction").setExecutor(new com.tnauctionhouse.commands.AuctionCreateCommand(this));
        }
        if (getCommand("auctionbid") != null) {
            getCommand("auctionbid").setExecutor(new com.tnauctionhouse.commands.AuctionBidCommand(this));
        }
        if (getCommand("auctions") != null) {
            getCommand("auctions").setExecutor(new com.tnauctionhouse.commands.AuctionsListCommand(this));
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

    private void settleEndedAuctions() {
        long now = System.currentTimeMillis();
        java.util.List<com.tnauctionhouse.orders.AuctionOrder> ended = new java.util.ArrayList<>();
        for (com.tnauctionhouse.orders.AuctionOrder ao : orderManager.getAuctionOrders()) {
            if (now >= ao.getEndAt()) ended.add(ao);
        }
        if (ended.isEmpty()) return;

        for (com.tnauctionhouse.orders.AuctionOrder ao : ended) {
            try {
                java.util.UUID highestBidder = ao.getHighestBidderId();
                int highestBid = ao.getHighestBid();
                if (highestBidder != null && highestBid > 0) {
                    // Pay seller and deliver item to winner
                    net.milkbowl.vault.economy.Economy econ = getEconomy();
                    org.bukkit.OfflinePlayer seller = getServer().getOfflinePlayer(ao.getSellerId());
                    econ.depositPlayer(seller, highestBid);

                    // Attempt direct delivery if online; otherwise queue withdrawal
                    org.bukkit.OfflinePlayer winner = getServer().getOfflinePlayer(highestBidder);
                    if (winner.isOnline()) {
                        org.bukkit.entity.Player p = winner.getPlayer();
                        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftovers = p.getInventory().addItem(ao.getItem());
                        if (!leftovers.isEmpty()) {
                            for (org.bukkit.inventory.ItemStack stack : leftovers.values()) {
                                orderManager.enqueueDelivery(winner.getUniqueId(), stack);
                            }
                            p.sendMessage("Your inventory was full; some items were sent to withdrawal.");
                        }
                        p.sendMessage("You won an auction and received the item.");
                    } else {
                        orderManager.enqueueDelivery(winner.getUniqueId(), ao.getItem());
                    }

                    if (seller.isOnline()) seller.getPlayer().sendMessage("Your auction ended. You received $" + highestBid + ".");
                } else {
                    // No bids: return item to seller via withdrawal queue
                    orderManager.enqueueDelivery(ao.getSellerId(), ao.getItem());
                    org.bukkit.OfflinePlayer seller = getServer().getOfflinePlayer(ao.getSellerId());
                    if (seller.isOnline()) seller.getPlayer().sendMessage("Your auction ended with no bids. Your item was returned.");
                }
            } catch (Throwable t) {
                getLogger().warning("Failed to settle auction " + ao.getOrderId() + ": " + t.getMessage());
            } finally {
                orderManager.removeAuctionOrder(ao);
            }
        }

        // Persist after settlements
        try {
            orderManager.save(new java.io.File(getDataFolder(), "orders.yml"));
        } catch (Exception ex) {
            getLogger().severe("Failed to save orders after auction settlement: " + ex.getMessage());
        }
    }
}


