package com.example.cloudmusicdemo.feature.search;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cloudmusicdemo.MainActivity;
import com.example.cloudmusicdemo.R;
import com.example.cloudmusicdemo.feature.voice.VoiceAssistantActivity;

public class SearchFragment extends Fragment{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_search,container,false);
        
        // 语音助手按钮
        ImageView ivVoiceAssistant = view.findViewById(R.id.ivVoiceAssistant);
        ivVoiceAssistant.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), VoiceAssistantActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 进入搜索页面时隐藏播放栏
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hidePlayControlBar();
        }
    }
}
