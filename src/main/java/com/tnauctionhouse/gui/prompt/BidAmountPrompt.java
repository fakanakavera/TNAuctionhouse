package com.tnauctionhouse.gui.prompt;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.Auction;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BidAmountPrompt implements Listener {

	private static final Map<UUID, BidAmountPrompt> ACTIVE = new ConcurrentHashMap<>();

	private final TNAuctionHousePlugin plugin;
	private final Player viewer;
	private final Auction auction;
	private final int returnPage;

	public BidAmountPrompt(TNAuctionHousePlugin plugin, Player viewer, Auction auction, int returnPage) {
		this.plugin = plugin;
		this.viewer = viewer;
		this.auction = auction;
		this.returnPage = returnPage;
	}

	public void begin() {
		BidAmountPrompt existing = ACTIVE.put(viewer.getUniqueId(), this);
		if (existing != null) existing.end(false);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		viewer.closeInventory();
		int minBid = Math.max(auction.getStartingPrice(), auction.getHighestBid() + 1);
		viewer.sendMessage("Type your bid amount (minimum $" + minBid + "), or type 'cancel'.");
	}

	private void end(boolean reopen) {
		ACTIVE.remove(viewer.getUniqueId());
		HandlerList.unregisterAll(this);
		if (reopen) new com.tnauctionhouse.gui.AuctionsGUI(plugin, viewer, returnPage).open();
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
		BidAmountPrompt active = ACTIVE.get(viewer.getUniqueId());
		if (active != this) return;
		e.setCancelled(true);
		String msg = e.getMessage().trim();
		if (msg.equalsIgnoreCase("cancel")) { viewer.sendMessage("Cancelled."); end(true); return; }
		int bid;
		try { bid = Integer.parseInt(msg); } catch (NumberFormatException ex) { viewer.sendMessage("Invalid amount. Type a positive number or 'cancel'."); return; }
		if (bid <= 0) { viewer.sendMessage("Bid must be greater than zero."); return; }
		int minBid = Math.max(auction.getStartingPrice(), auction.getHighestBid() + 1);
		if (bid < minBid) { viewer.sendMessage("Your bid must be at least $" + minBid + "."); return; }
		int finalBid = bid;
		Bukkit.getScheduler().runTask(plugin, () -> placeBid(finalBid));
	}

	private void placeBid(int bid) {
		Economy econ = plugin.getEconomy();
		if (!econ.has(viewer, bid)) { viewer.sendMessage("You don't have enough funds for that bid."); end(true); return; }
		// Reserve by withdrawing now; refund if outbid later (simple approach)
		econ.withdrawPlayer(viewer, bid);
		// Refund previous highest bidder
		if (auction.getHighestBidderId() != null && auction.getHighestBid() > 0) {
			org.bukkit.OfflinePlayer prev = plugin.getServer().getOfflinePlayer(auction.getHighestBidderId());
			econ.depositPlayer(prev, auction.getHighestBid());
		}
		auction.setHighestBid(viewer.getUniqueId(), bid);
		viewer.sendMessage("You are now the highest bidder at $" + bid + ".");
		end(true);
	}
}


