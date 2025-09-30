package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.OrderManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class SellOrderCreateCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public SellOrderCreateCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.sellorder")) {
            player.sendMessage("You don't have permission.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || isAirMaterial(hand.getType())) {
            player.sendMessage("Hold an item to sell.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("Usage: /" + label + " <price> [amount]");
            return true;
        }

		int pricePerUnit;
        try {
			pricePerUnit = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid price.");
            return true;
        }
		if (pricePerUnit <= 0) {
            player.sendMessage("Price must be > 0.");
            return true;
        }

        int amount = hand.getAmount();
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage("Invalid amount.");
                return true;
            }
        }

        if (amount <= 0) {
            player.sendMessage("Amount must be > 0.");
            return true;
        }
        if (amount > hand.getAmount()) {
            player.sendMessage("You don't have that many items.");
            return true;
        }

        // Tax handling per config
		boolean taxEnabled = plugin.getConfig().getBoolean("tax.enabled", true);
		double taxRate = Math.max(0.0, plugin.getConfig().getDouble("tax.rate", 0.10));
        String mode = String.valueOf(plugin.getConfig().getString("tax.mode", "UPFRONT")).toUpperCase(java.util.Locale.ROOT);

		int pricePerUnitEffective = pricePerUnit;
        if (taxEnabled) {
            switch (mode) {
                case "UPFRONT": {
					double upfrontFee = pricePerUnit * amount * taxRate;
                    net.milkbowl.vault.economy.Economy econ = plugin.getEconomy();
					if (!econ.has(player, upfrontFee)) {
                        player.sendMessage("You don't have enough money to pay the listing fee ($" + upfrontFee + ").");
                        return true;
                    }
					econ.withdrawPlayer(player, upfrontFee);
                    break;
                }
                case "ADD_TO_PRICE": {
					pricePerUnitEffective = (int) Math.round(pricePerUnit * (1.0 + taxRate));
                    break;
                }
                default: {
                    // Unknown mode -> treat as disabled
                    break;
                }
            }
        }

        OrderManager manager = plugin.getOrderManager();
        manager.createSellOrder(player.getUniqueId(), hand, pricePerUnitEffective, amount);

        // Remove items from player
        hand.setAmount(hand.getAmount() - amount);
        player.getInventory().setItemInMainHand(hand);

		player.sendMessage("Created sell order: " + amount + "x for $" + pricePerUnitEffective + " each.");
        return true;
    }

    private boolean isAirMaterial(Material material) {
        if (material == null) {
            return true;
        }
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
}


