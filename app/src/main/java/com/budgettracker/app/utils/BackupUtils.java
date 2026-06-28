package com.budgettracker.app.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for backing up and restoring the SQLite database file.
 * Backup goes to Downloads/BudgetTracker/ folder.
 */
public class BackupUtils {

    private static final String DB_NAME = "budget_tracker.db";
    private static final String BACKUP_FOLDER = "BudgetTracker";

    /**
     * Backup the database to external Downloads folder.
     * @return backup file path on success, null on failure
     */
    public static String backupDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) return null;

            File backupDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    BACKUP_FOLDER);
            if (!backupDir.exists()) backupDir.mkdirs();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            File backupFile = new File(backupDir, "budget_backup_" + timestamp + ".db");

            copyFile(dbFile, backupFile);
            return backupFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restore database from a backup file.
     * IMPORTANT: App should restart after restore.
     * @param context app context
     * @param backupFilePath path to the backup .db file
     * @return true on success
     */
    public static boolean restoreDatabase(Context context, String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) return false;

            File dbFile = context.getDatabasePath(DB_NAME);

            // Close the database before restoring
            // (caller must ensure Room DB is closed)
            copyFile(backupFile, dbFile);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Copy a file using NIO FileChannel for efficiency.
     */
    private static void copyFile(File source, File dest) throws IOException {
        try (FileChannel src = new FileInputStream(source).getChannel();
             FileChannel dst = new FileOutputStream(dest).getChannel()) {
            dst.transferFrom(src, 0, src.size());
        }
    }

    /**
     * Get list of available backups in Downloads/BudgetTracker/ folder.
     */
    public static File[] getAvailableBackups() {
        File backupDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BACKUP_FOLDER);
        if (!backupDir.exists()) return new File[0];
        File[] files = backupDir.listFiles(f ->
                f.getName().startsWith("budget_backup_") && f.getName().endsWith(".db"));
        return files != null ? files : new File[0];
    }
}
