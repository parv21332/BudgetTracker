package com.budgettracker.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budgettracker.app.data.model.User;

/**
 * DAO (Data Access Object) for User table operations.
 */
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE email = :email AND password_hash = :passwordHash LIMIT 1")
    User login(String email, String passwordHash);

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    int emailExists(String email);

    @Query("UPDATE users SET password_hash = :newHash WHERE id = :userId")
    void updatePassword(int userId, String newHash);

    @Query("UPDATE users SET dark_mode = :darkMode WHERE id = :userId")
    void updateDarkMode(int userId, boolean darkMode);

    @Query("UPDATE users SET currency_symbol = :symbol WHERE id = :userId")
    void updateCurrencySymbol(int userId, String symbol);

    @Query("UPDATE users SET name = :name WHERE id = :userId")
    void updateName(int userId, String name);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserLive(int userId);
}
