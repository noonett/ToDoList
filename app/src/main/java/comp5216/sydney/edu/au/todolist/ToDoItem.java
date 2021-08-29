package comp5216.sydney.edu.au.todolist;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "todolist2")
public class ToDoItem {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    @ColumnInfo(name = "toDoItemID")
    private int toDoItemID;

    @ColumnInfo(name = "toDoItemName")
    private String toDoItemName;

    @ColumnInfo(name = "toDoItemTime")
    private String toDoItemTime;

    public ToDoItem(String toDoItemName, String toDoItemTime) {
        this.toDoItemName = toDoItemName;
        this.toDoItemTime = toDoItemTime;
    }

    public int getToDoItemID() {
        return toDoItemID;
    }

    public void setToDoItemID(int toDoItemID) {
        this.toDoItemID = toDoItemID;
    }

    public String getToDoItemName() {
        return toDoItemName;
    }

    public void setToDoItemName(String toDoItemName) {
        this.toDoItemName = toDoItemName;
    }

    public String getToDoItemTime() {
        return toDoItemTime;
    }

    public void setToDoItemTime(String toDoItemTime) {
        this.toDoItemTime = toDoItemTime;
    }

}
