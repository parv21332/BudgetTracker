package com.budgettracker.app.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Category entity - stores expense categories.
 * user_id = null means system/default category.
 */
@Entity(tableName = "categories",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id")})
public class Category {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "user_id")
    private Integer userId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "icon")
    private String icon;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "is_system")
    private boolean isSystem;

    /**
     * Primary constructor used by Room.
     */
    public Category(Integer userId, String name, String icon, String color, boolean isSystem) {
        this.userId = userId;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isSystem = isSystem;
    }

    /** Constructor for system categories (userId = null) */
    @Ignore
    public Category(String name, String icon, String color) {
        this(null, name, icon, color, true);
    }

    /** Constructor for user-created categories */
    @Ignore
    public Category(int userId, String name, String icon, String color) {
        this(userId, name, icon, color, false);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }
}
