package com.cookandroid.project2025;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

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

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResultFoodFragment extends Fragment {

    private LinearLayout foodResultContainer;
    private String foodListJson;
    private JSONArray foodNameArray;
    private int processedCount = 0;

    public static ResultFoodFragment newInstance(String foodListJson) {
        ResultFoodFragment fragment = new ResultFoodFragment();
        Bundle args = new Bundle();
        args.putString("foodListJson", foodListJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_result_food_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        foodResultContainer = view.findViewById(R.id.foodResultContainer);

        if (getArguments() != null && getArguments().containsKey("foodListJson")) {
            foodListJson = getArguments().getString("foodListJson");
            loadNutritionDataList(foodListJson);
        }
    }

    private void loadNutritionDataList(String foodListJson) {
        try {
            foodNameArray = new JSONArray(foodListJson);
            processedCount = 0;

            for (int i = 0; i < foodNameArray.length(); i++) {
                String foodName = foodNameArray.getString(i).trim();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                        .getReference("UserNutritionData").child("데이터");

                final String currentFoodName = foodName;

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot item : snapshot.getChildren()) {
                            String dbFoodName = item.child("음식명").getValue(String.class);
                            if (dbFoodName != null && dbFoodName.trim().equalsIgnoreCase(currentFoodName)) {
                                Map<String, String> nutritionMap = new HashMap<>();
                                for (DataSnapshot nutrient : item.getChildren()) {
                                    nutritionMap.put(nutrient.getKey(), nutrient.getValue().toString());
                                }
                                addFoodCard(currentFoodName, nutritionMap);
                                break;
                            }
                        }
                        processedCount++;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        processedCount++;
                    }
                });
            }
        } catch (JSONException e) {
            Toast.makeText(getContext(), "JSON 파싱 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addFoodCard(String foodName, Map<String, String> nutritionMap) {
        View cardView = LayoutInflater.from(getContext()).inflate(R.layout.item_food_result_card, foodResultContainer, false);

        TextView foodNameText = cardView.findViewById(R.id.foodNameText);
        TextView nutritionText = cardView.findViewById(R.id.nutritionText);
        EditText gramInput = cardView.findViewById(R.id.gramInput);
        Button saveButton = cardView.findViewById(R.id.saveButton);

        foodNameText.setText(foodName);

        StringBuilder rawText = new StringBuilder();
        for (Map.Entry<String, String> entry : nutritionMap.entrySet()) {
            if (!entry.getKey().equals("음식명")) {
                rawText.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        nutritionText.setText(rawText.toString());

        saveButton.setOnClickListener(v -> {
            int gram = 100;
            try {
                gram = Integer.parseInt(gramInput.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            double ratio = gram / 100.0;
            StringBuilder adjustedSummary = new StringBuilder();

            for (Map.Entry<String, String> entry : nutritionMap.entrySet()) {
                String key = entry.getKey();
                if (key.equals("음식명")) continue;
                try {
                    double value = Double.parseDouble(entry.getValue());
                    double adjusted = Math.round(value * ratio * 10) / 10.0;
                    adjustedSummary.append(key).append(": ").append(adjusted).append("\n");
                } catch (Exception e) {
                    adjustedSummary.append(key).append(": ").append(entry.getValue()).append("\n");
                }
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("UserNutritionData").child(uid).child(today);

            userRef.child(foodName).setValue(adjustedSummary.toString())
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(), "저장 완료!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        foodResultContainer.addView(cardView);
    }
}