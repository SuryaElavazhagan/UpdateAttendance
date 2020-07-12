package com.example.kingslayer.updateattendance;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

import java.util.Arrays;

import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_AUTHORIZATION;
import static com.example.kingslayer.updateattendance.MainActivity.REQUEST_GOOGLE_PLAY_SERVICES;

public class  ComputeAttendanceActivity extends AppCompatActivity {

    ProgressDialog progressDialog;
    String mFormula = null;
    AlertDialog.Builder builder;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };
    private GoogleAccountCredential mCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compute_attendance);
        progressDialog = new ProgressDialog(this);

        progressDialog.setMessage("Please Wait....");

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        mCredential.setSelectedAccountName(getSharedPreferences("myAccountName",Context.MODE_PRIVATE)
                .getString(PREF_ACCOUNT_NAME, null));


        builder = new AlertDialog.Builder(ComputeAttendanceActivity.this);
        View mFormulaDialog = LayoutInflater.from(ComputeAttendanceActivity.this).inflate(R.layout.formula_dialog,null);
        builder.setTitle("Enter your formula");
        builder.setView(mFormulaDialog);
        final EditText mFormulaEditText = mFormulaDialog.findViewById(R.id.my_formula);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("Calculate", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mFormula = mFormulaEditText.getText().toString();
                new CalculateFormula(mCredential, ComputeAttendanceActivity.this).execute();
            }
        });

        validation();
    }

    private void validation() {
        if(!isDeviceOnline())
        {
            Toast.makeText(this, "No network available.", Toast.LENGTH_SHORT).show();
        }
        else {
            builder.show();
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                ComputeAttendanceActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
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

    private class CalculateFormula extends AsyncTask<Void, Void, String>{

        private Sheets sheets = null;
        private GoogleAccountCredential googleAccountCredential;
        private Context context;
        private Exception mError;
        private AttendanceSheetsHelper sheetsHelper;

        CalculateFormula(GoogleAccountCredential googleAccountCredential, Context context)
        {
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            sheets = new com.google.api.services.sheets.v4.Sheets
                    .Builder(httpTransport,jsonFactory,googleAccountCredential)
                    .setApplicationName("Update Attendance").build();

            sheetsHelper = new AttendanceSheetsHelper(sheets);
            this.context = context;
        }
        @Override
        protected void onPostExecute(String s) {
            progressDialog.dismiss();
            Toast.makeText(context, "Your formula is applied to a new column", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return sheetsHelper.updateFormula(mFormula);
            } catch (Exception e) {
                Log.e("Background","Failed",e);
                mError = e;
                cancel(true);
                return null;
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
                            REQUEST_AUTHORIZATION
                    );
                } else {
                    Toast.makeText(context, mError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "Request Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
