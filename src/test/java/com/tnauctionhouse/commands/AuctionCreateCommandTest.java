package com.tnauctionhouse.commands;

import com.tnauctionhouse.TNAuctionHousePlugin;
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
 

public class AuctionCreateCommandTest {

	private TNAuctionHousePlugin plugin;
	private AuctionCreateCommand command;

	@BeforeEach
	void setUp() {
		MockBukkit.mock();
		plugin = Mockito.mock(TNAuctionHousePlugin.class, Mockito.RETURNS_DEEP_STUBS);
		command = new AuctionCreateCommand(plugin);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void nonPlayerSenderGetsError() {
		ConsoleCommandSenderMock console = new ConsoleCommandSenderMock();
		boolean result = command.onCommand(console, null, "auction", new String[] {});
		assertEquals(true, result);
		assertEquals("Only players can use this command.", console.nextMessage());
	}

	@Test
	void missingPermission() {
		Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
		Mockito.when(player.hasPermission("tnauctionhouse.auction.create")).thenReturn(false);
		boolean result = command.onCommand(player, null, "auction", new String[] {});
		assertEquals(true, result);
		Mockito.verify(player).sendMessage("You don't have permission.");
	}

	@Test
	void emptyHandError() {
		Player player = Mockito.mock(Player.class, Mockito.RETURNS_DEEP_STUBS);
		Mockito.when(player.hasPermission("tnauctionhouse.auction.create")).thenReturn(true);
		PlayerInventory inv = Mockito.mock(PlayerInventory.class);
		Mockito.when(player.getInventory()).thenReturn(inv);
		ItemStack hand = Mockito.mock(ItemStack.class, Mockito.RETURNS_DEEP_STUBS);
		Mockito.when(hand.getType()).thenReturn(Material.AIR);
		Mockito.when(inv.getItemInMainHand()).thenReturn(hand);
		boolean result = command.onCommand(player, null, "auction", new String[] {});
		assertEquals(true, result);
		Mockito.verify(player).sendMessage("Hold an item to auction.");
	}
}

