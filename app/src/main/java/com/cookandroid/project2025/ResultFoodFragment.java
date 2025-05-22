package com.cookandroid.project2025;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultFoodFragment extends Fragment {

    private TextView nutritionInfoTextView;
    private TextView receivedJsonTextView;
    private EditText gramInput;
    private Button buttonConfirmSave;
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
        super.onViewCreated(view, savedInstanceState);

        nutritionInfoTextView = view.findViewById(R.id.nutritionInfoTextView);
        receivedJsonTextView = view.findViewById(R.id.receivedJsonTextView);
        gramInput = view.findViewById(R.id.gramInput);
        buttonConfirmSave = view.findViewById(R.id.buttonConfirmSave);

        if (getArguments() != null && getArguments().containsKey("foodListJson")) {
            foodListJson = getArguments().getString("foodListJson");
            receivedJsonTextView.setText("Received JSON: " + foodListJson);
            loadNutritionDataList(foodListJson);
        } else {
            nutritionInfoTextView.setText("데이터를 불러올 수 없습니다.");
            receivedJsonTextView.setText("Received JSON: 데이터 없음");
            Log.e("ResultFoodFragment", "foodListJson이 전달되지 않았습니다.");
        }

        buttonConfirmSave.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            int inputGram = 100;
            try {
                inputGram = Integer.parseInt(gramInput.getText().toString().trim());
            } catch (NumberFormatException ignored) {}

            double ratio = inputGram / 100.0;

            String uid = user.getUid();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("UserNutritionData").child(uid).child(today);

            try {
                for (int i = 0; i < foodNameArray.length(); i++) {
                    String foodName = foodNameArray.getString(i).trim();
                    String[] lines = allNutritionInfo.toString().split("--- ");
                    StringBuilder adjustedSummary = new StringBuilder();

                    for (String block : lines) {
                        if (block.startsWith(foodName)) {
                            String[] sublines = block.split("\n");
                            adjustedSummary.append("--- ").append(foodName).append(" ---\n");

                            for (int j = 1; j < sublines.length; j++) {
                                String[] parts = sublines[j].split(":");
                                if (parts.length == 2) {
                                    String nutrient = parts[0].trim();
                                    String valueStr = parts[1].trim().replaceAll("[^\\d.]", "");
                                    try {
                                        double value = Double.parseDouble(valueStr);
                                        double adjusted = Math.round(value * ratio * 10) / 10.0;
                                        adjustedSummary.append(nutrient).append(": ").append(adjusted).append("\n");
                                    } catch (NumberFormatException e) {
                                        adjustedSummary.append(sublines[j]).append("\n");
                                    }
                                } else {
                                    adjustedSummary.append(sublines[j]).append("\n");
                                }
                            }
                            break;
                        }
                    }

                    userRef.child(foodName).setValue(adjustedSummary.toString());
                }

                Toast.makeText(getContext(), "오늘 식단에 저장되었습니다.", Toast.LENGTH_SHORT).show();

            } catch (JSONException e) {
                Log.e("JSON", "저장 중 오류", e);
                Toast.makeText(getContext(), "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNutritionDataList(String foodListJson) {
        try {
            foodNameArray = new JSONArray(foodListJson);

            processedCount = 0;
            allNutritionInfo.setLength(0);
            notFoundFoods.clear();
            final boolean[] foundAnyFood = {false};

            for (int i = 0; i < foodNameArray.length(); i++) {
                String foodName = foodNameArray.getString(i).trim();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("UserNutritionData").child("데이터");
                final String currentFoodName = foodName;

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        StringBuilder nutritionInfo = new StringBuilder();
                        boolean currentFoodFound = false;
                        String normalizedCurrentFoodName = currentFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " ");

                        for (DataSnapshot numberSnapshot : snapshot.getChildren()) {
                            if (numberSnapshot.hasChild("음식명")) {
                                String dbFoodName = numberSnapshot.child("음식명").getValue(String.class);
                                if (dbFoodName != null) {
                                    String normalizedDbFoodName = dbFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " ").replace("\"", "");
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
                    }
                });
            }
        } catch (JSONException e) {
            Log.e("JSONError", "JSON 파싱 오류: " + e.getMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    nutritionInfoTextView.setText("데이터를 불러오는 중 오류가 발생했습니다.");
                });
            }
        }
    }
}
