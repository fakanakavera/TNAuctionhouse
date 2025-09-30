package com.tnauctionhouse.orders;

import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class OrderManagerDebugWriteTest {

    @Test
    void writeDebugYaml() throws Exception {
        MockBukkit.mock();
        try {
            OrderManager om = new OrderManager();
            UUID buyer = UUID.randomUUID();
            om.enqueueDelivery(buyer, new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT, 3));
            File file = Paths.get("target", "orders-debug.yml").toFile();
            if (file.exists()) file.delete();
            om.save(file);
            System.out.println("WROTE: " + file.getAbsolutePath() + " for buyer=" + buyer);

            OrderManager loaded = new OrderManager();
            loaded.load(file);
            System.out.println("LOADED deliveries size: " + loaded.getDeliveries(buyer).size());
        } finally {
            MockBukkit.unmock();
        }
    }
}


