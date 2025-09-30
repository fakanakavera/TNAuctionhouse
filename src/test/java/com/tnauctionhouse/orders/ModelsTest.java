package com.tnauctionhouse.orders;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ModelsTest {

    @BeforeAll
    static void bootMockBukkit() {
        MockBukkit.mock();
    }

    @AfterAll
    static void shutdownMockBukkit() {
        MockBukkit.unmock();
    }

    @Test
    void testSellOrderGettersClone() {
        UUID seller = UUID.randomUUID();
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 5);
        SellOrder so = new SellOrder(UUID.randomUUID(), seller, item, 7, 5, 123L);
        assertEquals(seller, so.getSellerId());
        assertEquals(7, so.getPricePerUnit());
        assertEquals(5, so.getAmount());
        assertEquals(123L, so.getCreatedAt());
        org.bukkit.inventory.ItemStack a = so.getItem();
        org.bukkit.inventory.ItemStack b = so.getItem();
        assertNotSame(a, b);
        a.setAmount(1);
        assertEquals(5, so.getItem().getAmount());
    }

    @Test
    void testBuyOrderGettersClone() {
        UUID buyer = UUID.randomUUID();
        org.bukkit.inventory.ItemStack tpl = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_INGOT, 2);
        BuyOrder bo = new BuyOrder(UUID.randomUUID(), buyer, tpl, 3, 8, 456L, 24);
        assertEquals(buyer, bo.getBuyerId());
        assertEquals(3, bo.getPricePerUnit());
        assertEquals(8, bo.getAmount());
        assertEquals(456L, bo.getCreatedAt());
        assertEquals(24, bo.getEscrowTotal());
        org.bukkit.inventory.ItemStack a = bo.getTemplateItem();
        org.bukkit.inventory.ItemStack b = bo.getTemplateItem();
        assertNotSame(a, b);
        a.setAmount(64);
        assertEquals(2, bo.getTemplateItem().getAmount());
    }

    @Test
    void testItemTypeCategoryValues() {
        assertNotNull(ItemTypeCategory.valueOf("WEAPONS"));
        assertEquals(5, ItemTypeCategory.values().length);
    }
}


