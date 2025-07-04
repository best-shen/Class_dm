// 文件路径: com/example/class_dm/database/AttendanceDao.java
package com.example.class_dm.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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
    @Query("SELECT s.id as studentId, s.studentNumber, s.name as studentName, a.status FROM attendance_records a " +
            "JOIN students s ON a.studentId = s.id WHERE a.sessionTimestamp = :sessionId " +
            "ORDER BY s.studentNumber ASC") // 【新增】按学号升序排列
    List<AttendanceDetails> getDetailsBySessionId(long sessionId);

    @Query("DELETE FROM attendance_records WHERE sessionTimestamp = :sessionId")
    void deleteBySessionId(long sessionId);

    // 根据学生ID和场次ID查询单条原始的考勤记录对象
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND sessionTimestamp = :sessionId")
    Attendance getAttendanceByStudentAndSession(int studentId, long sessionId);

    // 更新单条考勤记录
    @Update
    void update(Attendance attendance);
    // 获取一个班级点名过的所有课程名称（不重复）
    @Query("SELECT DISTINCT courseName FROM attendance_records WHERE className = :className")
    List<String> getDistinctCourseNamesByClass(String className);

    // 获取某课程的所有考勤记录
    @Query("SELECT * FROM attendance_records WHERE className = :className AND courseName = :courseName")
    List<Attendance> getRecordsByCourse(String className, String courseName);

    // 统计某课程总共点名了多少个场次（用于计算出勤率）
    @Query("SELECT COUNT(DISTINCT sessionTimestamp) FROM attendance_records WHERE className = :className AND courseName = :courseName")
    long countSessionsForCourse(String className, String courseName);

    @Query("SELECT sessionTimestamp FROM attendance_records WHERE " +
            "className = :className AND courseName = :courseName AND date = :date AND " +
            "startPeriod = :startPeriod AND endPeriod = :endPeriod LIMIT 1")
    Long findSessionIdByDetails(String className, String courseName, String date, int startPeriod, int endPeriod);
}