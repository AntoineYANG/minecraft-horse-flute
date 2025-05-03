package com.arnotts17.item;

import java.util.function.Function;

import com.arnotts17.ArnottsHorseFlute;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {

  public static final Identifier HORSE_FLUTE_ID = createId(HorseFluteItem.localId);
  public static final Item HORSE_FLUTE = registerItem(
    HORSE_FLUTE_ID,
    HorseFluteItem::new,
    HorseFluteItem.getDefaultSettings()
  );

  public static final Item registerItem(
    Identifier itemId,
    Function<Item.Settings, Item> itemFactory,
    Item.Settings settings
  ) {
    // Create the item key.
    final RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, itemId);
    // Create the item instance and register it.
    Item inst = Items.register(itemKey, itemFactory, settings);

    return inst;
  }

  public static final String groupId = ArnottsHorseFlute.MOD_ID;

  public static final RegistryKey<ItemGroup> ITEM_GROUP_KEY = RegistryKey.of(
    Registries.ITEM_GROUP.getKey(),
    createId(groupId)
  );

  public static final ItemGroup itemGroup = FabricItemGroup.builder()
    .icon(() -> new ItemStack(HORSE_FLUTE))
    .displayName(Text.translatable("item_group.arnotts_horse_flute"))
    .build();

  public static final Identifier createId(String localId) {
    return Identifier.of(ArnottsHorseFlute.MOD_ID, localId);
  }

  public static final void initialize() {
    Registry.register(Registries.ITEM_GROUP, groupId, itemGroup);
    
    ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP_KEY).register(group -> {
      group.add(HORSE_FLUTE);
    });
  }
  
}
