package com.charonsecho.gui;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Uses vanilla anvil rename packets for input, with no item transfer or XP cost. */
public final class NameInputMenu extends AnvilMenu {
    private final ItemStack submitIcon;
    private final Consumer<String> onSubmit;
    private String input = "";

    public static void open(ServerPlayer player, Component title, ItemStack submitIcon, Consumer<String> onSubmit) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, viewer) -> new NameInputMenu(id, inventory, submitIcon, onSubmit), title));
    }

    private NameInputMenu(int id, Inventory inventory, ItemStack submitIcon, Consumer<String> onSubmit) {
        super(id, inventory);
        this.submitIcon = submitIcon;
        this.onSubmit = onSubmit;

        ItemStack paper = new ItemStack(Items.PAPER);
        paper.set(DataComponents.ITEM_NAME, Component.empty());
        paper.set(DataComponents.CUSTOM_NAME, Component.empty());
        inputSlots.setItem(0, paper);
    }

    @Override
    protected void createResultInternal() {
        if (submitIcon == null) return;
        setCost(0);
        resultSlots.setItem(0, submitIcon);
    }

    @Override
    public boolean setItemName(String text) {
        String filtered = StringUtil.filterText(text);
        if (filtered.length() > 50) return false;
        input = filtered;
        // The vanilla client may locally rename the virtual result.
        var connection = ((ServerPlayer) player).connection;
        connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), getResultSlot(), submitIcon));
        connection.send(new ClientboundContainerSetDataPacket(containerId, 0, 0));
        return true;
    }

    @Override
    public boolean stillValid(Player viewer) {
        return true;
    }

    @Override
    public void clicked(int slot, int button, ContainerInput action, Player viewer) {
        if (slot == getResultSlot() && (action == ContainerInput.PICKUP || action == ContainerInput.QUICK_MOVE)) {
            onSubmit.accept(input);
        } else if (slot < 0 || slot > getResultSlot()) {
            super.clicked(slot, button, action, viewer);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player viewer, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.index > getResultSlot() && super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index > getResultSlot() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player viewer) {
        inputSlots.removeItemNoUpdate(0); // The displayed paper never belongs to the player.
        super.removed(viewer);
    }
}
