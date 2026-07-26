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

## Basics

The ME Logger is a **1×1×3 vertical multiblock** that allows an ME network to record its players' recent item activity. While one is connected, the network keeps track of items imported, exported, or requested through its terminals. You can then open an <ItemLink id="ae2:terminal" /> or any of its variants, turn on the **History** button, and see recorded items pinned at the top of the grid.

Power and channels can only be provided to the bottom block through AE2 cables.

The ME Logger always uses one channel and draws power to run (by default 10 AE per tick, configurable).

You need to craft a <ItemLink id="appliedhistory:dormant_me_logger" /> and awaken it to obtain an ME Logger.


## Dormant ME Logger

<Recipe id="appliedhistory:dormant_me_logger" />

A Dormant ME Logger must be initialized before it can become a deployable ME Logger. Hold it in your main hand and perform the following sequence:

1. crouch
2. crouch
3. left-click
4. left-click
5. crouch

A progress bar on the item shows how far along the ritual you are. Waiting too long between inputs causes the progress to roll back. Completing the ritual awakens the item into an ME Logger with its own UUID, making it ready to place.

You can also craft it the hard way using froglights. It grants nothing special other than adding a unique tooltip (and an advancement) though:

<Recipe id="appliedhistory:dormant_me_logger_lunatic" />

## Detailed Mechanics

* History entries are stored in the ME Logger itself and remain with it when the block is broken.
* Exactly one active ME Logger may be connected to a network at a time.
* The logger can only remember a limited number of entries. This limit is shown in the ME Logger GUI and can be changed in the mod configuration.
* The Purge History button permanently deletes the stored history, removes the ME Logger, and drops a new Dormant ME Logger that can be awakened again.

## Invalid Loggers

An ME Logger without a UUID, such as one obtained through `/give`, is **unusable**. Invalid loggers always show the error state and cannot record history. Players are instructed to use **Purge History** to recover a Dormant ME Logger.

## Multiblock Integrity

Like other multiblock machines, the ME Logger is sensitive to changes in its structure. It performs performance-friendly integrity checks at regular intervals. If any part is missing or misaligned, it automatically runs the purge sequence (delete history, remove blocks, drop a Dormant ME Logger), and **lightning strikes** its current position.

When moving the logger, it is recommended to break and re-place it manually.