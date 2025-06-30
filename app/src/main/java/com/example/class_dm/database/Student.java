// 文件路径: com/example/class_dm/database/Student.java
package com.example.class_dm.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index; // <-- 确保引入了这个包
import androidx.room.PrimaryKey;

// 修正：在@Entity注解中，添加 indices 属性为 className 字段建立索引
@Entity(tableName = "students",
        foreignKeys = @ForeignKey(entity = ClassInfo.class,
                parentColumns = "name",
                childColumns = "className",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "className")}) // <-- 添加这一行
public class Student {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String className; // 对应班级的名称

    public String studentNumber; // 学号

    public String name; // 姓名
}