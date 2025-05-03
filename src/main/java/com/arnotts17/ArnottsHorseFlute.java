package com.arnotts17;

import net.fabricmc.api.ModInitializer;

import com.arnotts17.item.ModItems;

public class ArnottsHorseFlute implements ModInitializer {
	
	public static final String MOD_ID = "arnotts_horse_flute";

	@Override
	public void onInitialize() {
		ModItems.initialize();
	}

}