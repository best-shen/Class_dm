// 文件路径: com/example/class_dm/adapter/ClassAdapter.java
package com.example.class_dm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.R;
import com.example.class_dm.database.ClassInfo;
import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<ClassInfo> classList = new ArrayList<>();
    private OnItemDeleteListener deleteListener;
    private OnItemClickListener clickListener;
    // 定义删除按钮的监听器接口
    public interface OnItemDeleteListener {
        void onDeleteClick(ClassInfo classInfo);
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    // 定义列表项点击的监听器接口
    public interface OnItemClickListener {
        void onItemClick(ClassInfo classInfo);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassInfo currentClass = classList.get(position);
        holder.tvClassName.setText(currentClass.name);

        // 设置删除按钮的点击事件
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(currentClass);
            }
        });
        // 设置整个列表项的点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(currentClass);
            }
        });
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    // 用于更新Adapter的数据
    public void setClasses(List<ClassInfo> classes) {
        this.classList = classes;
        notifyDataSetChanged(); // 通知RecyclerView刷新
    }

    // ViewHolder类
    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName;
        ImageView ivDelete;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tv_class_name);
            ivDelete = itemView.findViewById(R.id.iv_delete_class);
        }
    }
}