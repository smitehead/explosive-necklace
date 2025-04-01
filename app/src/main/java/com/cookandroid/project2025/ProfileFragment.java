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
    // UI 요소 (이메일은 삭제)
    private TextView tvNickname, tvGender, tvAge;
    private ImageView ivProfileImage;
    private Button btnChangeProfile;

    // Firebase 관련 변수
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseRef;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageRef;

    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Firebase 초기화
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("UserAccount");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mStorageRef = mFirebaseStorage.getReference("profile_images");

        // ActivityResultLauncher 등록 (Fragment에서도 registerForActivityResult 사용 가능)
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
        // fragment_profile.xml 레이아웃을 inflate 함
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // UI 요소 연결 (findViewById는 inflate된 view에서)
        tvNickname = view.findViewById(R.id.nicknameTextView);
        tvGender = view.findViewById(R.id.sexValue);
        tvAge = view.findViewById(R.id.yearValue);
        ivProfileImage = view.findViewById(R.id.imageView);
        btnChangeProfile = view.findViewById(R.id.btn_change_profile);

        // 현재 로그인한 유저 정보 가져오기
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

                            // Firebase Storage에서 프로필 이미지 가져오기
                            StorageReference profileImageRef = mStorageRef.child(userId + ".jpg");
                            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                if (ivProfileImage != null) {
                                    ivProfileImage.setImageURI(uri);
                                }
                            }).addOnFailureListener(e -> {
                                // 기본 프로필 이미지를 유지하거나 별도 처리를 할 수 있음
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // 에러 처리 (원한다면 Toast 메시지 등 추가)
                }
            });
        }

        // 프로필 변경 버튼 클릭 시 갤러리 열기
        btnChangeProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
    }

    // 갤러리 열기 메서드
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // Firebase Storage에 이미지 업로드
    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        if (firebaseUser == null) return;
        String userId = firebaseUser.getUid();
        StorageReference fileRef = mStorageRef.child(userId + ".jpg");

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(getContext(), "프로필 이미지 변경 완료!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "이미지 업로드 실패!", Toast.LENGTH_SHORT).show();
                });
    }
}
