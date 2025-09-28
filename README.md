TNauctionhouse
==============

A lightweight Auction House-style plugin for Paper/Spigot that lets players create Buy and Sell orders with a clean GUI, Vault economy support, per-order taxes, search, and delivery withdrawal.

Features
--------
- Sell Orders: list items for sale with unit pricing and quantity
- Buy Orders: request items; other players can fulfill partially or fully
- GUI Browsers: paginated views for buy/sell orders, type/category filters, and text search
- Confirmation Purchase: confirm/cancel GUI when buying sell orders
- Chat Amount Prompt: when fulfilling buy orders, players enter an amount via chat
- Full-Inventory Matching: fulfilling buy orders counts and removes across all stacks in the player inventory (and offhand)
- Taxes:
  - Sell and buy orders tax with modes UPFRONT or ADD_TO_PRICE
  - Separate buy order tax (applied at order creation)
- Delivery Queue: buy-order deliveries are queued for buyers to withdraw later

Requirements
------------
- Paper/Spigot (api-version: 1.21 declared)
- Vault and an economy provider (e.g., EssentialsX Economy)

Installation
------------
1. Build with Maven or use a provided release JAR
2. Place TNauctionhouse-*.jar in your server plugins/ folder
3. Restart the server to generate config files
4. Adjust config.yml as needed and restart/reload

Commands
--------
- /sellorder <price> [amount]
  - Create a sell order for the item in your hand (amount defaults to stack size if omitted)
- /buyorder <price> <amount>
  - Create a buy order using the item in your hand as the template
- /sellorders | /sellorders search <query>
  - Open the sell orders browser, or open results filtered by a query
- /buyorders | /buyorders search <query>
  - Open the buy orders browser, or open results filtered by a query
- /withdrawitems
  - Open the withdrawal GUI to collect items delivered to you from buy orders
- /myorders
  - Open a GUI listing your active Sell and Buy orders. Click an order to cancel it.
  - Cancelling a Sell order simply removes it. Any upfront listing tax previously paid is not refunded.
  - Cancelling a Buy order refunds the remaining escrow only. Any upfront fee paid at creation is not refunded.

All text search is case-insensitive and matches material names, custom display names, and lore text.

GUIs & UX
---------
- Sell Orders GUI
  - Shows stacks (up to max stack size) with price and total
  - Clicking an order opens a 3x9 confirmation: left 3x3 green confirm, center item, right 3x3 red cancel
- Buy Orders GUI
  - Shows template item with payout information
  - Clicking an order closes the GUI and prompts in chat: "Type the amount you want to sell (max X), or type 'cancel' to abort."
  - Fulfilling will consume items across your inventory and offhand, not just the main hand
- Filter Menu & Category Views
  - Optional access to filtered or category-based browsing

Taxes & Configuration
---------------------
plugins/TNauctionhouse/config.yml

Sell order tax configuration:

tax:
  enabled: true            # Master toggle for sell tax
  rate: 0.10               # Decimal percentage (0.10 = 10%)
  # Modes for sell orders:
  # - UPFRONT: Charge the seller at listing time (fee paid immediately)
  # - ADD_TO_PRICE: Tax added on top; buyer pays extra, seller gets base price
  mode: ADD_TO_PRICE

Buy order tax configuration (applies when placing buy orders):

buy_tax:
  enabled: true            # Master toggle for buy tax
  rate: 0.10               # Decimal percentage (0.10 = 10%)
  # Mode parity; currently taken as an upfront fee at creation time
  mode: UPFRONT

Behavior summary:
- Sell orders:
  - UPFRONT: Seller pays listing fee immediately
  - ADD_TO_PRICE: Buyer pays tax; seller receives the base price per unit
- Buy orders:
  - A fee (based on total escrow) is charged at order creation if enabled
  - Sellers fulfilling the order receive the base price per unit from escrow

Economy Handling
----------------
- Requires Vault; economy checked at startup
- Sell orders (ADD_TO_PRICE) deposit only the base price to the seller, tax is paid by buyer
- Buy orders decrement buyer balance on creation (escrow + any fee) and credit sellers upon fulfillment

Data Storage
------------
- Orders and pending deliveries are stored in plugins/TNauctionhouse/orders.yml
- Delivery queue: sellers' items delivered through buy orders are queued per player until withdrawal via GUI

Permissions
-----------
permissions:
  tnauctionhouse.sellorder:      true
  tnauctionhouse.buyorder:       true
  tnauctionhouse.sellorders:     true
  tnauctionhouse.buyorders:      true
  tnauctionhouse.withdraworder:  true
  tnauctionhouse.bypass.self:    op   # Allow buying your own sell order or fulfilling your own buy order
  tnauctionhouse.ah:             true

Building
--------
- Java & Maven installed
- From the project root:
mvn clean package -DskipTests
- Output JAR will be in target/

Notes & Compatibility
---------------------
- Designed for modern Paper/Spigot with Adventure API components
- Optional MMOItems integration: items can carry custom names/lore and will be matched by type
- Search is semantic across item name, display name, and lore (case-insensitive plain text matching)

Roadmap Ideas
-------------
- Per-player GUI settings and advanced filters
- History and analytics of orders
- Optional cooldowns or fees per listing/fulfillment

Support
-------
Please open an issue or contact the maintainer with logs and configuration snippets if you encounter problems.

