package com.tnauctionhouse.commands;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import com.tnauctionhouse.TNAuctionHousePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class SellOrderCreateCommandTest {

	private ServerMock server;
    private TNAuctionHousePlugin plugin;
    private SellOrderCreateCommand command;
    private YamlConfiguration config;
    private Economy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();

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
        PlayerMock player = server.addPlayer();
        // Ensure player does NOT have the permission
        player.setOp(false);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10"});
        assertEquals(true, result);
        assertEquals("You don't have permission.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void emptyHandError() {
        PlayerMock player = server.addPlayer();
        // Give permission
        player.setOp(true);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10"});
        assertEquals(true, result);
        assertEquals("Hold an item to sell.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void missingPriceArgShowsUsage() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        // Put an item in hand
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean result = command.onCommand(player, null, "sellorder", new String[] {});
        assertEquals(true, result);
        assertEquals("Usage: /sellorder <price> [amount]", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void invalidPrice() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"abc"});
        assertEquals(true, result);
        assertEquals("Invalid price.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void nonPositivePrice() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean resultZero = command.onCommand(player, null, "sellorder", new String[] {"0"});
        assertEquals(true, resultZero);
        assertEquals("Price must be > 0.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void invalidAmount() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "foo"});
        assertEquals(true, result);
        assertEquals("Invalid amount.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void nonPositiveAmount() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "0"});
        assertEquals(true, result);
        assertEquals("Amount must be > 0.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void amountGreaterThanInHand() {
        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 3));

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "5"});
        assertEquals(true, result);
        assertEquals("You don't have that many items.", player.nextMessage());
        assertNull(player.nextMessage());
    }

    @Test
    void upfrontTaxInsufficientFunds() {
        // Configure upfront tax mode
        config.set("tax.enabled", true);
        config.set("tax.mode", "UPFRONT");
        config.set("tax.rate", 0.10);

        PlayerMock player = server.addPlayer();
        player.setOp(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.DIAMOND, 1));

        // Player cannot afford upfront fee (price=10, amount=1, fee=1.0)
        when(economy.has(player, 1.0)).thenReturn(false);

        boolean result = command.onCommand(player, null, "sellorder", new String[] {"10", "1"});
        assertEquals(true, result);
        assertEquals("You don't have enough money to pay the listing fee ($1.0).", player.nextMessage());
        assertNull(player.nextMessage());
    }
}

