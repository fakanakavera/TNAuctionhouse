package com.tnauctionhouse.orders;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {
	private final UUID auctionId;
	private final UUID sellerId;
	private final ItemStack item;
	private final int amount;
	private final int startingPrice;
	private final long createdAt;
	private final long durationMs;

	public Auction(UUID auctionId, UUID sellerId, ItemStack item, int amount, int startingPrice, long createdAt, long durationMs) {
		this.auctionId = auctionId;
		this.sellerId = sellerId;
		this.item = item;
		this.amount = amount;
		this.startingPrice = startingPrice;
		this.createdAt = createdAt;
		this.durationMs = durationMs;
	}

	public UUID getAuctionId() { return auctionId; }
	public UUID getSellerId() { return sellerId; }
	public ItemStack getItem() { return item.clone(); }
	public int getAmount() { return amount; }
	public int getStartingPrice() { return startingPrice; }
	public long getCreatedAt() { return createdAt; }
	public long getDurationMs() { return durationMs; }

	public long getEndAt() { return createdAt + durationMs; }
}


