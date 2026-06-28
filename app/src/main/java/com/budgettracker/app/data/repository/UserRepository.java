package com.budgettracker.app.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.budgettracker.app.data.database.BudgetDatabase;
import com.budgettracker.app.data.database.UserDao;
import com.budgettracker.app.data.model.User;
import com.budgettracker.app.utils.PasswordUtils;
import com.budgettracker.app.utils.SessionManager;

/**
 * UserRepository - single source of truth for User data.
 * Handles registration, login, and profile updates.
 */
public class UserRepository {

    private final UserDao userDao;
    private final SessionManager sessionManager;

    public UserRepository(Application application) {
        BudgetDatabase db = BudgetDatabase.getDatabase(application);
        userDao = db.userDao();
        sessionManager = new SessionManager(application);
    }

    /**
     * Register a new user.
     * @return -1 if email already exists, userId otherwise
     */
    public long register(String name, String email, String password) {
        if (userDao.emailExists(email) > 0) {
            return -1; // Email already registered
        }
        String hashedPassword = PasswordUtils.hashPassword(password);
        User user = new User(name, email, hashedPassword);
        return userDao.insertUser(user);
    }

    /**
     * Authenticate user by email + password.
     * @return User object on success, null on failure
     */
    public User login(String email, String password) {
        String hashedPassword = PasswordUtils.hashPassword(password);
        return userDao.login(email, hashedPassword);
    }

    /**
     * Get current logged-in user.
     */
    public User getCurrentUser() {
        int userId = sessionManager.getUserId();
        if (userId == -1) return null;
        return userDao.getUserById(userId);
    }

    public LiveData<User> getCurrentUserLive() {
        int userId = sessionManager.getUserId();
        return userDao.getUserLive(userId);
    }

    /**
     * Save login session.
     */
    public void saveSession(User user, boolean rememberLogin) {
        sessionManager.saveSession(user.getId(), user.getName(), user.getEmail(), rememberLogin);
    }

    /**
     * Clear session on logout.
     */
    public void logout() {
        sessionManager.clearSession();
    }

    /**
     * Check if user is already logged in.
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * Change password - validates old password first.
     * @return true on success
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDao.getUserById(userId);
        if (user == null) return false;
        String oldHash = PasswordUtils.hashPassword(oldPassword);
        if (!oldHash.equals(user.getPasswordHash())) return false;
        String newHash = PasswordUtils.hashPassword(newPassword);
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                userDao.updatePassword(userId, newHash));
        return true;
    }

    public void updateDarkMode(int userId, boolean darkMode) {
        BudgetDatabase.databaseWriteExecutor.execute(() ->
                userDao.updateDarkMode(userId, darkMode));
    }

    public void updateName(int userId, String name) {
        BudgetDatabase.databaseWriteExecutor.execute(() -> {
            userDao.updateName(userId, name);
            sessionManager.updateName(name);
        });
    }
}
