package com.tnauctionhouse.orders;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrderManager {

    private final List<SellOrder> sellOrders = new CopyOnWriteArrayList<>();
    private final List<BuyOrder> buyOrders = new CopyOnWriteArrayList<>();
    private final List<Auction> auctions = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<ItemStack>> pendingDeliveries = new HashMap<>();

    public List<SellOrder> getSellOrders() {
        return Collections.unmodifiableList(sellOrders);
    }

    public List<BuyOrder> getBuyOrders() {
        return Collections.unmodifiableList(buyOrders);
    }

    public List<Auction> getAuctions() { return Collections.unmodifiableList(auctions); }

	public SellOrder createSellOrder(UUID sellerId, ItemStack item, int pricePerUnit, int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(amount);
        SellOrder order = new SellOrder(UUID.randomUUID(), sellerId, clone, pricePerUnit, amount, System.currentTimeMillis());
        sellOrders.add(order);
        return order;
    }

	public BuyOrder createBuyOrder(UUID buyerId, ItemStack item, int pricePerUnit, int amount, int escrowTotal) {
        ItemStack template = item.clone();
        template.setAmount(1);
        BuyOrder order = new BuyOrder(UUID.randomUUID(), buyerId, template, pricePerUnit, amount, System.currentTimeMillis(), escrowTotal);
        buyOrders.add(order);
        return order;
    }

    public List<SellOrder> getSellOrdersPage(int page, int pageSize) {
        return paginate(sellOrders, page, pageSize);
    }

    public List<BuyOrder> getBuyOrdersPage(int page, int pageSize) {
        return paginate(buyOrders, page, pageSize);
    }

    public List<Auction> getAuctionsPage(int page, int pageSize) {
        return paginate(auctions, page, pageSize);
    }

	public List<SellOrder> searchSellOrders(String query, int page, int pageSize) {
		if (query == null) query = "";
		final String q = query.toLowerCase();
		List<SellOrder> filtered = new ArrayList<>();
		for (SellOrder order : sellOrders) {
			if (matchesQuery(order.getItem(), q)) {
				filtered.add(order);
			}
		}
		return paginate(filtered, page, pageSize);
	}

    

	public List<BuyOrder> searchBuyOrders(String query, int page, int pageSize) {
		if (query == null) query = "";
		final String q = query.toLowerCase();
		List<BuyOrder> filtered = new ArrayList<>();
		for (BuyOrder order : buyOrders) {
			if (matchesQuery(order.getTemplateItem(), q)) {
				filtered.add(order);
			}
		}
		return paginate(filtered, page, pageSize);
	}

	public List<SellOrder> filterSellOrdersByCategory(ItemTypeCategory category, int page, int pageSize) {
		List<SellOrder> filtered = new ArrayList<>();
		for (SellOrder order : sellOrders) {
			if (categoryOf(order.getItem()) == category) filtered.add(order);
		}
		return paginate(filtered, page, pageSize);
	}

	public List<BuyOrder> filterBuyOrdersByCategory(ItemTypeCategory category, int page, int pageSize) {
		List<BuyOrder> filtered = new ArrayList<>();
		for (BuyOrder order : buyOrders) {
			if (categoryOf(order.getTemplateItem()) == category) filtered.add(order);
		}
		return paginate(filtered, page, pageSize);
	}

    private <T> List<T> paginate(List<T> list, int page, int pageSize) {
        if (page < 0) page = 0;
        int from = page * pageSize;
        if (from >= list.size()) return new ArrayList<>();
        int to = Math.min(from + pageSize, list.size());
        return new ArrayList<>(list.subList(from, to));
    }

	private boolean matchesQuery(ItemStack item, String lowercaseQuery) {
		if (item == null) return false;
		if (item.getType().name().toLowerCase().contains(lowercaseQuery)) return true;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            net.kyori.adventure.text.Component dn = meta.displayName();
            if (dn != null && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(dn).toLowerCase().contains(lowercaseQuery)) return true;
            java.util.List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore != null) {
                for (net.kyori.adventure.text.Component c : lore) {
                    String line = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
                    if (line != null && line.toLowerCase().contains(lowercaseQuery)) return true;
                }
            }
        }
		return false;
	}

	private ItemTypeCategory categoryOf(ItemStack item) {
		org.bukkit.Material type = item.getType();
		String name = type.name();
		if (name.endsWith("_SWORD") || name.endsWith("_BOW") || name.endsWith("_CROSSBOW") || name.endsWith("_TRIDENT") || name.endsWith("_AXE"))
			return ItemTypeCategory.WEAPONS;
		if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("SHIELD"))
			return ItemTypeCategory.ARMOR;
		if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.equals("FISHING_ROD") || name.equals("SHEARS"))
			return ItemTypeCategory.TOOLS;
		if (name.endsWith("_POTION") || name.equals("POTION") || name.equals("SPLASH_POTION") || name.equals("LINGERING_POTION"))
			return ItemTypeCategory.POTIONS;
		return ItemTypeCategory.MISC;
	}

    public void removeSellOrder(SellOrder order) {
        sellOrders.remove(order);
    }

    public void removeBuyOrder(BuyOrder order) {
        buyOrders.remove(order);
    }

    public void addAuction(Auction auction) { auctions.add(auction); }
    public void removeAuction(Auction auction) { auctions.remove(auction); }

    public synchronized void enqueueDelivery(UUID recipient, ItemStack item) {
        ItemStack clone = item.clone();
        pendingDeliveries.computeIfAbsent(recipient, k -> new ArrayList<>()).add(clone);
    }

    public synchronized List<ItemStack> drainDeliveries(UUID recipient) {
        List<ItemStack> items = pendingDeliveries.remove(recipient);
        if (items == null) return new ArrayList<>();
        return new ArrayList<>(items);
    }

    public synchronized List<ItemStack> getDeliveries(UUID recipient) {
        List<ItemStack> items = pendingDeliveries.get(recipient);
        if (items == null) return new ArrayList<>();
        List<ItemStack> copy = new ArrayList<>(items.size());
        for (ItemStack stack : items) copy.add(stack.clone());
        return copy;
    }

    public synchronized List<ItemStack> getDeliveriesPage(UUID recipient, int page, int pageSize) {
        List<ItemStack> items = pendingDeliveries.get(recipient);
        if (items == null) return new ArrayList<>();
        if (page < 0) page = 0;
        int from = page * pageSize;
        if (from >= items.size()) return new ArrayList<>();
        int to = Math.min(from + pageSize, items.size());
        List<ItemStack> out = new ArrayList<>(to - from);
        for (int i = from; i < to; i++) out.add(items.get(i).clone());
        return out;
    }

    public synchronized ItemStack removeDeliveryAt(UUID recipient, int index) {
        List<ItemStack> items = pendingDeliveries.get(recipient);
        if (items == null) return null;
        if (index < 0 || index >= items.size()) return null;
        return items.remove(index);
    }

    public synchronized void save(File file) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            // noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }
        YamlConfiguration cfg = new YamlConfiguration();

		for (SellOrder order : sellOrders) {
            String base = "sellOrders." + order.getOrderId();
            cfg.set(base + ".sellerId", order.getSellerId().toString());
			cfg.set(base + ".pricePerUnit", order.getPricePerUnit());
            cfg.set(base + ".amount", order.getAmount());
            cfg.set(base + ".createdAt", order.getCreatedAt());
            // Serialize ItemStack to a neutral map to avoid implementation-specific class names
            cfg.set(base + ".item", order.getItem().serialize());
        }

		for (BuyOrder bo : buyOrders) {
			String base = "buyOrders." + bo.getOrderId();
			cfg.set(base + ".buyerId", bo.getBuyerId().toString());
			cfg.set(base + ".pricePerUnit", bo.getPricePerUnit());
			cfg.set(base + ".amount", bo.getAmount());
			cfg.set(base + ".createdAt", bo.getCreatedAt());
			cfg.set(base + ".escrowTotal", bo.getEscrowTotal());
			cfg.set(base + ".templateItem", bo.getTemplateItem().serialize());
		}

        for (Auction auction : auctions) {
            String base = "auctions." + auction.getAuctionId();
            cfg.set(base + ".sellerId", auction.getSellerId().toString());
            cfg.set(base + ".amount", auction.getAmount());
            cfg.set(base + ".startingPrice", auction.getStartingPrice());
            cfg.set(base + ".createdAt", auction.getCreatedAt());
            cfg.set(base + ".durationMs", auction.getDurationMs());
            cfg.set(base + ".highestBid", auction.getHighestBid());
            cfg.set(base + ".highestBidderId", auction.getHighestBidderId() == null ? null : auction.getHighestBidderId().toString());
            cfg.set(base + ".item", auction.getItem().serialize());
        }

        for (Map.Entry<UUID, List<ItemStack>> entry : pendingDeliveries.entrySet()) {
            // Preferred: list of serialized maps
            List<Map<String, Object>> serialized = new ArrayList<>(entry.getValue().size());
            for (ItemStack it : entry.getValue()) serialized.add(it.serialize());
            cfg.set("pendingDeliveries." + entry.getKey(), serialized);

            // Also: numeric-keyed simple fields (type/amount) for extra compatibility
            List<ItemStack> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                ItemStack it = list.get(i);
                String base = "pendingDeliveries." + entry.getKey() + "." + i;
                cfg.set(base + ".type", it.getType().name());
                cfg.set(base + ".amount", it.getAmount());
            }
        }

        cfg.save(file);
    }

    public synchronized void load(File file) throws IOException {
        sellOrders.clear();
        buyOrders.clear();
        auctions.clear();
        pendingDeliveries.clear();
        if (!file.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

		ConfigurationSection sellSec = cfg.getConfigurationSection("sellOrders");
        if (sellSec != null) {
            for (String key : sellSec.getKeys(false)) {
                try {
                    UUID orderId = UUID.fromString(key);
                    UUID sellerId = UUID.fromString(sellSec.getString(key + ".sellerId"));
                    int price = sellSec.getInt(key + ".pricePerUnit");
                    int amount = sellSec.getInt(key + ".amount");
                    long createdAt = sellSec.getLong(key + ".createdAt");
                    Object node = sellSec.get(key + ".item");
                    ItemStack item = null;
                    try {
                        if (node instanceof ItemStack) {
                            item = ((ItemStack) node).clone();
                        } else if (node instanceof java.util.Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) node;
                            item = ItemStack.deserialize(map);
                        } else if (node instanceof org.bukkit.configuration.ConfigurationSection) {
                            org.bukkit.configuration.ConfigurationSection sec = (org.bukkit.configuration.ConfigurationSection) node;
                            item = ItemStack.deserialize(sec.getValues(true));
                        }
                    } catch (Exception ex) {
                        item = null;
                    }
                    if (item == null) {
                        // Fallback: attempt minimal reconstruction from type/amount
                        Object raw = sellSec.get(key + ".item");
                        if (raw instanceof Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
                            Object t = map.get("type");
                            Object a = map.get("amount");
                            if (t instanceof String) {
                                try {
                                    org.bukkit.Material mat = org.bukkit.Material.valueOf((String) t);
                                    int amt = (a instanceof Number) ? ((Number) a).intValue() : 1;
                                    if (amt <= 0) amt = 1;
                                    item = new ItemStack(mat, amt);
                                } catch (IllegalArgumentException ignored2) {}
                            }
                        } else if (raw instanceof org.bukkit.configuration.ConfigurationSection) {
                            org.bukkit.configuration.ConfigurationSection sec = (org.bukkit.configuration.ConfigurationSection) raw;
                            String typeName = sec.getString("type");
                            int amt = sec.getInt("amount", 1);
                            if (amt <= 0) amt = 1;
                            if (typeName != null) {
                                try {
                                    org.bukkit.Material mat = org.bukkit.Material.valueOf(typeName);
                                    item = new ItemStack(mat, amt);
                                } catch (IllegalArgumentException ignored2) {}
                            }
                        }
                    }
                    if (item == null) continue;
                    sellOrders.add(new SellOrder(orderId, sellerId, item, price, amount, createdAt));
                } catch (Exception ignored) {
                }
            }
        }

		ConfigurationSection buySec = cfg.getConfigurationSection("buyOrders");
        if (buySec != null) {
            for (String key : buySec.getKeys(false)) {
                try {
                    UUID orderId = UUID.fromString(key);
                    UUID buyerId = UUID.fromString(buySec.getString(key + ".buyerId"));
                    int price = buySec.getInt(key + ".pricePerUnit");
                    int amount = buySec.getInt(key + ".amount");
                    long createdAt = buySec.getLong(key + ".createdAt");
                    int escrowTotal = buySec.getInt(key + ".escrowTotal");
                    Object node = buySec.get(key + ".templateItem");
                    ItemStack tpl = null;
                    try {
                        if (node instanceof ItemStack) {
                            tpl = ((ItemStack) node).clone();
                        } else if (node instanceof java.util.Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) node;
                            tpl = ItemStack.deserialize(map);
                        } else if (node instanceof org.bukkit.configuration.ConfigurationSection) {
                            org.bukkit.configuration.ConfigurationSection sec = (org.bukkit.configuration.ConfigurationSection) node;
                            tpl = ItemStack.deserialize(sec.getValues(true));
                        }
                    } catch (Exception ex) {
                        tpl = null;
                    }
                    if (tpl == null) {
                        // Fallback minimal reconstruction
                        Object raw = buySec.get(key + ".templateItem");
                        if (raw instanceof Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) raw;
                            Object t = map.get("type");
                            Object a = map.get("amount");
                            if (t instanceof String) {
                                try {
                                    org.bukkit.Material mat = org.bukkit.Material.valueOf((String) t);
                                    int amt = (a instanceof Number) ? ((Number) a).intValue() : 1;
                                    if (amt <= 0) amt = 1;
                                    tpl = new ItemStack(mat, amt);
                                } catch (IllegalArgumentException ignored2) {}
                            }
                        } else if (raw instanceof org.bukkit.configuration.ConfigurationSection) {
                            org.bukkit.configuration.ConfigurationSection sec = (org.bukkit.configuration.ConfigurationSection) raw;
                            String typeName = sec.getString("type");
                            int amt = sec.getInt("amount", 1);
                            if (amt <= 0) amt = 1;
                            if (typeName != null) {
                                try {
                                    org.bukkit.Material mat = org.bukkit.Material.valueOf(typeName);
                                    tpl = new ItemStack(mat, amt);
                                } catch (IllegalArgumentException ignored2) {}
                            }
                        }
                    }
                    if (tpl == null) continue;
                    buyOrders.add(new BuyOrder(orderId, buyerId, tpl, price, amount, createdAt, escrowTotal));
                } catch (Exception ignored) {
                }
            }
        }

        ConfigurationSection aucSec = cfg.getConfigurationSection("auctions");
        if (aucSec != null) {
            for (String key : aucSec.getKeys(false)) {
                try {
                    UUID auctionId = UUID.fromString(key);
                    UUID sellerId = UUID.fromString(aucSec.getString(key + ".sellerId"));
                    int amount = aucSec.getInt(key + ".amount");
                    int startingPrice = aucSec.getInt(key + ".startingPrice", 1);
                    long createdAt = aucSec.getLong(key + ".createdAt");
                    long durationMs = aucSec.getLong(key + ".durationMs", 7L * 24L * 60L * 60L * 1000L);
                    int highestBid = aucSec.getInt(key + ".highestBid", 0);
                    String hbStr = aucSec.getString(key + ".highestBidderId");
                    UUID highestBidderId = null;
                    if (hbStr != null) { try { highestBidderId = UUID.fromString(hbStr); } catch (Exception ignored) {} }
                    Object node = aucSec.get(key + ".item");
                    ItemStack item = null;
                    try {
                        if (node instanceof ItemStack) {
                            item = ((ItemStack) node).clone();
                        } else if (node instanceof java.util.Map) {
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) node;
                            item = ItemStack.deserialize(map);
                        } else if (node instanceof org.bukkit.configuration.ConfigurationSection) {
                            org.bukkit.configuration.ConfigurationSection sec = (org.bukkit.configuration.ConfigurationSection) node;
                            item = ItemStack.deserialize(sec.getValues(true));
                        }
                    } catch (Exception ex) { item = null; }
                    if (item == null) continue;
                    Auction auc = new Auction(auctionId, sellerId, item, amount, startingPrice, createdAt, durationMs);
                    if (highestBidderId != null && highestBid > 0) auc.setHighestBid(highestBidderId, highestBid);
                    auctions.add(auc);
                } catch (Exception ignored) {}
            }
        }

        ConfigurationSection delSec = cfg.getConfigurationSection("pendingDeliveries");
        if (delSec != null) {
            for (String userKey : delSec.getKeys(false)) {
                try {
                    UUID userId = UUID.fromString(userKey);
                    List<ItemStack> items = new ArrayList<>();

                    // Try map-list at top level (preferred when created by Bukkit serialize())
                    List<Map<?, ?>> mapList = delSec.getMapList(userKey);
                    if (mapList != null && !mapList.isEmpty()) {
                        for (Map<?, ?> m : mapList) {
                            try {
                                @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) m;
                                try {
                                    items.add(ItemStack.deserialize(map));
                                } catch (Exception ex) {
                                    // Fallback: only use type/amount
                                    Object t = map.get("type");
                                    Object a = map.get("amount");
                                    if (t instanceof String && a instanceof Number) {
                                        try {
                                            org.bukkit.Material mat = org.bukkit.Material.valueOf((String) t);
                                            items.add(new ItemStack(mat, ((Number) a).intValue()));
                                        } catch (IllegalArgumentException ignored3) {}
                                    }
                                }
                            } catch (Exception ignored2) {}
                        }
                    }

                    // Try list node under the section (some YAML writers nest lists under UUID key)
                    if (items.isEmpty()) {
                        Object maybeList = delSec.get(userKey);
                        if (maybeList instanceof java.util.List) {
                            for (Object obj : (java.util.List<?>) maybeList) {
                                if (obj instanceof Map) {
                                    @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) obj;
                                    try { items.add(ItemStack.deserialize(map)); } catch (Exception ex) {
                                        Object t = map.get("type");
                                        Object a = map.get("amount");
                                        if (t instanceof String && a instanceof Number) {
                                            try {
                                                org.bukkit.Material mat = org.bukkit.Material.valueOf((String) t);
                                                items.add(new ItemStack(mat, ((Number) a).intValue()));
                                            } catch (IllegalArgumentException ignored3) {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Try numeric-keyed fallback (type/amount) nested under UUID
                    if (items.isEmpty()) {
                        ConfigurationSection itemsSec = delSec.getConfigurationSection(userKey);
                        if (itemsSec != null) {
                            for (String idxKey : itemsSec.getKeys(false)) {
                                ConfigurationSection node = itemsSec.getConfigurationSection(idxKey);
                                String typeName;
                                int amount;
                                if (node != null) {
                                    typeName = node.getString("type");
                                    amount = node.getInt("amount", 0);
                                } else {
                                    typeName = itemsSec.getString(idxKey + ".type");
                                    amount = itemsSec.getInt(idxKey + ".amount", 0);
                                }
                                if (typeName == null || amount <= 0) continue;
                                try {
                                    org.bukkit.Material mat = org.bukkit.Material.valueOf(typeName);
                                    ItemStack stack = new ItemStack(mat, amount);
                                    items.add(stack);
                                } catch (IllegalArgumentException ignored2) {}
                            }
                        }
                    }

                    // As last resort, attempt raw list retrieval again
                    if (items.isEmpty()) {
                        List<?> raw = delSec.getList(userKey);
                        if (raw != null && !raw.isEmpty()) {
                            for (Object obj : raw) {
                                if (obj instanceof ItemStack) {
                                    items.add(((ItemStack) obj).clone());
                                } else if (obj instanceof Map) {
                                    @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) obj;
                                    try { items.add(ItemStack.deserialize(map)); } catch (Exception ex) {
                                        Object t = map.get("type");
                                        Object a = map.get("amount");
                                        if (t instanceof String && a instanceof Number) {
                                            try {
                                                org.bukkit.Material mat = org.bukkit.Material.valueOf((String) t);
                                                items.add(new ItemStack(mat, ((Number) a).intValue()));
                                            } catch (IllegalArgumentException ignored3) {}
                                        }
                                    }
                                } else if (obj instanceof ConfigurationSection) {
                                    ConfigurationSection sec = (ConfigurationSection) obj;
                                    try { items.add(ItemStack.deserialize(sec.getValues(true))); } catch (Exception ex) {
                                        String typeName = sec.getString("type");
                                        int amount = sec.getInt("amount", 0);
                                        if (typeName != null && amount > 0) {
                                            try {
                                                org.bukkit.Material mat = org.bukkit.Material.valueOf(typeName);
                                                items.add(new ItemStack(mat, amount));
                                            } catch (IllegalArgumentException ignored3) {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!items.isEmpty()) {
                        pendingDeliveries.put(userId, items);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}


