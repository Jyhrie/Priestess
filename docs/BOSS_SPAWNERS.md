# Boss spawners

The two summoning altars, how they are drawn, and how to add a third.

For what the altars *do* — the catalyst, the clearance check, the re-arm poll — the code is the
reference: `block/BossSummonerBlock.java` and `block/entity/BossSummonerBlockEntity.java` are
both written to be read. **This file is about the model and the rendering**, which is the part
that is not obvious from the code alone.

---

## Contents

- [Do these need to be GeckoLib?](#do-these-need-to-be-geckolib)
- [How it is put together](#how-it-is-put-together)
- [The files](#the-files)
- [The model's coordinate frame](#the-models-coordinate-frame)
- [Two textures per altar](#two-textures-per-altar)
- [The spin](#the-spin)
- [Adding an altar](#adding-an-altar)
- [Giving an altar its own shape](#giving-an-altar-its-own-shape)
- [Adding real animations](#adding-real-animations)
- [Known gaps](#known-gaps)
- [Checking it worked](#checking-it-worked)

---

## Do these need to be GeckoLib?

**If they were static: no, and GeckoLib would be the wrong choice.** A complex static shape is
better served by an ordinary vanilla JSON block model with many elements.

| | vanilla JSON block model | GeckoLib block entity renderer |
|---|---|---|
| render cost | baked into the chunk mesh once — effectively free | re-rendered every frame, per block, on the render thread |
| animation | none, ever | keyframed clips or procedural bone writes |
| lighting and AO | full vanilla treatment | GeckoLib's own, and not identical |
| culling | vanilla occlusion culling applies | drawn whenever the block entity is in range |
| authoring | Blockbench → *Java Block/Item* export | Blockbench → *Bedrock / GeckoLib* export |
| shape beyond the block | awkward; needs neighbouring blocks | trivial |

On a static model, vanilla wins on every row that matters. The rule is simple: **GeckoLib earns
its place when the thing moves, and not before.**

### Why it is justified here

**These do move.** The core turns above the rim while the altar is armed, and stops dead when it
is spent — see [the spin](#the-spin). That is the second half of the armed/spent read, and it is
the more legible half: motion catches the eye across a dark room far more reliably than a colour
change does.

The render cost argument is also close to moot in this particular case. It scales with how many
of these blocks are in view, and boss altars are one per dungeon per world — you will realistically
never have more than one on screen. That would not be true of, say, a decorative pipe.

### If you want them static after all

Then move them back to vanilla JSON, and it is a real simplification: delete
`client/BossSummonerModel.java` and `client/BossSummonerRenderer.java`, drop the
`registerBlockEntityRenderer` line in `PriestessClient`, drop `GeoBlockEntity` from
`BossSummonerBlockEntity`, set `getRenderShape` back to `RenderShape.MODEL`, and build the shape
as elements in `ModBlockStateProvider.summoner` (or export it from Blockbench as a Java block
model and reference the file). The block entity keeps working untouched — it is the re-arm
poll and has nothing to do with rendering.

---

## How it is put together

```
BossSummonerBlock            getRenderShape → INVISIBLE, so no cube is drawn
   │
   ├── BossSummonerBlockEntity      implements GeoBlockEntity — holds the animation cache
   │
   └── blockstate → *_particles.json    a model with a particle texture and no elements
                                        (break particles only — see below)

PriestessClient.registerRenderers
   └── BossSummonerRenderer     extends GeoBlockRenderer, bound to the block entity type
          └── BossSummonerModel      picks geometry and texture off the block state
```

**One renderer and one model class cover both altars**, and would cover a dozen. Everything they
need is read off the block entity at render time rather than passed in: geometry from
`BossSummonerBlock.modelName()`, texture from the block's registry name plus `ARMED`. That is the
same trick `PriestessGeoRenderer` plays for the fourteen GeckoLib mobs, with the difference that
here the name comes from the world, because one block entity type serves both blocks.

### The blockstate model still matters

`RenderShape.INVISIBLE` means no geometry is drawn from the baked model — but the blockstate must
still point at *a* model, because that is where **break and landing particles** take their
texture. Without one they come out as missing-texture chequerboard.

Hence `particlesOnly()` in `ModBlockStateProvider`: a model with a `particle` texture and no
elements. No faces to draw, one texture to sample.

### The item is a separate thing

An item model is not affected by the block's render shape, so the altar in your hand and in the
inventory is still an ordinary cube using the 16×16 tile. It has to be — an altar that was
invisible in the inventory would be unplaceable in practice.

So each altar has **two representations** and both are real:

| | drawn by | texture |
|---|---|---|
| in the world | `BossSummonerRenderer` | 128×128 UV sheet in `textures/block/boss_summoner/` |
| in the inventory, and its particles | vanilla | 16×16 tile at `textures/block/<name>.png` |

---

## The files

| File | What it is |
|---|---|
| `block/BossSummonerBlock.java` | `getRenderShape` → `INVISIBLE`, and `modelName()` |
| `block/entity/BossSummonerBlockEntity.java` | `implements GeoBlockEntity`, animation cache, no-op controller |
| `client/BossSummonerModel.java` | resolves geo + texture per altar |
| `client/BossSummonerRenderer.java` | `GeoBlockRenderer`, and the spin |
| `client/PriestessClient.java` | one `registerBlockEntityRenderer` line |
| `datagen/ModBlockStateProvider.java` | `summoner()` — particle models + the cube item model |
| `assets/priestess/geo/block/boss_summoner.geo.json` | **generated** — the shared altar shape |
| `assets/priestess/textures/block/boss_summoner/*.png` | **generated** — four 128×128 sheets |
| `tools/generate_placeholder_models.py` | `BLOCK_ROSTER` and `altar()` — the shape's source |
| `tools/generate_placeholder_art.py` | `BOSS_ALTAR_MODELS` — the sheets' source |

**The two generated asset sets are outputs, not sources.** Edit the Python and re-run; a hand-edit
to the `.geo.json` is gone the next time anyone regenerates.

```
python tools/generate_placeholder_models.py
python tools/generate_placeholder_art.py
```

Both are idempotent and seeded, so re-running produces byte-identical files and touches nothing
else in the roster.

---

## The model's coordinate frame

**This is the one thing about block models that is different from mob models, and it is easy to
get wrong.**

`GeoBlockRenderer` puts the model origin at the block's **centre, on the floor**. So a block
model runs:

```
x   -8 .. +8      (centred)
z   -8 .. +8      (centred)
y     0 .. 16     (up from the floor)
```

A mob model, by contrast, is centred on x and z but its y=0 is the mob's feet with nothing above
it in particular. Anything outside the box above hangs into the neighbouring block — fine for an
altar standing alone in an arena, wrong in a corridor. The generated shape stays inside it:

| Bone | y range | What it is |
|---|---|---|
| `base` | 0–2 | full 16-wide plinth, so it still reads as filling its square from above |
| `pillar` | 2–8 | the waist |
| `rim` | 8–10 | 14-wide lip |
| `core` | 10–16 | the floating cube that spins |
| `prong_nw/ne/sw/se` | 10–15 | splayed outward, so the rim reads as holding the core |

`core` is a **root bone**, not a child of `rim`, so the renderer can spin it without dragging the
prongs round with it.

---

## Two textures per altar

```
textures/block/boss_summoner/jesselton_projector.png          armed
textures/block/boss_summoner/jesselton_projector_spent.png    spent
textures/block/boss_summoner/dorothys_terminal.png
textures/block/boss_summoner/dorothys_terminal_spent.png
```

**The subfolder is load-bearing.** `textures/block/jesselton_projector.png` already exists and is
a different thing — the 16×16 tile for the item model and the particles. These are 128×128 packed
UV sheets whose coordinates mean nothing except against `boss_summoner.geo.json`. Putting them in
the same folder would have meant one clobbering the other.

`BossSummonerModel.getTextureResource` derives the path from the block's registry name and its
`ARMED` value, so **a new altar gets its textures picked up automatically** — there is no list to
add to.

Each altar keeps the palette of its 16×16 tile, so the two representations read as the same block.
Spent is the same hue crushed dark with the accent nearly gone.

---

## The spin

The core turns at 24°/second while armed, and holds still once spent.

**It is code, not a keyframed clip.** A constant rotation is one line of arithmetic; an
`.animation.json` would be another file to keep in step with the model for the same result. So
`BossSummonerRenderer.preRender` writes the bone rotation directly:

```java
getGeoModel().getBone("core")
        .ifPresent(bone -> bone.setRotY((float) Math.toRadians(degrees)));
```

`getBone` returns an `Optional`, and an absent one is handled rather than thrown — a hand-drawn
replacement model that renamed the bone should stop spinning, not crash.

Driven off `RenderUtils.getCurrentTick()` plus the partial tick, so it is smooth between ticks.
There is **no per-altar phase offset**, so a row of them turns in lockstep. That is deliberate —
they are meant to read as one installation — and this is the place to change it if it ever looks
wrong.

---

## Standing down while the fight is on

**A spent altar is gone.** Not dark — gone. No geometry, no collision, no selection outline, and
not an obstacle as far as pathfinding is concerned. It comes back the moment the boss dies.

The reason is the fight. A one-block plinth in the middle of an arena is something the boss's
pathfinder has to solve around for the whole encounter, and the altar you summoned it from is a
poor thing for a boss to get stuck on.

**Three independent systems have to be told, and changing one without the others is the usual
bug:**

| Concern | Where | Armed | Spent |
|---|---|---|---|
| what bumps into it | `getCollisionShape` | full cube | empty |
| how the A* graph is built | `isPathfindable` | `false` (obstacle) | `true` (passable) |
| what the cursor can hit | `getShape` | full cube | empty |
| what is drawn | `BossSummonerRenderer.shouldRender` | the model | nothing |

An empty collision shape **alone is not enough**: the pathfinder builds its graph from
`isPathfindable`, so a mob would still refuse to plot a route through a block it could physically
walk into. Equally, `isPathfindable` alone would leave a solid invisible cube in the arena.

The block itself is still there throughout — it has to be, because its block entity is what
polls for the boss and re-arms it.

### It cannot be broken while spent

An empty `getShape` means no ray-trace hit, so a spent altar cannot be right-clicked or mined.
That is safe rather than a trap: the re-arm poll asks "is the boss there" rather than "did it
die", which has no failure mode that survives the next second, so the block always comes back on
its own. Worst case is a five-second wait.

### Why the render skip is `shouldRender`

`GeoBlockRenderer` declares `render` with the raw `BlockEntity` parameter rather than with its own
type variable, so **any `render` signature a subclass writes is a name clash rather than an
override** — in either direction. `shouldRender` is a `BlockEntityRenderer` default that GeckoLib
leaves alone, the dispatcher consults it before calling `render`, and it therefore skips strictly
more work. The vanilla distance test it replaces is restored by hand, because the interface
default cannot be reached with `super` from here.

### `noOcclusion()` is load-bearing

Both altars are registered with `.noOcclusion()`, and it is not tidiness.

Occlusion is decided **separately from rendering**. Left occluding, a neighbouring block culls the
face it shares with the altar — and because the altar's baked model is `INVISIBLE` and its
GeckoLib model has a narrow waist, you would see straight through the wall at that height. Every
BER-rendered vanilla block, chests included, sets this for the same reason.

---

## Adding an altar

Adding a third boss altar touches nothing in the rendering path. In full:

1. **The block** — a subclass of `BossSummonerBlock` in `block/boss_summons/`, answering
   `boss()`, `catalyst()`, `summonParticle()`, `summonSound()`, and `clearanceFor()` if the boss
   cannot walk out of a tight spot.
2. **Register** it in `ModBlocks`, copying `Blocks.LODESTONE` and
   `.lightLevel(BossSummonerBlock::glow)`.
3. **Add it to the block entity type** — `ModBlockEntities.BOSS_SUMMONER`'s `Builder.of(...)`
   takes a varargs list of valid blocks, and a block entity attached to a block not in that list
   is dropped on load.
4. **Two texture entries** in `generate_placeholder_art.py`'s `BOSS_ALTAR_MODELS`, named
   `<block_id>` and `<block_id>_spent`, plus the two 16×16 tiles in `BLOCKS` for the item.
5. **One line** in `ModBlockStateProvider` (`summoner(ModBlocks.YOUR_ALTAR)`), one in
   `ModLootTableProvider` (`dropSelf`), one in `ModLanguageProvider`.
6. `python tools/generate_placeholder_art.py && gradlew runData`

**No renderer change, no model change, no `PriestessClient` change.** The renderer is bound to the
block entity type, and the model resolves everything by name.

---

## Giving an altar its own shape

Both altars currently share one model, because they differ by texture rather than by geometry —
the same arrangement the cube tiles had. To split them:

1. Add an entry to `BLOCK_ROSTER` in `generate_placeholder_models.py`. The `altar()` helper takes
   `core_size` and `prong`, so a variation is one line; a genuinely different silhouette is a
   bone list written out longhand, like `sv_bishop_quintus` in the entity roster.
2. Override `modelName()` in the block subclass to return the new file's name.

That is all — `BossSummonerModel` reads `modelName()` per block at render time.

---

## Adding real animations

The spin covers a constant motion. **Events** — a flourish as the boss comes up, a shudder as the
altar re-arms — are what keyframed clips are actually for, and they are not wired up:

1. Write `assets/priestess/animations/block/boss_summoner.animation.json` (Blockbench, *Animate*
   tab, export as GeckoLib animation).
2. Point `BossSummonerModel.getAnimationResource` at it — it currently returns `null`, which is
   the supported way to say "static geometry".
3. Replace the no-op controller in `BossSummonerBlockEntity.registerControllers` with one that
   plays clips.
4. Trigger from the server with `triggerAnim(...)`, which `GeoBlockEntity` provides — a summon is
   a server-side event and the animation has to be told to the client.

---

## Known gaps

- **The armed collision box is still a full cube.** A spent altar has no collision at all, but an
  armed one is a solid metre cube while the model has a narrow waist between the rim and the
  plinth, so you bump into air either side of it. Matching `getShape`/`getCollisionShape` to the
  silhouette would fix it; it was left alone because a placeholder shape is not worth pinning
  collision to.
- **The spent textures are currently unreachable.** `BossSummonerModel` still resolves
  `<name>_spent.png`, and `generate_placeholder_art.py` still emits it, but nothing draws a spent
  altar any more. Kept because turning spent rendering back on is a one-line change in
  `shouldRender` — if you decide it should stay visible-but-passable, the art is already there.
- **No facing.** `GeoBlockRenderer.rotateBlock` will orient a model from a horizontal-facing
  blockstate property, and the altars have none, so every one of them faces the same way. Adding
  `HORIZONTAL_FACING` and setting it in `getStateForPlacement` would make them face the player who
  placed them.
- **Placeholder art.** The shape is generated geometry and the textures are seeded noise. Both are
  meant to be replaced by Blockbench work; see [the files](#the-files) for which script owns which.
- **No emissive layer.** The altars light their surroundings via `lightLevel`, but the model
  itself is not drawn glowing while armed. That wants a GeckoLib `AutoGlowingGeoLayer` and an
  emissive texture.

---

## Checking it worked

```
python tools/generate_placeholder_models.py
python tools/generate_placeholder_art.py
gradlew runData && gradlew runClient
```

Then:

1. `/give @s priestess:jesselton_projector` — it should look like an ordinary cube **in the
   inventory and in your hand**. That is correct; the cube is the item model.
2. Place it. It should now be a plinth, a waist, a rim and a floating cube — **not** a cube.
3. The core should turn slowly. Both altars should turn in step.
4. Right-click holding `priestess:tarnished_dog_tags`. The boss comes up and **the altar
   disappears completely** — no model, no outline when you point at it, and you can walk through
   the space it occupied.
5. Check the boss does not path around the empty square. It should walk straight over it.
6. Kill the boss and wait ~5 seconds. The altar reappears, lit, with the core turning again.
7. Break it while armed. Particles should be the altar's own colours, not black-and-magenta
   chequerboard — if they are, the blockstate is not pointing at a model with a `particle`
   texture.
8. Wall one in on all sides and look at the wall. No see-through holes — if there are,
   `noOcclusion()` has gone missing from `ModBlocks`.

If the altar is completely invisible in the world, the renderer is not registered — check the
`registerBlockEntityRenderer` line in `PriestessClient`. If it renders as a missing-texture blob,
the texture path is wrong; the model resolves it from the block's registry name, so a new altar
whose textures are not named after it will land here.

Test
