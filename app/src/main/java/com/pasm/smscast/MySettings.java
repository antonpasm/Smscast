package com.pasm.smscast;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


class MySettings {

    private static SharedPreferences pref;

    public static String BotToken = "";
    public static String ChatID = "";
    public static String HostPassword = "";
    public static String URL = "";
    public static String RegexpFilterList = "";
    public static String WhiteList = "";
    public static boolean DoNotify = true;
    public static boolean DoCopyPin = false;
    public static boolean MissedCall = false;
    public static boolean DoToast = true;
    public static boolean SendToTelegram = true;
    public static boolean SendToServer = false;
    public static String SenderPrefix = "";

    public static void init(Context context) {
        if (pref == null) {
            pref = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE);
            restore();
        }
    }

    private static String safeGetString(String key, String defValue) {
        String res;
        try {
            res = pref.getString(key, defValue);
            if (res == null) {
                res = defValue;
            }
        } catch (ClassCastException e) {
            res = defValue;
        }
        return res;
    }

    private static Boolean safeGetBoolean(String key, Boolean defValue) {
        try {
            return pref.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    public static void restore() {
        BotToken     = safeGetString("BotToken", BotToken);
        ChatID       = safeGetString("ChatID", ChatID);
        HostPassword = safeGetString("HostPassword", HostPassword);
        URL          = safeGetString("URL", URL);
        RegexpFilterList = safeGetString("RegexpFilterList", RegexpFilterList);
        WhiteList    = safeGetString("WhiteList", WhiteList);
        DoNotify     = safeGetBoolean("DoNotify", DoNotify);
        DoCopyPin    = safeGetBoolean("DoCopyPin", DoCopyPin);
        MissedCall   = safeGetBoolean("MissedCall", MissedCall);
        DoToast      = safeGetBoolean("DoToast", DoToast);
        SendToTelegram = safeGetBoolean("SendToTelegram", SendToTelegram);
        SendToServer = safeGetBoolean("SendToServer", SendToServer);
        SenderPrefix = safeGetString("SenderPrefix", SenderPrefix);
    }

    public static void store() {
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("BotToken", BotToken);
        editor.putString("ChatID", ChatID);
        editor.putString("HostPassword", HostPassword);
        editor.putString("URL", URL);
        editor.putString("RegexpFilterList", RegexpFilterList);
        editor.putString("WhiteList", WhiteList);
        editor.putBoolean("DoNotify", DoNotify);
        editor.putBoolean("DoCopyPin", DoCopyPin);
        editor.putBoolean("MissedCall", MissedCall);
        editor.putBoolean("DoToast", DoToast);
        editor.putBoolean("SendToTelegram", SendToTelegram);
        editor.putBoolean("SendToServer", SendToServer);
        editor.putString("SenderPrefix", SenderPrefix);
        editor.apply();
    }
}
