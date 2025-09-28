package com.tnauctionhouse.gui;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.ItemTypeCategory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class FilterMenuGUI extends BaseOrdersGUI {

    private final boolean forBuy;

    public FilterMenuGUI(TNAuctionHousePlugin plugin, Player viewer, boolean forBuy) {
        super(plugin, viewer, 0);
        this.forBuy = forBuy;
    }

    @Override
    protected String getTitle() {
        return "Filter by Type";
    }

    @Override
    protected void populateItems() {
        setButton(10, Material.DIAMOND_SWORD, "Weapons");
        setButton(12, Material.IRON_CHESTPLATE, "Armor");
        setButton(14, Material.DIAMOND_PICKAXE, "Tools");
        setButton(16, Material.POTION, "Potions");
        setButton(22, Material.CHEST, "Misc");
    }

    private void setButton(int slot, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    @Override
    protected void handleClick(int rawSlot, ItemStack clickedItem, org.bukkit.event.inventory.ClickType clickType) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        ItemTypeCategory category;
        switch (rawSlot) {
            case 10: category = ItemTypeCategory.WEAPONS; break;
            case 12: category = ItemTypeCategory.ARMOR; break;
            case 14: category = ItemTypeCategory.TOOLS; break;
            case 16: category = ItemTypeCategory.POTIONS; break;
            case 22: category = ItemTypeCategory.MISC; break;
            default: return;
        }
        if (forBuy) {
            new TypeFilteredBuyOrdersGUI(plugin, viewer, 0, category).open();
        } else {
            new TypeFilteredSellOrdersGUI(plugin, viewer, 0, category).open();
        }
    }
}


