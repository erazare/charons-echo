package com.charonsecho.platform;

import com.mojang.brigadier.CommandDispatcher;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Routes NeoForge events to the server-side gameplay handlers in registration order. */
public final class NeoEvents {
    private NeoEvents() {}

    public static void serverStarting(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> handler.accept(event.getServer()));
    }

    public static void serverStarted(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> handler.accept(event.getServer()));
    }

    public static void serverStopping(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> handler.accept(event.getServer()));
    }

    public static void serverTick(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> handler.accept(event.getServer()));
    }

    public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> handler) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> handler.accept(event.getDispatcher()));
    }

    public static void afterReload(Consumer<MinecraftServer> handler) {
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent event) -> {
            if (event.getPlayer() == null) handler.accept(event.getPlayerList().getServer());
        });
    }

    public static void playerJoin(Consumer<ServerPlayer> handler) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) handler.accept(player);
        });
    }

    public static void playerQuit(Consumer<ServerPlayer> handler) {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) handler.accept(player);
        });
    }

    public static void entityLoad(BiConsumer<Entity, ServerLevel> handler) {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level) handler.accept(event.getEntity(), level);
        });
    }

    public static void entityUnload(BiConsumer<Entity, ServerLevel> handler) {
        NeoForge.EVENT_BUS.addListener((EntityLeaveLevelEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level) handler.accept(event.getEntity(), level);
        });
    }

    public static void chunkLoad(BiConsumer<ServerLevel, LevelChunk> handler) {
        NeoForge.EVENT_BUS.addListener((ChunkEvent.Load event) -> {
            if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
                handler.accept(level, chunk);
            }
        });
    }

    public static void allowDeath(BiPredicate<LivingEntity, DamageSource> handler) {
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (!event.getEntity().level().isClientSide()
                    && !handler.test(event.getEntity(), event.getSource())) event.setCanceled(true);
        });
    }

    public static void allowDamage(DamageHandler handler) {
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
            if (!handler.allow(event.getEntity(), event.getSource(), event.getAmount())) event.setCanceled(true);
        });
    }

    public static void beforeBreak(BreakHandler handler) {
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (event.getLevel() instanceof ServerLevel level
                    && !handler.allow(level, event.getPlayer(), event.getPos(), event.getState())) {
                event.setNotifyClient(true);
                event.setCanceled(true);
            }
        });
    }

    public static void useBlock(BlockUseHandler handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            if (event.getLevel().isClientSide()) return;
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    public static void useEntity(EntityUseHandler handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
            if (event.getLevel().isClientSide()) return;
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getTarget());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    public static void useItem(UseItemHandler handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
            if (event.getLevel().isClientSide()) return;
            InteractionResult result = handler.use(event.getEntity(), event.getLevel(), event.getHand());
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        });
    }

    public static void attackBlock(Consumer<PlayerInteractEvent.LeftClickBlock> handler) {
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
            if (!event.getLevel().isClientSide()) handler.accept(event);
        });
    }

    public static void attackEntity(Consumer<AttackEntityEvent> handler) {
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent event) -> {
            if (!event.getEntity().level().isClientSide()) handler.accept(event);
        });
    }

    @FunctionalInterface
    public interface DamageHandler {
        boolean allow(LivingEntity entity, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface BreakHandler {
        boolean allow(Level level, Player player, BlockPos pos, BlockState state);
    }

    @FunctionalInterface
    public interface BlockUseHandler {
        InteractionResult use(Player player, Level level, InteractionHand hand, BlockHitResult hit);
    }

    @FunctionalInterface
    public interface EntityUseHandler {
        InteractionResult use(Player player, Level level, Entity target);
    }

    @FunctionalInterface
    public interface UseItemHandler {
        InteractionResult use(Player player, Level level, InteractionHand hand);
    }
}
