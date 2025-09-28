package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
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
import net.kyori.adventure.text.Component;

public abstract class BaseOrdersGUI implements Listener {

    protected final TNAuctionHousePlugin plugin;
    protected final Player viewer;
    protected final int page;
    protected Inventory inv;
    protected int size = 54;

    public BaseOrdersGUI(TNAuctionHousePlugin plugin, Player viewer, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
    }

    protected abstract String getTitle();

    protected abstract void populateItems();
    protected abstract void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType);

    public void open() {
        int invSize = (size <= 0 || size % 9 != 0) ? 54 : size;
        this.inv = Bukkit.createInventory(null, invSize, net.kyori.adventure.text.Component.text(getTitle()));
        populateItems();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        viewer.openInventory(inv);
    }

    protected void setSize(int size) {
        this.size = size;
    }

    protected ItemStack button(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void setLore(ItemStack item, java.util.List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
            java.util.List<Component> compLore = new java.util.ArrayList<>(lore.size());
            for (String line : lore) compLore.add(Component.text(line));
            meta.lore(compLore);
			item.setItemMeta(meta);
		}
	}

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        HumanEntity clicker = e.getWhoClicked();
        if (!clicker.getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getView().getTopInventory() != inv) return;
        if (e.getRawSlot() >= inv.getSize()) return; // ignore clicks in player inventory
        e.setCancelled(true);
        handleClick(e.getRawSlot(), e.getCurrentItem(), e.getClick());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!e.getPlayer().getUniqueId().equals(viewer.getUniqueId())) return;
        if (e.getInventory() != inv) return;
        HandlerList.unregisterAll(this);
    }
}


