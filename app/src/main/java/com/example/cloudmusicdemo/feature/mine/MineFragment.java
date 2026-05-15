package com.example.cloudmusicdemo.feature.mine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;

public class MineFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_mine,container,false);
        
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 进入我的页面时隐藏播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
    }
}
