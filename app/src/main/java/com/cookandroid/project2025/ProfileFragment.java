package com.cookandroid.project2025;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {
    private TextView tvNickname, tvGender, tvAge, tvHeight, tvWeight;
    private TextView tvBMIResult;
    private ImageView ivProfileImage;
    private Button btnChangeProfile;

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageRef;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("UserAccount");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageRef = mFirebaseStorage.getReference("profile_images");

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (ivProfileImage != null) {
                            ivProfileImage.setImageURI(selectedImageUri);
                        }
                        uploadImageToFirebase(selectedImageUri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNickname = view.findViewById(R.id.nicknameTextView);
        tvGender = view.findViewById(R.id.sexValue);
        tvAge = view.findViewById(R.id.yearValue);
        tvHeight = view.findViewById(R.id.heightValue);
        tvWeight = view.findViewById(R.id.weightValue);
        tvBMIResult = view.findViewById(R.id.bmiResultTextView); // 새로 추가
        ivProfileImage = view.findViewById(R.id.imageView);
        btnChangeProfile = view.findViewById(R.id.btn_change_profile);

        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            mDatabaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        UserAccount user = snapshot.getValue(UserAccount.class);
                        if (user != null) {
                            tvNickname.setText(user.getNickname());
                            tvGender.setText(user.getGender());
                            tvAge.setText(String.valueOf(user.getAge()));
                            tvHeight.setText(user.getHeight() + " cm");
                            tvWeight.setText(user.getWeight() + " kg");

                            calculateAndDisplayBMI(user.getHeight(), user.getWeight(), user.getGender());

                            StorageReference profileImageRef = mStorageRef.child(userId + ".jpg");
                            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                if (ivProfileImage != null) {
                                    ivProfileImage.setImageURI(uri);
                                }
                            }).addOnFailureListener(e -> {
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        btnChangeProfile.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null) return;
        String userId = firebaseUser.getUid();
        StorageReference fileRef = mStorageRef.child(userId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        Toast.makeText(getContext(), "프로필 이미지 변경 완료!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "이미지 업로드 실패!", Toast.LENGTH_SHORT).show());
    }

    private void calculateAndDisplayBMI(double heightCm, double weightKg, String gender) {
        if (heightCm <= 0 || weightKg <= 0) {
            tvBMIResult.setText("BMI 정보 없음");
            return;
        }

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        String category;

        if (bmi < 18.5) {
            category = "저체중";
        } else if (bmi < 23) {
            category = "정상";
        } else if (bmi < 25) {
            category = "과체중";
        } else {
            category = "비만";
        }

        tvBMIResult.setText(String.format("BMI: %.1f (%s)", bmi, category));
    }
}