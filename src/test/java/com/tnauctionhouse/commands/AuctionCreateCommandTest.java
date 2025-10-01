package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import com.tnauctionhouse.orders.OrderManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class AuctionCreateCommandTest {

    private TNAuctionHousePlugin plugin;
    private AuctionCreateCommand command;
    private OrderManager manager;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        manager = new OrderManager();
        when(plugin.getOrderManager()).thenReturn(manager);
        command = new AuctionCreateCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderGetsError() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        boolean result = command.onCommand(console, null, "auction", new String[] {"1"});
        assertEquals(true, result);
        assertEquals("Only players can use this command.", console.nextMessage());
    }

    @Test
    void permissionAndInputValidation() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.auction")).thenReturn(false);
        boolean resultNoPerm = command.onCommand(player, null, "auction", new String[] {"1"});
        assertEquals(true, resultNoPerm);
        Mockito.verify(player).sendMessage("You don't have permission.");

        when(player.hasPermission("tnauctionhouse.auction")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack air = new ItemStack(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(air);
        boolean resultEmpty = command.onCommand(player, null, "auction", new String[] {"1"});
        assertEquals(true, resultEmpty);
        Mockito.verify(player).sendMessage("Hold an item to auction.");

        ItemStack item = new ItemStack(Material.DIAMOND, 3);
        when(inv.getItemInMainHand()).thenReturn(item);
        boolean resultUsage = command.onCommand(player, null, "auction", new String[] {});
        assertEquals(true, resultUsage);
        Mockito.verify(player).sendMessage("Usage: /auction <amount>");

        boolean resultInvalid = command.onCommand(player, null, "auction", new String[] {"foo"});
        assertEquals(true, resultInvalid);
        Mockito.verify(player).sendMessage("Invalid amount.");

        boolean resultNonPositive = command.onCommand(player, null, "auction", new String[] {"0"});
        assertEquals(true, resultNonPositive);
        Mockito.verify(player).sendMessage("Amount must be > 0.");

        boolean resultTooMany = command.onCommand(player, null, "auction", new String[] {"5"});
        assertEquals(true, resultTooMany);
        Mockito.verify(player).sendMessage("You don't have that many items.");
    }

    @Test
    void successfulAuctionCreationRemovesItemsAndStoresOrder() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.auction")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack item = new ItemStack(Material.EMERALD, 4);
        when(inv.getItemInMainHand()).thenReturn(item);

        boolean result = command.onCommand(player, null, "auction", new String[] {"3"});
        assertEquals(true, result);
        assertEquals(1, manager.getAuctionOrders().size());
        assertEquals(1, item.getAmount());
        Mockito.verify(player).sendMessage(Mockito.contains("Created auction"));
    }
}

