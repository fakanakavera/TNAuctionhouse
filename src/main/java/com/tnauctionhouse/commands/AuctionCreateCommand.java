package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.prompt.ChatAmountPrompt;
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

		new ChatAmountPrompt(plugin, player, hand).begin();
		return true;
	}

	private boolean isAirMaterial(Material material) {
		if (material == null) return true;
		return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
	}
}


