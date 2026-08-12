# Custom rarities

How to add a rarity tier above vanilla's four, and how to put it on an item.

The worked example is `WeaponRarities.CALAMITOUS` in `weapons/WeaponRarities.java`. For the
tooltip *text* that usually accompanies a rarity, see [TOOLTIPS.md](TOOLTIPS.md) — they are two
separate mechanisms and confusing them is the most common mistake here.

---

## Contents

- [What a rarity actually does](#what-a-rarity-actually-does)
- [Vanilla's four](#vanillas-four)
- [Creating one](#creating-one)
- [The style-modifier overload](#the-style-modifier-overload)
- [Putting it on an item](#putting-it-on-an-item)
- [Rarity that changes at runtime](#rarity-that-changes-at-runtime)
- [Naming and collisions](#naming-and-collisions)
- [Load order](#load-order)
- [When rarity does nothing](#when-rarity-does-nothing)

---

## What a rarity actually does

**It colours the item's name. That is all.**

Vanilla adds no text, no label, and no extra tooltip line. When you see an item with a coloured
name and a word like *Calamitous* underneath it, those are two unrelated things:

| What you see | Where it comes from |
|---|---|
| the name's colour | the `Rarity` |
| a word naming the tier | a line you added yourself in `appendHoverText` |

Devil's Devastation has both, and they are written in different files. The "Calamitous" line in
its tooltip is hand-added — see [TOOLTIPS.md](TOOLTIPS.md#the-rarity-line). If you create a
rarity and expect its name to appear in game, nothing will show up; you have to write that line.

Mechanically, `ItemStack.getTooltipLines` styles the hover name with the rarity's style, and
nothing else consults it. It has no effect on drops, enchanting, durability or value.

---

## Vanilla's four

| Rarity | Colour |
|---|---|
| `Rarity.COMMON` | white |
| `Rarity.UNCOMMON` | yellow |
| `Rarity.RARE` | aqua |
| `Rarity.EPIC` | light purple |

`ModItems` already uses these — `Rarity.RARE` on the chapter keys, `Rarity.EPIC` on Dreamland
and the blueprint, `Rarity.UNCOMMON` on Medium and the summoning catalysts. **Reach for a custom
rarity only when none of the four fits**, because every one you add is a colour the player has
to learn.

---

## Creating one

`Rarity` is a vanilla enum that Forge patches to implement `IExtensibleEnum`, so new values come
from a factory rather than from a registry. There is no `DeferredRegister` involved.

```java
public final class WeaponRarities {

    public static final Rarity CALAMITOUS =
            Rarity.create("priestess_calamitous", ChatFormatting.GOLD);

    private WeaponRarities() {
    }
}
```

That is the whole thing. Two arguments: a unique name, and the colour.

The colour must be a `ChatFormatting` constant, which limits you to the sixteen classic chat
colours. For anything richer, use the other overload.

---

## The style-modifier overload

The second form takes a `UnaryOperator<Style>` instead of a colour, which lets a rarity do
anything a `Style` can — true RGB, bold, a custom font, even obfuscation:

```java
// A full 24-bit colour rather than one of the sixteen.
public static final Rarity ABYSSAL = Rarity.create("priestess_abyssal",
        style -> style.withColor(TextColor.fromRgb(0x5A2E78)));

// Colour and bold together.
public static final Rarity MYTHIC = Rarity.create("priestess_mythic",
        style -> style.withColor(TextColor.fromRgb(0xFF4BA5)).withBold(true));
```

This is a Forge addition — `Rarity.getStyleModifier()` is what `ItemStack` consults, and the
`ChatFormatting` overload is just a shorthand that builds one of these for you. Prefer it when
you want a colour outside the sixteen; there is no cost to it.

> It cannot animate. A `Style` is computed once per tooltip build from the rarity alone, with no
> access to the item or the tick count. A name that shifts colour over time — like Devil's
> Devastation's — is a `getName` override, not a rarity. See
> [TOOLTIPS.md](TOOLTIPS.md#the-animated-name).

---

## Putting it on an item

Two ways, and they behave differently.

### Fixed, in the item's properties

The normal case. Set it once and forget it:

```java
public static final RegistryObject<Item> YOUR_ITEM = ITEMS.register("your_item",
        () -> new Item(new Item.Properties().stacksTo(1).rarity(WeaponRarities.CALAMITOUS)));
```

This works for any item, including plain `new Item(...)` entries in `ModItems` — no subclass
needed.

### Overridden, in the item class

Needed only if the rarity depends on the stack. Devil's Devastation does this because it was
ported that way:

```java
@Override
public Rarity getRarity(ItemStack stack) {
    return WeaponRarities.CALAMITOUS;
}
```

An override always wins over the properties value, so do not set both — a `Properties.rarity()`
that is silently ignored is a confusing thing to leave behind.

Note that overriding `getRarity` **requires a custom item class**. If your item is a bare
`new Item(...)`, use the properties form.

---

## Rarity that changes at runtime

The `getRarity(ItemStack)` signature takes the stack, so rarity can depend on the item's state:

```java
@Override
public Rarity getRarity(ItemStack stack) {
    return stack.getOrCreateTag().getBoolean("Awakened")
            ? WeaponRarities.CALAMITOUS
            : Rarity.RARE;
}
```

Two cautions:

- **Read only.** `getRarity` is called every time a tooltip is built, which is every frame the
  item is hovered. Do not write NBT, roll randomness or touch the world from it.
- **Enchanted items are already special-cased.** Vanilla bumps an enchanted item's rarity one
  step on its own (`Item.getRarity` checks `stack.isEnchanted()`). If you override without
  calling `super`, you lose that — usually fine for a weapon that has its own tier, worth
  knowing for anything else.

---

## Naming and collisions

**Prefix the name.** The string passed to `Rarity.create` is a global identifier shared with
every other mod in the pack, and two mods creating `"calamitous"` is a hard crash on startup.

That is why the port uses `priestess_calamitous` rather than Lethality's `calamitous` — the two
mods can now be installed together. The constant is still called `CALAMITOUS`; only the wire
name is prefixed, and nothing in game shows that string.

| | |
|---|---|
| ✅ | `Rarity.create("priestess_calamitous", …)` |
| ❌ | `Rarity.create("calamitous", …)` |

---

## Load order

Extensible enum values should be created **during mod loading**, not lazily on first use. The
values array is appended to when the holding class is first classloaded, and the earlier that
happens the fewer surprises there are.

`WeaponRarities` is currently loaded lazily — nothing touches it until
`DevilsDevastationItem.getRarity` runs for the first time, which can be long after startup. That
works today, but if you add more rarities it is worth pinning down explicitly by touching the
class during construction:

```java
// in Priestess()
WeaponRarities.init();   // an empty static method, called only to force classload
```

with a matching no-op in the holder:

```java
/** Forces classload so the rarities are created during mod loading rather than on first use. */
public static void init() {
}
```

Cheap insurance, and it makes the dependency explicit rather than incidental.

---

## When rarity does nothing

| Symptom | Cause |
|---|---|
| Name is not coloured at all | the item overrides `getName` and paints its own style — that wins. See [TOOLTIPS.md](TOOLTIPS.md#the-animated-name) |
| The rarity's *name* never appears in the tooltip | expected — vanilla shows no rarity text. Add the line yourself |
| Colour is right but you wanted bold | `ChatFormatting` overload only sets colour; use the [style-modifier overload](#the-style-modifier-overload) |
| Crash on startup, duplicate enum constant | two mods created the same rarity name — [prefix yours](#naming-and-collisions) |
| `Properties.rarity()` seems ignored | the item class also overrides `getRarity`, which wins |
| Enchanting no longer bumps rarity | you overrode `getRarity` without calling `super.getRarity(stack)` |
