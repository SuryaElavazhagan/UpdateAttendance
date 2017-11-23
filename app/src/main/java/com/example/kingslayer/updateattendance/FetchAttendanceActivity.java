package com.example.kingslayer.updateattendance;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;

import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_AUTHORIZATION;
import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class FetchAttendanceActivity extends AppCompatActivity {

    private TextView mSelectDate;
    private RecyclerView recyclerView;
    private Button mSubmitButton;
    private StudentsRealmHelper studentsRealmHelper;
    private ProgressDialog progressDialog;
    private Realm realm;
    private GoogleAccountCredential mCredential;
    private RecyclerViewFetchAdapter recyclerViewFetchAdapter;
    private Calendar calendar;
    private DatePickerDialog.OnDateSetListener mDatePicker;
    List<Double> mPeriodsList;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch_attendance);

        init();

        setDatePicker();

        checkAndLoad();

        setupRecyclerView();

        mSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(FetchAttendanceActivity.this,mDatePicker,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FetchAttendance(mCredential,FetchAttendanceActivity.this,mSelectDate.getText().toString()).execute();
            }
        });
    }


    void init()
    {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        mSelectDate = findViewById(R.id.selecting_date);
        mSubmitButton = findViewById(R.id.submit_fetch_btn);

        Bundle bundle= getIntent().getExtras();
        String mDate = bundle != null ? bundle.getString("Date", "Select a Date") : "Select a Date";
        mSelectDate.setText(mDate);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(getSharedPreferences("myAccountName",Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null));

        Realm.init(this);
        realm = Realm.getDefaultInstance();
        studentsRealmHelper = new StudentsRealmHelper(realm);
    }

    void setDatePicker()
    {
        calendar = Calendar.getInstance();
        mDatePicker = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                StringBuilder mDate = new StringBuilder().append(dayOfMonth)
                        .append("/")
                        .append(month + 1)
                        .append("/")
                        .append(year);
                FetchAttendanceActivity.this.mSelectDate.setText(mDate);
            }

        };
    }

    protected void checkAndLoad()
    {
        try {
            studentsRealmHelper.loadIntoDB(realm);
        } catch (Exception e) {
            Toast.makeText(this, "Something is wrong in database", Toast.LENGTH_SHORT).show();
        }
    }

    void setupRecyclerView()
    {
        recyclerView = findViewById(R.id.recycler_view_2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFetchAdapter = new RecyclerViewFetchAdapter(this, studentsRealmHelper.getAllStudents());
        recyclerView.setAdapter(recyclerViewFetchAdapter);
    }

    private void validation() {
        if(!isDeviceOnline())
        {
            Toast.makeText(this, "No network available.", Toast.LENGTH_SHORT).show();
        }
        else {
            mSubmitButton.setEnabled(true);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    validation();
                }
                break;
        }
    }

    void updateUI()
    {
        studentsRealmHelper.updatePeriods(mPeriodsList);
        recyclerViewFetchAdapter.notifyDataSetChanged();
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                FetchAttendanceActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private class FetchAttendance extends AsyncTask<Void, Void, String>{

        private Sheets sheets = null;
        private Context context;
        private AttendanceSheetsHelper sheetsHelper;
        private String mDate;
        Exception mError;

        FetchAttendance(GoogleAccountCredential credential,Context context ,String mDate){
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            this.mDate = mDate;
            this.context = context;

            sheets = new com.google.api.services.sheets.v4.Sheets
                    .Builder(httpTransport,jsonFactory,credential)
                    .setApplicationName("Update Attendance").build();

            sheetsHelper =new AttendanceSheetsHelper(sheets);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                mPeriodsList = sheetsHelper.fetchAttendanceOnDate(mDate);
            } catch (Exception e) {
                mError = e;
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            try
            {
                progressDialog.hide();
                updateUI();
            }catch (Exception e)
            {
                mError = e;
                cancel(true);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            progressDialog.hide();
            if (mError != null) {
                if (mError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mError)
                                    .getConnectionStatusCode());
                } else if (mError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mError).getIntent(),
                            REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(context, mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "Request Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}