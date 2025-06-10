package com.cookandroid.project2025;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View blueCircle = findViewById(R.id.blueCircle);

        ObjectAnimator shrinkX = ObjectAnimator.ofFloat(blueCircle, "scaleX", 1f, 0.6f);
        ObjectAnimator shrinkY = ObjectAnimator.ofFloat(blueCircle, "scaleY", 1f, 0.6f);
        shrinkX.setDuration(1000);
        shrinkY.setDuration(1000);

        ObjectAnimator expandX = ObjectAnimator.ofFloat(blueCircle, "scaleX", 0.6f, 25f);
        ObjectAnimator expandY = ObjectAnimator.ofFloat(blueCircle, "scaleY", 0.6f, 25f);
        expandX.setDuration(1000);
        expandY.setDuration(1000);
        expandX.setInterpolator(new AccelerateInterpolator());
        expandY.setInterpolator(new AccelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(shrinkX).with(shrinkY);
        animatorSet.play(expandX).with(expandY).after(shrinkX);
        animatorSet.start();

        // 애니메이션 끝난 후 실행
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Intent intent;
                if (user != null) {
                    // 로그인된 경우
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // 로그인 안 된 경우
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                startActivity(intent);
                finish(); // SplashActivity 종료
            }
        });
    }
}

