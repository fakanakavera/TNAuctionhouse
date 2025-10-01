package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.OrderManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AuctionCreateCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public AuctionCreateCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.auction")) {
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
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid amount.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage("Amount must be > 0.");
            return true;
        }
        if (amount > hand.getAmount()) {
            player.sendMessage("You don't have that many items.");
            return true;
        }

        // Start price fixed at 1; duration 7 days
        long durationMs = 7L * 24L * 60L * 60L * 1000L;

        OrderManager manager = plugin.getOrderManager();
        manager.createAuctionOrder(player.getUniqueId(), hand, amount, durationMs);

        // Remove items from player
        hand.setAmount(hand.getAmount() - amount);
        player.getInventory().setItemInMainHand(hand);

        player.sendMessage("Created auction: " + amount + "x starting at $1, ending in 7 days.");
        return true;
    }

    private boolean isAirMaterial(Material material) {
        if (material == null) {
            return true;
        }
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
}

