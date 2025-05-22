package com.cookandroid.project2025;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabaseRef;

    private EditText mEtEmail, mEtPassword, mEtNickname, mEtAge, mEtHeight, mEtWeight;
    private RadioGroup mRgGender;
    private ImageButton backButton;
    private Button mBtnRegister;
    private String strGender = "남자";  // 기본값

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase 초기화
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("UserAccount");

        // UI 요소 연결
        mEtEmail = findViewById(R.id.editTextEmail);
        mEtPassword = findViewById(R.id.editText1);
        mEtNickname = findViewById(R.id.editTextNickname);
        mEtAge = findViewById(R.id.editTextBirthYear);
        mEtHeight = findViewById(R.id.editTextHeight);
        mEtWeight = findViewById(R.id.editTextWeight);
        mRgGender = findViewById(R.id.radioGroupGender);
        mBtnRegister = findViewById(R.id.buttonJoin);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        // 성별 라디오 버튼 리스너
        mRgGender.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedGender = findViewById(checkedId);
            if (selectedGender != null) {
                strGender = selectedGender.getText().toString();
            }
        });

        // 가입 버튼 클릭 처리
        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strEmail = mEtEmail.getText().toString().trim();
                String strPwd = mEtPassword.getText().toString().trim();
                String strNickname = mEtNickname.getText().toString().trim();
                String strAge = mEtAge.getText().toString().trim();
                String strHeight = mEtHeight.getText().toString().trim();
                String strWeight = mEtWeight.getText().toString().trim();

                // 이메일 유효성 검사
                if (!isValidEmail(strEmail)) {
                    Toast.makeText(RegisterActivity.this, "유효한 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 비밀번호 최소 길이 검사
                if (strPwd.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "비밀번호는 최소 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 숫자 입력 변환
                int age = strAge.isEmpty() ? 0 : Integer.parseInt(strAge);
                int height = strHeight.isEmpty() ? 0 : Integer.parseInt(strHeight);
                int weight = strWeight.isEmpty() ? 0 : Integer.parseInt(strWeight);

                // Firebase 회원가입
                mFirebaseAuth.createUserWithEmailAndPassword(strEmail, strPwd)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                                    UserAccount account = new UserAccount();
                                    account.setIdToken(firebaseUser.getUid());
                                    account.setEmailId(firebaseUser.getEmail());
                                    account.setPassword(strPwd);
                                    account.setNickname(strNickname);
                                    account.setGender(strGender);
                                    account.setAge(age);
                                    account.setHeight(height);
                                    account.setWeight(weight);

                                    // DB 저장
                                    mDatabaseRef.child(firebaseUser.getUid()).setValue(account);

                                    Toast.makeText(RegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    // 이메일 형식 검사
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
