package com.cookandroid.project2025;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class ResultFragment extends Fragment {

    private TextView receivedJsonTextView;
    private LinearLayout foodContainerLayout;
    private String foodListJson;
    private JSONArray foodNameArray;
    private int processedCount = 0;

    public static ResultFragment newInstance(String foodListJson) {
        ResultFragment fragment = new ResultFragment();
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
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        ImageButton buttonManualInput = view.findViewById(R.id.buttonManualInput);
        buttonManualInput.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        if (getArguments() != null && getArguments().containsKey("foodListJson")) {
            foodListJson = getArguments().getString("foodListJson");

            try {
                JSONArray foodArray = new JSONArray(foodListJson);
                StringBuilder resultText = new StringBuilder("다음 음식(들)이 보여요\n\n");

                for (int i = 0; i < foodArray.length(); i++) {
                    resultText.append((i + 1) + ". ").append(foodArray.getString(i)).append("\n");
                }

                receivedJsonTextView.setText(resultText.toString());

            } catch (JSONException e) {
                receivedJsonTextView.setText("음식 인식 결과를 처리할 수 없습니다");
                Log.e("JSONError", "파싱 오류: " + e.getMessage());
            }

            loadNutritionDataList(foodListJson);
        } else {
            receivedJsonTextView.setText("찾으시는 음식이 없습니다");
        }
    }


    private void loadNutritionDataList(String foodListJson) {
        try {
            foodNameArray = new JSONArray(foodListJson);
            processedCount = 0;

            for (int i = 0; i < foodNameArray.length(); i++) {
                String foodName = foodNameArray.getString(i).trim();
                DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                        .getReference("데이터");
                final String currentFoodName = foodName;

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        StringBuilder nutritionInfo = new StringBuilder();
                        String normalizedCurrentFoodName = currentFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " ");

                        for (DataSnapshot numberSnapshot : snapshot.getChildren()) {
                            if (numberSnapshot.hasChild("음식명")) {
                                String dbFoodName = numberSnapshot.child("음식명").getValue(String.class);
                                if (dbFoodName != null) {
                                    String normalizedDbFoodName = dbFoodName.trim().replaceAll("\\u00A0", "").replaceAll("\\s+", " ").replace("\"", "");
                                    if (normalizedDbFoodName.equals(normalizedCurrentFoodName)) {
                                        for (DataSnapshot nutrientSnapshot : numberSnapshot.getChildren()) {
                                            nutritionInfo.append(nutrientSnapshot.getKey()).append(": ").append(nutrientSnapshot.getValue()).append("\n");
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        getActivity().runOnUiThread(() -> {
                            if (!nutritionInfo.toString().isEmpty()) {
                                createFoodCard(currentFoodName, nutritionInfo.toString());
                            } else {
                                TextView errorView = new TextView(getContext());
                                errorView.setText(currentFoodName + ": 정보를 찾을 수 없습니다.");
                                foodContainerLayout.addView(errorView);
                            }
                        });

                        processedCount++;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "데이터 로드 실패: " + error.getMessage());
                        processedCount++;
                    }
                });
            }
        } catch (JSONException e) {
            Log.e("JSONError", "JSON 파싱 오류: " + e.getMessage());
        }
    }

    private void createFoodCard(String foodName, String nutritionInfo) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View cardView = inflater.inflate(R.layout.item_food_card, foodContainerLayout, false);

        TextView textNutrition = cardView.findViewById(R.id.textNutritionInfo);
        EditText editGram = cardView.findViewById(R.id.editGramInput);
        Button btnSave = cardView.findViewById(R.id.buttonSaveFood);

        textNutrition.setText("--- " + foodName + " ---\n" + nutritionInfo);

        btnSave.setOnClickListener(v -> {
            int inputGram;
            try {
                inputGram = Integer.parseInt(editGram.getText().toString().trim());
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "섭취량을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            double ratio = inputGram / 100.0;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("UserNutritionData").child(uid).child(today);

            StringBuilder adjusted = new StringBuilder("--- " + foodName + " ---\n");
            String[] lines = nutritionInfo.split("\n");
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String nutrient = parts[0].trim();
                    String valueStr = parts[1].trim().replaceAll("[^\\d.]", "");
                    try {
                        double value = Double.parseDouble(valueStr);
                        double result = Math.round(value * ratio * 10) / 10.0;
                        adjusted.append(nutrient).append(": ").append(result).append("\n");
                    } catch (NumberFormatException e) {
                        adjusted.append(line).append("\n");
                    }
                } else {
                    adjusted.append(line).append("\n");
                }
            }

            userRef.child(foodName).setValue(adjusted.toString());
            Toast.makeText(getContext(), foodName + " 저장 완료", Toast.LENGTH_SHORT).show();
        });

        foodContainerLayout.addView(cardView);
    }

}
