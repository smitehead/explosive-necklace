package com.cookandroid.project2025;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class DrawerMenuHandler {

    public static void setupDrawer(View view, Fragment fragment) {
        // 홈 메뉴 및 서브메뉴
        TextView menuHome = view.findViewById(R.id.menuHome);
        LinearLayout subHome = view.findViewById(R.id.subMenuHome);
        menuHome.setOnClickListener(v -> toggleVisibility(subHome));

        view.findViewById(R.id.subToday).setOnClickListener(v -> navigateTo(fragment, new HomeFragment()));
        view.findViewById(R.id.subOver).setOnClickListener(v -> navigateTo(fragment, new HomeFragment()));
        view.findViewById(R.id.subBurn).setOnClickListener(v -> navigateTo(fragment, new HomeFragment()));

        // 단일 음식 메뉴 및 서브메뉴
        TextView menuSingle = view.findViewById(R.id.menuSingle);
        LinearLayout subSingle = view.findViewById(R.id.subMenuSingle);
        menuSingle.setOnClickListener(v -> toggleVisibility(subSingle));

        view.findViewById(R.id.subSingleCheck).setOnClickListener(v -> navigateTo(fragment, new CheckFragment()));
        view.findViewById(R.id.subSingleSearch).setOnClickListener(v -> {
            Intent intent = new Intent(fragment.getContext(), SearchActivity.class);
            fragment.startActivity(intent);
        });
        view.findViewById(R.id.subSingleLabel).setOnClickListener(v -> navigateTo(fragment, new LabelFragment()));

        // 다중 음식 메뉴 및 서브메뉴
        TextView menuMulti = view.findViewById(R.id.menuMulti);
        LinearLayout subMulti = view.findViewById(R.id.subMenuMulti);
        menuMulti.setOnClickListener(v -> toggleVisibility(subMulti));

        view.findViewById(R.id.subMultiCheck).setOnClickListener(v -> navigateTo(fragment, new MultiCheckFragment()));

        // 달력 메뉴 및 서브메뉴
        TextView menuCalendar = view.findViewById(R.id.menuCalendar);
        LinearLayout subCalendar = view.findViewById(R.id.subMenuCalendar);
        menuCalendar.setOnClickListener(v -> toggleVisibility(subCalendar));

        view.findViewById(R.id.subCalendarHistory).setOnClickListener(v -> navigateTo(fragment, new KcalFragment()));

        // 프로필 메뉴 및 서브메뉴
        TextView menuProfile = view.findViewById(R.id.menuProfile);
        LinearLayout subProfile = view.findViewById(R.id.subMenuProfile);
        menuProfile.setOnClickListener(v -> toggleVisibility(subProfile));

        view.findViewById(R.id.subProfileEdit).setOnClickListener(v -> navigateTo(fragment, new ProfileFragment()));
        view.findViewById(R.id.subProfileBml).setOnClickListener(v -> navigateTo(fragment, new ProfileFragment()));
        view.findViewById(R.id.subProfileLogout).setOnClickListener(v -> navigateTo(fragment, new ProfileFragment()));
    }

    private static void toggleVisibility(LinearLayout layout) {
        layout.setVisibility(layout.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private static void navigateTo(Fragment fromFragment, Fragment toFragment) {
        FragmentTransaction transaction = fromFragment.getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, toFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
