# Curios and the Module slot

How a wearable accessory works in this mod: the **Module** slot, the **Template** module that
occupies it, and how to add more of either.

The worked example is `TemplateModuleItem` — it equips into the Module slot and does nothing,
which is the point. Everything about *making it wearable* is the part worth copying.

---

## The one thing to understand first

**Which slot an item fits is not decided in Java.** It comes from an item tag named
`curios:<slot id>`, and Curios' `tag` validator on the slot reads it.

```
TemplateModuleItem  ──registered──►  priestess:template
                                            │
                    ModItemTagsProvider ──adds to──►  curios:module   ◄── the Module slot's
                                                                          validator reads this
```

So a curio is three separate things, and **the two that make it wearable both fail silently**:

| | Where | Skip it and… |
|---|---|---|
| The item | `ModItems.java` | — |
| Its slot | `ModItemTagsProvider.java` | it is a normal inventory item, equippable nowhere |
| The slot existing at all | `ModCuriosDataProvider.java` | the slot never appears in the GUI |

Neither omission produces an error, in the log or anywhere else. If a curio "doesn't work",
one of the bottom two rows is why.

---

## The Module slot

| | |
|---|---|
| Slot id | `module` |
| Item tag | `curios:module` — **the `curios` namespace, not `priestess`** |
| Order | 100 |
| Icon | `assets/priestess/textures/slot/empty_module_slot.png` |
| Granted to | players, via `data/priestess/curios/entities/module_wearers.json` |
| Defined in | `datagen/ModCuriosDataProvider.java` |

Curios ships definitions for its own slot types — ring, belt, charm, necklace — but assigns
none of them to any entity. **Until a mod asks for a slot, it does not exist in the GUI.**
`ModCuriosDataProvider` is that request. This mod asks only for Module: naming Curios' built-in
slots there would put empty ring and belt slots in front of a player who has no use for them.

`order` is the left-to-right position in the GUI and only means anything relative to other
slots. 100 leaves room either side for later ones without renumbering.

**The `curios:tag` validator is not optional.** Without it the slot accepts any item at all,
dirt included — a failure that looks like a slot which works.

### Why the tag is in the `curios` namespace

We define the Module slot, so the slot definition is ours and lives in
`data/priestess/curios/slots/module.json`. But the tag its validator reads belongs to Curios
and always lands in `data/curios/tags/items/module.json`. We are adding entries to another
mod's tag. `ModTags.Items.CURIOS_MODULE` wraps this so it is never spelled out by hand.

---

## Adding a module

Six steps. Only 1 and 2 are curio-specific; the rest are what any item needs.

**1. Write the item class** — implement `ICurioItem`.

```java
public class MyModuleItem extends Item implements ICurioItem {
    public MyModuleItem(Properties properties) { super(properties); }
}
```

Every `ICurioItem` method has a default, so you override only what you want:

| Hook | Purpose |
|---|---|
| `getAttributeModifiers` | attributes applied while worn |
| `canEquipFromUse` | right-click in hand to equip, instead of opening the GUI |
| `getEquipSound` | sound on equip |
| `curioTick` | every tick while worn |
| `onEquip` / `onUnequip` | one-shot effects |

For a module that only carries flat stats, `TemplateModuleItem` is a complete skeleton —
copy it and fill in `getAttributeModifiers`.

**2. Register and tag it.**

```java
// item/ModItems.java
public static final RegistryObject<Item> HAZMAT_WEAVE = ITEMS.register("hazmat_weave",
        () -> new MyModuleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

// datagen/ModItemTagsProvider.java  ← the step that makes it wearable
tag(ModTags.Items.CURIOS_MODULE)
        .add(ModItems.HAZMAT_WEAVE.get());
```

`stacksTo(1)` is not enforced by Curios but is strongly conventional — a stack of 64 modules
in one slot means nothing.

**3. Texture** — `assets/priestess/textures/item/hazmat_weave.png`, 16×16. For a placeholder,
add it to `ITEMS` in `tools/generate_placeholder_art.py` and re-run that script.

**4. Model** — `basicItem(ModItems.HAZMAT_WEAVE.get());` in `ModItemModelProvider`. A curio
needs no special model; Curios draws the worn item from the ordinary one.

**5. Name it** — `add(ModItems.HAZMAT_WEAVE.get(), "Hazmat Weave");` in `ModLanguageProvider`.

**6.** `./gradlew runData`

It joins the Priestess creative tab automatically, like every other entry in `ModItems.ITEMS`.

### Attributes, and the UUID trap

```java
@Override
public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext,
                                                                    UUID uuid, ItemStack stack) {
    Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
    modifiers.put(Attributes.ARMOR,
            new AttributeModifier(uuid, "Hazmat weave armor", 2.0D, AttributeModifier.Operation.ADDITION));
    return modifiers;
}
```

**Use the `uuid` you are handed, never a constant.** Curios passes a UUID that is unique per
slot, which is what lets the same accessory worn in two slots apply twice. A constant makes the
second copy silently replace the first — and the symptom, "two of them only count once", points
nowhere near the cause.

---

## Adding a slot

Only needed when no existing slot fits. Four things.

**1. The tag key** — `item/ModTags.java`

```java
public static final TagKey<Item> CURIOS_IMPLANT = curios("implant");
```

**2. The slot, and granting it** — `datagen/ModCuriosDataProvider.java`

```java
this.createSlot("implant")
        .order(110)
        .icon(new ResourceLocation(Priestess.MOD_ID, "slot/empty_implant_slot"))
        .addValidator(new ResourceLocation("curios", "tag"));
```

then add `"implant"` to the `addSlots(...)` list in `createEntities("module_wearers")`. A slot
that is defined but not granted to anybody never appears.

**3. The icon** — `assets/priestess/textures/slot/empty_implant_slot.png`, 16×16 RGBA,
greyscale by convention. Add it to `SLOTS` in `tools/generate_placeholder_art.py` for a
placeholder; `slot_texture` drops the alpha so the icon reads as a hint rather than as an item
already sitting in the slot.

**4. Two language keys** — `datagen/ModLanguageProvider.java`

```java
add("curios.identifier.implant", "Implant");
add("curios.modifiers.implant", "When worn as an implant:");
```

Curios ships these for its own slot types but cannot know about ours. `identifier` labels the
slot in the GUI; `modifiers` is the header above the attribute list in a worn item's tooltip.
Miss either and the raw key shows on screen.

### Resizing a slot Curios already owns

```java
this.createSlot("ring").size(4).operation("SET");
```

`replace()` is left false, so this **merges into** the existing definition rather than
overwriting it — ring keeps its own icon, order and validator, and only the size changes. Spell
out `SET`; the alternative, `ADD`, stacks on top of the built-in size of 1 and gives you one
too many.

---

## Testing

```
/give @s priestess:template
```

Then open the Curios GUI — the button sits on the left of the inventory screen. Module should
be there with the chip icon in it; the Template should drop in, and right-clicking it in hand
should equip it too.

| Symptom | Cause |
|---|---|
| Item exists but will not equip | not in the `curios:module` tag, or `runData` not re-run |
| The slot is not in the GUI at all | slot id missing from `addSlots(...)`, or nothing granted it |
| The slot accepts anything, including dirt | `curios:tag` validator missing from the slot |
| Empty slot shows a missing texture | no `textures/slot/empty_<id>_slot.png`, or the name does not match the slot id |
| Slot header reads `curios.identifier.module` | the two `curios.*` language keys are missing |
| Two copies only apply their stats once | a constant UUID instead of the one Curios passes in |
| Attributes never apply | the item is a plain `Item`, not an `ICurioItem` |

---

## Dependency

Curios is **mandatory**, declared in `META-INF/mods.toml` and versioned from
`gradle.properties`. Making it optional would mean guarding every direct reference to the
Curios API behind a `ModList.get().isLoaded("curios")` check.

`build.gradle` compiles against the slim `:api` classifier and pulls the full artifact at
runtime, so Curios' internals stay off the compile classpath. Curios also ships mixins, which
is why dev runs need the SRG→official refmap remap already configured in `build.gradle` for
GeckoLib — the same two properties cover both.

Note `curios_version_range` is `[5,)` rather than the artifact's own `5.14.1+1.20.1`: that
build suffix is not something a Maven range can parse, and Forge refuses to load the mod rather
than complaining about it.
