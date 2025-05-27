package com.cookandroid.project2025;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ResultFragment extends Fragment {

    private TextView receivedJsonTextView;
    private LinearLayout foodContainerLayout;
    private Button buttonConfirmSave;
    private Button buttonManualInput;

    private String foodListJson;

    // ✅ [1] newInstance() 정의
    public static ResultFragment newInstance(String foodListJson) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString("foodListJson", foodListJson);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_result_food_fragment, container, false);

        receivedJsonTextView = view.findViewById(R.id.receivedJsonTextView);
        foodContainerLayout = view.findViewById(R.id.foodContainerLayout);
        buttonManualInput = view.findViewById(R.id.buttonManualInput);

        if (getArguments() != null) {
            foodListJson = getArguments().getString("foodListJson", "[]");
            receivedJsonTextView.setText("Received JSON: " + foodListJson);
            displayNutritionInfo(foodListJson);
        }

        buttonConfirmSave.setOnClickListener(v -> {
            try {
                saveNutritionInfoToFirebase(foodListJson);
            } catch (JSONException e) {
                Toast.makeText(getContext(), "저장 실패", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        // ✅ [2] 직접입력 버튼: SearchActivity 이동
        buttonManualInput.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void displayNutritionInfo(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            StringBuilder allNutritionInfo = new StringBuilder();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                String name = item.getString("name");
                JSONObject nutrients = item.getJSONObject("nutrients");

                allNutritionInfo.append("음식 이름: ").append(name).append("\n");
                for (int j = 0; j < nutrients.names().length(); j++) {
                    String key = nutrients.names().getString(j);
                    double value = nutrients.getDouble(key);
                    allNutritionInfo.append(key).append(": ").append(value).append("\n");
                }
                allNutritionInfo.append("\n");
            }

            TextView nutritionInfoTextView = new TextView(getContext());
            nutritionInfoTextView.setText(allNutritionInfo.toString());
            foodContainerLayout.addView(nutritionInfoTextView);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveNutritionInfoToFirebase(String jsonData) throws JSONException {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        JSONArray jsonArray = new JSONArray(jsonData);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String name = item.getString("name");
            JSONObject nutrients = item.getJSONObject("nutrients");

            DatabaseReference foodRef = userRef.child("food").child(today).push();
            foodRef.child("name").setValue(name);

            for (int j = 0; j < nutrients.names().length(); j++) {
                String key = nutrients.names().getString(j);
                double value = nutrients.getDouble(key);
                foodRef.child("nutrients").child(key).setValue(value);
            }
        }

        Toast.makeText(getContext(), "섭취 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
