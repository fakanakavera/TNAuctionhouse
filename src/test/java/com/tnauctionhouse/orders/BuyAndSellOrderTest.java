package com.tnauctionhouse.orders;

import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.bukkit.inventory.ItemStack;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class BuyAndSellOrderTest {

	@BeforeAll
	static void beforeAll() {
		MockBukkit.mock();
	}

	@AfterAll
	static void afterAll() {
		MockBukkit.unmock();
	}

	private static ItemStack mockClonableItem(int initialAmount) {
		ItemStack original = Mockito.mock(ItemStack.class);
		ItemStack clone = Mockito.mock(ItemStack.class);
		AtomicInteger cloneAmount = new AtomicInteger(initialAmount);
		when(original.clone()).thenReturn(clone);
		when(clone.clone()).thenReturn(clone);
		when(clone.getAmount()).thenAnswer(inv -> cloneAmount.get());
		Mockito.doAnswer(inv -> { cloneAmount.set((Integer) inv.getArgument(0)); return null; }).when(clone).setAmount(Mockito.anyInt());
		when(clone.getMaxStackSize()).thenReturn(64);
		return original;
	}

	@Test
	void buyOrder_getters_and_clone() {
		UUID orderId = UUID.randomUUID();
		UUID buyerId = UUID.randomUUID();
		ItemStack item = mockClonableItem(3);
		BuyOrder order = new BuyOrder(orderId, buyerId, item, 25, 10, 123L, 250);

		assertEquals(orderId, order.getOrderId());
		assertEquals(buyerId, order.getBuyerId());
		assertEquals(25, order.getPricePerUnit());
		assertEquals(10, order.getAmount());
		assertEquals(123L, order.getCreatedAt());
		assertEquals(250, order.getEscrowTotal());

		ItemStack tpl = order.getTemplateItem();
		assertNotSame(item, tpl);
		// the returned item is a clone of original.clone(); we stubbed clone amount to start at 3
		assertEquals(3, tpl.getAmount());
	}

	@Test
	void sellOrder_getters_and_clone() {
		UUID orderId = UUID.randomUUID();
		UUID sellerId = UUID.randomUUID();
		ItemStack item = mockClonableItem(5);
		SellOrder order = new SellOrder(orderId, sellerId, item, 7, 5, 456L);

		assertEquals(orderId, order.getOrderId());
		assertEquals(sellerId, order.getSellerId());
		assertEquals(7, order.getPricePerUnit());
		assertEquals(5, order.getAmount());
		assertEquals(456L, order.getCreatedAt());

		ItemStack sameClone = order.getItem();
		assertNotSame(item, sameClone);
	}
}