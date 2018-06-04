package com.hps.esecure.appflowsimulator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.hps.esecure.appflowsimulator.R;
import com.hps.esecure.model.emv.dto.network.AReq;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by namri on 03/10/2017.
 */

public class SettingsPreferenceHelper {

    private static final String HPS_MOBILE_SIMULATOR_PREFERENCES = "HPS_MOBILE_SIMULATOR_PREFERENCES";

    private static final String UI_INTERFACE = "UI_INTERFACE";
    private static final String UI_TYPE = "UI_TYPE";
    private static final String ACS_URL = "ACS_URL";
    private static final String DS_URL = "DS_URL";

    private SettingsPreferenceHelper() {
    }

    public static void setUiInterface(Context context, String uiInterface){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(UI_INTERFACE, uiInterface);
        editor.apply();
    }

    public static String getUiInterface(Context context){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        return settings.getString(UI_INTERFACE, AReq.DeviceRenderOptions.UIINTERFACE_NATIVE);
    }

    public static void setUiType(Context context, Set<String> uiTypes){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putStringSet(UI_TYPE, uiTypes);
        editor.apply();
    }

    public static Set<String> getUiType(Context context){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        return settings.getStringSet(UI_TYPE, new HashSet<>(Collections.singletonList(AReq.DeviceRenderOptions.UITYPE_TEXT)));
    }

    public static void setAcsUrl(Context context, String acsUrl){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACS_URL, acsUrl);
        editor.apply();
    }

    public static String getAcsUrl(Context context){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        return settings.getString(ACS_URL, context.getString(R.string.pref_default_acsurl));
    }

    public static void setDsUrl(Context context, String dsUrl){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DS_URL, dsUrl);
        editor.apply();
    }

    public static String getDsUrl(Context context){
        SharedPreferences settings = context.getSharedPreferences(HPS_MOBILE_SIMULATOR_PREFERENCES, Context.MODE_PRIVATE);
        return settings.getString(DS_URL, context.getString(R.string.pref_default_dsurl));
    }
}
