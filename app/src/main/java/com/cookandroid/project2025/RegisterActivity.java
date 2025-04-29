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
    private EditText mEtEmail, mEtPassword, mEtNickname, mEtAge;
    private RadioGroup mRgGender;
    private Button mBtnRegister;
    // 새 XML에서는 "남자"가 기본 선택값이므로
    private String strGender = "남자";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 새 XML 레이아웃 적용 (파일명이 activity_register.xml 인지 확인)
        setContentView(R.layout.activity_register);

        // Firebase 초기화
        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("UserAccount");

        // UI 요소 연결 (새 XML의 id 사용)
        mEtEmail = findViewById(R.id.editTextEmail);
        mEtPassword = findViewById(R.id.editText1);
        mEtNickname = findViewById(R.id.editTextNickname);
        mEtAge = findViewById(R.id.editTextBirthYear);
        mRgGender = findViewById(R.id.radioGroupGender);
        mBtnRegister = findViewById(R.id.buttonJoin);

        // 성별 선택 시 값 변경 (RadioButton의 텍스트를 사용)
        mRgGender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton selectedGender = findViewById(checkedId);
                if (selectedGender != null) {
                    strGender = selectedGender.getText().toString();
                }
            }
        });

        mBtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 입력값 가져오기
                String strEmail = mEtEmail.getText().toString().trim();
                String strPwd = mEtPassword.getText().toString().trim();
                String strNickname = mEtNickname.getText().toString().trim();
                String strAge = mEtAge.getText().toString().trim();

                // 이메일 유효성 검사
                if (!isValidEmail(strEmail)) {
                    Toast.makeText(RegisterActivity.this, "유효한 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 비밀번호 최소 길이 검사 (6자 이상)
                if (strPwd.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "비밀번호는 최소 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 나이 숫자 변환 (입력값이 없으면 0으로 처리)
                int age = strAge.isEmpty() ? 0 : Integer.parseInt(strAge);

                // Firebase 회원가입 처리
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

                                    // Firebase Realtime Database에 저장
                                    mDatabaseRef.child(firebaseUser.getUid()).setValue(account);

                                    Toast.makeText(RegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();

                                    // 회원가입 완료 후 LoginActivity로 이동
                                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "회원가입 실패: 이미 존재하는 이메일이거나 유효하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }

    // 이메일 형식 검사 메서드
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
