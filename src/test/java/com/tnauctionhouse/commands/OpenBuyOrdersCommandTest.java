package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenBuyOrdersCommandTest {

    private TNAuctionHousePlugin plugin;
    private OpenBuyOrdersCommand command;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        command = new OpenBuyOrdersCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderGetsError() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        boolean result = command.onCommand(console, null, "buyorders", new String[] {});
        assertEquals(true, result);
        assertEquals("Only players can use this command.", console.nextMessage());
    }

    @Test
    void missingPermission() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(player.hasPermission("tnauctionhouse.buyorders")).thenReturn(false);
        boolean result = command.onCommand(player, null, "buyorders", new String[] {});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have permission.");
    }

    // Intentionally no GUI-open tests in headless environment
}


