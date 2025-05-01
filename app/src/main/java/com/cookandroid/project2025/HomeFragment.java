package com.cookandroid.project2025;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class HomeFragment extends Fragment {

    public HomeFragment() {}

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        TextView overKcalTextView = view.findViewById(R.id.overKcalTextView);
        TextView recommendationTextView = view.findViewById(R.id.recommendationTextView);
        Button labelScanButton = view.findViewById(R.id.buttonLabelScan);

        labelScanButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new LabelFragment())
                    .addToBackStack(null)
                    .commit();
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

                        double overKcal = Math.max(totalKcal - kcalGoal, 0);
                        double overCarbs = Math.max(totalCarbs - carbsGoal, 0);
                        double overProtein = Math.max(totalProtein - proteinGoal, 0);
                        double overFat = Math.max(totalFat - fatGoal, 0);

                        overKcalTextView.setText("오늘 초과 섭취: " +
                                (int) overKcal + "Kcal, " +
                                (int) overCarbs + "g 탄수화물, " +
                                (int) overProtein + "g 단백질, " +
                                (int) overFat + "g 지방");

                        if (overKcal <= 0 && overCarbs <= 0 && overProtein <= 0 && overFat <= 0) {
                            recommendationTextView.setText("초과된 섭취가 없어 운동 추천이 필요하지 않습니다.");
                            return;
                        }

                        DatabaseReference exerciseRef = FirebaseDatabase.getInstance().getReference("운동소모칼로리");
                        exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Map<String, Double> scoreMap = new HashMap<>();
                                Map<String, Integer> timeMap = new HashMap<>();

                                for (DataSnapshot exercise : snapshot.getChildren()) {
                                    String name = exercise.getKey();
                                    double burn = exercise.child("소모칼로리").getValue(Double.class);
                                    DataSnapshot ratio = exercise.child("비율");

                                    double cRatio = ratio.child("탄수화물").getValue(Double.class);
                                    double pRatio = ratio.child("단백질").getValue(Double.class);
                                    double fRatio = ratio.child("지방").getValue(Double.class);

                                    double score = (overCarbs * cRatio) + (overProtein * pRatio) + (overFat * fRatio);
                                    int minutes = (int) ((overKcal / burn) * 30);

                                    scoreMap.put(name, score);
                                    timeMap.put(name, minutes);
                                }

                                List<Map.Entry<String, Double>> sorted = new ArrayList<>(scoreMap.entrySet());
                                sorted.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                                StringBuilder top3 = new StringBuilder("추천 운동 상위 3개\n");
                                for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                                    String name = sorted.get(i).getKey();
                                    int minutes = timeMap.get(name);
                                    top3.append("• ").append(name).append(": ").append(minutes).append("분\n");
                                }

                                recommendationTextView.setText(top3.toString());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                recommendationTextView.setText("운동 데이터를 불러올 수 없습니다.");
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        overKcalTextView.setText("섭취량 데이터 로드 실패");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                overKcalTextView.setText("사용자 정보 로드 실패");
            }
        });

        return view;
    }

    private double extractFloat(String line) {
        try {
            return Double.parseDouble(line.split(":")[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
