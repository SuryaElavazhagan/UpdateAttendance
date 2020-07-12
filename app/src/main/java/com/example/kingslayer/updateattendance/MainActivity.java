package com.example.kingslayer.updateattendance;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {

    private TextView mSelectDate;
    private Calendar calendar;
    private DatePickerDialog.OnDateSetListener mDatePicker;
    private GoogleAccountCredential mCredential;
    private Button updateAttendance;
    private Button fetchAttendance;
    private Button computeAttendance;
    private Intent mIntent;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        setDatePicker();


        mSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this,mDatePicker,
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        computeAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIntent = new Intent(MainActivity.this, ComputeAttendanceActivity.class);
                startActivity(mIntent);
            }
        });

        updateAttendance.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                mIntent = new Intent(MainActivity.this, UpdateAttendanceActivity.class);
                startActivity(mIntent);
            }
        });

        fetchAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIntent = new Intent(MainActivity.this, FetchAttendanceActivity.class);
                startActivity(mIntent);
            }
        });

        validation();
    }

    void init()
    {
        mSelectDate = findViewById(R.id.select_date_tv);
        updateAttendance = findViewById(R.id.update_attendance);
        fetchAttendance = findViewById(R.id.fetch_attendance);
        computeAttendance = findViewById(R.id.calculate_attendance);

        updateAttendance.setEnabled(false);
        fetchAttendance.setEnabled(false);

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }


    private void DateValidation()
    {
        Calendar mToday = Calendar.getInstance();
        String mDate = mSelectDate.getText().toString();
        if(mDate.equals("Select a Date") && (mToday.compareTo(calendar) > 1))
        {
            updateAttendance.setEnabled(false);
            fetchAttendance.setEnabled(false);
            Toast.makeText(this, "Select a valid date", Toast.LENGTH_SHORT).show();
        }
        else {
            updateAttendance.setEnabled(true);
            fetchAttendance.setEnabled(true);
        }
    }
    /*void signIn()
    {
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
*/
    private void validation() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (! isDeviceOnline()) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else if(mCredential.getSelectedAccountName() == null)
        {
           chooseAccount();
        }
        /*else if (GoogleSignIn.getLastSignedInAccount(this) == null) {
            signIn();
        }*/else {
            getSharedPreferences("myAccountName",Context.MODE_PRIVATE).edit()
                            .putString(PREF_ACCOUNT_NAME,mCredential.getSelectedAccountName()).apply();
            /*startActivity(mIntent);*/
            updateAttendance.setEnabled(true);
            fetchAttendance.setEnabled(true);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr != null ? connMgr.getActiveNetworkInfo() : null;
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getSharedPreferences("myAccountName",Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                validation();
            } else {
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();
                } else {
                    validation();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getSharedPreferences("myAccountName",Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        validation();
                    }
                }
                break;
            /*case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    validation();
                }
                break;*/
            /*case RC_SIGN_IN:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;*/
        }
    }

    /*private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            account = completedTask.getResult(ApiException.class);
        } catch (ApiException e) {
            Log.w("MainActivity", "signInResult:failed code=" + e.getStatusCode());
        }
    }*/


    void setDatePicker()
    {
        calendar = Calendar.getInstance();
        mDatePicker = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                StringBuilder mDate = new StringBuilder().append(dayOfMonth)
                        .append(":")
                        .append(month + 1)
                        .append(":")
                        .append(year);
                MainActivity.this.mSelectDate.setText(mDate);
            }

        };
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
