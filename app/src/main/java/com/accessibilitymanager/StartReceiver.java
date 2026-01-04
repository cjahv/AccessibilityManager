package com.accessibilitymanager;

import android.content.BroadcastReceiver;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.content.pm.PackageManager;

public class StartReceiver extends BroadcastReceiver {
    private static final int BOOT_JOB_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        AppPrefs.migrateIfUnlocked(context);
        SharedPreferences sharedPreferences = AppPrefs.get(context);
        if (!sharedPreferences.getBoolean("boot", true)) {
            return;
        }
        if (!notificationsAllowed(context)) {
            sharedPreferences.edit().putBoolean(AppPrefs.KEY_NEEDS_NOTIFICATION_PERMISSION, true).apply();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scheduleBootJob(context);
            return;
        }
        Intent serviceIntent = new Intent(context, daemonService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private static boolean notificationsAllowed(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            return manager == null || manager.areNotificationsEnabled();
        }
        return true;
    }

    private static void scheduleBootJob(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler == null) {
            return;
        }
        ComponentName component = new ComponentName(context, BootJobService.class);
        JobInfo jobInfo = new JobInfo.Builder(BOOT_JOB_ID, component)
                .setMinimumLatency(0)
                .setOverrideDeadline(0)
                .build();
        scheduler.schedule(jobInfo);
    }
}
