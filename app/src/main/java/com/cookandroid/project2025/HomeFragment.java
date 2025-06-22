package com.cookandroid.project2025;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
    private ProgressBar progressCarbs, progressProtein, progressFat;
    private TextView overKcalTextView, overCarbsTextView, overProteinTextView, overFatTextView, recommendationTextView;
    private PieChart pieChart;
    private Button buttonMultiFood;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        carbsTextView = view.findViewById(R.id.carbsText);
        proteinTextView = view.findViewById(R.id.proteinText);
        fatTextView = view.findViewById(R.id.fatText);
        textRemainingKcal = view.findViewById(R.id.textRemainingKcal);

        progressCarbs = view.findViewById(R.id.progressCarbs);
        progressProtein = view.findViewById(R.id.progressProtein);
        progressFat = view.findViewById(R.id.progressFat);

        overKcalTextView = view.findViewById(R.id.overKcalTextView);
        overCarbsTextView = view.findViewById(R.id.overCarbsTextView);
        overProteinTextView = view.findViewById(R.id.overProteinTextView);
        overFatTextView = view.findViewById(R.id.overFatTextView);
        recommendationTextView = view.findViewById(R.id.recommendationTextView);
        pieChart = view.findViewById(R.id.pieChart);
        DrawerLayout drawerLayout = requireActivity().findViewById(R.id.drawerLayout);
        drawerLayout.closeDrawer(GravityCompat.END, false);
        ImageButton rightButton = view.findViewById(R.id.rightButton);

        rightButton.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });


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

                        carbsTextView.setText(String.format("%,d g", (int) totalCarbs));
                        proteinTextView.setText(String.format("%,d g", (int) totalProtein));
                        fatTextView.setText(String.format("%,d g", (int) totalFat));

                        int carbsPercent = (int) ((totalCarbs / carbsGoal) * 100);
                        int proteinPercent = (int) ((totalProtein / proteinGoal) * 100);
                        int fatPercent = (int) ((totalFat / fatGoal) * 100);

                        progressCarbs.setProgress(Math.min(carbsPercent, 100));
                        progressProtein.setProgress(Math.min(proteinPercent, 100));
                        progressFat.setProgress(Math.min(fatPercent, 100));

                        // 색상 동적으로 변경 (100% 초과 시 빨강)
                        if (carbsPercent > 100) {
                            progressCarbs.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressCarbs.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                        if (proteinPercent > 100) {
                            progressProtein.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressProtein.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }

                        if (fatPercent > 100) {
                            progressFat.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.red));
                        } else {
                            progressFat.setProgressTintList(ContextCompat.getColorStateList(requireContext(), R.color.blue));
                        }


                        float kcalConsumed = (float) totalKcal;
                        float kcalRemaining = (float) Math.max(kcalGoal - totalKcal, 0);

                        List<PieEntry> entries = new ArrayList<>();
                        entries.add(new PieEntry(kcalConsumed, ""));
                        entries.add(new PieEntry(kcalRemaining, ""));

                        int colorConsumed;
                        if (totalKcal > kcalGoal) {
                            colorConsumed = ContextCompat.getColor(requireContext(), R.color.red);
                        } else {
                            colorConsumed = ContextCompat.getColor(requireContext(), R.color.teal_200);
                        }

                        int colorRemaining = ContextCompat.getColor(requireContext(), R.color.gray);

                        PieDataSet dataSet = new PieDataSet(entries, "");
                        dataSet.setDrawValues(false);
                        dataSet.setColors(new int[]{colorConsumed, colorRemaining});

                        PieData pieData = new PieData(dataSet);

                        pieChart.setData(pieData);
                        pieChart.setUsePercentValues(false);
                        pieChart.setDrawHoleEnabled(true);
                        pieChart.setHoleRadius(72f);
                        pieChart.setTransparentCircleRadius(74f);
                        pieChart.getLegend().setEnabled(false);
                        pieChart.setTouchEnabled(false);

                        Description desc = new Description();
                        desc.setText("");
                        pieChart.setDescription(desc);

                        Typeface pretendard = ResourcesCompat.getFont(requireContext(), R.font.pretendard_medium);
                        pieChart.setCenterTextTypeface(pretendard);

                        String kcalValue = String.format("%,d", (int) kcalConsumed);
                        SpannableString styledText = new SpannableString(kcalValue + "\nkcal");
                        styledText.setSpan(new RelativeSizeSpan(2.4f), 0, kcalValue.length(), 0);
                        styledText.setSpan(new StyleSpan(Typeface.BOLD), 0, kcalValue.length(), 0);
                        styledText.setSpan(new RelativeSizeSpan(1.6f), kcalValue.length() + 1, styledText.length(), 0);
                        pieChart.setCenterText(styledText);

                        pieChart.invalidate();
                        textRemainingKcal.setText(String.format("남은 칼로리: %,d kcal", (int) kcalRemaining));

                        double overKcal = Math.max(totalKcal - kcalGoal, 0);
                        double overCarbs = Math.max(totalCarbs - carbsGoal, 0);
                        double overProtein = Math.max(totalProtein - proteinGoal, 0);
                        double overFat = Math.max(totalFat - fatGoal, 0);

                        if (overKcal <= 0 && overCarbs <= 0 && overProtein <= 0 && overFat <= 0) {
                            recommendationTextView.setText("초과된 영양소가\n없습니다");
                            overKcalTextView.setText("0 k");
                            overCarbsTextView.setText("0 g");
                            overProteinTextView.setText("0 g");
                            overFatTextView.setText("0 g");
                        } else {
                            overKcalTextView.setText(String.format("%,d k", (int) overKcal));
                            overCarbsTextView.setText(String.format("%,d g", (int) overCarbs));
                            overProteinTextView.setText(String.format("%,d g", (int) overProtein));
                            overFatTextView.setText(String.format("%,d g", (int) overFat));
                            recommendationTextView.setText("초과된 영양소가\n없습니다");
                            DatabaseReference exerciseRef = FirebaseDatabase.getInstance().getReference("운동소모칼로리");
                            exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Map<String, Double> scoreMap = new HashMap<>();
                                    Map<String, Integer> timeMap = new HashMap<>();

                                    for (DataSnapshot exercise : snapshot.getChildren()) {
                                        String name = exercise.getKey();
                                        Double burn = exercise.child("소모칼로리").getValue(Double.class);
                                        if (burn == null || burn == 0) continue;

                                        DataSnapshot ratio = exercise.child("비율");

                                        Double cRatio = ratio.child("탄수화물").getValue(Double.class);
                                        Double pRatio = ratio.child("단백질").getValue(Double.class);
                                        Double fRatio = ratio.child("지방").getValue(Double.class);

                                        if (cRatio == null || pRatio == null || fRatio == null) continue;

                                        double score = (overCarbs * cRatio) + (overProtein * pRatio) + (overFat * fRatio);
                                        int minutes = (int) ((overKcal / burn) * 30);  // 30분 기준

                                        scoreMap.put(name, score);
                                        timeMap.put(name, minutes);
                                    }

                                    List<Map.Entry<String, Double>> sorted = new ArrayList<>(scoreMap.entrySet());
                                    sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                                    StringBuilder top3 = new StringBuilder();
                                    for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                                        String name = sorted.get(i).getKey();
                                        int minutes = timeMap.get(name);
                                        top3.append("•").append(name).append(": ").append(minutes).append("분\n");
                                    }
                                    recommendationTextView.setText(top3.toString());
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    recommendationTextView.setText("운동 데이터를 불러올 수 없습니다.");
                                }
                            });

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
