// 文件路径: com/example/class_dm/HistoryActivity.java
package com.example.class_dm;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.class_dm.adapter.HistoryAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.Attendance;
import com.example.class_dm.database.AttendanceDetails;
import com.example.class_dm.database.HistorySessionInfo;
import com.example.class_dm.database.SessionSummary;
import com.example.class_dm.database.Student;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HistoryActivity extends AppCompatActivity {

    private String currentClassName;
    private AppDatabase appDatabase;
    private HistoryAdapter historyAdapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> detailsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        currentClassName = getIntent().getStringExtra("CLASS_NAME");
        appDatabase = AppDatabase.getDatabase(this);

        detailsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadAllHistory()
        );

        Toolbar toolbar = findViewById(R.id.toolbar_history);
        toolbar.setTitle(currentClassName + " - 历史考勤");
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_view_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();
        recyclerView.setAdapter(historyAdapter);

        historyAdapter.setOnItemClickListener(session -> {
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailsActivity.class);
            intent.putExtra("SESSION_ID", session.getSessionId());
            detailsLauncher.launch(intent);
        });

        historyAdapter.setOnDeleteClickListener(session -> {
            new AlertDialog.Builder(this)
                    .setTitle("删除记录")
                    .setMessage("确定要删除这次的点名记录吗？\n" + session.getSessionTime())
                    .setPositiveButton("删除", (dialog, which) -> deleteSession(session.getSessionId()))
                    .setNegativeButton("取消", null)
                    .show();
        });

        loadAllHistory();
    }

    // 【新增】加载菜单布局
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    // 【新增】处理菜单项的点击事件
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export_excel) {
            showCourseSelectionDialogForExport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSession(long sessionId) {
        databaseExecutor.execute(() -> {
            appDatabase.attendanceDao().deleteBySessionId(sessionId);
            handler.post(() -> {
                Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                loadAllHistory();
            });
        });
    }

    private void loadAllHistory() {
        databaseExecutor.execute(() -> {
            List<HistorySessionInfo> sessions = appDatabase.attendanceDao().getAllSessionsForClass(currentClassName);
            List<SessionSummary> sessionSummaries = new ArrayList<>();
            if (!sessions.isEmpty()) {
                for (HistorySessionInfo sessionInfo : sessions) {
                    List<AttendanceDetails> detailsForThisSession = appDatabase.attendanceDao().getDetailsBySessionId(sessionInfo.sessionTimestamp);
                    long presentCount = detailsForThisSession.stream().filter(d -> "到课".equals(d.status)).count();
                    long absentCount = detailsForThisSession.stream().filter(d -> "缺勤".equals(d.status)).count();
                    long lateCount = detailsForThisSession.stream().filter(d -> "迟到".equals(d.status)).count();
                    long earlyCount = detailsForThisSession.stream().filter(d -> "早退".equals(d.status)).count();
                    long leaveCount = detailsForThisSession.stream().filter(d -> "请假".equals(d.status)).count();
                    String sessionTitle = sessionInfo.courseName;
                    String sessionSubtitle = String.format(Locale.CHINA, "%s  第%d-%d节",
                            sessionInfo.date, sessionInfo.startPeriod, sessionInfo.endPeriod);
                    String statistics = String.format(Locale.CHINA,
                            "到课:%d | 缺勤:%d | 迟到:%d | 早退:%d | 请假:%d",
                            presentCount, absentCount, lateCount, earlyCount, leaveCount);
                    sessionSummaries.add(new SessionSummary(sessionInfo.sessionTimestamp, sessionTitle + "\n" + sessionSubtitle, statistics));
                }
            }
            handler.post(() -> historyAdapter.setSessionList(sessionSummaries));
        });
    }

    // 【新增】方法一：弹出课程选择对话框
    private void showCourseSelectionDialogForExport() {
        databaseExecutor.execute(() -> {
            List<String> courseNames = appDatabase.attendanceDao().getDistinctCourseNamesByClass(currentClassName);
            handler.post(() -> {
                if (courseNames.isEmpty()) {
                    Toast.makeText(this, "没有课程记录可导出", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("选择要导出的课程")
                        .setItems(courseNames.toArray(new String[0]), (dialog, which) -> {
                            String selectedCourse = courseNames.get(which);
                            generateAndSaveExcel(selectedCourse);
                        })
                        .show();
            });
        });
    }

    // 【新增】方法二：生成并保存Excel文件的核心逻辑
    private void generateAndSaveExcel(String courseName) {
        databaseExecutor.execute(() -> {
            List<Student> students = appDatabase.studentDao().getStudentsByClass(currentClassName);
            List<Attendance> records = appDatabase.attendanceDao().getRecordsByCourse(currentClassName, courseName);
            Map<Integer, List<Attendance>> recordsByStudent = records.stream()
                    .collect(Collectors.groupingBy(a -> a.studentId));

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("考勤汇总 - " + courseName);
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue(currentClassName + "《" + courseName + "》课程考勤总览表");
            Row headerRow = sheet.createRow(2);
            String[] headers = {"学号", "姓名", "到课次数", "缺勤次数", "迟到次数", "早退次数", "请假次数", "总出勤率"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 3;
            for (Student student : students) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(student.studentNumber);
                row.createCell(1).setCellValue(student.name);

                List<Attendance> studentRecords = recordsByStudent.getOrDefault(student.id, new ArrayList<>());
                long totalSessions = appDatabase.attendanceDao().countSessionsForCourse(currentClassName, courseName);
                long present = studentRecords.stream().filter(r -> "到课".equals(r.status)).count();
                long absent = studentRecords.stream().filter(r -> "缺勤".equals(r.status)).count();
                long late = studentRecords.stream().filter(r -> "迟到".equals(r.status)).count();
                long early = studentRecords.stream().filter(r -> "早退".equals(r.status)).count();
                long leave = studentRecords.stream().filter(r -> "请假".equals(r.status)).count();
                row.createCell(2).setCellValue(present);
                row.createCell(3).setCellValue(absent);
                row.createCell(4).setCellValue(late);
                row.createCell(5).setCellValue(early);
                row.createCell(6).setCellValue(leave);
                if (totalSessions > 0) {
                    double attendanceRate = (double) present / totalSessions;
                    row.createCell(7).setCellValue(String.format(Locale.getDefault(), "%.2f%%", attendanceRate * 100));
                } else {
                    row.createCell(7).setCellValue("N/A");
                }
            }
            String fileName = currentClassName + "_" + courseName + "_考勤汇总.xlsx";
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                        workbook.write(outputStream);
                    }
                    handler.post(() -> Toast.makeText(this, "报表已成功保存到“下载”文件夹", Toast.LENGTH_LONG).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(this, "报表保存失败", Toast.LENGTH_SHORT).show());
            } finally {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}