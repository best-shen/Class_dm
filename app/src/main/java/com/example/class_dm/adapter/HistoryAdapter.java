// 文件路径: com/example/class_dm/adapter/HistoryAdapter.java
package com.example.class_dm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.class_dm.R;
import com.example.class_dm.database.SessionSummary; // 【注意】引用新的数据模型
import java.util.ArrayList;
import java.util.List;

// 【全新改造】这个适配器现在用于展示 SessionSummary 场次列表
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<SessionSummary> sessionList = new ArrayList<>();

    // 1. 【新增】定义一个点击监听器接口
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SessionSummary session);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    // 2. 【修改】ViewHolder现在用于寻找卡片布局中的控件
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSessionTime, tvSessionStats;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // 注意ID已经变为新布局中的ID
            tvSessionTime = itemView.findViewById(R.id.tv_session_time);
            tvSessionStats = itemView.findViewById(R.id.tv_session_stats);
        }
    }


    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 【注意】确保这里引用的布局是我们新设计的卡片布局 item_history.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        // 3. 【修改】绑定数据到卡片上
        SessionSummary session = sessionList.get(position);
        holder.tvSessionTime.setText(session.getSessionTime());
        holder.tvSessionStats.setText(session.getStatistics());

        // 4. 【新增】为整个卡片（itemView）设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(session);
            }
        });
    }


    @Override
    public int getItemCount() {
        return sessionList.size();
    }


    // 5. 【修改】提供一个新方法，用于更新场次列表数据
    public void setSessionList(List<SessionSummary> list) {
        this.sessionList = list;
        notifyDataSetChanged();
    }
}