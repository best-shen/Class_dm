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
            databaseExecutor.execute(() -> {
                if (student == null) {
                    Student newStudent = new Student();
                    newStudent.className = currentClassName;
                    newStudent.studentNumber = number;
                    newStudent.name = name;
                    appDatabase.studentDao().insert(newStudent);
                } else {
                    student.studentNumber = number;
                    student.name = name;
                    appDatabase.studentDao().update(student);
                }
                handler.post(this::loadStudents);
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

    private void showSetupRollCallDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("设置点名信息");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_setup_roll_call, null);

        final EditText etCourseName = dialogView.findViewById(R.id.et_course_name);
        final Button btnPickDate = dialogView.findViewById(R.id.btn_dialog_pick_date);
        final NumberPicker npStart = dialogView.findViewById(R.id.np_start_period);
        final NumberPicker npEnd = dialogView.findViewById(R.id.np_end_period);

        // 初始化日期按钮和节次选择器
        final java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        btnPickDate.setText(sdf.format(calendar.getTime()));

        npStart.setMinValue(1);
        npStart.setMaxValue(11);
        npEnd.setMinValue(1);
        npEnd.setMaxValue(11);

        // 设置日期选择
        btnPickDate.setOnClickListener(v -> new android.app.DatePickerDialog(StudentActivity.this, (view, year, month, day) -> {
            calendar.set(year, month, day);
            btnPickDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show());

        builder.setView(dialogView);
        builder.setPositiveButton("开始点名", (dialog, which) -> {
            String courseName = etCourseName.getText().toString().trim();
            int startPeriod = npStart.getValue();
            int endPeriod = npEnd.getValue();

            if (TextUtils.isEmpty(courseName)) {
                Toast.makeText(this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (startPeriod > endPeriod) {
                Toast.makeText(this, "开始节次不能大于结束节次", Toast.LENGTH_SHORT).show();
                return;
            }

            // 验证通过，打包所有信息，启动点名页面
            Intent intent = new Intent(StudentActivity.this, RollCallActivity.class);
            intent.putExtra("CLASS_NAME", currentClassName);
            intent.putExtra("SESSION_ID", System.currentTimeMillis());
            intent.putExtra("COURSE_NAME", courseName);
            intent.putExtra("DATE", btnPickDate.getText().toString());
            intent.putExtra("START_PERIOD", startPeriod);
            intent.putExtra("END_PERIOD", endPeriod);
            startActivity(intent);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}