package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.SellOrder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

/**
 * 3x9 confirmation GUI: left 3x3 green confirm, center item at slot 13, right 3x3 red cancel.
 */
public class ConfirmPurchaseGUI implements Listener {

    private final TNAuctionHousePlugin plugin;
    private final Player viewer;
    private final SellOrder order;
    private final int returnPage;
    private final String returnContext; // "ALL", "FILTERED", "CATEGORY"
    private final String query; // for filtered context
    private final com.tnauctionhouse.orders.ItemTypeCategory category; // for category context

    private Inventory inv;

    public ConfirmPurchaseGUI(TNAuctionHousePlugin plugin, Player viewer, SellOrder order, int returnPage, String returnContext, String query, com.tnauctionhouse.orders.ItemTypeCategory category) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.order = order;
        this.returnPage = returnPage;
        this.returnContext = returnContext;
        this.query = query;
        this.category = category;
    }

    public void open() {
        this.inv = Bukkit.createInventory(null, 27, Component.text("Confirm Purchase"));
        // Fill confirm (left 3x3: columns 0-2 rows 0-2)
        ItemStack green = button(Material.LIME_STAINED_GLASS_PANE, "Confirm");
        ItemStack red = button(Material.RED_STAINED_GLASS_PANE, "Cancel");

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                inv.setItem(row * 9 + col, green);
            }
        }
        // Cancel (right 3x3: columns 6-8)
        for (int row = 0; row < 3; row++) {
            for (int col = 6; col < 9; col++) {
                inv.setItem(row * 9 + col, red);
            }
        }

        // Place the item at center (slot 13)
        ItemStack display = order.getItem().clone();
        display.setAmount(Math.min(order.getAmount(), display.getMaxStackSize()));
        inv.setItem(13, display);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inv);
    }

    private ItemStack button(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity clicker = e.getWhoClicked();
        if (!clicker.getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getView().getTopInventory() != inv) return;
        if (e.getRawSlot() >= inv.getSize()) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        // Confirm zones: columns 0-2 any row
        boolean isConfirm = (slot % 9) <= 2;
        // Cancel zones: columns 6-8 any row
        boolean isCancel = (slot % 9) >= 6;

        if (isConfirm) {
            completePurchase();
        } else if (isCancel) {
            goBack();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getInventory() != inv) return;
        HandlerList.unregisterAll(this);
    }

    private void completePurchase() {
        Economy econ = plugin.getEconomy();
        int unit = order.getPricePerUnit();
        boolean taxEnabled = plugin.getConfig().getBoolean("tax.enabled", true);
        double taxRate = Math.max(0.0, plugin.getConfig().getDouble("tax.rate", 0.10));
        String mode = String.valueOf(plugin.getConfig().getString("tax.mode", "UPFRONT")).toUpperCase(java.util.Locale.ROOT);
        int unitToCharge = unit;
        if (taxEnabled && "ADD_TO_PRICE".equals(mode)) unitToCharge = (int) Math.round(unit * (1.0 + taxRate));
        int total = unitToCharge * order.getAmount();
        if (!econ.has(viewer, total)) {
            viewer.sendMessage("You don't have enough money.");
            goBack();
            return;
        }

        econ.withdrawPlayer(viewer, total);
        double sellerReceive = (taxEnabled && "ADD_TO_PRICE".equals(mode)) ? (unit * order.getAmount()) : total;
        econ.depositPlayer(Bukkit.getOfflinePlayer(order.getSellerId()), sellerReceive);

        ItemStack toGive = order.getItem();
        viewer.getInventory().addItem(toGive);

        plugin.getOrderManager().removeSellOrder(order);
        plugin.getOrderLogger().logCompletedOrder(
                order.getOrderId(),
                "SELL",
                order.getSellerId(),
                viewer.getUniqueId(),
                order.getItem(),
                order.getAmount(),
                order.getPricePerUnit(),
                System.currentTimeMillis()
        );
        viewer.sendMessage("Purchased " + order.getAmount() + "x for $" + unitToCharge + " each.");
        goBack();
    }

    private void goBack() {
        HandlerList.unregisterAll(this);
        if ("FILTERED".equals(returnContext)) {
            new FilteredSellOrdersGUI(plugin, viewer, returnPage, query).open();
            return;
        }
        if ("CATEGORY".equals(returnContext)) {
            new TypeFilteredSellOrdersGUI(plugin, viewer, returnPage, category).open();
            return;
        }
        new SellOrdersGUI(plugin, viewer, returnPage).open();
    }
}


