package com.budgettracker.app.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.budgettracker.app.data.model.Category;
import com.budgettracker.app.data.model.Expense;
import com.budgettracker.app.data.model.Income;
import com.budgettracker.app.data.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room Database singleton.
 * SQLite version = 1, update version + add migration when schema changes.
 *
 * Tables: users, income, expenses, categories
 */
@Database(entities = {User.class, Income.class, Expense.class, Category.class},
        version = 1,
        exportSchema = false)
public abstract class BudgetDatabase extends RoomDatabase {

    private static volatile BudgetDatabase INSTANCE;

    // Thread pool for background DB operations
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    // Abstract DAO methods
    public abstract UserDao userDao();
    public abstract IncomeDao incomeDao();
    public abstract ExpenseDao expenseDao();
    public abstract CategoryDao categoryDao();

    /**
     * Get singleton instance of the database.
     */
    public static BudgetDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BudgetDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    BudgetDatabase.class,
                                    "budget_tracker.db")
                            .addCallback(sRoomDatabaseCallback)
                            // Enable WAL mode for better performance
                            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback to seed default categories on first database creation.
     */
    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            databaseWriteExecutor.execute(() -> {
                if (INSTANCE != null) {
                    seedDefaultCategories(INSTANCE.categoryDao());
                }
            });
        }
    };

    /**
     * Insert default system categories.
     */
    private static void seedDefaultCategories(CategoryDao categoryDao) {
        if (categoryDao.getSystemCategoryCount() > 0) return;

        categoryDao.insertCategory(new Category("Food & Dining", "restaurant", "#FF5722"));
        categoryDao.insertCategory(new Category("Transport", "directions_car", "#2196F3"));
        categoryDao.insertCategory(new Category("Shopping", "shopping_cart", "#9C27B0"));
        categoryDao.insertCategory(new Category("Bills & Utilities", "receipt_long", "#FF9800"));
        categoryDao.insertCategory(new Category("Healthcare", "local_hospital", "#F44336"));
        categoryDao.insertCategory(new Category("Entertainment", "movie", "#00BCD4"));
        categoryDao.insertCategory(new Category("Education", "school", "#4CAF50"));
        categoryDao.insertCategory(new Category("Personal Care", "self_improvement", "#E91E63"));
        categoryDao.insertCategory(new Category("Home & Rent", "home", "#795548"));
        categoryDao.insertCategory(new Category("Others", "more_horiz", "#607D8B"));
    }
}
