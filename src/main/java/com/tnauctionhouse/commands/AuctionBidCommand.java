package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.AuctionOrder;
import com.tnauctionhouse.orders.OrderManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public class AuctionBidCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public AuctionBidCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.bid")) {
            player.sendMessage("You don't have permission.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /" + label + " <auctionId> <amount>");
            return true;
        }

        UUID auctionId;
        try {
            auctionId = UUID.fromString(args[0]);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Invalid auctionId. Expecting UUID.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("Bid must be > 0.");
            return true;
        }

        OrderManager manager = plugin.getOrderManager();
        AuctionOrder auction = manager.getAuctionById(auctionId);
        if (auction == null) {
            player.sendMessage("Auction not found.");
            return true;
        }

        long now = System.currentTimeMillis();
        if (now >= auction.getEndAt()) {
            player.sendMessage("This auction has ended.");
            return true;
        }

        if (auction.getSellerId().equals(player.getUniqueId()) && !player.hasPermission("tnauctionhouse.bypass.self")) {
            player.sendMessage("You cannot bid on your own auction.");
            return true;
        }

        int current = Math.max(auction.getStartPrice(), auction.getHighestBid());
        if (amount <= current) {
            player.sendMessage("Your bid must be greater than the current highest bid ($" + current + ").");
            return true;
        }

        Economy econ = plugin.getEconomy();
        if (!econ.has(player, amount)) {
            player.sendMessage("You don't have enough money to bid $" + amount + ".");
            return true;
        }

        // Withdraw first to lock funds
        econ.withdrawPlayer(player, amount);

        UUID previousBidder = auction.getHighestBidderId();
        int previousAmount = auction.getHighestBid();

        // Update highest bid
        synchronized (manager) {
            // Re-check after locking
            int latest = Math.max(auction.getStartPrice(), auction.getHighestBid());
            if (amount <= latest) {
                // someone outbid in the meantime; refund immediately
                econ.depositPlayer(player, amount);
                player.sendMessage("You were outbid just now; your bid was not placed.");
                return true;
            }
            auction.setHighestBidderId(player.getUniqueId());
            auction.setHighestBid(amount);
        }

        // Refund previous bidder if exists
        if (previousBidder != null && previousAmount > 0) {
            try {
                OfflinePlayer prev = Bukkit.getOfflinePlayer(previousBidder);
                econ.depositPlayer(prev, previousAmount);
                if (prev.isOnline()) {
                    prev.getPlayer().sendMessage("You were outbid. $" + previousAmount + " has been returned to you.");
                }
            } catch (Throwable ignored) {}
        }

        player.sendMessage(String.format(Locale.ROOT, "You are now the highest bidder at $%d.", amount));
        return true;
    }
}

