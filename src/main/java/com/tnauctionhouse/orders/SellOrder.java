package com.tnauctionhouse.orders;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SellOrder {
    private final UUID orderId;
    private final UUID sellerId;
    private final ItemStack item;
	private final int pricePerUnit;
    private final int amount;
    private final long createdAt;

	public SellOrder(UUID orderId, UUID sellerId, ItemStack item, int pricePerUnit, int amount, long createdAt) {
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.item = item;
        this.pricePerUnit = pricePerUnit;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ItemStack getItem() {
        return item.clone();
    }

	public int getPricePerUnit() {
        return pricePerUnit;
    }

    public int getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}


