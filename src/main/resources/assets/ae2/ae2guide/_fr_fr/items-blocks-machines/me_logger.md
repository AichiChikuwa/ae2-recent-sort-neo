---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME Logger
  icon: appliedhistory:me_logger
  position: 215
categories:
- devices
item_ids:
- appliedhistory:me_logger
- appliedhistory:dormant_me_logger
---

# The ME Logger









<GameScene zoom="5" background="transparent">
  <Block id="appliedhistory:me_logger" y="0" p:facing="north" p:status="on" />
  <Block id="appliedhistory:me_logger_bounding" y="1" />
  <Block id="appliedhistory:me_logger_bounding" y="2" />
</GameScene>

The ME Logger owns a network's interaction history. While it is present and active, the network remembers
items that were recently imported, exported, or requested through its terminals. That history powers the
history rows and pinning in the ME Terminal.

The placeable logger is a **1×1×3 vertical multiblock**. Crafting does not give a ready logger directly.

## Dormant ME Logger

Crafting produces a <ItemLink id="appliedhistory:dormant_me_logger" />. Hold it in the main hand and perform
this sequence:

1. crouch
2. crouch
3. left-click
4. left-click
5. crouch

A progress bar on the item shows how far along the ritual is. Wrong inputs reset progress. Leaving the
sequence idle for too long also rolls progress back. Completing it awakens the item into an ME Logger that
already carries a unique history identity and is ready to place.

There is also a harder crafting variant that uses froglights instead of the clock and books. A dormant logger
from that recipe (and anything awakened or purged from it) shows a special tooltip: that it was obtained the
lunatic way. Functionally it behaves the same.

## What It Does

*   It records recent item interactions on its network and keeps the ordered history.
*   History is tied to the logger's identity, not to the shape of the network. That identity lives on the
    block item when the logger is broken, so removing and replacing it keeps the history intact.
*   The history toggle in ME Terminals only works while a single, active ME Logger is on the network.
*   The number of remembered entries is limited by the mod's config (the same setting used for the history
    rows).

## Blank Loggers

An ME Logger without an identity (for example from `/give`) is **blank**. Blank loggers always show the
error state, cannot record history, and tell the player in their GUI to use **Purge History** to recover a
Dormant ME Logger. Only awakened loggers are meant to be placed for real use.

## Power and Channels

The ME Logger always uses one channel and draws power to run (by default 10 AE per tick, configurable). If it
loses power or a channel it stops logging. AE cables can connect to every face of the **bottom** segment,
including underneath.

Status shown on the logger:

*   **Off** – no power or no channel; not recording.
*   **On** – powered and recording as the sole logger of the network.
*   **Error** – more than one logger is on the network, or this logger is blank.

## Multiblock Integrity

Every five seconds the logger checks that its three vertical segments are still present and correctly linked
to the bottom block. If anything is missing or misaligned, it runs the purge sequence (history wipe,
structure removal, dormant drop) and summons a lightning strike at its **current** position. Moving the whole
structure is fine as long as the three blocks stay together.

## Conflicts

Only one ME Logger should be on a network at a time. If two or more are connected, they conflict: history
behaves as if there were no logger, the terminal toggle shows a conflict message, and every conflicting
logger shows the error state until only one remains.

## GUI and Purging

Right-clicking the ME Logger opens a small screen with the current entry count and the configured maximum.
**Purge History** uses a two-step confirmation: the first click shows a warning, and a second click within
five seconds permanently deletes this logger's stored history, removes the multiblock, and drops a Dormant
ME Logger.

## Recipes

Craft a Dormant ME Logger, then awaken it:

<Recipe id="appliedhistory:dormant_me_logger" />

Harder froglight variant:

<Recipe id="appliedhistory:dormant_me_logger_lunatic" />
