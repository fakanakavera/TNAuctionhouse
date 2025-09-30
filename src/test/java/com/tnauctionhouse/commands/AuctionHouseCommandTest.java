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

public class AuctionHouseCommandTest {

    private TNAuctionHousePlugin plugin;
    private AuctionHouseCommand command;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        command = new AuctionHouseCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderGetsError() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        boolean result = command.onCommand(console, null, "auctionhouse", new String[] {});
        assertEquals(true, result);
        assertEquals("Only players can use this command.", console.nextMessage());
    }

    @Test
    void missingArgsShowsUsage() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        boolean result = command.onCommand(player, null, "auctionhouse", new String[] {});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Usage: /auctionhouse search <query> [sell|buy]");
    }

    // Intentionally no GUI-open tests in headless environment

    @Test
    void unknownSubcommandShowsHelp() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        boolean result = command.onCommand(player, null, "auctionhouse", new String[] {"noop"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Unknown subcommand. Try: /auctionhouse search <query> [sell|buy]");
    }
}


