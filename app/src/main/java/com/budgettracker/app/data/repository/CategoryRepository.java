package com.budgettracker.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.CategoryDao;
import com.budgettracker.app.data.model.Category;

import java.util.List;

/**
 * CategoryRepository - manages expense categories.
 */
public class CategoryRepository {

    private final CategoryDao categoryDao;

    public CategoryRepository(Application application) {
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        categoryDao = db.categoryDao();
    }

    public void insert(Category category) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                categoryDao.insertCategory(category));
    }

    public void update(Category category) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                categoryDao.updateCategory(category));
    }

    public void deleteUserCategory(int id, int userId) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                categoryDao.deleteUserCategory(id, userId));
    }

    public LiveData<List<Category>> getAllCategories(int userId) {
        return categoryDao.getAllCategories(userId);
    }

    public List<Category> getAllCategoriesSync(int userId) {
        return categoryDao.getAllCategoriesSync(userId);
    }

    public Category getCategoryById(int id) {
        return categoryDao.getCategoryById(id);
    }
}
