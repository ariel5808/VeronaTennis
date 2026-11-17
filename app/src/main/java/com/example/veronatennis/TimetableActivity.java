package com.example.veronatennis;


import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableActivity extends Activity {

    private final String[] days = {
            "Sun", "Mon", "Tue", "Wed",
            "Thu", "Fri", "Sat"
    };

    private final String[] slots = {
            "07-10", "10-13", "13-17",
            "17-20", "20-23"
    };

    // 7 days, 5 time slots
    private final boolean[][] available =
            new boolean[7][5];

    private final TextView[][] cells =
            new TextView[7][5];

    private LinearLayout container;
    private Button btnContinue;

    private FirebaseFirestore db;
    private FirebaseUser user;

    private final int GREEN =
            Color.rgb(57, 139, 36);

    private final int WHITE =
            Color.WHITE;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_timetable);

        container = findViewById(
                R.id.timetableContainer);

        btnContinue = findViewById(
                R.id.btnContinue);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (user == null) {
            Toast.makeText(this,
                    "Please login first",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        createTimetable();
        loadTimetable();

        btnContinue.setOnClickListener(
                v -> saveTimetable());
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private TextView createLabel(String text) {
        TextView tv = new TextView(this);

        tv.setText(text);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.DKGRAY);
        tv.setTypeface(null, Typeface.BOLD);

        return tv;
    }

    private void createTimetable() {

        // Header row: days
        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL);

        TextView corner = createLabel("Time");
        header.addView(corner,
                new LinearLayout.LayoutParams(
                        dp(62), dp(42)));

        for (String day : days) {
            TextView label = createLabel(day);

            header.addView(label,
                    new LinearLayout.LayoutParams(
                            0, dp(42), 1));
        }

        container.addView(header);

        // Each time slot is one row
        for (int s = 0; s < slots.length; s++) {

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL);

            TextView time =
                    createLabel(slots[s]);

            row.addView(time,
                    new LinearLayout.LayoutParams(
                            dp(62), dp(52)));

            for (int d = 0; d < days.length; d++) {

                final int dayIndex = d;
                final int slotIndex = s;

                TextView cell =
                        new TextView(this);

                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setTextColor(WHITE);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                0, dp(44), 1);

                params.setMargins(
                        dp(2), dp(4),
                        dp(2), dp(4));

                row.addView(cell, params);

                cells[d][s] = cell;

                updateCell(d, s);

                cell.setOnClickListener(v -> {
                    available[dayIndex][slotIndex] =
                            !available[dayIndex][slotIndex];

                    updateCell(dayIndex, slotIndex);
                });
            }

            container.addView(row);
        }
    }

    private void updateCell(int day, int slot) {

        TextView cell = cells[day][slot];

        if (available[day][slot]) {
            cell.setBackgroundTintList(null);
            cell.setBackgroundColor(GREEN);
            cell.setText("✓");
        } else {
            cell.setBackgroundTintList(null);
            cell.setBackgroundColor(
                    Color.rgb(222, 230, 222));
            cell.setText("");
        }
    }

    private void saveTimetable() {

        btnContinue.setEnabled(false);

        Map<String, Object> timetable =
                new HashMap<>();

        for (int d = 0; d < days.length; d++) {

            List<Boolean> daySlots =
                    new ArrayList<>();

            for (int s = 0; s < slots.length; s++) {
                daySlots.add(available[d][s]);
            }

            timetable.put(days[d], daySlots);
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("timetable", timetable);

        db.collection("users")
                .document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    btnContinue.setEnabled(true);

                    Toast.makeText(this,
                            "Timetable saved!",
                            Toast.LENGTH_SHORT).show();

                    // Open MatchingActivity here
                    // once that Activity is implemented.
                    // Intent intent = new Intent(
                    //    this, MatchingActivity.class);
                    // startActivity(intent);

                })
                .addOnFailureListener(e -> {

                    btnContinue.setEnabled(true);

                    Toast.makeText(this,
                            "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadTimetable() {

        btnContinue.setEnabled(false);

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    btnContinue.setEnabled(true);

                    if (!doc.exists()) return;

                    Map<String, Object> timetable =
                            (Map<String, Object>)
                                    doc.get("timetable");

                    if (timetable == null) return;

                    for (int d = 0;
                         d < days.length; d++) {

                        Object value =
                                timetable.get(days[d]);

                        if (!(value instanceof List))
                            continue;

                        List<?> saved = (List<?>) value;

                        for (int s = 0;
                             s < slots.length
                                     && s < saved.size(); s++) {

                            available[d][s] =
                                    Boolean.TRUE.equals(
                                            saved.get(s));

                            updateCell(d, s);
                        }
                    }
                })
                .addOnFailureListener(e -> {

                    btnContinue.setEnabled(true);

                    Toast.makeText(this,
                            "Could not load timetable",
                            Toast.LENGTH_SHORT).show();
                });
    }
}
