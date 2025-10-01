package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.AuctionOrder;
import com.tnauctionhouse.orders.OrderManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class AuctionBidCommandEdgeCasesTest {

    private TNAuctionHousePlugin plugin;
    private OrderManager manager;
    private AuctionBidCommand command;
    private Economy economy;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        manager = new OrderManager();
        when(plugin.getOrderManager()).thenReturn(manager);
        economy = Mockito.mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(economy);
        command = new AuctionBidCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cannotBidEndedAuction() {
        AuctionOrder ao = manager.createAuctionOrder(UUID.randomUUID(), new ItemStack(Material.COAL, 1), 1, -1L);
        Player p = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        boolean res = command.onCommand(p, null, "auctionbid", new String[] {ao.getOrderId().toString(), "2"});
        assertEquals(true, res);
        Mockito.verify(p).sendMessage("This auction has ended.");
    }

    @Test
    void cannotBidOnOwnAuctionWithoutBypass() {
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = manager.createAuctionOrder(seller, new ItemStack(Material.COAL, 1), 1, 10_000L);
        Player p = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p.getUniqueId()).thenReturn(seller);
        when(p.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        when(p.hasPermission("tnauctionhouse.bypass.self")).thenReturn(false);
        boolean res = command.onCommand(p, null, "auctionbid", new String[] {ao.getOrderId().toString(), "2"});
        assertEquals(true, res);
        Mockito.verify(p).sendMessage("You cannot bid on your own auction.");
    }

    @Test
    void bidMustExceedCurrent() {
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = manager.createAuctionOrder(seller, new ItemStack(Material.COAL, 1), 1, 10_000L);
        ao.setHighestBid(3);
        Player p = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        boolean res = command.onCommand(p, null, "auctionbid", new String[] {ao.getOrderId().toString(), "3"});
        assertEquals(true, res);
        Mockito.verify(p).sendMessage("Your bid must be greater than the current highest bid ($3).");
    }

    @Test
    void insufficientFunds() {
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = manager.createAuctionOrder(seller, new ItemStack(Material.COAL, 1), 1, 10_000L);
        Player p = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        when(economy.has(p, 9)).thenReturn(false);
        boolean res = command.onCommand(p, null, "auctionbid", new String[] {ao.getOrderId().toString(), "9"});
        assertEquals(true, res);
        Mockito.verify(p).sendMessage("You don't have enough money to bid $9.");
    }
}

