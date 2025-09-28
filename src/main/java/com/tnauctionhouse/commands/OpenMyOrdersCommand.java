package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.MyOrdersGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenMyOrdersCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public OpenMyOrdersCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.myorders")) {
            player.sendMessage("You don't have permission.");
            return true;
        }
        new MyOrdersGUI(plugin, player, 0).open();
        return true;
    }
}


