package com.budgettracker.app.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.budgettracker.app.R;

/**
 * NotificationHelper - handles budget alert notifications.
 *
 * Flow:
 *  1. Call createNotificationChannel() once at app startup (MainActivity.onCreate).
 *  2. After every expense is saved, check if monthly total > budget limit.
 *  3. If exceeded, call sendBudgetExceededNotification().
 */
public class NotificationHelper {

    public static final String CHANNEL_ID   = "budget_alert_channel";
    public static final String CHANNEL_NAME = "Budget Alerts";

    private static final int NOTIF_ID_EXCEEDED = 1001;
    private static final int NOTIF_ID_WARNING  = 1002;

    /** Register the notification channel (safe to call multiple times). */
    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Alerts when your monthly expenses approach or exceed your budget");
        channel.enableVibration(true);
        getManager(context).createNotificationChannel(channel);
    }

    /**
     * Fire a "budget exceeded" notification.
     * Guards against missing POST_NOTIFICATIONS permission on Android 13+.
     *
     * @param context      application context
     * @param budgetLimit  the monthly limit the user set
     * @param totalSpent   total expenses so far this month
     */
    public static void sendBudgetExceededNotification(Context context,
                                                      double budgetLimit,
                                                      double totalSpent) {
        if (!hasPermission(context)) return;

        String title   = "Budget Limit Exceeded! 🚨";
        String message = String.format(
                "You've spent %s%.0f of your %s%.0f monthly budget.",
                getCurrency(context), totalSpent,
                getCurrency(context), budgetLimit);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_expense)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        getManager(context).notify(NOTIF_ID_EXCEEDED, builder.build());
    }

    /**
     * Fire an "approaching budget" warning when spent >= 80 % of limit.
     */
    public static void sendBudgetWarningNotification(Context context,
                                                     double budgetLimit,
                                                     double totalSpent) {
        if (!hasPermission(context)) return;

        int pct = (int) ((totalSpent / budgetLimit) * 100);
        String title   = "Budget Warning ⚠️";
        String message = String.format(
                "You've used %d%% of your %s%.0f monthly budget.",
                pct, getCurrency(context), budgetLimit);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_expense)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        getManager(context).notify(NOTIF_ID_WARNING, builder.build());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static NotificationManager getManager(Context context) {
        return (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static boolean hasPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // below Android 13 no runtime permission needed
    }

    private static String getCurrency(Context context) {
        return AppPrefs.getCurrencySymbol(context);
    }
}
