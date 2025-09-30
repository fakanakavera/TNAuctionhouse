package com.tnauctionhouse.orders;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class OrderManagerTest {

	@BeforeAll
	static void beforeAll() {
		MockBukkit.mock();
	}

	@AfterAll
	static void afterAll() {
		MockBukkit.unmock();
	}

	private static ItemStack mockItem(int initialAmount, String typeName) {
		ItemStack original = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
		ItemStack clone1 = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
		AtomicInteger amount = new AtomicInteger(initialAmount);
		when(original.clone()).thenReturn(clone1);
		when(clone1.clone()).thenAnswer(inv -> Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS));
		when(clone1.getAmount()).thenAnswer(inv -> amount.get());
		Mockito.doAnswer(inv -> { amount.set((Integer) inv.getArgument(0)); return null; }).when(clone1).setAmount(Mockito.anyInt());
		when(clone1.getMaxStackSize()).thenReturn(64);
		when(clone1.getType().name()).thenReturn(typeName);
		when(original.getType().name()).thenReturn(typeName);
		return original;
	}

	@Test
	void create_and_paginate_orders() {
		OrderManager m = new OrderManager();
		UUID u1 = UUID.randomUUID();
		UUID u2 = UUID.randomUUID();
		ItemStack diamond = mockItem(32, "DIAMOND");
		ItemStack sword = mockItem(1, "DIAMOND_SWORD");

		m.createSellOrder(u1, sword, 100, 1);
		m.createBuyOrder(u2, diamond, 2, 10, 20);

		List<SellOrder> s0 = m.getSellOrdersPage(0, 45);
		List<BuyOrder> b0 = m.getBuyOrdersPage(0, 45);
		assertEquals(1, s0.size());
		assertEquals(1, b0.size());

		assertTrue(m.getSellOrdersPage(1, 45).isEmpty());
		assertTrue(m.getBuyOrdersPage(1, 45).isEmpty());

		assertTrue(m.getSellOrdersPage(-1, 45).size() > 0);
	}

	@Test
	void search_and_category_filters() {
		OrderManager m = new OrderManager();
		UUID uid = UUID.randomUUID();
		m.createSellOrder(uid, mockItem(1, "DIAMOND_SWORD"), 10, 1);
		m.createSellOrder(uid, mockItem(1, "IRON_CHESTPLATE"), 20, 1);
		m.createSellOrder(uid, mockItem(1, "DIAMOND_PICKAXE"), 15, 1);
		m.createSellOrder(uid, mockItem(1, "POTION"), 5, 1);
		m.createSellOrder(uid, mockItem(64, "DIRT"), 1, 64);

		assertEquals(1, m.searchSellOrders("sword", 0, 45).size());
		assertEquals(1, m.searchSellOrders("CHESTPLATE", 0, 45).size());
		assertEquals(1, m.searchSellOrders("pickaxe", 0, 45).size());
		assertEquals(1, m.searchSellOrders("potion", 0, 45).size());
		assertEquals(1, m.searchSellOrders("dirt", 0, 45).size());
		assertEquals(0, m.searchSellOrders("nonexistent", 0, 45).size());

		assertEquals(1, m.filterSellOrdersByCategory(ItemTypeCategory.WEAPONS, 0, 45).size());
		assertEquals(1, m.filterSellOrdersByCategory(ItemTypeCategory.ARMOR, 0, 45).size());
		assertEquals(1, m.filterSellOrdersByCategory(ItemTypeCategory.TOOLS, 0, 45).size());
		assertEquals(1, m.filterSellOrdersByCategory(ItemTypeCategory.POTIONS, 0, 45).size());
		assertEquals(1, m.filterSellOrdersByCategory(ItemTypeCategory.MISC, 0, 45).size());
	}

	@Test
	void deliveries_enqueue_drain_get_and_pagination_and_removeAt() {
		OrderManager m = new OrderManager();
		UUID uid = UUID.randomUUID();
		m.enqueueDelivery(uid, mockItem(3, "DIAMOND"));
		m.enqueueDelivery(uid, mockItem(2, "DIAMOND"));
		List<ItemStack> first = m.getDeliveries(uid);
		List<ItemStack> second = m.getDeliveries(uid);
		assertEquals(2, first.size());
		assertEquals(2, second.size());
		assertNotSame(first.get(0), second.get(0));

		List<ItemStack> page0 = m.getDeliveriesPage(uid, 0, 1);
		List<ItemStack> page1 = m.getDeliveriesPage(uid, 1, 1);
		assertEquals(1, page0.size());
		assertEquals(1, page1.size());
		assertTrue(m.getDeliveriesPage(uid, 5, 1).isEmpty());
		assertTrue(m.getDeliveriesPage(uid, -1, 1).size() > 0);

		assertNull(m.removeDeliveryAt(uid, -1));
		assertNull(m.removeDeliveryAt(uid, 5));
		assertNotNull(m.removeDeliveryAt(uid, 0));

		List<ItemStack> drained = m.drainDeliveries(uid);
		assertEquals(1, drained.size());
		assertTrue(m.getDeliveries(uid).isEmpty());
	}
}