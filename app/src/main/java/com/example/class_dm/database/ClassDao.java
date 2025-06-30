// 文件路径: com/example/class_dm/database/ClassDao.java
package com.example.class_dm.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ClassDao {
    // 修正：将 ORDER BY id DESC 改为 ORDER BY name ASC (按班级名称升序排列)
    @Query("SELECT * FROM classes ORDER BY name ASC")
    List<ClassInfo> getAllClasses();

    @Insert
    void insert(ClassInfo classInfo);

    @Update
    void update(ClassInfo classInfo);

    @Delete
    void delete(ClassInfo classInfo);
}