package com.tnauctionhouse.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import com.tnauctionhouse.TNAuctionHousePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class SellOrderCreateCommandTest {

    private TNAuctionHousePlugin plugin;
    private SellOrderCreateCommand command;
    private YamlConfiguration config;
    private Economy economy;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        config = new YamlConfiguration();
        // Default tax config
        config.set("tax.enabled", true);
        config.set("tax.rate", 0.10);
        config.set("tax.mode", "ADD_TO_PRICE");
        when(plugin.getConfig()).thenReturn(config);

        economy = Mockito.mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(economy);

        command = new SellOrderCreateCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderGetsError() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        boolean result = command.onCommand(console, null, "sellorder", new String[] {"10"});
        assertEquals(true, result);
        assertEquals("Only players can use this command.", console.nextMessage());
        assertNull(console.nextMessage());
    }

    @Test
    void missingPermission() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(false);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have permission.");
    }

    @Test
    void emptyHandError() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Hold an item to sell.");
    }

    @Test
    void missingPriceArgShowsUsage() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Usage: /sellorder <price> [amount]");
    }

    @Test
    void invalidPrice() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"abc"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Invalid price.");
    }

    @Test
    void nonPositivePrice() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean resultZero = command.onCommand(player, null, "sellorder", new String[] {"0"});
        assertEquals(true, resultZero);
        Mockito.verify(player).sendMessage("Price must be > 0.");
    }

    @Test
    void invalidAmount() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "foo"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Invalid amount.");
    }

    @Test
    void nonPositiveAmount() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "0"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Amount must be > 0.");
    }

    @Test
    void amountGreaterThanInHand() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(3);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "5"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have that many items.");
    }

    @Test
    void upfrontTaxInsufficientFunds() {
        // Configure upfront tax mode
        config.set("tax.enabled", true);
        config.set("tax.mode", "UPFRONT");
        config.set("tax.rate", 0.10);

        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.sellorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.DIAMOND);
        when(hand.getAmount()).thenReturn(1);
        when(inv.getItemInMainHand()).thenReturn(hand);

        // Player cannot afford upfront fee (price=10, amount=1, fee=1.0)
        when(economy.has(player, 1.0)).thenReturn(false);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "1"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have enough money to pay the listing fee ($1.0).");
    }
}

