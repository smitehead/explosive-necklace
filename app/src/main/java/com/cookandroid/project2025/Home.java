package com.cookandroid.project2025;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // PieChart 설정
        PieChart pieChart = findViewById(R.id.pieChart);
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

        pieChart.setData(data);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("1,280 kcal");
        pieChart.setCenterTextSize(18f);
        pieChart.invalidate();

        // 날짜 텍스트 설정
        TextView textViewDate = findViewById(R.id.nutrientTitle);
        Date currentDate = Calendar.getInstance().getTime();
        String formattedDate = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(currentDate);
        textViewDate.setText(formattedDate);
    }
}