# GDD & PROMPT: "DEAD TERRA" ARKNIGHTS TECH MOD - COLUMBIA CHAPTER
**Context for AI:** I am building a hardcore Minecraft tech/exploration mod set in a post-apocalyptic, dead version of Terra from the game *Arknights*. The player arrives via spaceship. There are no living NPCs—only monsters, rogue machines, and ghosts. I need you to generate the KubeJS scripts, questbook entries (FTB Quests), and mob mechanics for the first major region: Columbia.

*(Note: This mod is designed to slot into a larger tech modpack. Do not hardcode specific tech tiers or voltage names into the lore/quests; refer generally to "advanced machinery," "high-tier energy," or "heavy industrial processing".)*

## OVERVIEW: THE COLUMBIA PROGRESSION LOOP
The player lands fully equipped with advanced, late-game technology. However, their creative flight is disabled by the Starpod EMP. The goal of this chapter is to survive the wastes, explore three major ruined structures, defeat their respective bosses, and secure the blueprint to safely process Originium.

---

## 1. THE LANDING: COLUMBIAN FRONTIER
**Biome:** The Barrenlands (Coarse dirt, dead bushes, craters, sparse ruined pioneer camps).
**Objective:** Survive the landing, establish a primitive forward operating base, and harvest early Terran scrap.

*   **Mobs:** **Originium Slugs**
    *   *Mechanic:* Weak, fast-moving pests. They are attracted to electromagnetic fields. If they touch active energy cables or generators, they drain the machine's stored power. If killed near machines, they explode into corrosive acid that damages machine casings.
*   **Gameplay Loop:** The player must secure their crashed rocket, fend off the slugs, and scavenge broken Columbian Rovers for Titanium, Tungstensteel scrap, and raw Originium shards (requires a Hazmat suit or it deals rapid tick damage).

---

## 2. FIRST DUNGEON: MANSFIELD STATE PRISON
**Structure:** A massive, ruined, mobile penitentiary that has crashed in the wastes. **(Rule: Only 1 generates per world).**
**Atmosphere:** Pitch-black, claustrophobic cell blocks. The prison is haunted by the infected inmates and wardens who died locked inside.

*   **Dungeon Mechanics:**
    *   The player must navigate the dark, labyrinthine cell blocks to find the Warden's Office.
    *   Loot chests contain advanced Columbian circuitry and riot gear (used to craft early Terran shielding).
*   **Boss Fight: Jesselton’s Shadow**
    *   *Lore:* The ghost of the mercenary Jesselton Miller, forever trapped in the prison he tried to conquer.
    *   *Mechanics:* Jesselton is a spectral entity (immune to standard physical knockback).
    *   *Phase 1:* He attacks with ranged spectral iron arts, dealing heavy kinetic damage.
    *   *Phase 2:* He switches to armor-piercing Void/Magic damage and summons "Imprisoned Shadows" (adds) that attempt to swarm the player.
    *   *Drop:* "Mansfield Master Key" (used to unlock the next tier of exploration) and high-capacity fluid cells.

---

## 3. SECOND DUNGEON: DOROTHY’S VISION (PIONEER LABS)
**Structure:** An underground, bio-mechanical research facility hidden beneath a ruined pioneer town.
**Atmosphere:** Flickering neon lights, overgrown with synthetic flesh and Originium crystals.

*   **Dungeon Mechanics:**
    *   The player explores abandoned test chambers.
    *   **Mobs:** "The Franks" (Biomechanical Pioneer Mutants). Fast, frail enemies that inflict *Mining Fatigue* and *Nausea* when they hit the player, simulating sensory overload from Dorothy's dead Originium hivemind.
*   **Boss Fight: The Failed Vision**
    *   *Lore:* The amalgamated, horrific result of the Originium neural-network experiment left to mutate for 300 years.
    *   *Mechanics:* A massive, fleshy Originium mass. It cannot move, but it manipulates the arena. It constantly spawns "Franks" and shoots lasers of concentrated Originium Arts. The player must use explosives or chemical throwers to destroy its neural-nodes before the core takes damage.
    *   *Drop:* "Dorothy's Neural Processor" (a critical component for crafting high-tier, multi-block assembly lines).

---

## 4. FINAL DUNGEON: RHINE LAB HEADQUARTERS
**Structure:** The crown jewel of Columbia. A sprawling, ruined corporate skyscraper complex surrounding the broken "Star-Piercer" launchpad at the center of a crater.
**Atmosphere:** Cold, metallic, high-tech decay. Defended by rogue automated systems.

*   **Dungeon Mechanics:**
    *   **Mobs:** Rogue Columbian Power Armor (Heavy bruisers, high physical resistance) and Rhine Security Drones (Airborne, shoots armor-draining lasers).
    *   The player must scale the ruined skyscraper to reach the Director's Office at the top.
*   **The Reward: Originium Refinement**
    *   At the top, there is no traditional boss. Instead, the player finds the **Rhine Lab Archival Mainframe**.
    *   The player must insert the items gathered from the previous dungeons (Mansfield Key + Dorothy's Processor) and pump massive amounts of power into the mainframe to reboot it.
    *   *Final Loot:* **Blueprint: Originium Refinement**.
    *   *Progression Unlocked:* This blueprint item is required to safely construct the industrial machinery needed to refine Raw Originium into dusts and ingots, officially unlocking the path to the next major technological tier and allowing travel to the Ursus region.

---