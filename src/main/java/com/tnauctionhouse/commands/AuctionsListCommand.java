package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.AuctionOrder;
import com.tnauctionhouse.orders.OrderManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class AuctionsListCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public AuctionsListCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.auctions")) {
            player.sendMessage("You don't have permission.");
            return true;
        }

        int page = 0;
        if (args.length >= 1) {
            try { page = Math.max(0, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {}
        }

        OrderManager manager = plugin.getOrderManager();
        List<AuctionOrder> auctions = manager.getAuctionOrdersPage(page, 10);
        if (auctions.isEmpty()) {
            player.sendMessage("No auctions found on this page.");
            return true;
        }

        player.sendMessage(String.format(Locale.ROOT, "Auctions (page %d):", page));
        long now = System.currentTimeMillis();
        for (AuctionOrder ao : auctions) {
            long remainingMs = Math.max(0L, ao.getEndAt() - now);
            Duration d = Duration.ofMillis(remainingMs);
            long days = d.toDays();
            long hours = d.minusDays(days).toHours();
            int current = Math.max(ao.getStartPrice(), ao.getHighestBid());
            player.sendMessage(String.format(Locale.ROOT,
                "- %s: %dx %s | current $%d | ends in %dd %dh",
                ao.getOrderId(),
                ao.getAmount(), ao.getItem().getType().name(), current, days, hours));
        }
        return true;
    }
}

