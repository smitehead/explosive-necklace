package com.cookandroid.project2025;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.cookandroid.project2025.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if(itemId==R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId==R.id.profile) {
                replaceFragment(new ProfileFragment());
            } else if (itemId==R.id.check) {
                replaceFragment(new CheckFragment());
            } else if (itemId == R.id.kcal) {
                replaceFragment(new KcalFragment());
            } else if (itemId == R.id.multicheck) {
                replaceFragment(new MultiCheckActivity());
            }
            return true;

        });
    }

    private void replaceFragment(Fragment fragment){

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout,fragment);
        fragmentTransaction.commit();
    }
}