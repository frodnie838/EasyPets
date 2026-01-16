package com.easypets;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText edadEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        edadEditText = findViewById(R.id.edadEditText);

        edadEditText.setOnClickListener(v -> abrirDatePicker());


    }

    private void abrirDatePicker() {
        //Forzar idioma español
        Locale locale = new Locale("es", "ES");
        Locale.setDefault(locale);

        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                RegisterActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {

                    String fecha = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%d",
                            selectedDay,
                            selectedMonth + 1,
                            selectedYear
                    );

                    edadEditText.setText(fecha);
                },
                year, month, day
        );

        // Evitar fechas futuras (importante para fecha de nacimiento)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }


}