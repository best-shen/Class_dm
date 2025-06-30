// 文件路径: com/example/class_dm/database/Attendance.java
package com.example.class_dm.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.Index;
@Entity(tableName = "attendance_records",
        foreignKeys = @ForeignKey(entity = ClassInfo.class,
                parentColumns = "name",
                childColumns = "className",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("className")}) // 【新增】为className字段创建索引
public class Attendance {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int studentId; // 对应的学生ID

    public String className; // 对应的班级名称

    public String date; // 考勤日期，例如 "2024-06-25"

    public String status; // 考勤状态: 到课, 缺勤, 迟到, 早退, 请假
    public long sessionTimestamp; // 我们保留这个作为场次的唯一ID

    // 【新增字段】
    public String courseName;
    public int startPeriod;
    public int endPeriod;
}