package com.budgettracker.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budgettracker.app.data.model.Category;

import java.util.List;

/**
 * DAO for Categories table.
 * Returns both system categories (user_id IS NULL) and user-specific categories.
 */
@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertCategory(Category category);

    @Update
    void updateCategory(Category category);

    @Delete
    void deleteCategory(Category category);

    @Query("DELETE FROM categories WHERE id = :id AND user_id = :userId AND is_system = 0")
    void deleteUserCategory(int id, int userId);

    // Get all categories available to a user (system + their own)
    @Query("SELECT * FROM categories WHERE user_id IS NULL OR user_id = :userId ORDER BY is_system DESC, name ASC")
    LiveData<List<Category>> getAllCategories(int userId);

    @Query("SELECT * FROM categories WHERE user_id IS NULL OR user_id = :userId ORDER BY is_system DESC, name ASC")
    List<Category> getAllCategoriesSync(int userId);

    // Get only system categories
    @Query("SELECT * FROM categories WHERE is_system = 1 ORDER BY name ASC")
    List<Category> getSystemCategories();

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getCategoryById(int id);

    @Query("SELECT COUNT(*) FROM categories WHERE is_system = 1")
    int getSystemCategoryCount();
}
