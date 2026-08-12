# Tooltips

How the long, multi-line "hold Shift for details" tooltips work, and how to put one on any item.

The worked example is `DevilsDevastationItem.appendHoverText`. For the *colour of the item's
name*, which is a separate mechanism, see [RARITIES.md](RARITIES.md).

---

## Contents

- [Anatomy of the tooltip](#anatomy-of-the-tooltip)
- [The hook](#the-hook)
- [Where the text lives](#where-the-text-lives)
- [Colour codes](#colour-codes)
- [Hold Shift to expand](#hold-shift-to-expand)
- [Putting one on a plain item](#putting-one-on-a-plain-item)
- [A reusable tooltip item](#a-reusable-tooltip-item)
- [The rarity line](#the-rarity-line)
- [The animated name](#the-animated-name)
- [Long text and wrapping](#long-text-and-wrapping)
- [When it does not work](#when-it-does-not-work)

---

## Anatomy of the tooltip

Devil's Devastation's tooltip, top to bottom, and where each part comes from:

```
Devil's Devastation          ← getName override (animated gradient)
Calamitous                   ← appendHoverText, line 1 (hand-written, not the Rarity)
                             ← appendHoverText, a literal " "
« ...And you shall burn them all. »
                             ← another " "
Hold Shift for details       ← appendHoverText, the collapsed branch
                             ─────────────────────────────────────
16 Attack Damage             ← vanilla, from the item's attributes
1.6 Attack Speed             ← vanilla
```

**Everything above the line is yours; everything below is vanilla.** `appendHoverText` inserts
between the name and the attribute block, which is why the flavour text sits where it does and
why you cannot push a line below the damage numbers without more work.

---

## The hook

```java
@Override
public void appendHoverText(ItemStack stack, @Nullable Level level,
                            List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("tooltip.priestess.your_item.flavour"));
    super.appendHoverText(stack, level, tooltip, flag);
}
```

Each `Component` you add is one line. There is no wrapping — see
[below](#long-text-and-wrapping).

Four things worth knowing:

- **`level` is nullable.** The tooltip is built in places with no world (the creative search
  screen, a recipe viewer). Null-check it before touching it.
- **`flag.isAdvanced()`** is F3+H mode. Use it to hide developer detail behind that toggle.
- **This is client-side in practice**, but it is not annotated as such, so do not reach for
  `Minecraft.getInstance()` without going through something dist-safe.
- **Call `super`.** It is cheap and some parent classes add lines of their own.

---

## Where the text lives

Not in the Java. Every string goes through `ModLanguageProvider` so it can be translated:

```java
// datagen/ModLanguageProvider.java
add("tooltip.priestess.your_item.flavour", "§8« Some flavour text. »");
add("tooltip.priestess.your_item.detail",  "§7What it actually does.");
```

Then `gradlew runData`. The convention already in use is:

```
tooltip.priestess.<item_id>.<part>
```

`Component.translatable(key)` looks the key up; `Component.literal(text)` does not and should be
used only for text that is genuinely not language — a blank spacer, or a number.

> A missing key renders as the raw key string in game (`tooltip.priestess.your_item.flavour`).
> That is your signal that you forgot `runData`.

---

## Colour codes

The `§` codes are the simplest way to style tooltip text, and they are what the ported strings
use.

| Code | Colour | Code | Colour |
|---|---|---|---|
| `§0` | black | `§8` | dark grey |
| `§1` | dark blue | `§9` | blue |
| `§2` | dark green | `§a` | green |
| `§3` | dark aqua | `§b` | aqua |
| `§4` | dark red | `§c` | red |
| `§5` | dark purple | `§d` | light purple |
| `§6` | gold | `§e` | yellow |
| `§7` | grey | `§f` | white |

Plus `§l` bold, `§o` italic, `§n` underline, `§m` strikethrough, `§k` obfuscated, `§r` reset.

Codes persist until reset or end of line, so `"§6On Swing —"` colours the whole line.

The alternative, for anything conditional, is `Style`:

```java
tooltip.add(Component.translatable("tooltip.priestess.your_item.detail")
        .withStyle(style -> style.withColor(TextColor.fromRgb(0x5FC8E8)).withItalic(true)));
```

Use `§` codes for static text in the lang file; use `Style` when the colour depends on something.

---

## Hold Shift to expand

The whole mechanism is one `if`. `Screen.hasShiftDown()` is a static poll of the keyboard, so
there is no event or state to manage:

```java
@Override
public void appendHoverText(ItemStack stack, @Nullable Level level,
                            List<Component> tooltip, TooltipFlag flag) {

    tooltip.add(Component.translatable("tooltip.priestess.your_item.flavour"));
    tooltip.add(Component.literal(" "));            // blank spacer line

    if (Screen.hasShiftDown()) {
        tooltip.add(Component.translatable("tooltip.priestess.your_item.on_hit"));
        tooltip.add(Component.translatable("tooltip.priestess.your_item.on_hit_detail"));
    } else {
        tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
    }

    super.appendHoverText(stack, level, tooltip, flag);
}
```

**`tooltip.priestess.hold_shift` already exists** — reuse it rather than adding a second copy.

`Screen` also offers `hasControlDown()` and `hasAltDown()`, so a second tier is possible. If you
use one, **make the prompt name the right key** — the ported code had a `hold_ctrl` key next to
a `hasShiftDown()` check, which is the kind of mismatch nobody notices for a year.

> `Screen` is a client class. Referencing it from `appendHoverText` is standard and safe, because
> the method only ever runs while a tooltip is being drawn. Do not call it from anywhere that
> could run on a dedicated server.

---

## Putting one on a plain item

Most of `ModItems` is bare `new Item(new Item.Properties()…)`, which has nowhere to put an
override. You have two options.

**One-off:** write a small item class.

```java
public class DreamlandItem extends Item {
    public DreamlandItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.priestess.dreamland.flavour"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
```

```java
public static final RegistryObject<Item> DREAMLAND = ITEMS.register("dreamland",
        () -> new DreamlandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
```

**More than one-off:** use the reusable class below instead, so ten items do not become ten
near-identical files.

---

## A reusable tooltip item

If several `ModItems` entries want tooltips, this is worth adding once — it derives its keys from
the item's own registry name, so registering it is the only step.

```java
package com.jyhrie.priestess.item;

/**
 * An item with a flavour line and an optional Shift-to-expand detail block.
 *
 * <p>Keys are derived from the registry name, so an item registered as {@code dreamland} reads:
 * <ul>
 *   <li>{@code tooltip.priestess.dreamland.flavour} — always shown</li>
 *   <li>{@code tooltip.priestess.dreamland.detail.0}, {@code .1}, … — shown while Shift is held</li>
 * </ul>
 * Pass {@code detailLines = 0} for an item that has flavour text and nothing to expand.
 */
public class TooltipItem extends Item {

    private final int detailLines;

    public TooltipItem(Properties properties, int detailLines) {
        super(properties);
        this.detailLines = detailLines;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String base = "tooltip.priestess." + ForgeRegistries.ITEMS.getKey(this).getPath();

        tooltip.add(Component.translatable(base + ".flavour"));

        if (detailLines > 0) {
            tooltip.add(Component.literal(" "));
            if (Screen.hasShiftDown()) {
                for (int i = 0; i < detailLines; i++) {
                    tooltip.add(Component.translatable(base + ".detail." + i));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.priestess.hold_shift"));
            }
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}
```

Then in `ModItems`:

```java
public static final RegistryObject<Item> DREAMLAND = ITEMS.register("dreamland",
        () -> new TooltipItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));
```

and in `ModLanguageProvider`:

```java
add("tooltip.priestess.dreamland.flavour",  "§8« A place that was never built. »");
add("tooltip.priestess.dreamland.detail.0", "§7Dropped by \"Awaken\".");
add("tooltip.priestess.dreamland.detail.1", "§7Nothing consumes it yet.");
```

Adding a tooltip to another item is then one constructor swap and three lang lines.

---

## The rarity line

The word *Calamitous* under Devil's Devastation's name is **not** produced by its `Rarity`.
Vanilla renders no rarity text at all — the rarity only colours the name. That line is added by
hand, as the first thing in `appendHoverText`:

```java
tooltip.add(WeaponText.gradient(Component.literal("Calamitous"), 0.25F, 5.0F, /* palette */));
```

So a tier label is always two pieces of work: the [rarity](RARITIES.md) for the name colour, and
a tooltip line for the word. If you want the label without the animation, a plain component is
enough:

```java
tooltip.add(Component.literal("Calamitous").withStyle(ChatFormatting.GOLD));
```

---

## The animated name

Devil's Devastation's name shimmers because it overrides `getName` and rebuilds the string as
one coloured component per character, sampling a palette at a position that advances with the
tick count:

```java
@Override
public Component getName(ItemStack stack) {
    return WeaponText.gradient(Component.translatable(this.getDescriptionId(stack)),
            0.25F,                      // speed: cycles per second
            2.0F,                       // spread: how much palette is visible at once
            new int[]{255, 254, 251},   // each int[] is one RGB stop
            new int[]{255, 0, 0},
            new int[]{255, 0, 255},
            new int[]{255, 254, 251})   // repeat the first stop to close the loop
            .withStyle(ChatFormatting.BOLD);
}
```

`WeaponText.gradient` also has a form taking a `ResourceLocation` font, used for the rarity line.

Three things to know before using it elsewhere:

- **It overrides the rarity colour.** Whatever style the gradient applies wins, so an item with
  an animated name shows no rarity colour. That is fine — but do not then wonder why the rarity
  is not working.
- **Close the loop.** Repeat the first colour as the last stop, or the cycle jumps at the wrap.
- **It costs a component per character.** Fine for one item name. Do not gradient a paragraph.

`WeaponText` lives in `weapons/`, which is meant to be a deletable compartment. If you want
gradient names on non-weapon items, move `WeaponText` up to a shared package first rather than
importing across the boundary — see [LETHALITY WEAPONS.md](LETHALITY%20WEAPONS.md#where-everything-lives).

---

## Long text and wrapping

**Minecraft does not wrap tooltips.** One `Component` is one line, however long, and a long line
pushes the tooltip off the screen edge.

Split it yourself, one key per line:

```java
add("tooltip.priestess.your_item.detail.0", "§7The first half of the sentence,");
add("tooltip.priestess.your_item.detail.1", "§7and the second half.");
```

Rules of thumb:

- **Keep lines under about 45 characters.** Longer than that and the tooltip starts crowding the
  inventory on smaller GUI scales.
- **Blank spacers are `Component.literal(" ")`** — a space, not an empty string. An empty
  component can be collapsed and gives you nothing.
- **Do not translate the spacers.** They are layout, not language.

If you genuinely need wrapping, `Minecraft.getInstance().font.split(component, width)` returns a
`List<FormattedCharSequence>` — but that is a client-only call and does not fit the
`List<Component>` this method wants, so it means building the tooltip a different way. For
almost everything, splitting by hand in the lang file is the right answer.

---

## When it does not work

| Symptom | Cause |
|---|---|
| Tooltip shows the raw key (`tooltip.priestess.…`) | key missing from `ModLanguageProvider`, or you forgot `gradlew runData` |
| No tooltip at all on a `ModItems` entry | it is a bare `new Item(...)` with nothing to override — see [above](#putting-one-on-a-plain-item) |
| Shift does nothing | prompt says Shift but the code checks `hasControlDown()`, or vice versa |
| Text runs off the screen | no wrapping exists; split it into more keys |
| Blank line does not appear | used `Component.empty()` or `""` instead of `Component.literal(" ")` |
| Lines appear below the damage numbers | they cannot — `appendHoverText` always inserts above the attribute block |
| Rarity colour is gone | the item overrides `getName` with a gradient, which wins |
| Gradient jumps at the loop point | first colour not repeated as the last stop |
| Crash on a dedicated server | something called `Screen` or `Minecraft` outside a tooltip path |
