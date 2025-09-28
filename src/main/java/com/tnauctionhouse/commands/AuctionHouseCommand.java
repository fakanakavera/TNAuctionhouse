package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.gui.FilteredBuyOrdersGUI;
import com.tnauctionhouse.gui.FilteredSellOrdersGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuctionHouseCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public AuctionHouseCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("Usage: /" + label + " search <query> [sell|buy]");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("search")) {
            if (args.length < 2) {
                player.sendMessage("Usage: /" + label + " search <query> [sell|buy]");
                return true;
            }
            String query = joinArgs(args, 1, args.length);
            // default to sell orders view unless explicitly buy
            boolean isBuy = query.endsWith(" buy") || (args.length >= 3 && args[args.length - 1].equalsIgnoreCase("buy"));
            boolean isSell = query.endsWith(" sell") || (args.length >= 3 && args[args.length - 1].equalsIgnoreCase("sell"));
            if (isBuy || isSell) {
                // trim the trailing tag from query if included inline
                int idx = query.lastIndexOf(' ');
                if (idx > 0) query = query.substring(0, idx);
            }
            if (isBuy) {
                new FilteredBuyOrdersGUI(plugin, player, 0, query).open();
            } else {
                new FilteredSellOrdersGUI(plugin, player, 0, query).open();
            }
            return true;
        }

        player.sendMessage("Unknown subcommand. Try: /" + label + " search <query> [sell|buy]");
        return true;
    }

    private static String joinArgs(String[] args, int from, int toExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < toExclusive; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}


