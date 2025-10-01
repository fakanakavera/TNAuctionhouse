package com.tnauctionhouse;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TNAuctionHousePluginLifecycleTest {

    private ServerMock server;

    @BeforeEach
    void setup() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testOnEnableWithoutVaultDisablesPlugin() {
        TNAuctionHousePlugin plugin = MockBukkit.load(TNAuctionHousePlugin.class);
        assertFalse(plugin.isEnabled(), "Plugin should be disabled if Vault is missing");
    }

    @Test
    void testOnEnableWithVaultRegistersManagersAndCommands() {
        // Create a mock Vault plugin and register a dummy Economy provider
        Plugin vault = MockBukkit.createMockPlugin("Vault");
        Economy dummy = new DummyEconomy();
        server.getServicesManager().register(Economy.class, dummy, vault, ServicePriority.Normal);

        TNAuctionHousePlugin plugin = MockBukkit.load(TNAuctionHousePlugin.class);
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getEconomy());
        assertNotNull(plugin.getOrderManager());
        assertNotNull(plugin.getOrderLogger());
        assertNotNull(plugin.getNotificationManager());

        // Commands should be registered
        assertNotNull(server.getCommandMap().getCommand("sellorder"));
        assertNotNull(server.getCommandMap().getCommand("buyorder"));
        assertNotNull(server.getCommandMap().getCommand("sellorders"));
        assertNotNull(server.getCommandMap().getCommand("buyorders"));
        assertNotNull(server.getCommandMap().getCommand("withdrawitems"));
        assertNotNull(server.getCommandMap().getCommand("auctionhouse"));
        assertNotNull(server.getCommandMap().getCommand("myorders"));
        // New auction commands
        assertNotNull(server.getCommandMap().getCommand("auction"));
        assertNotNull(server.getCommandMap().getCommand("auctions"));
    }

    // Minimal Economy stub sufficient for plugin boot and simple has/withdraw/deposit used in code
    static class DummyEconomy implements Economy {
        @Override public boolean isEnabled() { return true; }
        @Override public String getName() { return "dummy"; }
        @Override public boolean hasBankSupport() { return false; }
        @Override public int fractionalDigits() { return 0; }
        @Override public String format(double amount) { return String.valueOf((int) amount); }
        @Override public String currencyNamePlural() { return "dollars"; }
        @Override public String currencyNameSingular() { return "dollar"; }
        @Override public boolean hasAccount(String playerName) { return true; }
        @Override public boolean hasAccount(org.bukkit.OfflinePlayer player) { return true; }
        @Override public boolean hasAccount(String playerName, String worldName) { return true; }
        @Override public boolean hasAccount(org.bukkit.OfflinePlayer player, String worldName) { return true; }
        @Override public EconomyResponse withdrawPlayer(org.bukkit.OfflinePlayer player, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse depositPlayer(org.bukkit.OfflinePlayer player, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public boolean has(String playerName, double amount) { return true; }
        @Override public boolean has(org.bukkit.OfflinePlayer player, double amount) { return true; }
        @Override public boolean has(String playerName, String worldName, double amount) { return true; }
        @Override public boolean has(org.bukkit.OfflinePlayer player, String worldName, double amount) { return true; }
        @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse withdrawPlayer(org.bukkit.OfflinePlayer player, String worldName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse depositPlayer(String playerName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse depositPlayer(org.bukkit.OfflinePlayer player, String worldName, double amount) { return new EconomyResponse(amount, 0, EconomyResponse.ResponseType.SUCCESS, ""); }
        @Override public EconomyResponse createBank(String name, String player) { return notSupported(); }
        @Override public EconomyResponse createBank(String name, org.bukkit.OfflinePlayer player) { return notSupported(); }
        @Override public EconomyResponse deleteBank(String name) { return notSupported(); }
        @Override public EconomyResponse bankBalance(String name) { return notSupported(); }
        @Override public EconomyResponse bankHas(String name, double amount) { return notSupported(); }
        @Override public EconomyResponse bankWithdraw(String name, double amount) { return notSupported(); }
        @Override public EconomyResponse bankDeposit(String name, double amount) { return notSupported(); }
        @Override public EconomyResponse isBankOwner(String name, String playerName) { return notSupported(); }
        @Override public EconomyResponse isBankOwner(String name, org.bukkit.OfflinePlayer player) { return notSupported(); }
        @Override public EconomyResponse isBankMember(String name, String playerName) { return notSupported(); }
        @Override public EconomyResponse isBankMember(String name, org.bukkit.OfflinePlayer player) { return notSupported(); }
        @Override public java.util.List<String> getBanks() { return java.util.Collections.emptyList(); }
        @Override public boolean createPlayerAccount(String playerName) { return true; }
        @Override public boolean createPlayerAccount(org.bukkit.OfflinePlayer player) { return true; }
        @Override public boolean createPlayerAccount(String playerName, String worldName) { return true; }
        @Override public boolean createPlayerAccount(org.bukkit.OfflinePlayer player, String worldName) { return true; }

        private EconomyResponse notSupported() { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, ""); }
        @Override public double getBalance(String playerName) { return 0; }
        @Override public double getBalance(org.bukkit.OfflinePlayer player) { return 0; }
        @Override public double getBalance(String playerName, String world) { return 0; }
        @Override public double getBalance(org.bukkit.OfflinePlayer player, String world) { return 0; }
      }
}


