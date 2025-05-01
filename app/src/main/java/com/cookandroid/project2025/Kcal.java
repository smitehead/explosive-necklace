package com.cookandroid.project2025;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.CalendarMonth;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder;
import com.kizitonwose.calendarview.ui.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Kcal extends AppCompatActivity {

    // Day ViewContainer
    public static class DayViewContainer extends ViewContainer {
        public final TextView textView;

        public DayViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.calendarDayText);
        }
    }

    // Month ViewContainer
    public static class MonthViewContainer extends ViewContainer {
        public final TextView textView;

        public MonthViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.monthText);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kcal);

        // PieChart 설정
        int carbsColor = ContextCompat.getColor(this, R.color.carbsColor);
        int proteinColor = ContextCompat.getColor(this, R.color.proteinColor);
        int fatColor = ContextCompat.getColor(this, R.color.fatColor);

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(512f, "탄수화물"));
        entries.add(new PieEntry(280f, "단백질"));
        entries.add(new PieEntry(288f, "지방"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        List<Integer> colors = new ArrayList<>();
        colors.add(carbsColor);
        colors.add(proteinColor);
        colors.add(fatColor);
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(0f); // 내부 텍스트 제거

        // 날짜 텍스트 설정 (예: nutrientTitle 사용 시 활성화)
        Date currentDate = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
        String formattedDate = dateFormat.format(currentDate);

        // LocalDate 계산
        java.time.LocalDate localDate = currentDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // CalendarView 설정
        CalendarView calendarView = findViewById(R.id.calendarView);

        // DayBinder 설정
        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                container.textView.setText(String.valueOf(day.getDate().getDayOfMonth()));

                if (day.getDate().equals(localDate)) {
                    container.textView.setTextColor(Color.RED);
                } else {
                    container.textView.setTextColor(Color.BLACK);
                }
            }
        });

        // MonthHeaderBinder 설정
        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<MonthViewContainer>() {
            @Override
            public MonthViewContainer create(View view) {
                return new MonthViewContainer(view);
            }

            @Override
            public void bind(MonthViewContainer container, CalendarMonth month) {
                container.textView.setText(month.getYearMonth().getYear() + "년 " + month.getYearMonth().getMonthValue() + "월");
            }
        });

        // 달력 범위 설정
        calendarView.setup(
                YearMonth.now().minusMonths(6),
                YearMonth.now().plusMonths(6),
                DayOfWeek.SUNDAY
        );
        calendarView.scrollToMonth(YearMonth.now());
    }
}
