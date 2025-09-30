package com.tnauctionhouse.orders;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class OrderManagerRoundtripDebugTest {

    @Test
    void writeRoundtripYaml() throws Exception {
        MockBukkit.mock();
        try {
            OrderManager om = new OrderManager();
            UUID seller = UUID.randomUUID();
            UUID buyer = UUID.randomUUID();
            om.createSellOrder(seller, new org.bukkit.inventory.ItemStack(org.bukkit.Material.EMERALD, 12), 3, 12);
            om.createBuyOrder(buyer, new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 7), 9, 7, 63);
            om.enqueueDelivery(buyer, new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 3));

            File file = Paths.get("target", "orders-roundtrip.yml").toFile();
            if (file.exists()) file.delete();
            om.save(file);
            System.out.println("WROTE roundtrip: " + file.getAbsolutePath() + " buyer=" + buyer);

            OrderManager loaded = new OrderManager();
            loaded.load(file);
            System.out.println("LOADED roundtrip deliveries: " + loaded.getDeliveries(buyer).size());
        } finally {
            MockBukkit.unmock();
        }
    }
}


