// 文件路径: com/example/class_dm/StudentActivity.java
package com.example.class_dm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.class_dm.adapter.StudentAdapter;
import com.example.class_dm.database.AppDatabase;
import com.example.class_dm.database.Student;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudentActivity extends AppCompatActivity {

    private String currentClassName;
    private AppDatabase appDatabase;
    private StudentAdapter studentAdapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // --- 新增：文件选择器的启动器 ---
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        // --- 新增：初始化文件选择器 ---
        // 注册一个用于处理文件选择结果的回调
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 检查用户是否成功选择了一个文件
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // 调用新方法来处理选中的文件
                            parseExcelAndSave(uri);
                        }
                    }
                });

        currentClassName = getIntent().getStringExtra("CLASS_NAME");

        Toolbar toolbar = findViewById(R.id.toolbar_students);
        toolbar.setTitle(currentClassName);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        appDatabase = AppDatabase.getDatabase(this);
        RecyclerView recyclerView = findViewById(R.id.recycler_view_students);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentAdapter = new StudentAdapter();
        recyclerView.setAdapter(studentAdapter);

        studentAdapter.setListener(new StudentAdapter.OnItemInteractionListener() {
            @Override
            public void onEditClick(Student student) {
                showAddEditStudentDialog(student);
            }

            @Override
            public void onDeleteClick(Student student) {
                showDeleteConfirmationDialog(student);
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab_add_student);
        fab.setOnClickListener(v -> showAddEditStudentDialog(null));

        // --- 修改：“从Excel导入”按钮的点击事件 ---
        Button btnImport = findViewById(R.id.btn_import_excel);
        btnImport.setOnClickListener(v -> openFilePicker()); // 改为调用打开文件选择器的方法

        Button btnViewHistory = findViewById(R.id.btn_view_history);
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, HistoryActivity.class);
            intent.putExtra("CLASS_NAME", currentClassName);
            startActivity(intent);
        });

        Button btnStartRollCall = findViewById(R.id.btn_start_roll_call);
        btnStartRollCall.setOnClickListener(v -> showSetupRollCallDialog());
        loadStudents();
    }

    // --- 新增：打开系统文件选择器的方法 ---
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 我们只希望用户选择Excel文件。注意这里的MIME类型。
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        filePickerLauncher.launch(intent);
    }

    // --- 新增：解析Excel并保存到数据库的核心方法 ---
    private void parseExcelAndSave(final Uri uri) {
        // 在后台线程执行耗时的文件读取和数据库操作
        databaseExecutor.execute(() -> {
            List<Student> newStudents = new ArrayList<>();
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                Workbook workbook = new XSSFWorkbook(inputStream);
                Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
                for (Row row : sheet) {
                    Cell cellNumber = row.getCell(0); // 第A列 - 学号
                    Cell cellName = row.getCell(1);   // 第B列 - 姓名

                    if (cellNumber != null && cellName != null) {
                        Student student = new Student();
                        student.className = currentClassName;
                        // 根据单元格类型获取学号，防止纯数字学号被识别为数字类型
                        switch (cellNumber.getCellType()) {
                            case NUMERIC:
                                student.studentNumber = String.valueOf((long)cellNumber.getNumericCellValue());
                                break;
                            case STRING:
                            default:
                                student.studentNumber = cellNumber.getStringCellValue();
                                break;
                        }
                        student.name = cellName.getStringCellValue();

                        if (!TextUtils.isEmpty(student.studentNumber) && !TextUtils.isEmpty(student.name)) {
                            newStudents.add(student);
                        }
                    }
                }
                // 批量插入数据库
                if (!newStudents.isEmpty()) {
                    appDatabase.studentDao().insertAll(newStudents);
                    // 操作成功，回到主线程提示用户并刷新列表
                    final int count = newStudents.size();
                    handler.post(() -> {
                        Toast.makeText(this, "成功导入 " + count + " 名学生", Toast.LENGTH_SHORT).show();
                        loadStudents();
                    });
                }
            } catch (Exception e) {
                // 如果发生错误，回到主线程提示用户
                handler.post(() -> Toast.makeText(this, "文件解析失败，请检查文件格式是否正确", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void loadStudents() {
        databaseExecutor.execute(() -> {
            List<Student> studentList = appDatabase.studentDao().getStudentsByClass(currentClassName);
            handler.post(() -> studentAdapter.setStudents(studentList));
        });
    }

    private void showAddEditStudentDialog(final Student student) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(student == null ? "添加新学生" : "编辑学生信息");
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_edit_student, null);
        final EditText etStudentNumber = dialogView.findViewById(R.id.et_student_number);
        final EditText etStudentName = dialogView.findViewById(R.id.et_student_name);
        builder.setView(dialogView);
        if (student != null) {
            etStudentNumber.setText(student.studentNumber);
            etStudentName.setText(student.name);
        }
        builder.setPositiveButton("确定", (dialog, which) -> {
            String number = etStudentNumber.getText().toString().trim();
            String name = etStudentName.getText().toString().trim();
            if (TextUtils.isEmpty(number) || TextUtils.isEmpty(name)) {
                Toast.makeText(this, "学号和姓名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 【核心改造】在后台线程中执行查重和保存操作
            databaseExecutor.execute(() -> {
                // a. 先根据输入的学号，查询数据库中是否已存在该学号的学生
                Student existingStudent = appDatabase.studentDao().findStudentByNumber(currentClassName, number);

                // b. 根据不同情况进行处理
                if (student == null) {
                    // 情况一：正在“添加”新学生
                    if (existingStudent != null) {
                        // 如果能找到，说明学号已被占用，提示用户
                        handler.post(() -> Toast.makeText(this, "该学号已存在，请重新输入！", Toast.LENGTH_SHORT).show());
                    } else {
                        // 如果找不到，说明学号可用，执行插入操作
                        Student newStudent = new Student();
                        newStudent.className = currentClassName;
                        newStudent.studentNumber = number;
                        newStudent.name = name;
                        appDatabase.studentDao().insert(newStudent);
                        handler.post(this::loadStudents); // 刷新列表
                    }
                } else {
                    // 情况二：正在“编辑”旧学生
                    // 如果能找到同号的学生，并且这个学生的ID和我们正在编辑的学生的ID不一致
                    // 这才说明学号被“别人”占用了
                    if (existingStudent != null && existingStudent.id != student.id) {
                        handler.post(() -> Toast.makeText(this, "该学号已被其他学生使用，请重新输入！", Toast.LENGTH_SHORT).show());
                    } else {
                        // 否则，学号可用（要么没被占用，要么就是自己正在用），执行更新操作
                        student.studentNumber = number;
                        student.name = name;
                        appDatabase.studentDao().update(student);
                        handler.post(this::loadStudents); // 刷新列表
                    }
                }
            });
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(final Student student) {
        new AlertDialog.Builder(this)
                .setTitle("删除学生")
                .setMessage("确定要删除学生 \"" + student.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    databaseExecutor.execute(() -> {
                        appDatabase.studentDao().delete(student);
                        handler.post(this::loadStudents);
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

// 在 StudentActivity.java 中，用这个新方法替换旧的 showSetupRollCallDialog 方法

    private void showSetupRollCallDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置点名信息");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_setup_roll_call, null);

        final EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        final Button btnPickDate = dialogView.findViewById(R.id.btn_dialog_pick_date);
        final NumberPicker npStart = dialogView.findViewById(R.id.np_start_period);
        final NumberPicker npEnd = dialogView.findViewById(R.id.np_end_period);

        final java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        btnPickDate.setText(sdf.format(calendar.getTime()));

        npStart.setMinValue(1);
        npStart.setMaxValue(11);
        npEnd.setMinValue(1);
        npEnd.setMaxValue(11);

        btnPickDate.setOnClickListener(v -> new android.app.DatePickerDialog(StudentActivity.this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            btnPickDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show());

        builder.setView(dialogView);
        builder.setPositiveButton("开始点名", null); // 【关键修改】我们先将监听器设为null，以便自定义
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create(); // 先创建dialog

        // 【关键修改】为“开始点名”按钮设置自定义的点击监听器
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String courseName = etCourseName.getText().toString().trim();
                String date = btnPickDate.getText().toString();
                int startPeriod = npStart.getValue();
                int endPeriod = npEnd.getValue();

                // 输入验证
                if (TextUtils.isEmpty(courseName)) {
                    Toast.makeText(this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (startPeriod > endPeriod) {
                    Toast.makeText(this, "开始节次不能大于结束节次", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 在后台线程执行查重操作
                databaseExecutor.execute(() -> {
                    Long existingSessionId = appDatabase.attendanceDao().findSessionIdByDetails(
                            currentClassName, courseName, date, startPeriod, endPeriod);

                    handler.post(() -> {
                        if (existingSessionId != null) {
                            // 如果找到了已存在的场次，弹出二次确认对话框
                            new AlertDialog.Builder(this)
                                    .setTitle("记录已存在")
                                    .setMessage("已存在一个完全相同的点名场次，是否要覆盖它？")
                                    .setPositiveButton("覆盖", (dialogInterface, i) -> {
                                        // 用户选择覆盖，使用旧的sessionId启动点名页
                                        proceedToRollCall(existingSessionId, courseName, date, startPeriod, endPeriod);
                                        dialog.dismiss(); // 关闭设置弹窗
                                    })
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            // 如果没找到，是新场次，生成新的sessionId并启动点名页
                            proceedToRollCall(System.currentTimeMillis(), courseName, date, startPeriod, endPeriod);
                            dialog.dismiss(); // 关闭设置弹窗
                        }
                    });
                });
            });
        });

        dialog.show();
    }

    // 【新增】一个用于跳转到点名页的辅助方法，避免代码重复
    private void proceedToRollCall(long sessionId, String courseName, String date, int startPeriod, int endPeriod) {
        Intent intent = new Intent(StudentActivity.this, RollCallActivity.class);
        intent.putExtra("CLASS_NAME", currentClassName);
        intent.putExtra("SESSION_ID", sessionId);
        intent.putExtra("COURSE_NAME", courseName);
        intent.putExtra("DATE", date);
        intent.putExtra("START_PERIOD", startPeriod);
        intent.putExtra("END_PERIOD", endPeriod);
        startActivity(intent);
    }
}