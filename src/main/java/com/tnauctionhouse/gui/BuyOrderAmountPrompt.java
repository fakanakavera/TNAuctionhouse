package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.BuyOrder;
import com.tnauctionhouse.orders.ItemTypeCategory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuyOrderAmountPrompt implements Listener {

    private static final Map<UUID, BuyOrderAmountPrompt> ACTIVE = new ConcurrentHashMap<>();

    private final TNAuctionHousePlugin plugin;
    private final Player viewer;
    private final BuyOrder order;
    private final int returnPage;
    private final String returnContext; // "ALL" | "FILTERED" | "CATEGORY"
    private final String query;
    private final ItemTypeCategory category;

    public BuyOrderAmountPrompt(TNAuctionHousePlugin plugin, Player viewer, BuyOrder order, int returnPage, String returnContext, String query, ItemTypeCategory category) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.order = order;
        this.returnPage = returnPage;
        this.returnContext = returnContext;
        this.query = query;
        this.category = category;
    }

    public void begin() {
        // Only one active prompt per player
        BuyOrderAmountPrompt existing = ACTIVE.put(viewer.getUniqueId(), this);
        if (existing != null) existing.end(false);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.closeInventory();
        // Compute availability across full inventory (storage + offhand)
        org.bukkit.Material type = order.getTemplateItem().getType();
        int available = countAvailable(type);
        int maxSell = Math.min(available, order.getAmount());
        viewer.sendMessage("Type the amount you want to sell (max " + maxSell + "), or type 'cancel' to abort.");
    }

    private void end(boolean reopen) {
        ACTIVE.remove(viewer.getUniqueId());
        HandlerList.unregisterAll(this);
        if (reopen) goBack();
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        BuyOrderAmountPrompt active = ACTIVE.get(viewer.getUniqueId());
        if (active != this) return;

        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            viewer.sendMessage("Cancelled.");
            end(true);
            return;
        }
        int requested;
        try {
            requested = Integer.parseInt(msg);
        } catch (NumberFormatException ex) {
            viewer.sendMessage("Invalid amount. Type a positive number or 'cancel'.");
            return;
        }
        if (requested <= 0) {
            viewer.sendMessage("Amount must be greater than zero. Type a positive number or 'cancel'.");
            return;
        }

        // Execute the fulfillment on the main thread
        int finalRequested = requested;
        Bukkit.getScheduler().runTask(plugin, () -> fulfill(finalRequested));
    }

    private void fulfill(int requested) {
        org.bukkit.Material type = order.getTemplateItem().getType();
        int available = countAvailable(type);
        int deliver = Math.min(Math.min(requested, order.getAmount()), available);
        if (deliver <= 0) {
            viewer.sendMessage("You don't have enough of that item.");
            end(true);
            return;
        }

        int total = deliver * order.getPricePerUnit();
        Economy econ = plugin.getEconomy();
        econ.depositPlayer(viewer, total);

        // Remove items across storage slots and offhand
        int toRemove = deliver;
        org.bukkit.inventory.PlayerInventory pInv = viewer.getInventory();
        ItemStack[] storage = pInv.getStorageContents();
        for (int i = 0; i < storage.length && toRemove > 0; i++) {
            ItemStack s = storage[i];
            if (s == null || s.getType() != type) continue;
            int remove = Math.min(toRemove, s.getAmount());
            s.setAmount(s.getAmount() - remove);
            if (s.getAmount() <= 0) storage[i] = null;
            toRemove -= remove;
        }
        pInv.setStorageContents(storage);
        if (toRemove > 0) {
            ItemStack off = pInv.getItemInOffHand();
            if (off != null && off.getType() == type) {
                int remove = Math.min(toRemove, off.getAmount());
                off.setAmount(off.getAmount() - remove);
                pInv.setItemInOffHand(off.getAmount() <= 0 ? null : off);
                toRemove -= remove;
            }
        }

        // Enqueue items for buyer
        ItemStack delivered = order.getTemplateItem().clone();
        delivered.setAmount(deliver);
        plugin.getOrderManager().enqueueDelivery(order.getBuyerId(), delivered);

        // Update order: full or partial
        if (deliver == order.getAmount()) {
            plugin.getOrderManager().removeBuyOrder(order);
            plugin.getOrderLogger().logCompletedOrder(
                    order.getOrderId(),
                    "BUY",
                    null,
                    order.getBuyerId(),
                    order.getTemplateItem(),
                    order.getAmount(),
                    order.getPricePerUnit(),
                    System.currentTimeMillis()
            );
        } else {
            plugin.getOrderManager().removeBuyOrder(order);
            plugin.getOrderManager().createBuyOrder(order.getBuyerId(), order.getTemplateItem(), order.getPricePerUnit(), order.getAmount() - deliver, order.getEscrowTotal() - total);
        }

        viewer.sendMessage("Sold " + deliver + "x for $" + order.getPricePerUnit() + " each.");
        end(true);
    }

    private int countAvailable(org.bukkit.Material type) {
        int count = 0;
        org.bukkit.inventory.PlayerInventory pInv = viewer.getInventory();
        ItemStack[] storage = pInv.getStorageContents();
        if (storage != null) {
            for (ItemStack s : storage) {
                if (s != null && s.getType() == type) count += s.getAmount();
            }
        }
        ItemStack off = pInv.getItemInOffHand();
        if (off != null && off.getType() == type) count += off.getAmount();
        return count;
    }

    private void goBack() {
        if ("FILTERED".equals(returnContext)) {
            new FilteredBuyOrdersGUI(plugin, viewer, returnPage, query).open();
            return;
        }
        if ("CATEGORY".equals(returnContext)) {
            new TypeFilteredBuyOrdersGUI(plugin, viewer, returnPage, category).open();
            return;
        }
        new BuyOrdersGUI(plugin, viewer, returnPage).open();
    }
}


