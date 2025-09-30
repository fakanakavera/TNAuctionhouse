package com.tnauctionhouse.orders;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OrderManagerTest {

    @BeforeAll
    static void bootMockBukkit() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdownMockBukkit() {
        MockBukkit.unmock();
    }

    @Test
    void testCreateSellOrderClonesAndStores() {
        OrderManager om = new OrderManager();
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 32);
        int price = 25;
        int amount = 10;

        SellOrder so = om.createSellOrder(UUID.randomUUID(), item, price, amount);

        assertEquals(1, om.getSellOrders().size());
        SellOrder stored = om.getSellOrders().get(0);
        assertEquals(so.getOrderId(), stored.getOrderId());
        assertEquals(price, stored.getPricePerUnit());
        assertEquals(amount, stored.getAmount());
        // ensure clone: modifying returned item does not affect stored
        org.bukkit.inventory.ItemStack got = stored.getItem();
        int originalAmount = got.getAmount();
        got.setAmount(1);
        assertEquals(originalAmount, stored.getItem().getAmount());
        // source item unchanged
        assertEquals(32, item.getAmount());
    }

    @Test
    void testCreateBuyOrderTemplateClonedAndStores() {
        OrderManager om = new OrderManager();
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 16);
        int price = 5;
        int amount = 12;
        int escrow = price * amount;

        BuyOrder bo = om.createBuyOrder(UUID.randomUUID(), item, price, amount, escrow);

        assertEquals(1, om.getBuyOrders().size());
        BuyOrder stored = om.getBuyOrders().get(0);
        assertEquals(bo.getOrderId(), stored.getOrderId());
        assertEquals(price, stored.getPricePerUnit());
        assertEquals(amount, stored.getAmount());
        assertEquals(1, stored.getTemplateItem().getAmount());
        // source item unchanged
        assertEquals(16, item.getAmount());
    }

    @Test
    void testPagination() {
        OrderManager om = new OrderManager();
        for (int i = 0; i < 100; i++) {
            om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE, 64), 1, 1);
        }
        List<SellOrder> page0 = om.getSellOrdersPage(0, 45);
        List<SellOrder> page1 = om.getSellOrdersPage(1, 45);
        List<SellOrder> page2 = om.getSellOrdersPage(2, 45);
        assertEquals(45, page0.size());
        assertEquals(45, page1.size());
        assertEquals(10, page2.size());
        // negative page coerced to 0
        assertEquals(page0.size(), om.getSellOrdersPage(-5, 45).size());
        // out of range page returns empty
        assertTrue(om.getSellOrdersPage(10, 45).isEmpty());
    }

    @Test
    void testSearchByTypeDisplayNameAndLore() {
        OrderManager om = new OrderManager();
        // type name contains "diamond_sword"
        org.bukkit.inventory.ItemStack sword = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1);
        // display name contains "Epic Pick"
        org.bukkit.inventory.ItemStack pick = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE, 1);
        org.bukkit.inventory.meta.ItemMeta pickMeta = pick.getItemMeta();
        pickMeta.displayName(net.kyori.adventure.text.Component.text("Epic Pick"));
        pick.setItemMeta(pickMeta);
        // lore contains "legendary"
        org.bukkit.inventory.ItemStack shovel = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SHOVEL, 1);
        org.bukkit.inventory.meta.ItemMeta shovelMeta = shovel.getItemMeta();
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("A legendary tool"));
        shovelMeta.lore(lore);
        shovel.setItemMeta(shovelMeta);

        om.createSellOrder(UUID.randomUUID(), sword, 10, 1);
        om.createSellOrder(UUID.randomUUID(), pick, 20, 1);
        om.createSellOrder(UUID.randomUUID(), shovel, 30, 1);

        assertEquals(1, om.searchSellOrders("sword", 0, 45).size());
        assertEquals(1, om.searchSellOrders("epic", 0, 45).size());
        assertEquals(1, om.searchSellOrders("legendary", 0, 45).size());
        assertEquals(0, om.searchSellOrders("nonexistent", 0, 45).size());
        // null query behaves as empty (match none here due to pagination on empty query)
        assertNotNull(om.searchSellOrders(null, 0, 45));
    }

    @Test
    void testCategoryFilters() {
        OrderManager om = new OrderManager();
        om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1), 1, 1);
        om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE, 1), 1, 1);
        om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND_PICKAXE, 1), 1, 1);
        om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.POTION, 1), 1, 1);
        om.createSellOrder(UUID.randomUUID(), new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIRT, 1), 1, 1);

        assertEquals(1, om.filterSellOrdersByCategory(ItemTypeCategory.WEAPONS, 0, 45).size());
        assertEquals(1, om.filterSellOrdersByCategory(ItemTypeCategory.ARMOR, 0, 45).size());
        assertEquals(1, om.filterSellOrdersByCategory(ItemTypeCategory.TOOLS, 0, 45).size());
        assertEquals(1, om.filterSellOrdersByCategory(ItemTypeCategory.POTIONS, 0, 45).size());
        assertEquals(1, om.filterSellOrdersByCategory(ItemTypeCategory.MISC, 0, 45).size());
    }

    @Test
    void testDeliveriesEnqueueDrainGetPageAndRemove() {
        OrderManager om = new OrderManager();
        UUID uid = UUID.randomUUID();
        for (int i = 0; i < 100; i++) {
            org.bukkit.inventory.ItemStack it = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COBBLESTONE, i + 1);
            om.enqueueDelivery(uid, it);
        }
        // getDeliveries clone
        List<org.bukkit.inventory.ItemStack> snapshot = om.getDeliveries(uid);
        assertEquals(100, snapshot.size());
        // removeDeliveryAt bounds
        assertNull(om.removeDeliveryAt(uid, -1));
        assertNull(om.removeDeliveryAt(uid, 100));
        // pagination
        assertEquals(45, om.getDeliveriesPage(uid, 0, 45).size());
        assertEquals(45, om.getDeliveriesPage(uid, 1, 45).size());
        assertEquals(10, om.getDeliveriesPage(uid, 2, 45).size());
        assertTrue(om.getDeliveriesPage(uid, 3, 45).isEmpty());
        // remove actual item
        org.bukkit.inventory.ItemStack removed = om.removeDeliveryAt(uid, 0);
        assertNotNull(removed);
        assertEquals(org.bukkit.Material.COBBLESTONE, removed.getType());
        assertEquals(100 - 1, om.getDeliveries(uid).size());
        // drain
        List<org.bukkit.inventory.ItemStack> drained = om.drainDeliveries(uid);
        assertEquals(99, drained.size());
        assertTrue(om.getDeliveries(uid).isEmpty());
    }

    @Test
    void testSaveLoadRoundtrip(@TempDir Path tempDir) throws Exception {
        OrderManager om = new OrderManager();
        UUID seller = UUID.randomUUID();
        UUID buyer = UUID.randomUUID();
        om.createSellOrder(seller, new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, 12), 3, 12);
        om.createBuyOrder(buyer, new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 7), 9, 7, 63);
        om.enqueueDelivery(buyer, new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 3));

        File file = tempDir.resolve("orders.yml").toFile();
        om.save(file);

        OrderManager loaded = new OrderManager();
        loaded.load(file);

        assertEquals(1, loaded.getSellOrders().size());
        assertEquals(1, loaded.getBuyOrders().size());
        assertEquals(1, loaded.getDeliveries(buyer).size());

        SellOrder so = loaded.getSellOrders().get(0);
        assertEquals(3, so.getPricePerUnit());
        assertEquals(12, so.getAmount());
        assertEquals(org.bukkit.Material.EMERALD, so.getItem().getType());

        BuyOrder bo = loaded.getBuyOrders().get(0);
        assertEquals(9, bo.getPricePerUnit());
        assertEquals(7, bo.getAmount());
        assertEquals(63, bo.getEscrowTotal());
        assertEquals(org.bukkit.Material.GOLD_INGOT, bo.getTemplateItem().getType());
    }
}


