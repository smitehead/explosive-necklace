package com.cookandroid.project2025;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ResultFoodFragment extends Fragment {

    private TextView nutritionInfoTextView;
    private TextView receivedJsonTextView;
    private String foodListJson;
    private int processedCount = 0;
    private StringBuilder allNutritionInfo = new StringBuilder();
    private List<String> notFoundFoods = new ArrayList<>();
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
        super.onViewCreated(view, savedInstanceState); // 올바른 메소드 이름 사용

        nutritionInfoTextView = view.findViewById(R.id.nutritionInfoTextView);
        receivedJsonTextView = view.findViewById(R.id.receivedJsonTextView);

        if (getArguments() != null && getArguments().containsKey("foodListJson")) {
            foodListJson = getArguments().getString("foodListJson");
            receivedJsonTextView.setText("Received JSON: " + foodListJson);
            loadNutritionDataList(foodListJson);
        } else {
            nutritionInfoTextView.setText("데이터를 불러올 수 없습니다.");
            receivedJsonTextView.setText("Received JSON: 데이터 없음");
            Log.e("ResultFoodFragment", "foodListJson이 전달되지 않았습니다.");
        }
    }

    private void loadNutritionDataList(String foodListJson) {
        try {
            foodNameArray = new JSONArray(foodListJson); // JSON 배열로 파싱

            processedCount = 0;
            allNutritionInfo.setLength(0);
            notFoundFoods.clear();
            final boolean[] foundAnyFood = {false};

            for (int i = 0; i < foodNameArray.length(); i++) {
                String foodName = foodNameArray.getString(i).trim();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("영양소 데이터베이스");
                final String currentFoodName = foodName;
                final int currentIndex = i;

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        StringBuilder nutritionInfo = new StringBuilder();
                        boolean currentFoodFound = false;
                        String normalizedCurrentFoodName = currentFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " "); // 공백 및 non-breaking space 제거 후 중간 공백 하나로 축소

                        for (DataSnapshot numberSnapshot : snapshot.getChildren()) {
                            if (numberSnapshot.hasChild("음식명")) {
                                String dbFoodName = numberSnapshot.child("음식명").getValue(String.class);
                                if (dbFoodName != null) {
                                    String normalizedDbFoodName = dbFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " "); // 데이터베이스 값도 정규화 후 중간 공백 하나로 축소
                                    if (normalizedDbFoodName.equals(normalizedCurrentFoodName)) {
                                        currentFoodFound = true;
                                        for (DataSnapshot nutrientSnapshot : numberSnapshot.getChildren()) {
                                            nutritionInfo.append(nutrientSnapshot.getKey()).append(": ").append(nutrientSnapshot.getValue()).append("\n");
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        final String result = nutritionInfo.toString();
                        if (!result.isEmpty()) {
                            allNutritionInfo.append("--- ").append(currentFoodName).append(" ---\n");
                            allNutritionInfo.append(result).append("\n");
                            foundAnyFood[0] = true;
                        } else {
                            notFoundFoods.add(currentFoodName);
                        }

                        processedCount++;
                        if (processedCount == foodNameArray.length()) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    StringBuilder finalNutritionText = new StringBuilder();
                                    if (foundAnyFood[0]) {
                                        finalNutritionText.append(allNutritionInfo);
                                    }
                                    if (!notFoundFoods.isEmpty()) {
                                        finalNutritionText.append("\n찾을 수 없는 음식:\n");
                                        for (String notFoundFood : notFoundFoods) {
                                            finalNutritionText.append("- ").append(notFoundFood).append("\n");
                                        }
                                    } else if (!foundAnyFood[0]) {
                                        finalNutritionText.append("해당 음식들의 정보를 찾을 수 없습니다.");
                                    }
                                    nutritionInfoTextView.setText(finalNutritionText.toString());
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "데이터 로드 실패 for " + currentFoodName + ": " + error.getMessage());
                        processedCount++;
                        if (processedCount == foodNameArray.length()) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    StringBuilder finalNutritionText = new StringBuilder();
                                    if (foundAnyFood[0]) {
                                        finalNutritionText.append(allNutritionInfo);
                                    }
                                    finalNutritionText.append("\n데이터 로드 중 일부 실패.");
                                    if (!notFoundFoods.isEmpty()) {
                                        finalNutritionText.append("\n찾을 수 없는 음식:\n");
                                        for (String notFoundFood : notFoundFoods) {
                                            finalNutritionText.append("- ").append(notFoundFood).append("\n");
                                        }
                                    } else if (!foundAnyFood[0] && allNutritionInfo.length() == 0) {
                                        nutritionInfoTextView.setText("데이터 로드 실패.");
                                    }
                                    nutritionInfoTextView.setText(finalNutritionText.toString());
                                });
                            }
                        }
                    }
                });
            }

        } catch (JSONException e) {
            Log.e("JSONError", "JSON 파싱 오류: " + e.getMessage() + "\nReceived JSON: " + foodListJson);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    nutritionInfoTextView.setText("데이터를 불러오는 중 오류가 발생했습니다.");
                    receivedJsonTextView.setText("Received JSON (Error): " + foodListJson);
                });
            }
            return;
        }
    }
}