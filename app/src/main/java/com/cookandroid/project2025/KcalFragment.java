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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class KcalFragment extends Fragment {

    private TextView kcalTextView, carbTextView, proteinTextView, fatTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kcal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ID 연결
        kcalTextView = view.findViewById(R.id.textView7);      // kcal
        carbTextView = view.findViewById(R.id.textViewt5);     // 탄수화물
        proteinTextView = view.findViewById(R.id.textView5);   // 단백질
        fatTextView = view.findViewById(R.id.textView6);       // 지방

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserNutritionData").child(uid).child(today);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalEnergy = 0;
                int totalCarbs = 0;
                int totalProtein = 0;
                int totalFat = 0;

                for (DataSnapshot foodSnap : snapshot.getChildren()) {
                    String value = foodSnap.getValue(String.class);
                    if (value == null) continue;

                    String[] lines = value.split("\n");
                    for (String line : lines) {
                        if (line.contains("에너지")) {
                            totalEnergy += extractInt(line);
                        } else if (line.contains("탄수화물")) {
                            totalCarbs += extractInt(line);
                        } else if (line.contains("단백질")) {
                            totalProtein += extractInt(line);
                        } else if (line.contains("지방")) {
                            totalFat += extractInt(line);
                        }
                    }
                }

                // 위치에 맞춰서 출력
                kcalTextView.setText(totalEnergy + " Kcal");
                carbTextView.setText("탄수화물 " + totalCarbs + "g");
                proteinTextView.setText("단백질 " + totalProtein + "g");
                fatTextView.setText("지방 " + totalFat + "g");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int extractInt(String line) {
        String digits = line.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }
}
