package com.budgettracker.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.budgettracker.app.data.model.User;
import com.budgettracker.app.data.repository.UserRepository;
import com.budgettracker.app.utils.PasswordUtils;

/**
 * ViewModel for Authentication (Login / Register).
 * Runs DB operations on background thread.
 */
public class AuthViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    // LiveData for UI state
    public final MutableLiveData<AuthResult> authResult = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    /**
     * Attempt to register a new user.
     * Runs on background thread, posts result to authResult LiveData.
     */
    public void register(String name, String email, String password,
                         String confirmPassword, boolean rememberLogin) {
        // Input validation
        if (name == null || name.trim().isEmpty()) {
            authResult.postValue(new AuthResult(false, "Name is required"));
            return;
        }
        if (email == null || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            authResult.postValue(new AuthResult(false, "Invalid email address"));
            return;
        }
        String pwError = PasswordUtils.validatePassword(password);
        if (pwError != null) {
            authResult.postValue(new AuthResult(false, pwError));
            return;
        }
        if (!password.equals(confirmPassword)) {
            authResult.postValue(new AuthResult(false, "Passwords do not match"));
            return;
        }

        isLoading.postValue(true);
        new Thread(() -> {
            try {
                long userId = userRepository.register(name.trim(), email.trim().toLowerCase(), password);
                if (userId == -1) {
                    authResult.postValue(new AuthResult(false, "Email already registered"));
                } else {
                    User user = new User(name.trim(), email.trim().toLowerCase(),
                            com.budgettracker.app.utils.PasswordUtils.hashPassword(password));
                    user.setId((int) userId);
                    userRepository.saveSession(user, rememberLogin);
                    authResult.postValue(new AuthResult(true, "Registration successful", (int) userId));
                }
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Attempt to log in with email + password.
     */
    public void login(String email, String password, boolean rememberLogin) {
        if (email == null || email.trim().isEmpty()) {
            authResult.postValue(new AuthResult(false, "Email is required"));
            return;
        }
        if (password == null || password.isEmpty()) {
            authResult.postValue(new AuthResult(false, "Password is required"));
            return;
        }

        isLoading.postValue(true);
        new Thread(() -> {
            try {
                User user = userRepository.login(email.trim().toLowerCase(), password);
                if (user == null) {
                    authResult.postValue(new AuthResult(false, "Invalid email or password"));
                } else {
                    userRepository.saveSession(user, rememberLogin);
                    authResult.postValue(new AuthResult(true, "Login successful", user.getId()));
                }
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * Check if user is already logged in (for splash screen logic).
     */
    public boolean isLoggedIn() {
        return userRepository.isLoggedIn();
    }

    // ---- Result wrapper ----
    public static class AuthResult {
        public final boolean success;
        public final String message;
        public final int userId;

        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.userId = -1;
        }

        public AuthResult(boolean success, String message, int userId) {
            this.success = success;
            this.message = message;
            this.userId = userId;
        }
    }
}
