package com.arnotts17.item;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.arnotts17.ArnottsHorseFlute;
import com.mojang.serialization.Codec;

import net.minecraft.block.Block;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Instrument;
import net.minecraft.item.Instruments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public final class HorseFluteItem extends Item {

  public static final String localId = "horse_flute";

  public HorseFluteItem(Item.Settings settings) {
    super(settings);
  }

  @Override
  public ActionResult use(World world, PlayerEntity user, Hand hand) {
    ItemStack itemStack = user.getStackInHand(hand);
    user.setCurrentHand(hand);
    if (!itemStack.contains(VEHICLE_ID_COMPONENT)) {
      user.sendMessage(Text.translatable("message.not_bound").formatted(Formatting.AQUA), true);
      return ActionResult.FAIL;
    }
    Entity target = HorseFluteItem.getTargetEntity(user, itemStack);
    Instrument instrument = HorseFluteItem.getInstrument(user);
    HorseFluteItem.playSound(world, user, instrument);
    if (!user.isCreative() && !user.isSpectator()) {
      user.getItemCooldownManager().set(itemStack, MathHelper.floor(instrument.useDuration() * 20.0f));
    }
    user.incrementStat(Stats.USED.getOrCreateStat(this));
    if (target == null) {
      user.sendMessage(Text.translatable("message.target_not_found").formatted(Formatting.RED), true);
      return ActionResult.FAIL;
    }
    HorseFluteItem.teleport(user, target);
    return ActionResult.CONSUME;
  }

  @Override
  public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
    if (user.getWorld().isClient) {
      return ActionResult.PASS;
    }
    if (entity instanceof AbstractHorseEntity vehicle) {
      if (vehicle.isAlive()) {
        HorseFluteItem.setVehicleId(stack, vehicle.getUuidAsString());
        HorseFluteItem.setVehicleName(stack, vehicle.getName().getString());
        if (vehicle instanceof HorseEntity horse) {
          HorseFluteItem.setVehicleColor(stack, horse.getHorseColor());
        }
      }
      return ActionResult.SUCCESS;
    }
    return ActionResult.PASS;
  }

  @Override
  public int getMaxUseTime(ItemStack stack, LivingEntity user) {
    float distance = this.getVelocityDistance(user, stack);
    if (distance >= Integer.MAX_VALUE) {
      return 0;
    }
    Instrument instrument = HorseFluteItem.getInstrument(user);
    if (instrument == null) {
      return 0;
    }
    return MathHelper.floor(instrument.useDuration() * 20.0f);
  }

  @Override
  public UseAction getUseAction(ItemStack stack) {
    return UseAction.TOOT_HORN;
  }

  @Nullable
  public static Instrument getInstrument(LivingEntity user) {
    Optional<RegistryEntry.Reference<Instrument>> optional = user.getRegistryManager().getOrThrow(RegistryKeys.INSTRUMENT).getOptional(Instruments.CALL_GOAT_HORN);
    if (optional.isEmpty()) {
      return null;
    }
    Instrument instrument = optional.get().value();
    return instrument;    
  }

  protected static boolean teleport(LivingEntity user, Entity target) {
    World world = user.getWorld();
    if (world instanceof ServerWorld serverWorld) {
      Vec3d playerPos = user.getPos();
      
      for (Vec3i offset : offsets) {
        Vec3d relative = HorseFluteItem.rotateOffset(offset, user.getYaw());
        BlockPos targetPos = BlockPos.ofFloored(playerPos.add(relative));
        BlockPos groundPos = targetPos.down();
        
        boolean isSpaceAir = serverWorld.isAir(targetPos) && serverWorld.isAir(targetPos.up());
        boolean isSpaceAllowedToStand = Block.isFaceFullSquare(world.getBlockState(groundPos).getCollisionShape(world, groundPos), Direction.UP);
        if (isSpaceAir && isSpaceAllowedToStand) {
          target.teleport(serverWorld, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, Set.of(), user.getYaw(), user.getPitch(), false);
          if (user instanceof PlayerEntity player) {
            String name = target.getName().getString();
            player.sendMessage(Text.translatable("message.teleported", name), true);
          }
          Random random = target.getRandom();
          world.addParticleClient(
            ParticleTypes.DUST_PLUME,
            target.getX(), target.getY(), target.getZ(),
            (random.nextDouble() - 0.5) * 0.3,
            random.nextDouble() * 0.2,
            (random.nextDouble() - 0.5) * 0.3
          );
          return true;
        }
      }

      if (user instanceof PlayerEntity player) {
        player.sendMessage(Text.translatable("message.not_enough_space").formatted(Formatting.RED), true);
      }
    }
    return false;
  }

  protected static void playSound(World world, PlayerEntity player, Instrument instrument) {
    SoundEvent soundEvent = instrument.soundEvent().value();
    float volume = instrument.range() / 16.0f;
    world.playSoundFromEntity(player, player, soundEvent, SoundCategory.RECORDS, volume, 1.0f);
    world.emitGameEvent(GameEvent.INSTRUMENT_PLAY, player.getPos(), GameEvent.Emitter.of(player));
  }

  @Override
  public boolean canBeNested() {
    return false;
  }

  public static final ComponentType<String> VEHICLE_ID_COMPONENT = Registry.register(
    Registries.DATA_COMPONENT_TYPE,
    ModItems.createId("vehicle_id"),
    ComponentType.<String>builder().codec(Codec.STRING).build()
  );

  public static final void setVehicleId(ItemStack stack, String vehicleId) {
    stack.set(VEHICLE_ID_COMPONENT, vehicleId);
  }

  public static final ComponentType<String> VEHICLE_NAME_COMPONENT = Registry.register(
    Registries.DATA_COMPONENT_TYPE,
    ModItems.createId("vehicle_name"),
    ComponentType.<String>builder().codec(Codec.STRING).build()
  );

  public static final void setVehicleName(ItemStack stack, String vehicleName) {
    stack.set(VEHICLE_NAME_COMPONENT, vehicleName);
  }

  public static final ComponentType<Integer> VEHICLE_COLOR_COMPONENT = Registry.register(
    Registries.DATA_COMPONENT_TYPE,
    ModItems.createId("vehicle_color"),
    ComponentType.<Integer>builder().codec(Codec.INT).build()
  );

  public static final void setVehicleColor(ItemStack stack, HorseColor horseColor) {
    switch (horseColor) {
      case HorseColor.WHITE:
        stack.set(VEHICLE_COLOR_COMPONENT, 0xdddddf); // 221,221,223
        break;
      case HorseColor.CREAMY:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x875824); // 135,88,36
        break;
      case HorseColor.CHESTNUT:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x8f491b); // 143,73,27
        break;
      case HorseColor.BROWN:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x4e210b); // 78,33,11
        break;
      case HorseColor.BLACK:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x24262f); // 36,38,47
        break;
      case HorseColor.GRAY:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x5f5f5f); // 95,95,95
        break;
      case HorseColor.DARK_BROWN:
        stack.set(VEHICLE_COLOR_COMPONENT, 0x2a160b); // 42,22,13
        break;
      default:
        break;
    }
  }

  protected static final Vec3i[] offsets = new Vec3i[]{
    new Vec3i(1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1),
    new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0),
    new Vec3i(1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(-1, 0, -1)
  };

  @Nullable
  protected static final Entity getTargetEntity(Iterable<ServerWorld> worlds, ItemStack itemStack) {
    if (itemStack.contains(VEHICLE_ID_COMPONENT)) {
      String velocityId = itemStack.get(VEHICLE_ID_COMPONENT);
      UUID uuid = UUID.fromString(velocityId);
      for (ServerWorld w : worlds) {
        Entity target = w.getEntity(uuid);
        if (target != null) {
          HorseFluteItem.setVehicleName(itemStack, target.getName().getString());
          return target;
        }
      }
    }
    return null;
  }

  @Nullable
  protected static final Entity getTargetEntity(LivingEntity user, ItemStack itemStack) {
    MinecraftServer server = user.getServer();
    if (server == null) {
      return null;
    }
    return HorseFluteItem.getTargetEntity(server.getWorlds(), itemStack);
  }

  protected static final int getEncodedColor(ItemStack itemStack) {
    if (itemStack.contains(VEHICLE_COLOR_COMPONENT)) {
      return itemStack.get(VEHICLE_COLOR_COMPONENT);
    }
    return 0x946734;
  }

  protected final float getVelocityDistance(LivingEntity user, ItemStack itemStack) {
    Entity target = HorseFluteItem.getTargetEntity(user, itemStack);
    if (target != null) {
    }
    return Integer.MAX_VALUE;
  }

  public static void stackTooltipRenderer(ItemStack itemStack, TooltipContext context, List<Text> list) {
    list.clear();
    MutableText title = Text.translatable("item." + ArnottsHorseFlute.MOD_ID + "." + HorseFluteItem.localId);
    if (itemStack.contains(VEHICLE_ID_COMPONENT)) {
      if (itemStack.contains(VEHICLE_NAME_COMPONENT)) {
        String vehicleName = itemStack.get(VEHICLE_NAME_COMPONENT);
        list.add(title.append(Text.literal(" - ").append(Text.literal(vehicleName).withColor(HorseFluteItem.getEncodedColor(itemStack)))));
      } else {
        list.add(title.append(Text.literal(" (" + itemStack.get(VEHICLE_ID_COMPONENT) + ")").formatted(Formatting.GRAY)));
      }
    } else {
      list.add(title.append(Text.translatable("tooltip.not_bound").formatted(Formatting.GRAY)));
      list.add(Text.translatable("message.not_bound").formatted(Formatting.DARK_AQUA, Formatting.ITALIC));
    }
  }

  public static Item.Settings getDefaultSettings() {
    return new Item.Settings().maxCount(1);
  }

  protected static final Vec3d rotateOffset(Vec3i offset, float yawDegrees) {
    double yawRad = Math.toRadians(-yawDegrees);
    double cos = Math.cos(yawRad), sin = Math.sin(yawRad);

    double x = offset.getX() * cos - offset.getZ() * sin;
    double z = offset.getX() * sin + offset.getZ() * cos;

    return new Vec3d(x, offset.getY(), z);
  }

}
