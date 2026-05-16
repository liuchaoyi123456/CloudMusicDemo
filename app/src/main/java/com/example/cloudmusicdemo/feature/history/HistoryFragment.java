    package com.example.cloudmusicdemo.feature.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.local.UserDataManager;
import com.example.cloudmusicdemo.data.model.Music;
import com.example.cloudmusicdemo.feature.home.MusicAdapter;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private TextView tvHistoryCount;
    private MusicAdapter adapter;
    private UserDataManager userDataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        userDataManager = UserDataManager.getInstance(requireContext());

        initViews(view);
        loadHistory();

        return view;
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rvHistory);
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(null);
        rvHistory.setAdapter(adapter);

        adapter.setOnPlayClickListener((music, position) -> {
            if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
                List<Music> history = userDataManager.getHistory();
                ((com.example.cloudmusicdemo.MainActivity) getActivity()).playMusicFromSearch(history, position);
            }
        });
        
        view.findViewById(R.id.ivBack).setOnClickListener(v -> {
            if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
                com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
                mainActivity.getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void loadHistory() {
        List<Music> history = userDataManager.getHistory();

        if (history != null && !history.isEmpty()) {
            adapter.updateData(history);
            tvHistoryCount.setText("共 " + history.size() + " 首歌曲");
        } else {
            tvHistoryCount.setText("暂无播放历史");
            Toast.makeText(getContext(), "暂无播放历史", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
            com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
            mainActivity.hidePlayControlBar();
            mainActivity.getBottomNavigationView().setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onStop() {
        super.onStop();
        
        // 恢复底部导航栏
        if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
            com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
            mainActivity.getBottomNavigationView().setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}