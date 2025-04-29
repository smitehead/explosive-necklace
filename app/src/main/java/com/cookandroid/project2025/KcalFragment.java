package com.cookandroid.project2025;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KcalFragment extends Fragment {

    private TextView kcalTextView, carbsTextView, proteinTextView, fatTextView;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");  // 소수점 둘째 자리까지 표시

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kcal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // XML에 맞는 ID 연결
        kcalTextView = view.findViewById(R.id.kcalAmountTextView);
        carbsTextView = view.findViewById(R.id.carbsAmountTextView);
        proteinTextView = view.findViewById(R.id.proteinAmountTextView);
        fatTextView = view.findViewById(R.id.fatAmountTextView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

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
                        line = line.toLowerCase(); // 대소문자 무시 처리

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

                // 최종 누적 영양소 결과 표시
                kcalTextView.setText(decimalFormat.format(totalEnergy) + " Kcal");
                carbsTextView.setText(decimalFormat.format(totalCarbs) + "g");
                proteinTextView.setText(decimalFormat.format(totalProtein) + "g");
                fatTextView.setText(decimalFormat.format(totalFat) + "g");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // "에너지: 253.7" → 253.7
    private double extractFloat(String line) {
        try {
            return Double.parseDouble(line.split(":")[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
