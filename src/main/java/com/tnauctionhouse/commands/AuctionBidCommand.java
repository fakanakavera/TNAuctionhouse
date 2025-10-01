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

        // We will perform the withdraw and the state update while holding a lock on this specific auction
        // to prevent races between concurrent bidders. Using the auction instance avoids blocking other auctions.
        synchronized (auction) {
            // Re-check auction state under lock
            if (System.currentTimeMillis() >= auction.getEndAt()) {
                player.sendMessage("This auction has ended.");
                return true;
            }

            if (auction.getSellerId().equals(player.getUniqueId()) && !player.hasPermission("tnauctionhouse.bypass.self")) {
                player.sendMessage("You cannot bid on your own auction.");
                return true;
            }

            int currentBid = Math.max(auction.getStartPrice(), auction.getHighestBid());
            if (amount <= currentBid) {
                player.sendMessage("Your bid must be greater than the current highest bid ($" + currentBid + ").");
                return true;
            }

            // Attempt to withdraw now; do not rely solely on has(), check the response for success
            // If this fails, we do nothing to auction state
            net.milkbowl.vault.economy.EconomyResponse withdraw = null;
            try {
                // Optional pre-check for nicer message
                if (!econ.has(player, amount)) {
                    player.sendMessage("You don't have enough money to bid $" + amount + ".");
                    return true;
                }
                withdraw = econ.withdrawPlayer(player, amount);
            } catch (Throwable t) {
                withdraw = null;
            }
            if (withdraw == null || withdraw.type != net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
                player.sendMessage("Your bid could not be processed by the economy. Please try again.");
                return true;
            }

            // IMPORTANT race re-check: since the withdraw involved external I/O,
            // another thread might have updated the highest bid meanwhile (on a different auction
            // we would not impact, but for the same auction we still hold the lock; however a plugin
            // could have updated state between our pre-check and now). Revalidate and undo withdraw if outbid.
            int latestNow = Math.max(auction.getStartPrice(), auction.getHighestBid());
            if (amount <= latestNow) {
                // We were outbid in the tiny window; undo our withdrawal and exit
                try {
                    net.milkbowl.vault.economy.EconomyResponse undo = econ.depositPlayer(player, amount);
                    if (undo == null || undo.type != net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
                        plugin.getLogger().severe("Economy undo failed after race for auction " + auction.getOrderId());
                    }
                } catch (Throwable ignored) {}
                player.sendMessage("You were outbid just now; your bid was not placed.");
                return true;
            }

            // Capture previous winning bid while still under the lock
            UUID previousBidder = auction.getHighestBidderId();
            int previousAmount = auction.getHighestBid();

            // Update auction winner
            auction.setHighestBidderId(player.getUniqueId());
            auction.setHighestBid(amount);

            // If there was a previous bidder, try refunding them now while we still hold the lock.
            // If the refund fails, we must roll back our update to avoid money loss for the previous bidder.
            if (previousBidder != null && previousAmount > 0) {
                OfflinePlayer prev = Bukkit.getOfflinePlayer(previousBidder);
                net.milkbowl.vault.economy.EconomyResponse refund = null;
                try {
                    refund = econ.depositPlayer(prev, previousAmount);
                } catch (Throwable t) {
                    refund = null;
                }
                if (refund == null || refund.type != net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
                    // Refund failed: revert winner and refund the new bidder instead
                    auction.setHighestBidderId(previousBidder);
                    auction.setHighestBid(previousAmount);
                    // Refund new bidder
                    try {
                        net.milkbowl.vault.economy.EconomyResponse undo = econ.depositPlayer(player, amount);
                        if (undo == null || undo.type != net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS) {
                            // Catastrophic: both refund and undo failed. Log and inform player.
                            plugin.getLogger().severe("Economy rollback failed for auction " + auction.getOrderId());
                        }
                    } catch (Throwable ignored) {}
                    player.sendMessage("Your bid could not be placed due to an economy error; your money was returned.");
                    return true;
                }
                // Notify previous bidder if online
                if (prev.isOnline()) {
                    prev.getPlayer().sendMessage("You were outbid. $" + previousAmount + " has been returned to you.");
                }
            }
        }

        // Outside the lock: notify the bidder of success
        player.sendMessage(String.format(Locale.ROOT, "You are now the highest bidder at $%d.", amount));
        return true;
    }
}

