package com.budgettracker.app.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Income entity - stores all income records.
 * Single-user app: user_id is always 1 (no FK constraint needed).
 */
@Entity(tableName = "income",
        indices = {@Index("user_id"), @Index("date")})
public class Income {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "user_id")
    private int userId;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "source")
    private String source;

    @ColumnInfo(name = "date")
    private long date;  // stored as Unix timestamp (milliseconds)

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    // Constructor
    public Income(int userId, double amount, String source, long date, String notes) {
        this.userId = userId;
        this.amount = amount;
        this.source = source;
        this.date = date;
        this.notes = notes != null ? notes : "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
