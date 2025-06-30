// 文件路径: com/example/class_dm/adapter/StudentAdapter.java
package com.example.class_dm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.R;
import com.example.class_dm.database.Student;
import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentList = new ArrayList<>();
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onEditClick(Student student);
        void onDeleteClick(Student student);
    }

    public void setListener(OnItemInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student currentStudent = studentList.get(position);
        holder.tvStudentName.setText(currentStudent.name);
        holder.tvStudentNumber.setText(currentStudent.studentNumber);

        holder.ivEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(currentStudent);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentStudent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void setStudents(List<Student> students) {
        this.studentList = students;
        notifyDataSetChanged();
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvStudentNumber;
        ImageView ivEdit, ivDelete;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name);
            tvStudentNumber = itemView.findViewById(R.id.tv_student_number);
            ivEdit = itemView.findViewById(R.id.iv_edit_student);
            ivDelete = itemView.findViewById(R.id.iv_delete_student);
        }
    }
}