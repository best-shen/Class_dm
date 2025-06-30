// 文件路径: com/example/class_dm/database/AppDatabase.java
package com.example.class_dm.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// 版本号从 2 升级到 3
@Database(entities = {ClassInfo.class, Student.class, Attendance.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ClassDao classDao();
    public abstract StudentDao studentDao();
    public abstract AttendanceDao attendanceDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "class_dm_database")
                            // 在开发期间，当数据库版本升级时，允许破坏性迁移（会清空所有数据）
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}