package com.cookandroid.project2025;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private ImageView imageView;
    private TextInputEditText editTextNickname, editTextBirthYear, editTall, editWeigh;
    private RadioGroup radioGroupGender;
    private Button buttonSave, buttonChangePic;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // 뷰 연결
        imageView = view.findViewById(R.id.imageView);
        editTextNickname = view.findViewById(R.id.editTextNickname);
        editTextBirthYear = view.findViewById(R.id.editTextBirthYear);
        editTall = view.findViewById(R.id.editTall);
        editWeigh = view.findViewById(R.id.editWeigh);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonChangePic = view.findViewById(R.id.button); // 사진변경 버튼

        if (currentUser != null) {
            String uid = currentUser.getUid();
            databaseRef = FirebaseDatabase.getInstance().getReference("UserAccount").child(uid);

            // 기존 사용자 정보 불러오기
            databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        editTextNickname.setText(snapshot.child("nickname").getValue(String.class));
                        editTextBirthYear.setText(String.valueOf(snapshot.child("age").getValue(Integer.class)));
                        editTall.setText(String.valueOf(snapshot.child("height").getValue(Integer.class)));
                        editWeigh.setText(String.valueOf(snapshot.child("weight").getValue(Integer.class)));
                        String gender = snapshot.child("gender").getValue(String.class);
                        if ("남자".equals(gender)) {
                            radioGroupGender.check(R.id.radioMale);
                        } else if ("여자".equals(gender)) {
                            radioGroupGender.check(R.id.radioFemale);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 변경하기 버튼 클릭 시
        buttonSave.setOnClickListener(v -> {
            if (currentUser == null) return;

            String uid = currentUser.getUid();
            String nickname = editTextNickname.getText().toString().trim();
            String ageStr = editTextBirthYear.getText().toString().trim();
            String heightStr = editTall.getText().toString().trim();
            String weightStr = editWeigh.getText().toString().trim();
            String gender = "";

            int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
            if (selectedGenderId == R.id.radioMale) {
                gender = "남자";
            } else if (selectedGenderId == R.id.radioFemale) {
                gender = "여자";
            }

            // 필수 항목 유효성 검사
            if (TextUtils.isEmpty(nickname)) {
                editTextNickname.setError("닉네임을 입력해주세요");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("nickname", nickname);
            updates.put("age", Integer.parseInt(ageStr));
            updates.put("height", Integer.parseInt(heightStr));
            updates.put("weight", Integer.parseInt(weightStr));
            updates.put("gender", gender);

            databaseRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(getContext(), "프로필이 수정되었습니다.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
