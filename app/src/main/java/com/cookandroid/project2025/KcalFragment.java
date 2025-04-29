package com.cookandroid.project2025;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
        userInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.child("gender").getValue(String.class);
                Long ageLong = snapshot.child("age").getValue(Long.class);
                int age = (ageLong != null) ? ageLong.intValue() : 25;

                // 기준 권장량 설정
                double kcalGoal, carbsGoal, proteinGoal, fatGoal;

                if ("여자".equals(gender)) {
                    if (age < 30) {
                        kcalGoal = 2100;
                        carbsGoal = 300;
                        proteinGoal = 80;
                        fatGoal = 60;
                    } else {
                        kcalGoal = 1900;
                        carbsGoal = 270;
                        proteinGoal = 70;
                        fatGoal = 55;
                    }
                } else {
                    if (age < 30) {
                        kcalGoal = 2600;
                        carbsGoal = 330;
                        proteinGoal = 100;
                        fatGoal = 80;
                    } else {
                        kcalGoal = 2400;
                        carbsGoal = 300;
                        proteinGoal = 90;
                        fatGoal = 70;
                    }
                }

                DatabaseReference ref = FirebaseDatabase.getInstance()
                        .getReference("UserNutritionData").child(uid).child(today);

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

                        // 정수로 변환하여 텍스트 표시
                        kcalTextView.setText((int) totalEnergy + " Kcal");
                        carbsTextView.setText((int) totalCarbs + "g");
                        proteinTextView.setText((int) totalProtein + "g");
                        fatTextView.setText((int) totalFat + "g");

                        // 색상 로딩
                        int blue = ContextCompat.getColor(requireContext(), R.color.blue);
                        int red = ContextCompat.getColor(requireContext(), R.color.red);

                        // 진행도 및 색상 설정
                        setProgressWithColor(progressKcal, totalEnergy, kcalGoal, blue, red);
                        setProgressWithColor(progressCarbs, totalCarbs, carbsGoal, blue, red);
                        setProgressWithColor(progressProtein, totalProtein, proteinGoal, blue, red);
                        setProgressWithColor(progressFat, totalFat, fatGoal, blue, red);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "섭취량 데이터 로드 실패", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "사용자 정보 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double extractFloat(String line) {
        try {
            return Double.parseDouble(line.split(":")[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private void setProgressWithColor(ProgressBar bar, double value, double goal, int normalColor, int exceedColor) {
        int percent = (int) ((value / goal) * 100);
        bar.setProgress(Math.min(percent, 100));
        bar.setProgressTintList(ColorStateList.valueOf((value > goal) ? exceedColor : normalColor));
    }
}
