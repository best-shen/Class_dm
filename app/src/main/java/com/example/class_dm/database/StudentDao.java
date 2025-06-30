// 文件路径: com/example/class_dm/database/StudentDao.java
package com.example.class_dm.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface StudentDao {
    // 根据班级名称查询所有学生
    @Query("SELECT * FROM students WHERE className = :className")
    List<Student> getStudentsByClass(String className);

    @Insert
    void insert(Student student);

    // 支持批量插入
    @Insert
    void insertAll(List<Student> students);

    @Update
    void update(Student student);

    @Delete
    void delete(Student student);
    @Query("SELECT * FROM students WHERE className = :className AND studentNumber = :studentNumber LIMIT 1")
    Student findStudentByNumber(String className, String studentNumber);
}