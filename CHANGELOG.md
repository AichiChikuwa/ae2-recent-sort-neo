# Changelog

## [1.0.0] - 2026-07-25

First full release of Applied History!

### ME Logger
- ME Logger is now a 1×1×3 multiblock with some new mechanics. It's no longer craftable, players now instead need to craft a-

### Dormant ME Logger
- craftable blank / dormant logger item that does nothing on its own
- activate it to turn it into a working ME Logger

### Misc.
- GuideME entry for the ME Logger and the Dormant ME Logger
- creative tab, commands, and localization updates

## [0.2.0] - 2026-07-21

This update reworks how history works from the ground up. Players now require an **ME Logger** block to record and use.

- **ME Logger** — new AE2 block that holds a network's interaction history
- history only works when exactly one powered ME Logger is connected to the network
- rewiring a network no longer randomly loses history; it stays with the logger itself
- breaking and re-placing a logger keeps its stored history on the block item
- the terminal history button stays disabled until a logger is present, with messages when one is missing or when multiple are connected
- multiple loggers on the same network conflict; logging pauses until only one remains
- the logger's top face shows status at a glance: off, running, or conflict
- right-clicking a logger shows how many entries are saved, or allows purging history entirely (with confirmation)
- added crafting recipe, custom textures, AE2 guidebook entry, and translations

## [0.1.4] - 2026-05-14

- fix server crash when ME topology changes: `AEBasePart.getSide()` can be null while rewiring; history grid keys now handle null owner and null facing safely

## [0.1.3] - 2026-05-09

- rewrote history behavior from true pinned rows to top-priority rows inside normal scrollable grid

## [0.1.2] - 2026-05-09

- fixed a visual bug

## [0.1.1] - 2026-05-08

- fixed a bug where the history buffer isn't consistant within the same ME network
- adjusted shift behavior so row ordering freezes on shift-export and refreshes on shift release

## [0.1.0] - 2026-05-08

- initial release
