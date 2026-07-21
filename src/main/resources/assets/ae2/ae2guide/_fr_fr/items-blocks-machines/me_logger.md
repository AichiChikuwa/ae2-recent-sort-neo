---
navigation:
  parent: items-blocks-machines/items-blocks-machines-index.md
  title: Enregistreur ME
  icon: appliedhistory:me_logger
  position: 215
categories:
- devices
item_ids:
- appliedhistory:me_logger
---

# L'Enregistreur ME

<BlockImage id="appliedhistory:me_logger" scale="8" />

L'Enregistreur ME est le bloc auquel appartient l'historique des interactions d'un réseau. Tant qu'il est
présent, le réseau se souvient des objets récemment importés, exportés ou demandés via ses terminaux, ce qui
alimente les lignes d'historique et l'épinglage dans le Terminal ME.

## Fonctionnement

*   Il enregistre les interactions récentes avec les objets de son réseau et conserve l'historique dans
    l'ordre.
*   L'historique est lié à l'enregistreur lui-même, et non à la forme du réseau. Chaque enregistreur possède
    un identifiant unique, conservé dans l'objet-bloc lorsqu'il est cassé. Retirer l'enregistreur puis le
    reposer préserve donc son historique.
*   Le bouton d'historique des Terminaux ME ne fonctionne que si un seul Enregistreur ME actif est présent
    sur le réseau. Sans lui, le bouton est inactif et indique qu'un enregistreur est nécessaire.
*   Le nombre d'entrées mémorisées est limité par la configuration du mod (le même réglage que pour les lignes
    d'historique).

## Énergie et canaux

L'Enregistreur ME utilise toujours un canal et consomme de l'énergie pour fonctionner (par défaut 10 AE par
tick, configurable). S'il perd son énergie ou un canal, il cesse d'enregistrer. Sa face supérieure indique son
état actuel :

*   **Éteint** – pas d'énergie ou pas de canal ; l'enregistreur n'enregistre pas.
*   **Allumé** – alimenté et enregistrant en tant que seul enregistreur du réseau.
*   **Erreur** – plus d'un enregistreur est présent sur le réseau.

## Conflits

Il ne doit y avoir qu'un seul Enregistreur ME par réseau à la fois. Si deux ou plus sont connectés, ils
entrent en conflit : l'historique se comporte comme s'il n'y avait aucun enregistreur, le bouton du terminal
affiche un message de conflit dédié, et chaque enregistreur en conflit affiche l'état d'erreur jusqu'à ce
qu'il n'en reste qu'un.

## Interface et purge

Un clic droit sur l'Enregistreur ME ouvre un petit écran indiquant le nombre d'entrées actuellement stockées
et le maximum configuré. Il comporte aussi un bouton **Purger l'historique** avec une confirmation en deux
temps : le premier clic affiche un avertissement, et un second clic dans les cinq secondes supprime
définitivement l'historique stocké de cet enregistreur et retire le bloc, laissant tomber un enregistreur
vierge sans identifiant stocké.

## Recette

<RecipeFor id="appliedhistory:me_logger" />
