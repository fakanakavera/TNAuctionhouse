package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.BuyOrdersGUI;
import com.tnauctionhouse.gui.FilteredBuyOrdersGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenBuyOrdersCommand implements CommandExecutor {
    private final TNAuctionHousePlugin plugin;

    public OpenBuyOrdersCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.buyorders")) {
            player.sendMessage("You don't have permission.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            String query = joinArgs(args, 1);
            new FilteredBuyOrdersGUI(plugin, player, 0, query).open();
            return true;
        }
        new BuyOrdersGUI(plugin, player, 0).open();
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


