package com.ohhapple.carpetplus;

import com.ohhapple.carpetplus.settings.ohhappleinit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;


public class CarpetPlus implements ModInitializer
{
    public static final String MOD_ID = "carpetplus";
    private static String version;

    @Override
    public void onInitialize()
    {
        version = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(RuntimeException::new).getMetadata().getVersion().getFriendlyString();
        ohhappleinit.open();
    }

    public static String getModId(){
        return MOD_ID;
    }
    public static String getVersion() {return version;}

}