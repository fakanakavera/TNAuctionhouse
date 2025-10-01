package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.Auction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class AuctionCreateCommand implements CommandExecutor {

	private final TNAuctionHousePlugin plugin;

	public AuctionCreateCommand(TNAuctionHousePlugin plugin) { this.plugin = plugin; }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players can use this command.");
			return true;
		}
		Player player = (Player) sender;
		if (!player.hasPermission("tnauctionhouse.auction.create")) {
			player.sendMessage("You don't have permission.");
			return true;
		}

		ItemStack hand = player.getInventory().getItemInMainHand();
		if (hand == null || isAirMaterial(hand.getType())) {
			player.sendMessage("Hold an item to auction.");
			return true;
		}

		if (args.length < 1) {
			player.sendMessage("Usage: /" + label + " <amount>");
			return true;
		}

		int amount;
		try { amount = Integer.parseInt(args[0]); } catch (NumberFormatException ex) { player.sendMessage("Invalid amount."); return true; }
		if (amount <= 0) { player.sendMessage("Amount must be > 0."); return true; }
		if (amount > hand.getAmount()) { player.sendMessage("You don't have that many items."); return true; }

		// Create auction at starting price 1, duration 7 days
		int startingPrice = 1;
		long durationMs = 7L * 24L * 60L * 60L * 1000L;
		ItemStack auctionItem = hand.clone();
		auctionItem.setAmount(amount);
		Auction auction = new Auction(java.util.UUID.randomUUID(), player.getUniqueId(), auctionItem, amount, startingPrice, System.currentTimeMillis(), durationMs);
		plugin.getOrderManager().addAuction(auction);

		// Remove items from player hand
		hand.setAmount(hand.getAmount() - amount);
		player.getInventory().setItemInMainHand(hand);
		player.sendMessage("Created auction: " + amount + "x at starting price $" + startingPrice + ".");
		return true;
	}

	private boolean isAirMaterial(Material material) {
		if (material == null) return true;
		return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
	}
}

