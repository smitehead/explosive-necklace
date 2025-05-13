package com.cookandroid.project2025;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    private TextView kcalTextView, carbsTextView, proteinTextView, fatTextView, textRemainingKcal;
    private ProgressBar progressKcal, progressCarbs, progressProtein, progressFat;
    private TextView overKcalTextView, recommendationTextView;
    private PieChart pieChart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        kcalTextView = view.findViewById(R.id.kcalText);
        carbsTextView = view.findViewById(R.id.carbsText);
        proteinTextView = view.findViewById(R.id.proteinText);
        fatTextView = view.findViewById(R.id.fatText);
        textRemainingKcal = view.findViewById(R.id.textRemainingKcal);


        progressKcal = view.findViewById(R.id.progressKcal);
        progressCarbs = view.findViewById(R.id.progressCarbs);
        progressProtein = view.findViewById(R.id.progressProtein);
        progressFat = view.findViewById(R.id.progressFat);

        overKcalTextView = view.findViewById(R.id.overKcalTextView);
        recommendationTextView = view.findViewById(R.id.recommendationTextView);
        pieChart = view.findViewById(R.id.pieChart);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return view;

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference userInfoRef = FirebaseDatabase.getInstance().getReference("UserAccount").child(uid);
        DatabaseReference nutritionRef = FirebaseDatabase.getInstance().getReference("UserNutritionData").child(uid).child(today);

        userInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String gender = snapshot.child("gender").getValue(String.class);
                int age = snapshot.child("age").getValue(Integer.class);

                double kcalGoal = ("여자".equals(gender)) ? (age < 30 ? 2100 : 1900) : (age < 30 ? 2600 : 2400);
                double carbsGoal = ("여자".equals(gender)) ? (age < 30 ? 300 : 270) : (age < 30 ? 330 : 300);
                double proteinGoal = ("여자".equals(gender)) ? (age < 30 ? 80 : 70) : (age < 30 ? 100 : 90);
                double fatGoal = ("여자".equals(gender)) ? (age < 30 ? 60 : 55) : (age < 30 ? 80 : 70);

                nutritionRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        double totalKcal = 0, totalCarbs = 0, totalProtein = 0, totalFat = 0;

                        for (DataSnapshot food : dataSnapshot.getChildren()) {
                            String value = food.getValue(String.class);
                            if (value == null) continue;

                            for (String line : value.split("\n")) {
                                line = line.toLowerCase();
                                if (line.contains("에너지") || line.contains("kcal") || line.contains("칼로리")) {
                                    totalKcal += extractFloat(line);
                                } else if (line.contains("탄수화물")) {
                                    totalCarbs += extractFloat(line);
                                } else if (line.contains("단백질")) {
                                    totalProtein += extractFloat(line);
                                } else if (line.contains("지방")) {
                                    totalFat += extractFloat(line);
                                }
                            }
                        }

                        kcalTextView.setText((int) totalKcal + "k");
                        carbsTextView.setText((int) totalCarbs + "g");
                        proteinTextView.setText((int) totalProtein + "g");
                        fatTextView.setText((int) totalFat + "g");

                        int kcalPercent = (int) ((totalKcal / kcalGoal) * 100);
                        int carbsPercent = (int) ((totalCarbs / carbsGoal) * 100);
                        int proteinPercent = (int) ((totalProtein / proteinGoal) * 100);
                        int fatPercent = (int) ((totalFat / fatGoal) * 100);

                        progressKcal.setProgress(Math.min(kcalPercent, 100));
                        progressCarbs.setProgress(Math.min(carbsPercent, 100));
                        progressProtein.setProgress(Math.min(proteinPercent, 100));
                        progressFat.setProgress(Math.min(fatPercent, 100));

                        float kcalConsumed = (float) totalKcal;
                        float kcalRemaining = (float) Math.max(kcalGoal - totalKcal, 0);

                        // Pie Chart 설정
                        List<PieEntry> entries = new ArrayList<>();
                        entries.add(new PieEntry(kcalConsumed, ""));
                        entries.add(new PieEntry(kcalRemaining, ""));

                        PieDataSet dataSet = new PieDataSet(entries, "");
                        dataSet.setDrawValues(false);
                        dataSet.setColors(new int[]{R.color.teal_200, R.color.gray}, requireContext());
                        PieData pieData = new PieData(dataSet);

                        pieChart.setData(pieData);
                        pieChart.setUsePercentValues(false);
                        pieChart.setDrawHoleEnabled(true);
                        pieChart.setHoleRadius(72f);
                        pieChart.setTransparentCircleRadius(74f);
                        pieChart.getLegend().setEnabled(false);

                        Description desc = new Description();
                        desc.setText("");
                        pieChart.setDescription(desc);

                        Typeface pretendard = ResourcesCompat.getFont(requireContext(), R.font.pretendard_medium);
                        pieChart.setCenterTextTypeface(pretendard);

                        String kcalValue = String.valueOf((int) kcalConsumed);
                        SpannableString styledText = new SpannableString(kcalValue + "\nkcal");
                        styledText.setSpan(new RelativeSizeSpan(2.4f), 0, kcalValue.length(), 0);
                        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, kcalValue.length(), 0);
                        styledText.setSpan(new RelativeSizeSpan(1.6f), kcalValue.length() + 1, styledText.length(), 0);
                        pieChart.setCenterText(styledText);

                        pieChart.invalidate();
                        textRemainingKcal.setText("남은 칼로리: " + (int) kcalRemaining + " kcal");

                        // 초과 섭취 정보 계산 및 표시
                        double overKcal = Math.max(totalKcal - kcalGoal, 0);
                        double overCarbs = Math.max(totalCarbs - carbsGoal, 0);
                        double overProtein = Math.max(totalProtein - proteinGoal, 0);
                        double overFat = Math.max(totalFat - fatGoal, 0);

                        if (overKcal <= 0 && overCarbs <= 0 && overProtein <= 0 && overFat <= 0) {
                            overKcalTextView.setText("초과된 섭취 없음");
                        } else {
                            overKcalTextView.setText("오늘 초과 섭취: " +
                                    (int) overKcal + "Kcal, " +
                                    (int) overCarbs + "g 탄수화물, " +
                                    (int) overProtein + "g 단백질, " +
                                    (int) overFat + "g 지방");
                        }
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

        return view;
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
