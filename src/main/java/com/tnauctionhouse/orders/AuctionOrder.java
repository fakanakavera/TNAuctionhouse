package com.tnauctionhouse.orders;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionOrder {
    private final UUID orderId;
    private final UUID sellerId;
    private final ItemStack item;
    private final int amount;
    private final long createdAt;
    private final long endAt;
    private final int startPrice;

    private volatile UUID highestBidderId;
    private volatile int highestBid;

    public AuctionOrder(UUID orderId, UUID sellerId, ItemStack item, int amount, long createdAt, long endAt, int startPrice, UUID highestBidderId, int highestBid) {
        this.orderId = orderId;
        this.sellerId = sellerId;
        this.item = item;
        this.amount = amount;
        this.createdAt = createdAt;
        this.endAt = endAt;
        this.startPrice = startPrice;
        this.highestBidderId = highestBidderId;
        this.highestBid = highestBid;
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

    public int getAmount() {
        return amount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getEndAt() {
        return endAt;
    }

    public int getStartPrice() {
        return startPrice;
    }

    public UUID getHighestBidderId() {
        return highestBidderId;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public void setHighestBidderId(UUID highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public void setHighestBid(int highestBid) {
        this.highestBid = highestBid;
    }
}

