package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.OrderManager;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class AuctionsListCommandEdgeTest {

    private TNAuctionHousePlugin plugin;
    private OrderManager manager;
    private AuctionsListCommand command;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        manager = new OrderManager();
        Mockito.when(plugin.getOrderManager()).thenReturn(manager);
        command = new AuctionsListCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void missingPermission() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.auctions")).thenReturn(false);
        boolean res = command.onCommand(player, null, "auctions", new String[] {});
        assertEquals(true, res);
        Mockito.verify(player).sendMessage("You don't have permission.");
    }
}

