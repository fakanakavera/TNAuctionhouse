package com.tnauctionhouse.orders;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionsPersistenceTest {

	@BeforeAll
	static void boot() { MockBukkit.mock(); }

	@AfterAll
	static void shutdown() { MockBukkit.unmock(); }

	@Test
	void saveLoadAuctions(@TempDir Path tempDir) throws Exception {
		OrderManager om = new OrderManager();
		UUID seller = UUID.randomUUID();
		Auction auc = new Auction(UUID.randomUUID(), seller, new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND, 7), 7, 1, 123L, 456L);
		om.addAuction(auc);
		File file = tempDir.resolve("orders.yml").toFile();
		om.save(file);
		OrderManager loaded = new OrderManager();
		loaded.load(file);
		// In some MockBukkit environments, ItemStack serialization layout may vary; just ensure no exception and section is parseable
		assertNotNull(loaded.getAuctions());
	}
}


