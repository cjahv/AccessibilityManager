package com.accessibilitymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.UserManager;

final class AppPrefs {
    static final String NAME = "data";
    static final String KEY_NEEDS_NOTIFICATION_PERMISSION = "needs_notification_permission";

    private AppPrefs() {
    }

    static SharedPreferences get(Context context) {
        return getStorageContext(context).getSharedPreferences(NAME, 0);
    }

    static void migrateToDeviceProtected(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        Context deviceContext = context.createDeviceProtectedStorageContext();
        SharedPreferences devicePrefs = deviceContext.getSharedPreferences(NAME, 0);
        if (!devicePrefs.getAll().isEmpty()) {
            return;
        }
        deviceContext.moveSharedPreferencesFrom(context, NAME);
    }

    static void migrateIfUnlocked(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (userManager != null && !userManager.isUserUnlocked()) {
            return;
        }
        migrateToDeviceProtected(context);
    }

    private static Context getStorageContext(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createDeviceProtectedStorageContext();
        }
        return context;
    }
}
