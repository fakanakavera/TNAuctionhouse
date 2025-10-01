package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.Auction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public class AuctionsGUI extends BaseOrdersGUI {

	public AuctionsGUI(TNAuctionHousePlugin plugin, Player viewer, int page) {
		super(plugin, viewer, page);
	}

	@Override
	protected String getTitle() { return "Auctions - Page " + (page + 1); }

	@Override
	protected void populateItems() {
		List<Auction> pageAuctions = plugin.getOrderManager().getAuctionsPage(page, 45);
		int slot = 0;
		long now = System.currentTimeMillis();
		for (Auction auction : pageAuctions) {
			ItemStack display = auction.getItem().clone();
			ItemMeta meta = display.getItemMeta();
			if (meta != null) {
				meta.displayName(Component.text(display.getType().name()));
				display.setItemMeta(meta);
				long msLeft = Math.max(0L, auction.getEndAt() - now);
				String timeLeft = formatDuration(msLeft);
				setLore(display, java.util.Arrays.asList(
					"Amount: " + auction.getAmount(),
					"Starting Price: $" + auction.getStartingPrice(),
					"Time left: " + timeLeft
				));
			}
			display.setAmount(Math.min(auction.getAmount(), display.getMaxStackSize()));
			inv.setItem(slot++, display);
		}

		inv.setItem(45, button(Material.ARROW, "Previous"));
		inv.setItem(53, button(Material.ARROW, "Next"));
	}

	@Override
	protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
		if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
		if (rawSlot == 45) { if (page > 0) new AuctionsGUI(plugin, viewer, page - 1).open(); return; }
		if (rawSlot == 53) { new AuctionsGUI(plugin, viewer, page + 1).open(); return; }
		// No bidding implementation provided; read-only viewer for now
	}

	private String formatDuration(long ms) {
		long seconds = ms / 1000L;
		long days = seconds / 86400L; seconds %= 86400L;
		long hours = seconds / 3600L; seconds %= 3600L;
		long minutes = seconds / 60L; seconds %= 60L;
		StringBuilder sb = new StringBuilder();
		if (days > 0) sb.append(days).append("d ");
		if (hours > 0 || days > 0) sb.append(hours).append("h ");
		if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
		sb.append(seconds).append("s");
		return sb.toString();
	}
}


