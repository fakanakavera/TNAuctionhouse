package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.BuyOrder;
import com.tnauctionhouse.orders.OrderManager;
import com.tnauctionhouse.orders.SellOrder;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyOrdersGUI implements Listener {

    private final TNAuctionHousePlugin plugin;
    private final Player viewer;
    private int page;
    private Inventory inv;
    private List<Object> pageEntries;

    public MyOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
    }

    public void open() {
        this.inv = Bukkit.createInventory(null, 54, Component.text("My Orders - Page " + (page + 1)));
        populate();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inv);
    }

    private void populate() {
        inv.clear();
        OrderManager om = plugin.getOrderManager();
        UUID uid = viewer.getUniqueId();

        List<SellOrder> mySell = new ArrayList<>();
        for (SellOrder so : om.getSellOrders()) if (so.getSellerId().equals(uid)) mySell.add(so);
        List<BuyOrder> myBuy = new ArrayList<>();
        for (BuyOrder bo : om.getBuyOrders()) if (bo.getBuyerId().equals(uid)) myBuy.add(bo);

        List<Object> all = new ArrayList<>(mySell.size() + myBuy.size());
        all.addAll(mySell);
        all.addAll(myBuy);

        int from = page * 45;
        int to = Math.min(from + 45, all.size());
        if (from >= all.size()) {
            pageEntries = new ArrayList<>();
        } else {
            pageEntries = new ArrayList<>(all.subList(from, to));
        }

        int slot = 0;
        for (Object entry : pageEntries) {
            if (entry instanceof SellOrder) {
                SellOrder so = (SellOrder) entry;
                ItemStack it = so.getItem().clone();
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("SELL: " + it.getType().name()));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Amount: " + so.getAmount()));
                    lore.add(Component.text("Unit Price: $" + so.getPricePerUnit()));
                    lore.add(Component.text("Click to cancel"));
                    meta.lore(lore);
                    it.setItemMeta(meta);
                }
                it.setAmount(Math.min(so.getAmount(), it.getMaxStackSize()));
                inv.setItem(slot++, it);
            } else if (entry instanceof BuyOrder) {
                BuyOrder bo = (BuyOrder) entry;
                ItemStack it = bo.getTemplateItem().clone();
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text("BUY: " + it.getType().name()));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Amount: " + bo.getAmount()));
                    lore.add(Component.text("Payout per unit: $" + bo.getPricePerUnit()));
                    lore.add(Component.text("Escrow: $" + bo.getEscrowTotal()));
                    lore.add(Component.text("Click to cancel"));
                    meta.lore(lore);
                    it.setItemMeta(meta);
                }
                it.setAmount(Math.min(bo.getAmount(), it.getMaxStackSize()));
                inv.setItem(slot++, it);
            }
        }

        inv.setItem(45, button(Material.ARROW, "Previous"));
        inv.setItem(49, button(Material.BARRIER, "Close"));
        inv.setItem(53, button(Material.ARROW, "Next"));
    }

    private ItemStack button(Material m, String name) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            i.setItemMeta(meta);
        }
        return i;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity clicker = e.getWhoClicked();
        if (!clicker.getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getView().getTopInventory() != inv) return;
        if (e.getRawSlot() >= inv.getSize()) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot == 45) { if (page > 0) { page--; populate(); } return; }
        if (slot == 53) { page++; populate(); return; }
        if (slot == 49) { viewer.closeInventory(); return; }
        if (slot < 0 || slot >= pageEntries.size()) return;

        Object entry = pageEntries.get(slot);
        OrderManager om = plugin.getOrderManager();
        Economy econ = plugin.getEconomy();

        if (entry instanceof SellOrder) {
            SellOrder so = (SellOrder) entry;
            if (!so.getSellerId().equals(viewer.getUniqueId())) return;
			// Cancel sell order: return item to player, no refund of upfront listing tax
			ItemStack returnItem = so.getItem().clone();
			java.util.Map<Integer, ItemStack> leftover = viewer.getInventory().addItem(returnItem);
			if (!leftover.isEmpty()) {
				for (ItemStack lf : leftover.values()) {
					if (lf == null) continue;
					viewer.getWorld().dropItemNaturally(viewer.getLocation(), lf);
				}
			}
            om.removeSellOrder(so);
            viewer.sendMessage("Cancelled sell order.");
        } else if (entry instanceof BuyOrder) {
            BuyOrder bo = (BuyOrder) entry;
            if (!bo.getBuyerId().equals(viewer.getUniqueId())) return;
            // Refund remaining escrow only; any upfront fee was already paid and is not refunded
            int refund = bo.getEscrowTotal();
            om.removeBuyOrder(bo);
            if (refund > 0) econ.depositPlayer(viewer, refund);
            viewer.sendMessage("Cancelled buy order. Refunded $" + refund + " escrow.");
        }

        populate();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getInventory() != inv) return;
        HandlerList.unregisterAll(this);
    }
}


