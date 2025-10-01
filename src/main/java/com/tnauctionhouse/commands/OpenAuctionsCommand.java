package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.AuctionsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenAuctionsCommand implements CommandExecutor {

	private final TNAuctionHousePlugin plugin;

	public OpenAuctionsCommand(TNAuctionHousePlugin plugin) { this.plugin = plugin; }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) { sender.sendMessage("Only players can use this command."); return true; }
		Player player = (Player) sender;
		if (!player.hasPermission("tnauctionhouse.auctions")) { player.sendMessage("You don't have permission."); return true; }
		new AuctionsGUI(plugin, player, 0).open();
		return true;
	}
}


