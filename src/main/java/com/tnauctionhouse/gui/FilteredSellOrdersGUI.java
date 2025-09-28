package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.SellOrder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class FilteredSellOrdersGUI extends BaseOrdersGUI {

    private final String query;

    public FilteredSellOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page, String query) {
        super(plugin, viewer, page);
        this.query = query == null ? "" : query;
    }

    @Override
    protected String getTitle() {
        return "Sell Orders: '" + (query.length() > 16 ? query.substring(0, 16) + "…" : query) + "' - Page " + (page + 1);
    }

    @Override
    protected void populateItems() {
        List<SellOrder> pageOrders = plugin.getOrderManager().searchSellOrders(query, page, 45);
        int slot = 0;
		for (SellOrder order : pageOrders) {
            ItemStack display = order.getItem();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
				meta.displayName(Component.text(display.getType().name()));
				display.setItemMeta(meta);
				boolean taxEnabled = plugin.getConfig().getBoolean("tax.enabled", true);
				double taxRate = Math.max(0.0, plugin.getConfig().getDouble("tax.rate", 0.10));
				String mode = String.valueOf(plugin.getConfig().getString("tax.mode", "UPFRONT")).toUpperCase(java.util.Locale.ROOT);
				int unit = order.getPricePerUnit();
				int unitToCharge = unit;
				if (taxEnabled && "ADD_TO_PRICE".equals(mode)) unitToCharge = (int) Math.round(unit * (1.0 + taxRate));
				int total = unitToCharge * order.getAmount();
				setLore(display, java.util.Arrays.asList(
						"Amount: " + order.getAmount(),
						"Unit Price: $" + unitToCharge,
						"Total: $" + total,
						"Click to buy: $" + total
				));

            }
            inv.setItem(slot++, display);
        }

        inv.setItem(45, button(Material.ARROW, "Previous"));
        inv.setItem(53, button(Material.ARROW, "Next"));
    }

    @Override
    protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (rawSlot == 45) {
            if (page > 0) new FilteredSellOrdersGUI(plugin, viewer, page - 1, query).open();
            return;
        }
        if (rawSlot == 53) {
            new FilteredSellOrdersGUI(plugin, viewer, page + 1, query).open();
            return;
        }

        int index = rawSlot;
        List<SellOrder> pageOrders = plugin.getOrderManager().searchSellOrders(query, page, 45);
        if (index < 0 || index >= pageOrders.size()) return;
        SellOrder order = pageOrders.get(index);

        if (order.getSellerId().equals(viewer.getUniqueId()) && !viewer.hasPermission("tnauctionhouse.bypass.self")) {
            viewer.sendMessage("You cannot buy your own order.");
            return;
        }

		new ConfirmPurchaseGUI(plugin, viewer, order, page, "FILTERED", query, null).open();
    }
}


