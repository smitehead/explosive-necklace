package com.cookandroid.project2025;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SettingsFragment extends Fragment {

    private ImageView imageView, backButton;
    private TextInputEditText editTextNickname, editTextBirthYear, editTall, editWeigh;
    private RadioGroup radioGroupGender;
    private Button buttonSave, buttonChangePic;

    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageRef;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        imageView = view.findViewById(R.id.imageView);
        editTextNickname = view.findViewById(R.id.editTextNickname);
        editTextBirthYear = view.findViewById(R.id.editTextBirthYear);
        editTall = view.findViewById(R.id.editTall);
        editWeigh = view.findViewById(R.id.editWeigh);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonChangePic = view.findViewById(R.id.button);
        backButton = view.findViewById(R.id.backButton);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return view;

        firebaseStorage = FirebaseStorage.getInstance();
        storageRef = firebaseStorage.getReference("profile_images");
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        imageView.setImageURI(selectedImageUri);
                        uploadImageToFirebase(selectedImageUri);
                    }
                });

        buttonChangePic.setOnClickListener(v -> openGallery());
        buttonSave.setOnClickListener(v -> saveProfile());
        backButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        loadProfile();
        return view;
    }

    private void loadProfile() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    editTextNickname.setText(snapshot.child("nickname").getValue(String.class));
                    editTextBirthYear.setText(snapshot.child("birthYear").getValue(String.class));
                    editTall.setText(snapshot.child("height").getValue(String.class));
                    editWeigh.setText(snapshot.child("weight").getValue(String.class));
                    String gender = snapshot.child("gender").getValue(String.class);

                    if ("남자".equals(gender)) {
                        radioGroupGender.check(R.id.radioMale);
                    } else if ("여자".equals(gender)) {
                        radioGroupGender.check(R.id.radioFemale);
                    }

                    // 프로필 이미지 로드
                    StorageReference profileImageRef = storageRef.child(currentUser.getUid() + ".jpg");
                    profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageView.setImageURI(uri);
                    }).addOnFailureListener(e -> {
                        imageView.setImageResource(R.drawable.profile_man); // fallback image
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "데이터 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String nickname = editTextNickname.getText().toString().trim();
        String birthYear = editTextBirthYear.getText().toString().trim();
        String height = editTall.getText().toString().trim();
        String weight = editWeigh.getText().toString().trim();

        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        String gender = null;
        if (selectedGenderId == R.id.radioMale) {
            gender = "남자";
        } else if (selectedGenderId == R.id.radioFemale) {
            gender = "여자";
        }

        if (TextUtils.isEmpty(nickname) || TextUtils.isEmpty(birthYear)
                || TextUtils.isEmpty(height) || TextUtils.isEmpty(weight) || gender == null) {
            Toast.makeText(getContext(), "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.child("nickname").setValue(nickname);
        userRef.child("birthYear").setValue(birthYear);
        userRef.child("height").setValue(height);
        userRef.child("weight").setValue(weight);
        userRef.child("gender").setValue(gender);

        Toast.makeText(getContext(), "프로필 저장 완료", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        String userId = currentUser.getUid();
        StorageReference fileRef = storageRef.child(userId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        Toast.makeText(getContext(), "프로필 이미지 변경 완료!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "이미지 업로드 실패!", Toast.LENGTH_SHORT).show());
    }
}
