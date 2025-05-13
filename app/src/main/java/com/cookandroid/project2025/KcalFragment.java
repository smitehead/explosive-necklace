package com.cookandroid.project2025;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KcalFragment extends Fragment {

    private TextView kcalTextView, carbsTextView, proteinTextView, fatTextView;
    private ProgressBar progressKcal, progressCarbs, progressProtein, progressFat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kcal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        kcalTextView = view.findViewById(R.id.kcalText);
        carbsTextView = view.findViewById(R.id.carbsText);
        proteinTextView = view.findViewById(R.id.proteinText);
        fatTextView = view.findViewById(R.id.fatText);

        progressKcal = view.findViewById(R.id.progressKcal);
        progressCarbs = view.findViewById(R.id.progressCarbs);
        progressProtein = view.findViewById(R.id.progressProtein);
        progressFat = view.findViewById(R.id.progressFat);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference userInfoRef = FirebaseDatabase.getInstance().getReference("UserAccount").child(uid);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("UserNutritionData").child(uid).child(today);

        userInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.child("gender").getValue(String.class);
                int age = snapshot.child("age").getValue(Integer.class);

                double kcalGoal = ("여자".equals(gender)) ? (age < 30 ? 2100 : 1900) : (age < 30 ? 2600 : 2400);
                double carbsGoal = ("여자".equals(gender)) ? (age < 30 ? 300 : 270) : (age < 30 ? 330 : 300);
                double proteinGoal = ("여자".equals(gender)) ? (age < 30 ? 80 : 70) : (age < 30 ? 100 : 90);
                double fatGoal = ("여자".equals(gender)) ? (age < 30 ? 60 : 55) : (age < 30 ? 80 : 70);

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        double totalEnergy = 0;
                        double totalCarbs = 0;
                        double totalProtein = 0;
                        double totalFat = 0;

                        for (DataSnapshot foodSnap : snapshot.getChildren()) {
                            String value = foodSnap.getValue(String.class);
                            if (value == null) continue;

                            String[] lines = value.split("\n");
                            for (String line : lines) {
                                line = line.toLowerCase();

                                if (line.contains("에너지") || line.contains("kcal") || line.contains("칼로리")) {
                                    totalEnergy += extractFloat(line);
                                } else if (line.contains("탄수화물")) {
                                    totalCarbs += extractFloat(line);
                                } else if (line.contains("단백질")) {
                                    totalProtein += extractFloat(line);
                                } else if (line.contains("지방")) {
                                    totalFat += extractFloat(line);
                                }
                            }
                        }

                        kcalTextView.setText((int) totalEnergy + "k");
                        carbsTextView.setText((int) totalCarbs + "g");
                        proteinTextView.setText((int) totalProtein + "g");
                        fatTextView.setText((int) totalFat + "g");

                        // 게이지 반영
                        int kcalPercent = (int) ((totalEnergy / kcalGoal) * 100);
                        int carbsPercent = (int) ((totalCarbs / carbsGoal) * 100);
                        int proteinPercent = (int) ((totalProtein / proteinGoal) * 100);
                        int fatPercent = (int) ((totalFat / fatGoal) * 100);

                        progressKcal.setProgress(Math.min(kcalPercent, 100));
                        progressCarbs.setProgress(Math.min(carbsPercent, 100));
                        progressProtein.setProgress(Math.min(proteinPercent, 100));
                        progressFat.setProgress(Math.min(fatPercent, 100));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "사용자 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double extractFloat(String line) {
        try {
            String value = line.split(":")[1].trim().replaceAll("[^\\d.]", "");
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }
}