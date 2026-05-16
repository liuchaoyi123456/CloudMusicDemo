package com.example.cloudmusicdemo.feature.favorites;

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

public class FavoritesFragment extends Fragment {
    
    private RecyclerView rvFavorites;
    private TextView tvFavoriteCount;
    private MusicAdapter adapter;
    private UserDataManager userDataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        
        userDataManager = UserDataManager.getInstance(requireContext());
        
        initViews(view);
        loadFavorites();
        
        return view;
    }
    
    private void initViews(View view) {
        rvFavorites = view.findViewById(R.id.rvFavorites);
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount);
        
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MusicAdapter(null);
        rvFavorites.setAdapter(adapter);
        
        adapter.setOnPlayClickListener((music, position) -> {
            if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
                List<Music> favorites = userDataManager.getFavorites();
                com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
                mainActivity.playMusicFromSearch(favorites, position);
                
                if (mainActivity.getCurrentFragment() instanceof com.example.cloudmusicdemo.feature.player.PlayerFragment) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        com.example.cloudmusicdemo.feature.player.PlayerFragment playerFragment = 
                            (com.example.cloudmusicdemo.feature.player.PlayerFragment) mainActivity.getCurrentFragment();
                        if (playerFragment != null && playerFragment.isAdded()) {
                            playerFragment.refreshUI();
                        }
                    }, 1000);
                }
            }
        });
        
        view.findViewById(R.id.ivBack).setOnClickListener(v -> {
            if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
                com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
                mainActivity.getSupportFragmentManager().popBackStack();
            }
        });
    }
    
    private void loadFavorites() {
        List<Music> favorites = userDataManager.getFavorites();
        
        if (favorites != null && !favorites.isEmpty()) {
            adapter.updateData(favorites);
            tvFavoriteCount.setText("共 " + favorites.size() + " 首歌曲");
        } else {
            tvFavoriteCount.setText("暂无收藏歌曲");
            Toast.makeText(getContext(), "暂无收藏歌曲", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (getActivity() instanceof com.example.cloudmusicdemo.MainActivity) {
            com.example.cloudmusicdemo.MainActivity mainActivity = (com.example.cloudmusicdemo.MainActivity) getActivity();
            mainActivity.showPlayControlBarView();
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