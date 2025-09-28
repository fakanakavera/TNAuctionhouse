package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.BuyOrder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class FilteredBuyOrdersGUI extends BaseOrdersGUI {

    private final String query;

    public FilteredBuyOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page, String query) {
        super(plugin, viewer, page);
        this.query = query == null ? "" : query;
    }

    @Override
    protected String getTitle() {
        return "Buy Orders: '" + (query.length() > 16 ? query.substring(0, 16) + "…" : query) + "' - Page " + (page + 1);
    }

    @Override
    protected void populateItems() {
        List<BuyOrder> pageOrders = plugin.getOrderManager().searchBuyOrders(query, page, 45);
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
        inv.setItem(53, button(Material.ARROW, "Next"));
    }

    @Override
    protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (rawSlot == 45) {
            if (page > 0) new FilteredBuyOrdersGUI(plugin, viewer, page - 1, query).open();
            return;
        }
        if (rawSlot == 53) {
            new FilteredBuyOrdersGUI(plugin, viewer, page + 1, query).open();
            return;
        }

        int index = rawSlot;
        List<BuyOrder> pageOrders = plugin.getOrderManager().searchBuyOrders(query, page, 45);
        if (index < 0 || index >= pageOrders.size()) return;
        BuyOrder order = pageOrders.get(index);

        if (order.getBuyerId().equals(viewer.getUniqueId()) && !viewer.hasPermission("tnauctionhouse.bypass.self")) {
            viewer.sendMessage("You cannot fulfill your own buy order.");
            return;
        }

		new BuyOrderAmountPrompt(plugin, viewer, order, page, "FILTERED", query, null).begin();
    }
}


