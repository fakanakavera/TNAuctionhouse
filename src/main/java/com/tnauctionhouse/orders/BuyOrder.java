package com.tnauctionhouse.orders;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BuyOrder {
    private final UUID orderId;
    private final UUID buyerId;
    private final ItemStack templateItem;
	private final int pricePerUnit;
    private final int amount;
    private final long createdAt;
	private final int escrowTotal;

	public BuyOrder(UUID orderId, UUID buyerId, ItemStack templateItem, int pricePerUnit, int amount, long createdAt, int escrowTotal) {
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.templateItem = templateItem;
        this.pricePerUnit = pricePerUnit;
        this.amount = amount;
        this.createdAt = createdAt;
        this.escrowTotal = escrowTotal;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public ItemStack getTemplateItem() {
        return templateItem.clone();
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

	public int getEscrowTotal() {
        return escrowTotal;
    }
}


