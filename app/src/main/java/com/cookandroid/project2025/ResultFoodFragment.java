package com.cookandroid.project2025;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultFoodFragment extends Fragment {

    private LinearLayout foodContainerLayout;
    private TextView receivedJsonTextView;
    private String foodListJson;
    private JSONArray foodNameArray;

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

        receivedJsonTextView = view.findViewById(R.id.receivedJsonTextView);
        foodContainerLayout = view.findViewById(R.id.foodContainerLayout);

        if (getArguments() != null && getArguments().containsKey("foodListJson")) {
            foodListJson = getArguments().getString("foodListJson");
            receivedJsonTextView.setText("Received JSON: " + foodListJson);
            loadNutritionDataList(foodListJson);
        } else {
            receivedJsonTextView.setText("Received JSON: 데이터 없음");
        }
    }

    private void loadNutritionDataList(String foodListJson) {
        try {
            foodNameArray = new JSONArray(foodListJson);
            for (int i = 0; i < foodNameArray.length(); i++) {
                String foodName = foodNameArray.getString(i).trim();
                fetchAndDisplayNutrition(foodName);
            }
        } catch (JSONException e) {
            Log.e("JSONError", "JSON 파싱 오류: " + e.getMessage());
        }
    }

    private void fetchAndDisplayNutrition(String foodName) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("영양소 데이터베이스");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder nutritionInfo = new StringBuilder();
                String normalizedFoodName = foodName.replaceAll("\\u00A0", "").replaceAll("\\s+", " ");

                for (DataSnapshot item : snapshot.getChildren()) {
                    if (item.hasChild("음식명")) {
                        String dbFoodName = item.child("음식명").getValue(String.class);
                        if (dbFoodName != null && dbFoodName.replaceAll("\\u00A0", "").replaceAll("\\s+", " ").equals(normalizedFoodName)) {
                            for (DataSnapshot nutrient : item.getChildren()) {
                                nutritionInfo.append(nutrient.getKey()).append(": ").append(nutrient.getValue()).append("\n");
                            }
                            break;
                        }
                    }
                }

                addFoodView(foodName, nutritionInfo.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "데이터 로드 실패: " + error.getMessage());
            }
        });
    }

    private void addFoodView(String foodName, String nutritionText) {
        TextView textView = new TextView(getContext());
        textView.setText("--- " + foodName + " ---\n" + nutritionText);
        textView.setPadding(0, 16, 0, 8);

        Button saveButton = new Button(getContext());
        saveButton.setText("저장");

        saveButton.setOnClickListener(v -> {
            saveToFirebase(foodName, nutritionText);
            saveButton.setEnabled(false);
            saveButton.setText("저장됨");
        });

        foodContainerLayout.addView(textView);
        foodContainerLayout.addView(saveButton);
    }

    private void saveToFirebase(String foodName, String summary) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("UserNutritionData").child(uid).child(today);

        ref.child(foodName).setValue(summary)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), foodName + " 저장 완료", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}