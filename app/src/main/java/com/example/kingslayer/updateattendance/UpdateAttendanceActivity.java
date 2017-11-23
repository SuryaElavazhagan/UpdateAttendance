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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.google.api.services.sheets.v4.model.RowData;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import pub.devrel.easypermissions.EasyPermissions;

import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_AUTHORIZATION;
import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class UpdateAttendanceActivity extends AppCompatActivity {

    private StudentsRealmHelper studentsRealmHelper;
    private RecyclerView recyclerView;
    private RecyclerViewUpdateAdapter recyclerViewUpdateAdapter;
    private Realm realm;
    private ProgressDialog progressDialog;
    private TextView mSelectDate;
    private Button mSubmitUpdate;
    private Calendar calendar;
    private DatePickerDialog.OnDateSetListener mDatePicker;
    private GoogleAccountCredential mCredential;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_attendance);

        init();

        loadStudents();

        setDatePicker();

        setupRecyclerView();

        validation();

        mSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(UpdateAttendanceActivity.this,mDatePicker,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        mSubmitUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(DateValidation()){
                    Toast.makeText(UpdateAttendanceActivity.this, "Select a valid Date.", Toast.LENGTH_SHORT).show();
                } else
                {
                    new UpdateData(mCredential, UpdateAttendanceActivity.this.
                            studentsRealmHelper.getData(UpdateAttendanceActivity.this.mSelectDate.getText().toString()),
                            UpdateAttendanceActivity.this).execute();
                }
            }
        });
    }

    protected void init()
    {

        mSelectDate = findViewById(R.id.select_date);
        recyclerView = findViewById(R.id.recycler_view);
        mSubmitUpdate = findViewById(R.id.submit_update_btn);
        mSubmitUpdate.setEnabled(false);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait");
        calendar = Calendar.getInstance();

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(getSharedPreferences("myAccountName",Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null));

        Realm.init(this);
        realm = Realm.getDefaultInstance();
        studentsRealmHelper = new StudentsRealmHelper(realm);
    }


    private boolean DateValidation()
    {

        return false;
    }

    private void validation() {
        if(!isDeviceOnline())
        {
            Toast.makeText(this, "No network available.", Toast.LENGTH_SHORT).show();
        }
        else {
            mSubmitUpdate.setEnabled(true);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return (networkInfo != null && networkInfo.isConnected());
    }


    protected void loadStudents()
    {
        try {
            studentsRealmHelper.loadIntoDB(realm);
        } catch (Exception e) {
            Toast.makeText(this, "Something is wrong in database", Toast.LENGTH_SHORT).show();
        }
    }

    protected void setDatePicker()
    {
        mDatePicker = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                StringBuilder mDate = new StringBuilder().append(dayOfMonth)
                        .append("/")
                        .append(month + 1)
                        .append("/")
                        .append(year);
                UpdateAttendanceActivity.this.mSelectDate.setText(mDate);
                calendar.set(year,month,dayOfMonth);
            }

        };
    }

    protected void setupRecyclerView()
    {
        recyclerViewUpdateAdapter = new RecyclerViewUpdateAdapter(this, studentsRealmHelper.getAllStudents(),realm);
        recyclerView.setAdapter(recyclerViewUpdateAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
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


    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                UpdateAttendanceActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    public class UpdateData extends AsyncTask<Void, Void, String>{

        private Sheets sheets = null;
        private List<RowData> mRowDataList;
        private AttendanceSheetsHelper sheetsHelper;
        private Context context;
        Exception mError;

        UpdateData(GoogleAccountCredential credential, List<RowData> mRowDataList,Context context){

            Log.d("UD:","Async");
            this.mRowDataList = mRowDataList;
            this.context = context;


            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            sheets = new com.google.api.services.sheets.v4.Sheets
                    .Builder(httpTransport,jsonFactory,credential)
                    .setApplicationName("Update Attendance").build();

            sheetsHelper = new AttendanceSheetsHelper(sheets);

        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {

            try
            {
                return sheetsHelper.updateAttendance(mRowDataList);
            } catch(Exception e) {
                Log.e("Background","Failed",e);
                mError = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.hide();
            Log.d("HEHE",result);
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
