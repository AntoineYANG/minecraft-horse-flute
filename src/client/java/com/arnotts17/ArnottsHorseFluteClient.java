package com.arnotts17;

import com.arnotts17.item.HorseFluteItem;
import com.arnotts17.item.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

public class ArnottsHorseFluteClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		initializeStackTooltips();
	}

	private static void initializeStackTooltips() {
		StackToolTipHandler.register(ModItems.HORSE_FLUTE_ID, HorseFluteItem::stackTooltipRenderer);
		ItemTooltipCallback.EVENT.register(new StackToolTipHandler());
	}

}