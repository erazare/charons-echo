package com.charonsecho.gui;

import java.util.LinkedHashSet;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

public final class MenuItem {
    record Entry(ItemStack stack, Consumer<ChestGui> onClick) {}

    private final ItemStack stack;
    private Consumer<ChestGui> onClick = gui -> {};

    public MenuItem(Item item) {
        this(new ItemStack(item));
    }

    private MenuItem(ItemStack stack) {
        this.stack = stack;
    }

    public static MenuItem from(ItemStack stack) {
        return new MenuItem(stack.copy());
    }

    public MenuItem setName(Component name) {
        stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(style -> style.withItalic(false)));
        return this;
    }

    public MenuItem addLoreLine(Component line) {
        stack.update(DataComponents.LORE, ItemLore.EMPTY,
                lore -> lore.withLineAdded(line.copy().withStyle(style -> style.withItalic(false))));
        return this;
    }

    public MenuItem glow() {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    public MenuItem hideDefaultTooltip() {
        var hidden = new LinkedHashSet<>(stack.getComponents().keySet());
        hidden.remove(DataComponents.CUSTOM_NAME);
        hidden.remove(DataComponents.ITEM_NAME);
        hidden.remove(DataComponents.LORE);
        stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hidden));
        return this;
    }

    public MenuItem setCallback(Consumer<ChestGui> action) {
        onClick = action;
        return this;
    }

    Entry entry() {
        return new Entry(icon(), onClick);
    }

    public ItemStack icon() {
        return stack.copy();
    }
}
