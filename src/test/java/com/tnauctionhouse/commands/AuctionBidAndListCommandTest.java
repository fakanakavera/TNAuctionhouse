package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.AuctionOrder;
import com.tnauctionhouse.orders.OrderManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AuctionBidAndListCommandTest {

    private TNAuctionHousePlugin plugin;
    private OrderManager manager;
    private AuctionBidCommand bidCommand;
    private AuctionsListCommand listCommand;
    private Economy economy;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        manager = new OrderManager();
        when(plugin.getOrderManager()).thenReturn(manager);
        economy = Mockito.mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(economy);
        bidCommand = new AuctionBidCommand(plugin);
        listCommand = new AuctionsListCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cannotBidInvalidInputOrPermissions() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.bid")).thenReturn(false);
        boolean res = bidCommand.onCommand(player, null, "auctionbid", new String[] {"id", "10"});
        assertEquals(true, res);
        Mockito.verify(player).sendMessage("You don't have permission.");

        when(player.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        boolean resId = bidCommand.onCommand(player, null, "auctionbid", new String[] {"not-a-uuid", "10"});
        assertEquals(true, resId);
        Mockito.verify(player).sendMessage("Invalid auctionId. Expecting UUID.");

        boolean resAmt = bidCommand.onCommand(player, null, "auctionbid", new String[] {UUID.randomUUID().toString(), "foo"});
        assertEquals(true, resAmt);
        Mockito.verify(player).sendMessage("Invalid amount.");
    }

    @Test
    void biddingFlowWithdrawsAndRefundsPreviousBidder() {
        // Create auction
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = manager.createAuctionOrder(seller, new ItemStack(Material.DIAMOND, 2), 2, 60_000L);

        Player p1 = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        UUID p1Id = UUID.randomUUID();
        when(p1.getUniqueId()).thenReturn(p1Id);
        when(p1.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        when(economy.has(p1, 5)).thenReturn(true);
        when(p1.hasPermission("tnauctionhouse.bypass.self")).thenReturn(false);

        boolean r1 = bidCommand.onCommand(p1, null, "auctionbid", new String[] {ao.getOrderId().toString(), "5"});
        assertEquals(true, r1);
        Mockito.verify(economy).withdrawPlayer(p1, 5);

        // Second bidder outbids; previous refunded
        Player p2 = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p2.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        when(economy.has(p2, 8)).thenReturn(true);

        boolean r2 = bidCommand.onCommand(p2, null, "auctionbid", new String[] {ao.getOrderId().toString(), "8"});
        assertEquals(true, r2);
        Mockito.verify(economy).withdrawPlayer(p2, 8);

        // Verify a deposit was made to an OfflinePlayer with p1's UUID
        ArgumentCaptor<OfflinePlayer> captor = ArgumentCaptor.forClass(OfflinePlayer.class);
        Mockito.verify(economy).depositPlayer(captor.capture(), Mockito.eq(5.0));
        OfflinePlayer refunded = captor.getValue();
        assertEquals(p1Id, refunded.getUniqueId());
    }

    @Test
    void raceConditionRefundsNewBidderImmediately() {
        UUID seller = UUID.randomUUID();
        AuctionOrder ao = manager.createAuctionOrder(seller, new ItemStack(Material.IRON_INGOT, 1), 1, 60_000L);

        Player p = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        when(p.hasPermission("tnauctionhouse.bid")).thenReturn(true);
        when(p.hasPermission("tnauctionhouse.bypass.self")).thenReturn(false);
        when(economy.has(p, 6)).thenReturn(true);

        // When withdraw happens, another bid appears making latest > amount
        Mockito.doAnswer((Answer<Object>) invocation -> {
            int amt = ((Number) invocation.getArguments()[1]).intValue();
            ao.setHighestBid(amt + 1);
            ao.setHighestBidderId(UUID.randomUUID());
            return null;
        }).when(economy).withdrawPlayer(eq(p), eq(6.0));

        boolean res = bidCommand.onCommand(p, null, "auctionbid", new String[] {ao.getOrderId().toString(), "6"});
        assertEquals(true, res);
        // Refund to same player since bid was not placed
        Mockito.verify(economy).depositPlayer(p, 6.0);
        Mockito.verify(p).sendMessage(Mockito.contains("You were outbid just now"));
    }

    @Test
    void listShowsAuctionsAndPagination() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.auctions")).thenReturn(true);

        // First page is empty
        boolean empty = listCommand.onCommand(player, null, "auctions", new String[] {});
        assertEquals(true, empty);
        Mockito.verify(player).sendMessage("No auctions found on this page.");

        // Create some auctions and list again
        Player invOwner = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(invOwner.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(invOwner.getInventory()).thenReturn(inv);
        ItemStack itm = new ItemStack(Material.GOLD_INGOT, 2);
        when(inv.getItemInMainHand()).thenReturn(itm);
        manager.createAuctionOrder(invOwner.getUniqueId(), itm, 2, 60_000L);

        boolean listed = listCommand.onCommand(player, null, "auctions", new String[] {"0"});
        assertEquals(true, listed);
        Mockito.verify(player).sendMessage(Mockito.contains("Auctions (page 0):"));
    }
}

