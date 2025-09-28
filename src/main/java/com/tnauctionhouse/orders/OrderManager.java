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
    private final Map<UUID, List<ItemStack>> pendingDeliveries = new HashMap<>();

    public List<SellOrder> getSellOrders() {
        return Collections.unmodifiableList(sellOrders);
    }

    public List<BuyOrder> getBuyOrders() {
        return Collections.unmodifiableList(buyOrders);
    }

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
            cfg.set(base + ".item", order.getItem());
        }

		for (BuyOrder order : buyOrders) {
            String base = "buyOrders." + order.getOrderId();
            cfg.set(base + ".buyerId", order.getBuyerId().toString());
			cfg.set(base + ".pricePerUnit", order.getPricePerUnit());
            cfg.set(base + ".amount", order.getAmount());
            cfg.set(base + ".createdAt", order.getCreatedAt());
			cfg.set(base + ".escrowTotal", order.getEscrowTotal());
            cfg.set(base + ".templateItem", order.getTemplateItem());
        }

        for (Map.Entry<UUID, List<ItemStack>> entry : pendingDeliveries.entrySet()) {
            cfg.set("pendingDeliveries." + entry.getKey(), entry.getValue());
        }

        cfg.save(file);
    }

    public synchronized void load(File file) throws IOException {
        sellOrders.clear();
        buyOrders.clear();
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
                    ItemStack item = sellSec.getItemStack(key + ".item");
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
                    ItemStack tpl = buySec.getItemStack(key + ".templateItem");
                    if (tpl == null) continue;
                    buyOrders.add(new BuyOrder(orderId, buyerId, tpl, price, amount, createdAt, escrowTotal));
                } catch (Exception ignored) {
                }
            }
        }

        ConfigurationSection delSec = cfg.getConfigurationSection("pendingDeliveries");
        if (delSec != null) {
            for (String key : delSec.getKeys(false)) {
                try {
                    UUID userId = UUID.fromString(key);
                    List<?> raw = delSec.getList(key);
                    if (raw == null) continue;
                    List<ItemStack> items = new ArrayList<>();
                    for (Object obj : raw) {
                        if (obj instanceof ItemStack) items.add(((ItemStack) obj).clone());
                    }
                    if (!items.isEmpty()) pendingDeliveries.put(userId, items);
                } catch (Exception ignored) {
                }
            }
        }
    }
}


