package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.OrderManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BuyOrderCreateCommand implements CommandExecutor {

    private final TNAuctionHousePlugin plugin;

    public BuyOrderCreateCommand(TNAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("tnauctionhouse.buyorder")) {
            player.sendMessage("You don't have permission.");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage("Hold an item to create a buy order for.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /" + label + " <price> <amount>");
            return true;
        }

		int pricePerUnit;
		int amount;
        try {
			pricePerUnit = Integer.parseInt(args[0]);
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid price or amount.");
            return true;
        }
        if (pricePerUnit <= 0 || amount <= 0) {
            player.sendMessage("Price and amount must be > 0.");
            return true;
        }

        Economy econ = plugin.getEconomy();
		int totalCost = pricePerUnit * amount;
		// Apply buy order tax if enabled
		boolean buyTaxEnabled = plugin.getConfig().getBoolean("buy_tax.enabled", true);
		double buyTaxRate = Math.max(0.0, plugin.getConfig().getDouble("buy_tax.rate", 0.10));
		String buyTaxMode = String.valueOf(plugin.getConfig().getString("buy_tax.mode", "UPFRONT")).toUpperCase(java.util.Locale.ROOT);
		int fee = 0;
		if (buyTaxEnabled) {
			if ("UPFRONT".equals(buyTaxMode)) {
				fee = (int) Math.round(totalCost * buyTaxRate);
			} else if ("ADD_TO_PRICE".equals(buyTaxMode)) {
				// For buy orders, ADD_TO_PRICE effectively behaves as an extra fee at creation
				fee = (int) Math.round(totalCost * buyTaxRate);
			}
		}
		int finalDebit = totalCost + fee;
		if (!econ.has(player, finalDebit)) {
			player.sendMessage("You don't have enough money ($" + finalDebit + ").");
            return true;
        }
		econ.withdrawPlayer(player, finalDebit);

        OrderManager manager = plugin.getOrderManager();
		manager.createBuyOrder(player.getUniqueId(), hand, pricePerUnit, amount, totalCost);

		if (fee > 0) {
			player.sendMessage("Created buy order: " + amount + "x for $" + pricePerUnit + " each. Cost: $" + totalCost + ", Tax: $" + fee + ", Total: $" + finalDebit + ".");
		} else {
			player.sendMessage("Created buy order: " + amount + "x for $" + pricePerUnit + " each. ($" + totalCost + ")");
		}
        return true;
    }
}


