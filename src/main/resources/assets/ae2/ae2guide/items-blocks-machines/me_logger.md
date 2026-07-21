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
---

# The ME Logger

<BlockImage id="appliedhistory:me_logger" scale="8" />

The ME Logger is the block that owns a network's interaction history. While it is present, the network
remembers the items that were recently imported, exported, or requested through its terminals, which is
what powers the history rows and pinning in the ME Terminal.

## What It Does

*   It records recent item interactions on its network and keeps the ordered history.
*   The history is tied to the logger itself, not to the network's shape. Every logger carries a unique
    identity, and that identity is kept on the block item when you break it, so pulling the logger out and
    placing it back down keeps its history intact.
*   The history toggle button in ME Terminals only works while a single, active ME Logger is on the network.
    Without one, the button is inert and explains that a logger is required.
*   The number of remembered entries is limited by the mod's config (the same setting used for the history
    rows).

## Power and Channels

The ME Logger always uses one channel and draws power to run (by default 10 AE per tick, configurable). If it
loses power or a channel it stops logging. Its top face shows its current state:

*   **Off** – no power or no channel; the logger is not recording.
*   **On** – powered and recording as the sole logger of the network.
*   **Error** – more than one logger is on the network.

## Conflicts

Only one ME Logger should be on a network at a time. If two or more are connected, they conflict: history
behaves as if there were no logger at all, the terminal toggle shows a dedicated conflict message, and every
conflicting logger displays the error state until only one remains.

## GUI and Purging

Right-clicking the ME Logger opens a small screen showing how many entries are currently stored and the
configured maximum. It also has a **Purge History** button with a two-step confirmation: the first click
shows a warning, and a second click within five seconds permanently deletes this logger's stored history and
removes the block, dropping a blank logger with no stored identity.

## Recipe

<RecipeFor id="appliedhistory:me_logger" />
