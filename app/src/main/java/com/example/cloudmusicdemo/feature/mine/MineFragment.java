package com.example.cloudmusicdemo.feature.mine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.data.local.UserDataManager;

public class MineFragment extends Fragment{
    
    private TextView tvListeningTime;
    private TextView tvSongCount;
    private TextView tvTodayTime;
    private TextView tvFavoriteCount;
    private TextView tvHistoryCount;
    
    private LinearLayout layoutFavorites;
    private LinearLayout layoutHistory;
    private LinearLayout layoutLocalMusic;
    private LinearLayout layoutSettings;
    
    private UserDataManager userDataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_mine,container,false);
        
        userDataManager = UserDataManager.getInstance(requireContext());
        
        initViews(view);
        loadStatistics();
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        tvListeningTime = view.findViewById(R.id.tvListeningTime);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        tvTodayTime = view.findViewById(R.id.tvTodayTime);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);
        
        layoutFavorites = view.findViewById(R.id.layoutFavorites);
        layoutHistory = view.findViewById(R.id.layoutHistory);
        layoutLocalMusic = view.findViewById(R.id.layoutLocalMusic);
        layoutSettings = view.findViewById(R.id.layoutSettings);
    }
    
    private void loadStatistics() {
        long totalMinutes = userDataManager.getTotalListeningTime();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        
        tvListeningTime.setText(hours + "." + String.format("%02d", minutes));
        tvSongCount.setText(String.valueOf(userDataManager.getTotalSongCount()));
        tvTodayTime.setText(String.valueOf(userDataManager.getTodayListeningTime()));
        tvFavoriteCount.setText(String.valueOf(userDataManager.getFavoritesCount()));
        tvHistoryCount.setText(String.valueOf(userDataManager.getHistoryCount()));
    }
    
    private void setupClickListeners() {
        layoutFavorites.setOnClickListener(v -> {
            Toast.makeText(getContext(), "查看我的收藏", Toast.LENGTH_SHORT).show();
        });
        
        layoutHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "查看播放历史", Toast.LENGTH_SHORT).show();
        });
        
        layoutLocalMusic.setOnClickListener(v -> {
            Toast.makeText(getContext(), "查看本地音乐", Toast.LENGTH_SHORT).show();
        });
        
        layoutSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "打开设置", Toast.LENGTH_SHORT).show();
        });
    }
    
    public void refreshStatistics() {
        if (userDataManager != null) {
            long totalMinutes = userDataManager.getTotalListeningTime();
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            
            tvListeningTime.setText(hours + "." + String.format("%02d", minutes));
            tvSongCount.setText(String.valueOf(userDataManager.getTotalSongCount()));
            tvTodayTime.setText(String.valueOf(userDataManager.getTodayListeningTime()));
            tvFavoriteCount.setText(String.valueOf(userDataManager.getFavoritesCount()));
            tvHistoryCount.setText(String.valueOf(userDataManager.getHistoryCount()));
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadStatistics();
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
    }
}
