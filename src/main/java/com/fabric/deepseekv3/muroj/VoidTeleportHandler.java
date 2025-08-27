package com.fabric.deepseekv3.muroj;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

public class VoidTeleportHandler {
    private static final int COOLDOWN_TICKS = 20; // 防止连续传送的冷却时间（1秒）
    private static long lastProcessedTick = 0; // 上次处理的时间戳

    public static void handleVoidTeleport(PlayerEntity player) {
        // 只在服务器端处理，且玩家存活、不在创造模式
        if (player.getWorld().isClient || !player.isAlive() || player.isCreative()) {
        //if (player.getWorld().isClient || !player.isAlive()) { //或者创造模式也能进? 搞不懂ai咋想的
                return;
        }

        // 防止每tick都处理，降低性能消耗
        long currentTick = player.getWorld().getTime();
        if (currentTick - lastProcessedTick < COOLDOWN_TICKS) {
            return;
        }
        lastProcessedTick = currentTick;

        World world = player.getWorld();
        Vec3d pos = player.getPos();
        double y = pos.y;

        // 根据当前维度处理不同的虚空情况
        if (world.getRegistryKey() == World.OVERWORLD) {
            handleOverworldVoid(player, y);
        } else if (world.getRegistryKey() == World.NETHER) {
            handleNetherVoid(player, y);
        } else if (world.getRegistryKey() == World.END) {
            handleEndVoid(player, y);
        }
    }

    /**
     * 处理主世界虚空坠落
     * @param player 玩家实体
     * @param y 玩家的Y坐标
     */
    private static void handleOverworldVoid(PlayerEntity player, double y) {
        // 主世界虚空高度通常是Y=-64以下
        if (y < -64) {
            teleportToNetherTop(player);
        }
    }

    /**
     * 处理下界虚空
     * @param player 玩家实体
     * @param y 玩家的Y坐标
     */
    private static void handleNetherVoid(PlayerEntity player, double y) {
        // 下界顶部突破检测 (Y=128+)
        if (y > 128) {
            teleportToOverworldBottom(player);
        }
        // 下界虚空坠落检测 (Y=0以下)
        else if (y < 0) {
            teleportToEndTop(player);
        }
    }

    /**
     * 处理末地虚空
     * @param player 玩家实体
     * @param y 玩家的Y坐标
     */
    private static void handleEndVoid(PlayerEntity player, double y) {
        // 末地顶部突破检测 (Y=256+)
        if (y > 256) {
            teleportToNetherTop(player);
        }
        // 末地虚空坠落 - 不处理，让玩家按原版机制死亡
    }

    /**
     * 传送玩家到下界顶部
     * @param player 玩家实体
     */
    private static void teleportToNetherTop(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ServerWorld netherWorld = serverPlayer.getServer().getWorld(World.NETHER);
        if (netherWorld == null) return;

        // 获取玩家在主世界的X和Z坐标（下界坐标是主世界的1/8）
        double x = player.getX() / 8.0;
        double z = player.getZ() / 8.0;

        // 下界顶部基岩层在Y=127，传送到其下方2格(Y=125)
        double y = 125;

        // 确保传送位置在世界边界内
        WorldBorder border = netherWorld.getWorldBorder();
        BlockPos targetPos = BlockPos.ofFloored(x, y, z);
        if (!border.contains(targetPos)) {
            // 如果超出边界，调整到边界内
            x = Math.max(border.getBoundWest() + 1, Math.min(border.getBoundEast() - 1, x));
            z = Math.max(border.getBoundNorth() + 1, Math.min(border.getBoundSouth() - 1, z));
            targetPos = BlockPos.ofFloored(x, y, z);
        }

        // 传送玩家 - 使用正确的teleport方法
        serverPlayer.teleport(netherWorld, x, y, z, player.getYaw(), player.getPitch());

        // 将玩家头顶的基岩转换为空气
        BlockPos playerHeadPos = BlockPos.ofFloored(x, y + 1, z);
        if (netherWorld.getBlockState(playerHeadPos).getBlock() == Blocks.BEDROCK) {
            netherWorld.setBlockState(playerHeadPos, Blocks.AIR.getDefaultState());
        }
    }

    /**
     * 传送玩家到主世界底部
     * @param player 玩家实体
     */
    private static void teleportToOverworldBottom(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ServerWorld overworld = serverPlayer.getServer().getWorld(World.OVERWORLD);
        if (overworld == null) return;

        // 获取玩家在下界的X和Z坐标（主世界坐标是下界的8倍）
        double x = player.getX() * 8.0;
        double z = player.getZ() * 8.0;

        // 主世界最低建筑高度是Y=-64，传送到基岩上方
        double y = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, (int)x, (int)z);
        y = Math.max(-63, y); // 确保不低于基岩层上方

        // 确保传送位置在世界边界内
        WorldBorder border = overworld.getWorldBorder();
        BlockPos targetPos = BlockPos.ofFloored(x, y, z);
        if (!border.contains(targetPos)) {
            // 如果超出边界，调整到边界内
            x = Math.max(border.getBoundWest() + 1, Math.min(border.getBoundEast() - 1, x));
            z = Math.max(border.getBoundNorth() + 1, Math.min(border.getBoundSouth() - 1, z));
            y = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, (int)x, (int)z);
            targetPos = BlockPos.ofFloored(x, y, z);
        }

        // 传送玩家 - 使用正确的teleport方法
        serverPlayer.teleport(overworld, x, y, z, player.getYaw(), player.getPitch());
    }

    /**
     * 传送玩家到末地顶部
     * @param player 玩家实体
     */
    private static void teleportToEndTop(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ServerWorld endWorld = serverPlayer.getServer().getWorld(World.END);
        if (endWorld == null) return;

        // 保持X和Z坐标不变
        double x = player.getX();
        double z = player.getZ();

        // 末地顶部高度 (Y=255)
        double y = 255;

        // 确保传送位置在世界边界内
        WorldBorder border = endWorld.getWorldBorder();
        BlockPos targetPos = BlockPos.ofFloored(x, y, z);
        if (!border.contains(targetPos)) {
            // 如果超出边界，调整到边界内
            x = Math.max(border.getBoundWest() + 1, Math.min(border.getBoundEast() - 1, x));
            z = Math.max(border.getBoundNorth() + 1, Math.min(border.getBoundSouth() - 1, z));
            targetPos = BlockPos.ofFloored(x, y, z);
        }

        // 传送玩家 - 使用正确的teleport方法
        serverPlayer.teleport(endWorld, x, y, z, player.getYaw(), player.getPitch());
    }
}