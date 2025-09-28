package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.SellOrdersGUI;
import com.tnauctionhouse.gui.FilteredSellOrdersGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenSellOrdersCommand implements CommandExecutor {
    private final TNAuctionHousePlugin plugin;

    public OpenSellOrdersCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.sellorders")) {
            player.sendMessage("You don't have permission.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            String query = joinArgs(args, 1);
            new FilteredSellOrdersGUI(plugin, player, 0, query).open();
            return true;
        }
        new SellOrdersGUI(plugin, player, 0).open();
        return true;
    }

    private String joinArgs(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}


