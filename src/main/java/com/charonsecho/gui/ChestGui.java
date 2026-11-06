package com.charonsecho.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Vanilla chest screen with virtual buttons and the player's real inventory below. */
public class ChestGui {
    private final ServerPlayer player;
    private final MenuType<ChestMenu> type;
    private final int rows;
    private final MenuItem.Entry[] buttons;
    private Component title = Component.empty();
    private AbstractContainerMenu menu;

    public ChestGui(ServerPlayer player, int rows) {
        this.player = player;
        this.rows = rows;
        type = switch (rows) {
            case 3 -> MenuType.GENERIC_9x3;
            case 6 -> MenuType.GENERIC_9x6;
            default -> throw new IllegalArgumentException("Unsupported chest row count: " + rows);
        };
        buttons = new MenuItem.Entry[rows * 9];
    }

    public void setTitle(Component title) {
        this.title = title;
    }

    public void setSlot(int index, MenuItem button) {
        buttons[index] = button.entry();
    }

    public void open() {
        var display = new SimpleContainer(buttons.length);
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) display.setItem(i, buttons[i].stack());
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, viewer) -> menu = new ButtonMenu(id, inventory, display), title));
    }

    public void close() {
        if (menu != null && player.containerMenu == menu) player.closeContainer();
    }

    protected void onClose() {}

    private final class ButtonMenu extends ChestMenu {
        private ButtonMenu(int id, Inventory inventory, SimpleContainer display) {
            super(type, id, inventory, display, rows);
        }

        @Override
        public void clicked(int slot, int button, ContainerInput input, Player viewer) {
            if (slot >= 0 && slot < buttons.length) {
                var entry = buttons[slot];
                if (entry != null && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
                    entry.onClick().accept(ChestGui.this);
                }
                return;
            }
            super.clicked(slot, button, input, viewer);
        }

        @Override
        public ItemStack quickMoveStack(Player viewer, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canDragTo(Slot slot) {
            return slot.index >= buttons.length && super.canDragTo(slot);
        }

        @Override
        public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
            return slot.index >= buttons.length && super.canTakeItemForPickAll(stack, slot);
        }

        @Override
        public void removed(Player viewer) {
            super.removed(viewer);
            onClose();
        }
    }
}
