package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.command.ConsoleCommandSenderMock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class BuyOrderCreateCommandTest {

    private TNAuctionHousePlugin plugin;
    private BuyOrderCreateCommand command;
    private YamlConfiguration config;
    private Economy economy;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
        config = new YamlConfiguration();
        // Defaults
        config.set("buy_tax.enabled", true);
        config.set("buy_tax.rate", 0.10);
        config.set("buy_tax.mode", "UPFRONT");
        when(plugin.getConfig()).thenReturn(config);

        economy = Mockito.mock(Economy.class);
        when(plugin.getEconomy()).thenReturn(economy);

        command = new BuyOrderCreateCommand(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderGetsError() {
        ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
        boolean result = command.onCommand(console, null, "buyorder", new String[] {"10", "2"});
        assertEquals(true, result);
        assertEquals("Only players can use this command.", console.nextMessage());
    }

    @Test
    void missingPermission() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(false);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"10", "2"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have permission.");
    }

    @Test
    void emptyHandError() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
        when(hand.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"10", "2"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Hold an item to create a buy order for.");
    }

    @Test
    void missingArgsShowsUsage() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = new ItemStack(Material.DIAMOND, 1);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"10"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Usage: /buyorder <price> <amount>");
    }

    @Test
    void invalidArgs() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = new ItemStack(Material.DIAMOND, 1);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"foo", "bar"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Invalid price or amount.");
    }

    @Test
    void nonPositiveValues() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = new ItemStack(Material.DIAMOND, 1);
        when(inv.getItemInMainHand()).thenReturn(hand);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"0", "-1"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("Price and amount must be > 0.");
    }

    @Test
    void insufficientFundsWithTax() {
        Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
        when(player.hasPermission("tnauctionhouse.buyorder")).thenReturn(true);
        PlayerInventory inv = Mockito.mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inv);
        ItemStack hand = new ItemStack(Material.DIAMOND, 1);
        when(inv.getItemInMainHand()).thenReturn(hand);

        // price=10, amount=2, total=20, fee=2, final=22 -> cannot afford
        when(economy.has(player, 22)).thenReturn(false);

        boolean result = command.onCommand(player, null, "buyorder", new String[] {"10", "2"});
        assertEquals(true, result);
        Mockito.verify(player).sendMessage("You don't have enough money ($22).");
    }
}


