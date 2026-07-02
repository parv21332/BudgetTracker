package com.budgettracker.app.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BackupUtils - uses app-specific external storage.
 * No READ/WRITE_EXTERNAL_STORAGE permission needed on Android 10+.
 * Backup location: /sdcard/Android/data/com.budgettracker.app/files/BudgetBackups/
 */
public class BackupUtils {

    private static final String TAG = "BackupUtils";
    private static final String DB_NAME = "budget_tracker.db";
    private static final String BACKUP_FOLDER = "BudgetBackups";

    /**
     * Get backup directory — app-specific external storage, no permission needed.
     */
    private static File getBackupDir(Context context) {
        // Try external app-specific storage first
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir != null) {
            File backupDir = new File(externalDir, BACKUP_FOLDER);
            if (!backupDir.exists()) backupDir.mkdirs();
            return backupDir;
        }
        // Fallback to internal storage
        File internalDir = new File(context.getFilesDir(), BACKUP_FOLDER);
        if (!internalDir.exists()) internalDir.mkdirs();
        return internalDir;
    }

    /**
     * Backup the SQLite database.
     * @return backup file path on success, null on failure.
     */
    public static String backupDatabase(Context context) {
        try {
            File dbFile = context.getDatabasePath(DB_NAME);
            if (!dbFile.exists()) {
                Log.e(TAG, "Database file not found: " + dbFile.getAbsolutePath());
                return null;
            }

            File backupDir = getBackupDir(context);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            File backupFile = new File(backupDir, "budget_backup_" + timestamp + ".db");

            // Close the active Room database before copying files so the backup is consistent.
            com.budgettracker.app.data.database.BudgetDatabase db =
                    com.budgettracker.app.data.database.BudgetDatabase.getDatabase(context);
            db.close();
            com.budgettracker.app.data.database.BudgetDatabase.resetInstance();

            copyFile(dbFile, backupFile);

            // Also copy WAL/SHM files if they exist.
            File walFile = new File(dbFile.getParent(), DB_NAME + "-wal");
            if (walFile.exists() && walFile.length() > 0) {
                copyFile(walFile, new File(backupDir, "budget_backup_" + timestamp + ".db-wal"));
            }
            File shmFile = new File(dbFile.getParent(), DB_NAME + "-shm");
            if (shmFile.exists() && shmFile.length() > 0) {
                copyFile(shmFile, new File(backupDir, "budget_backup_" + timestamp + ".db-shm"));
            }

            Log.d(TAG, "Backup saved to: " + backupFile.getAbsolutePath());
            return backupFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Backup failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Restore database from backup.
     * NOTE: App must be restarted after restore for Room to pick up changes.
     */
    public static boolean restoreDatabase(Context context, String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file not found: " + backupFilePath);
                return false;
            }

            File dbFile = context.getDatabasePath(DB_NAME);

            // Close any active Room instance and clear the cached singleton before replacing files.
            com.budgettracker.app.data.database.BudgetDatabase db =
                    com.budgettracker.app.data.database.BudgetDatabase.getDatabase(context);
            db.close();
            com.budgettracker.app.data.database.BudgetDatabase.resetInstance();

            // Delete existing database files to avoid conflicts.
            new File(dbFile.getParent(), DB_NAME + "-wal").delete();
            new File(dbFile.getParent(), DB_NAME + "-shm").delete();
            if (dbFile.exists()) {
                dbFile.delete();
            }

            copyFile(backupFile, dbFile);

            // Restore WAL/SHM if available.
            File walBackup = new File(backupFile.getParentFile(), backupFile.getName() + "-wal");
            File shmBackup = new File(backupFile.getParentFile(), backupFile.getName() + "-shm");
            if (walBackup.exists() && walBackup.length() > 0) {
                copyFile(walBackup, new File(dbFile.getParent(), DB_NAME + "-wal"));
            }
            if (shmBackup.exists() && shmBackup.length() > 0) {
                copyFile(shmBackup, new File(dbFile.getParent(), DB_NAME + "-shm"));
            }

            Log.d(TAG, "Database restored from: " + backupFilePath);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Restore failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get list of available backup files.
     */
    public static List<File> getAvailableBackups(Context context) {
        List<File> backups = new ArrayList<>();
        File backupDir = getBackupDir(context);
        File[] files = backupDir.listFiles(f ->
                f.getName().startsWith("budget_backup_") && f.getName().endsWith(".db"));
        if (files != null) {
            for (File f : files) backups.add(f);
        }
        // Sort newest first
        backups.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return backups;
    }

    /**
     * Get human-readable backup location description.
     */
    public static String getBackupLocation(Context context) {
        File dir = getBackupDir(context);
        return dir.getAbsolutePath();
    }

    private static void copyFile(File source, File dest) throws IOException {
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try (FileChannel src = new FileInputStream(source).getChannel();
             FileChannel dst = new FileOutputStream(dest).getChannel()) {
            dst.transferFrom(src, 0, src.size());
        }
    }
}
