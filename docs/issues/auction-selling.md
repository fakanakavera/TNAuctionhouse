# Feature: Auction-Type Selling

## Summary
Add an auction selling type with command /auction <amount>. Starting price fixed at 1. Behaves like sellorder but places item into an auction where players bid. Duration is 7 days; highest bidder at end wins.

## Requirements
- Command: /auction <amount>
  - Permission: tnauctionhouse.auction (default: true)
  - Amount refers to item quantity to auction; item is taken from player hand/inventory.
- Starting price: 1 (cannot be changed at listing)
- Auction duration: 7 days (168 hours) from creation
- Bidding mechanics:
  - Players place bids greater than current highest bid.
  - On bid: immediately withdraw bid amount from bidder and hold in escrow.
  - If outbid: immediately refund full previous bid to the former highest bidder.
  - Disallow bidding if bidder cannot pay.
  - Disallow bidding by the seller unless bypass permission.
- Completion:
  - At auction end, transfer escrow to seller and deliver item to winner.
  - If no bids: return item to seller, no funds transferred.
- Cancellation:
  - Seller can cancel before first bid; item returned.
  - After first bid, cancellation disabled (or admin-only).
- UI/UX:
  - Auction orders appear in a new or existing browser tab with time remaining and top bid.
  - Confirmation GUI on creating auction like sellorder.
  - Chat or GUI flow for placing bids.
- Economy
  - Vault required. All bid funds held per-auction escrow.
  - Taxes: follow existing tax model if applicable, else none.

## Data Model
- New AuctionOrder entity:
  - id, sellerUuid, itemStack, amount, startTime, endTime, startPrice=1, highestBid, highestBidderUuid, escrowBalance, status (ACTIVE, ENDED, CANCELLED)
  - bid history list (bidderUuid, amount, time) optional.

## Scheduling
- Periodic task to close auctions past endTime and process settlement.

## Integration
- Parity with sellorder creation validations (hand item presence, amount checks, inventory removal).
- Add to /myorders and relevant browsers.
- Permissions: tnauctionhouse.auction, tnauctionhouse.bid, tnauctionhouse.admin.auction.cancel

## Edge Cases
- Bids from players who disconnect; escrow persists.
- Economy failures must roll back bid placement.
- Inventory full on settlement: send to withdrawal queue.
- Server restart: persistence ensures auctions resume with correct remaining time.

## Acceptance Criteria
- /auction <amount> lists an item as auction with start price 1.
- Bidding holds funds; outbid refunds immediately.
- After 7 days, highest bidder receives item; seller receives funds.
- No-bid auctions return item to seller.
