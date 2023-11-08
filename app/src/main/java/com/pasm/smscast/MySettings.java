package com.pasm.smscast;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;


class MySettings {

    private static SharedPreferences pref;

    // SMS Monitor
    public static boolean SMSMonitor = true;
    // Phone Monitor
    public static boolean CallMonitor = false;
    // Notification Monitor
    public static boolean NotificationMonitor = false;
    public static boolean NotificationIgnoreOngoing = true;
    public static int NotificationInfoLevel = 0;
    public static String NotificationMonitorIgnoreList = "android\ncom.android.dialer\ncom.google.android.apps.messaging\norg.telegram.messenger";
    public static boolean DoCopyPin = false;
    public static boolean DoNotify = true;
    public static boolean DoToast = true;
    public static boolean SendToTelegram = true;
    public static String BotToken = "";
    public static String ChatID = "";
    public static boolean SendToServer = false;
    public static String URL = "";
    public static String HostPassword = "";
    public static String RegexpFilterList = "";
    public static String WhiteList = "";
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

    private static int safeGetInt(String key, int defValue) {
        try {
            return pref.getInt(key, defValue);
        } catch (ClassCastException e) {
            return defValue;
        }
    }

    public static void restore() {
        SMSMonitor = safeGetBoolean("SMSMonitor", SMSMonitor);
        CallMonitor   = safeGetBoolean("CallMonitor", CallMonitor);
        NotificationMonitor = safeGetBoolean("NotificationMonitor", NotificationMonitor);
        NotificationIgnoreOngoing = safeGetBoolean("NotificationIgnoreOngoing", NotificationIgnoreOngoing);
        NotificationInfoLevel = safeGetInt("NotificationInfoLevel", NotificationInfoLevel);
        NotificationMonitorIgnoreList = safeGetString("NotificationMonitorIgnoreList", NotificationMonitorIgnoreList);
        DoCopyPin    = safeGetBoolean("DoCopyPin", DoCopyPin);
        DoNotify     = safeGetBoolean("DoNotify", DoNotify);
        DoToast      = safeGetBoolean("DoToast", DoToast);
        SendToTelegram = safeGetBoolean("SendToTelegram", SendToTelegram);
        BotToken     = safeGetString("BotToken", BotToken);
        ChatID       = safeGetString("ChatID", ChatID);
        SendToServer = safeGetBoolean("SendToServer", SendToServer);
        URL          = safeGetString("URL", URL);
        HostPassword = safeGetString("HostPassword", HostPassword);
        RegexpFilterList = safeGetString("RegexpFilterList", RegexpFilterList);
        WhiteList    = safeGetString("WhiteList", WhiteList);
        SenderPrefix = safeGetString("SenderPrefix", SenderPrefix);
    }

    public static void store() {
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("SMSMonitor", SMSMonitor);
        editor.putBoolean("CallMonitor", CallMonitor);
        editor.putBoolean("NotificationMonitor", NotificationMonitor);
        editor.putBoolean("NotificationIgnoreOngoing", NotificationIgnoreOngoing);
        editor.putInt("NotificationInfoLevel", NotificationInfoLevel);
        editor.putString("NotificationMonitorIgnoreList", NotificationMonitorIgnoreList);
        editor.putBoolean("DoCopyPin", DoCopyPin);
        editor.putBoolean("DoNotify", DoNotify);
        editor.putBoolean("DoToast", DoToast);
        editor.putBoolean("SendToTelegram", SendToTelegram);
        editor.putString("BotToken", BotToken);
        editor.putString("ChatID", ChatID);
        editor.putBoolean("SendToServer", SendToServer);
        editor.putString("URL", URL);
        editor.putString("HostPassword", HostPassword);
        editor.putString("RegexpFilterList", RegexpFilterList);
        editor.putString("WhiteList", WhiteList);
        editor.putString("SenderPrefix", SenderPrefix);
        editor.apply();
    }
}
