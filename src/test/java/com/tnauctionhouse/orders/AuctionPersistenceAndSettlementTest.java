package com.tnauctionhouse.orders;

import com.tnauctionhouse.TNAuctionHousePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionPersistenceAndSettlementTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void saveAndLoadAuctionsRoundtrip() throws Exception {
        OrderManager om = new OrderManager();
        AuctionOrder ao = om.createAuctionOrder(UUID.randomUUID(), new ItemStack(Material.DIAMOND, 2), 2, 1000L);
        // simulate a bid
        ao.setHighestBidderId(UUID.randomUUID());
        ao.setHighestBid(10);

        File temp = File.createTempFile("orders", ".yml");
        try {
            om.save(temp);
            OrderManager om2 = new OrderManager();
            om2.load(temp);
            List<AuctionOrder> loaded = om2.getAuctionOrders();
            assertEquals(1, loaded.size());
            AuctionOrder copy = loaded.get(0);
            assertEquals(ao.getOrderId(), copy.getOrderId());
            assertEquals(10, copy.getHighestBid());
            assertEquals(2, copy.getAmount());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    @Test
    void settlementPaysSellerAndDeliversOrReturns() throws Exception {
        // Register mocked Vault economy before loading plugin
        org.bukkit.plugin.Plugin vault = MockBukkit.createMockPlugin("Vault");
        Economy econ = mock(Economy.class);
        server.getServicesManager().register(Economy.class, econ, vault, org.bukkit.plugin.ServicePriority.Normal);

        // Load plugin normally
        TNAuctionHousePlugin plugin = MockBukkit.load(TNAuctionHousePlugin.class);
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getEconomy());

        // Create an auction that already ended
        OrderManager om = plugin.getOrderManager();
        AuctionOrder ao = om.createAuctionOrder(UUID.randomUUID(), new ItemStack(Material.EMERALD, 1), 1, -1000L);
        ao.setHighestBidderId(UUID.randomUUID());
        ao.setHighestBid(7);

        // Invoke settlement via reflection
        java.lang.reflect.Method m = TNAuctionHousePlugin.class.getDeclaredMethod("settleEndedAuctions");
        m.setAccessible(true);
        m.invoke(plugin);

        // Verify deposit to seller (match by UUID)
        ArgumentCaptor<OfflinePlayer> captor = ArgumentCaptor.forClass(OfflinePlayer.class);
        verify(econ, atLeastOnce()).depositPlayer(captor.capture(), eq(7.0));
        assertEquals(ao.getSellerId(), captor.getValue().getUniqueId());

        // Auction removed after settlement
        assertTrue(om.getAuctionOrders().isEmpty());
    }

    @Test
    void settlementNoBidReturnsItemToSellerQueue() throws Exception {
        org.bukkit.plugin.Plugin vault = MockBukkit.createMockPlugin("Vault");
        Economy econ = mock(Economy.class);
        server.getServicesManager().register(Economy.class, econ, vault, org.bukkit.plugin.ServicePriority.Normal);

        TNAuctionHousePlugin plugin = MockBukkit.load(TNAuctionHousePlugin.class);
        assertTrue(plugin.isEnabled());

        OrderManager om = plugin.getOrderManager();
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = om.createAuctionOrder(seller, new ItemStack(Material.COAL, 3), 3, -1000L);
        // no bids

        java.lang.reflect.Method m = TNAuctionHousePlugin.class.getDeclaredMethod("settleEndedAuctions");
        m.setAccessible(true);
        m.invoke(plugin);

        // No deposit to seller (verify zero interactions on deposit)
        verify(econ, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        // Item queued for withdrawal
        assertFalse(om.getDeliveries(seller).isEmpty());
        assertTrue(om.getAuctionOrders().isEmpty());
    }
}

