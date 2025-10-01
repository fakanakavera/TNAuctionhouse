package com.tnauctionhouse.gui.prompt;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.Auction;
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

public class ChatAmountPrompt implements Listener {

	private static final Map<UUID, ChatAmountPrompt> ACTIVE = new ConcurrentHashMap<>();

	private final TNAuctionHousePlugin plugin;
	private final Player player;
	private final ItemStack itemInHand;

	public ChatAmountPrompt(TNAuctionHousePlugin plugin, Player player, ItemStack itemInHand) {
		this.plugin = plugin;
		this.player = player;
		this.itemInHand = itemInHand.clone();
	}

	public void begin() {
		ChatAmountPrompt existing = ACTIVE.put(player.getUniqueId(), this);
		if (existing != null) existing.end();
		Bukkit.getPluginManager().registerEvents(this, plugin);
		player.closeInventory();
		player.sendMessage("Type the amount you want to auction (max " + itemInHand.getAmount() + "), or 'cancel'.");
	}

	private void end() {
		ACTIVE.remove(player.getUniqueId());
		HandlerList.unregisterAll(this);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		if (!e.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
		ChatAmountPrompt active = ACTIVE.get(player.getUniqueId());
		if (active != this) return;
		e.setCancelled(true);
		String msg = e.getMessage().trim();
		if (msg.equalsIgnoreCase("cancel")) { player.sendMessage("Cancelled."); end(); return; }
		int requested;
		try { requested = Integer.parseInt(msg); } catch (NumberFormatException ex) { player.sendMessage("Invalid amount. Type a positive number or 'cancel'."); return; }
		if (requested <= 0) { player.sendMessage("Amount must be greater than zero."); return; }
		if (requested > itemInHand.getAmount()) { player.sendMessage("You don't have that many items."); return; }
		int finalRequested = requested;
		Bukkit.getScheduler().runTask(plugin, () -> fulfill(finalRequested));
	}

	private void fulfill(int amount) {
		// Remove items from player hand
		org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand == null || hand.getType() != itemInHand.getType() || hand.getAmount() < amount) { player.sendMessage("Item changed; aborted."); end(); return; }
		hand.setAmount(hand.getAmount() - amount);
		player.getInventory().setItemInMainHand(hand);

		// Create auction at starting price 1, duration 7 days
		int startingPrice = 1;
		long durationMs = 7L * 24L * 60L * 60L * 1000L;
		org.bukkit.inventory.ItemStack auctionItem = itemInHand.clone();
		auctionItem.setAmount(amount);
		Auction auction = new Auction(UUID.randomUUID(), player.getUniqueId(), auctionItem, amount, startingPrice, System.currentTimeMillis(), durationMs);
		plugin.getOrderManager().addAuction(auction);
		player.sendMessage("Created auction: " + amount + "x at starting price $" + startingPrice + ".");
		end();
	}
}


