package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.BuyOrder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class BuyOrdersGUI extends BaseOrdersGUI {

    public BuyOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page) {
        super(plugin, viewer, page);
    }

    @Override
    protected String getTitle() {
        return "Buy Orders - Page " + (page + 1);
    }

    @Override
    protected void populateItems() {
        List<BuyOrder> pageOrders = plugin.getOrderManager().getBuyOrdersPage(page, 45);
        int slot = 0;
		for (BuyOrder order : pageOrders) {
			ItemStack display = order.getTemplateItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
				meta.displayName(Component.text(display.getType().name()));
				display.setItemMeta(meta);
				setLore(display, java.util.Arrays.asList(
						"Amount: " + order.getAmount(),
						"Payout per unit: $" + order.getPricePerUnit(),
						"Max payout: $" + (order.getPricePerUnit() * order.getAmount()),
						"Escrow: $" + order.getEscrowTotal()
				));

            }
			// Show as a stack up to the item's max stack size, without mutating the stored order item
			display.setAmount(Math.min(order.getAmount(), display.getMaxStackSize()));
            inv.setItem(slot++, display);
        }

        inv.setItem(45, button(Material.ARROW, "Previous"));
        inv.setItem(49, button(Material.HOPPER, "Filter"));
        inv.setItem(53, button(Material.ARROW, "Next"));
    }

    @Override
    protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (rawSlot == 45) {
            if (page > 0) new BuyOrdersGUI(plugin, viewer, page - 1).open();
            return;
        }
        if (rawSlot == 53) {
            new BuyOrdersGUI(plugin, viewer, page + 1).open();
            return;
        }
        if (rawSlot == 49) {
            new FilterMenuGUI(plugin, viewer, true).open();
            return;
        }

        int index = rawSlot;
        List<BuyOrder> pageOrders = plugin.getOrderManager().getBuyOrdersPage(page, 45);
        if (index < 0 || index >= pageOrders.size()) return;
        BuyOrder order = pageOrders.get(index);

        // Prevent fulfilling your own order
        if (order.getBuyerId().equals(viewer.getUniqueId()) && !viewer.hasPermission("tnauctionhouse.bypass.self")) {
            viewer.sendMessage("You cannot fulfill your own buy order.");
            return;
        }

		// Switch to chat prompt for amount entry
		new BuyOrderAmountPrompt(plugin, viewer, order, page, "ALL", null, null).begin();
    }
}


