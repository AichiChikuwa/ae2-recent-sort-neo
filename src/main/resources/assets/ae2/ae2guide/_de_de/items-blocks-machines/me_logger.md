---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: ME-Logger
  icon: appliedhistory:me_logger
  position: 215
categories:
- devices
item_ids:
- appliedhistory:me_logger
---

# Der ME-Logger

<BlockImage id="appliedhistory:me_logger" scale="8" />

Der ME-Logger ist der Block, dem der Interaktionsverlauf eines Netzwerks gehört. Solange er vorhanden ist,
merkt sich das Netzwerk, welche Gegenstände zuletzt über seine Terminals importiert, exportiert oder
angefordert wurden. Das ist die Grundlage für die Verlaufszeilen und das Anheften im ME-Terminal.

## Funktion

*   Er zeichnet die letzten Gegenstands-Interaktionen seines Netzwerks auf und hält den Verlauf in
    Reihenfolge.
*   Der Verlauf ist an den Logger selbst gebunden, nicht an die Form des Netzwerks. Jeder Logger trägt eine
    eindeutige Kennung, und diese Kennung bleibt beim Abbauen im Block-Item erhalten. Den Logger zu entfernen
    und wieder zu platzieren bewahrt also seinen Verlauf.
*   Die Verlaufsschaltfläche in ME-Terminals funktioniert nur, wenn genau ein aktiver ME-Logger im Netzwerk
    ist. Ohne einen ist die Schaltfläche wirkungslos und weist darauf hin, dass ein Logger benötigt wird.
*   Die Anzahl der gemerkten Einträge wird durch die Konfiguration des Mods begrenzt (dieselbe Einstellung
    wie für die Verlaufszeilen).

## Energie und Kanäle

Der ME-Logger belegt immer einen Kanal und verbraucht Energie zum Betrieb (standardmäßig 10 AE pro Tick,
konfigurierbar). Verliert er Energie oder einen Kanal, hört er auf aufzuzeichnen. Seine Oberseite zeigt den
aktuellen Zustand:

*   **Aus** – keine Energie oder kein Kanal; der Logger zeichnet nicht auf.
*   **An** – mit Energie versorgt und als einziger Logger des Netzwerks am Aufzeichnen.
*   **Fehler** – mehr als ein Logger befindet sich im Netzwerk.

## Konflikte

Es sollte immer nur ein ME-Logger pro Netzwerk vorhanden sein. Sind zwei oder mehr verbunden, geraten sie in
Konflikt: Der Verlauf verhält sich, als gäbe es gar keinen Logger, die Terminal-Schaltfläche zeigt eine
eigene Konfliktmeldung, und jeder betroffene Logger zeigt den Fehlerzustand, bis nur noch einer übrig ist.

## GUI und Löschen

Ein Rechtsklick auf den ME-Logger öffnet ein kleines Fenster, das anzeigt, wie viele Einträge derzeit
gespeichert sind und wie hoch das konfigurierte Maximum ist. Es enthält außerdem eine Schaltfläche
**Verlauf löschen** mit zweistufiger Bestätigung: Der erste Klick zeigt eine Warnung, ein zweiter Klick
innerhalb von fünf Sekunden löscht den gespeicherten Verlauf dieses Loggers endgültig und entfernt den Block,
wobei ein leerer Logger ohne gespeicherte Kennung fallen gelassen wird.

## Rezept

<RecipeFor id="appliedhistory:me_logger" />
