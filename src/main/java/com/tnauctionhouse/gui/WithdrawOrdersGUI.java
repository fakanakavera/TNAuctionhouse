package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class WithdrawOrdersGUI extends BaseOrdersGUI {

    public WithdrawOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page) {
        super(plugin, viewer, page);
    }

    @Override
    protected String getTitle() {
        return "Order Withdrawals - Page " + (page + 1);
    }

    @Override
    protected void populateItems() {
        List<ItemStack> pageItems = plugin.getOrderManager().getDeliveriesPage(viewer.getUniqueId(), page, 45);
        int slot = 0;
        for (ItemStack stack : pageItems) {
            ItemStack display = stack.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(display.getType().name()));
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
        }

        inv.setItem(45, button(Material.ARROW, "Previous"));
        inv.setItem(49, button(Material.CHEST_MINECART, "Withdraw All"));
        inv.setItem(53, button(Material.ARROW, "Next"));
    }

    @Override
    protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (rawSlot == 45) { if (page > 0) new WithdrawOrdersGUI(plugin, viewer, page - 1).open(); return; }
        if (rawSlot == 53) { new WithdrawOrdersGUI(plugin, viewer, page + 1).open(); return; }
        if (rawSlot == 49) { withdrawAll(); return; }

        int index = page * 45 + rawSlot;
        ItemStack removed = plugin.getOrderManager().removeDeliveryAt(viewer.getUniqueId(), index);
        if (removed == null) return;

        java.util.Map<Integer, ItemStack> leftovers = viewer.getInventory().addItem(removed);
        if (!leftovers.isEmpty()) {
            // If inventory is full, re-enqueue leftover amounts
            for (ItemStack lf : leftovers.values()) {
                plugin.getOrderManager().enqueueDelivery(viewer.getUniqueId(), lf);
            }
            viewer.sendMessage("Inventory full. Some items were returned to your inbox.");
        } else {
            viewer.sendMessage("Withdrew " + removed.getAmount() + "x " + removed.getType().name() + ".");
        }

        new WithdrawOrdersGUI(plugin, viewer, page).open();
    }

    private void withdrawAll() {
        List<ItemStack> items = plugin.getOrderManager().drainDeliveries(viewer.getUniqueId());
        if (items.isEmpty()) {
            viewer.sendMessage("No items to withdraw.");
            return;
        }
        java.util.List<ItemStack> leftovers = new java.util.ArrayList<>();
        for (ItemStack it : items) {
            java.util.Map<Integer, ItemStack> lf = viewer.getInventory().addItem(it);
            leftovers.addAll(lf.values());
        }
        if (!leftovers.isEmpty()) {
            for (ItemStack lf : leftovers) plugin.getOrderManager().enqueueDelivery(viewer.getUniqueId(), lf);
            viewer.sendMessage("Inventory full. Returned " + leftovers.size() + " stacks to inbox.");
        } else {
            viewer.sendMessage("Withdrew all items.");
        }
        new WithdrawOrdersGUI(plugin, viewer, page).open();
    }
}


