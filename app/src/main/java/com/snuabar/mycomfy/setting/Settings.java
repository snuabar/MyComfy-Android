package com.snuabar.mycomfy.setting;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private final SharedPreferences preferences;

    private static Settings Instance;

    public static Settings init(Context context) {
        Instance = new Settings(context);
        return Instance;
    }

    public static Settings getInstance() {
        return Instance;
    }

    private Settings(Context context) {
        preferences = context.getSharedPreferences(context.getPackageName() + ".main.settings", Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}
