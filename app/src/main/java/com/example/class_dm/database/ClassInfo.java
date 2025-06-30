// 文件路径: com/example/class_dm/database/ClassInfo.java
package com.example.class_dm.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// 直接使用班级名称作为主键
@Entity(tableName = "classes")
public class ClassInfo {
    @PrimaryKey
    @NonNull // 主键不能为空
    public String name;
}