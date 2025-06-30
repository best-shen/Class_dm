// 文件路径: com/example/class_dm/database/AttendanceDao.java
package com.example.class_dm.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AttendanceDao {
    // 插入单条考勤记录
    @Insert
    void insert(Attendance attendance);

    // 插入多条考勤记录
    @Insert
    void insertAll(List<Attendance> attendances);

    // 根据日期和学生ID查询考勤记录，用于防止重复记录
    @Query("SELECT * FROM attendance_records WHERE date = :date AND studentId = :studentId LIMIT 1")
    Attendance findAttendanceByDateAndStudent(String date, int studentId);

    // 根据日期和班级名称查询所有考勤记录
    @Query("SELECT * FROM attendance_records WHERE date = :date AND className = :className")
    List<Attendance> getAttendancesByDateAndClass(String date, String className);

    // 这个方法用于获取一个班级下所有不重复的点名场次的基础信息
    @Query("SELECT DISTINCT sessionTimestamp, courseName, date, startPeriod, endPeriod FROM attendance_records " +
            "WHERE className = :className ORDER BY date DESC, sessionTimestamp DESC")
    List<HistorySessionInfo> getAllSessionsForClass(String className);

    // 这个方法用于根据场次ID，精确地获取这一个场次所有学生的出勤详情
    @Query("SELECT s.studentNumber, s.name as studentName, a.status FROM attendance_records a " +
            "JOIN students s ON a.studentId = s.id WHERE a.sessionTimestamp = :sessionId")
    List<AttendanceDetails> getDetailsBySessionId(long sessionId);

    @Query("DELETE FROM attendance_records WHERE sessionTimestamp = :sessionId")
    void deleteBySessionId(long sessionId);
}