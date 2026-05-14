package com.example.cloudmusicdemo;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.cloudmusicdemo.feature.mine.MineFragment;
import com.example.cloudmusicdemo.feature.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.cloudmusicdemo.feature.home.HomeFragment;


public class MainActivity extends AppCompatActivity {
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav=findViewById(R.id.bottom_nav);

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId=item.getItemId();

                if(itemId==R.id.nav_home){
                    switchFragment(new HomeFragment());
                    return true;
                }else if(itemId==R.id.nav_search){
                    switchFragment(new SearchFragment());
                    return true;
                }else if(itemId==R.id.nav_mine){
                    switchFragment(new MineFragment());
                    return true;
                }
                return false;
            }
        });

        switchFragment(new HomeFragment());
    }

    private void switchFragment(Fragment targetFragment){
        if(currentFragment==targetFragment){
            return;
        }

        FragmentManager fm=getSupportFragmentManager();
        FragmentTransaction transaction=fm.beginTransaction();

        if(currentFragment!=null){
            transaction.hide(currentFragment);
        }

        if(targetFragment.isAdded()){
            transaction.show(targetFragment);
        }else {
            transaction.add(R.id.fragment_container,targetFragment);
        }
        transaction.commit();
        currentFragment=targetFragment;
    }
}