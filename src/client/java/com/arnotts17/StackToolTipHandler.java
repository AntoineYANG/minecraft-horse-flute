package com.arnotts17;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class StackToolTipHandler implements ItemTooltipCallback {

  @FunctionalInterface
  public interface StackTooltipRenderer {
    void render(ItemStack itemStack, TooltipContext context, List<Text> list);
  }

  private static final Map<Identifier, StackTooltipRenderer> rendererRegistry = new HashMap<>();

  @Override
  public void getTooltip(ItemStack itemStack, TooltipContext tooltipContext, TooltipType tooltipType, List<Text> list) {
    Item item = itemStack.getItem();
    
    Identifier itemId = Registries.ITEM.getId(item);
    if (itemId == null) {
      return;
    }
    StackTooltipRenderer renderer = rendererRegistry.get(itemId);
    if (renderer != null) {
      renderer.render(itemStack, tooltipContext, list);
    }
  }

  public static void register(Identifier itemId, StackTooltipRenderer renderer) {
    rendererRegistry.put(itemId, renderer);
  }

}
