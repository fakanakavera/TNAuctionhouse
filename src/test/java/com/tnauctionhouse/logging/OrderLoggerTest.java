package com.tnauctionhouse.logging;

import com.tnauctionhouse.TNAuctionHousePlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class OrderLoggerTest {

	@BeforeAll
	static void beforeAll() {
		MockBukkit.mock();
	}

	@AfterAll
	static void afterAll() {
		MockBukkit.unmock();
	}

	@Test
	void writes_completed_order_yaml() throws Exception {
		TNAuctionHousePlugin plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
		File data = Files.createTempDirectory("ah-data").toFile();
		when(plugin.getDataFolder()).thenReturn(data);
		when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

		OrderLogger logger = new OrderLogger(plugin);
		UUID orderId = UUID.randomUUID();
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();
		ItemStack item = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
		when(item.clone()).thenReturn(item);
		logger.logCompletedOrder(orderId, "SELL", seller, buyer, item, 2, 5, 999L);

		File logFile = new File(data, "completed-orders.yml");
		assertTrue(logFile.exists());
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(logFile);
		assertTrue(yaml.getKeys(true).stream().anyMatch(k -> k.startsWith("orders.")));
		String base = yaml.getKeys(false).stream().filter(k -> k.equals("orders")).findFirst().map(k -> "orders." + yaml.getConfigurationSection(k).getKeys(false).iterator().next()).orElse(null);
		assertNotNull(base);
		assertEquals("SELL", yaml.getString(base + ".type"));
		assertEquals(10, yaml.getInt(base + ".totalPrice"));
		assertEquals(999L, yaml.getLong(base + ".timestamp"));
		assertTrue(yaml.contains(base + ".item"));
	}
}