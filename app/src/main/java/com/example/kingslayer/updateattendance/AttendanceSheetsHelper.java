package com.example.kingslayer.updateattendance;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendDimensionRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.DataFilter;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GetSpreadsheetByDataFilterRequest;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * MX1RcsAOx9rWi6oW2QiZHWVFO-XPPkKJD
 * https://script.google.com/macros/s/AKfycbwYb2ea4BjvN_XfnGUJyU7CvnXc_T2pYJdN7KmNvN5MVNBGeOc4/exec
 * Created by kingslayer on 21/11/17.
 */

public class AttendanceSheetsHelper {
    private String SPREADSHEET_ID = "1BjgpYrXGM28vy_L2Et2dhQvCpq98ezzGXXRoXBuBc0c";
    private Sheets sheets = null;
    private int mColumnCount;
    AttendanceSheetsHelper(Sheets sheets)
    {
        this.sheets = sheets;
    }

    public String updateAttendance(List<RowData> mRowDataList) throws Exception
    {

        int mLastColumn = getLastColumn();

        if(mLastColumn <= mColumnCount)
        {
            addColumn();
        }

        UpdateCellsRequest updateCellsRequest = new UpdateCellsRequest();
        GridRange gridRange = new GridRange().setSheetId(0).setStartColumnIndex(mLastColumn)
                .setEndColumnIndex(mLastColumn + 1)
                .setStartRowIndex(0).setEndRowIndex(58);

        updateCellsRequest.setFields("userEnteredValue").setRange(gridRange)
                .setRows(mRowDataList);
        List<Request> requests = new ArrayList<>();
        requests.add(new Request().setUpdateCells(updateCellsRequest));

        executeRequest(requests);
        return "Success";
    }


    public void addColumn() throws IOException {
        AppendDimensionRequest appendDimensionRequest = new AppendDimensionRequest();
        appendDimensionRequest.setDimension("COLUMNS")
                                .setLength(1)
                                .setSheetId(0);
        List<Request> requestList = new ArrayList<>();
        requestList.add(new Request().setAppendDimension(appendDimensionRequest));
        executeRequest(requestList);
    }

    private void executeRequest(List<Request> requests) throws IOException {
        BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);

        BatchUpdateSpreadsheetResponse response = sheets.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateSpreadsheetRequest).execute();
        mColumnCount = response.getUpdatedSpreadsheet().getSheets().get(0).getProperties().getGridProperties().getColumnCount();
    }

    private JSONObject getData() throws Exception
    {
        try {
            OkHttpClient client = new OkHttpClient();
            com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                    .url("https://script.google.com/macros/s/AKfycbwYb2ea4BjvN_XfnGUJyU7CvnXc_T2pYJdN7KmNvN5MVNBGeOc4/exec")
                    .build();
            Response response = client.newCall(request).execute();
            return new JSONObject(response.body().string());


        } catch (@NonNull IOException e) {
            Log.e("GetData:", "recieving null " + e.getLocalizedMessage());
        }
        return null;
    }

    private int getLastColumn() throws Exception {
       JSONObject jsonObject = getData();
       return jsonObject.getInt("column");
    }

    public List<Double> fetchAttendanceOnDate(String mDate) throws Exception
    {
        int mColumn = fetchColumnOnDate(mDate);
        GetSpreadsheetByDataFilterRequest getSpreadsheetByDataFilterRequest = new GetSpreadsheetByDataFilterRequest();

        DataFilter dataFilter = new DataFilter().setGridRange(new GridRange()
                .setStartRowIndex(1)
                .setEndRowIndex(58)
                .setStartColumnIndex(mColumn)
                .setEndColumnIndex(mColumn + 1)
                .setSheetId(0));
        List<DataFilter> dataFilterList = new ArrayList<>();
        dataFilterList.add(dataFilter);
        getSpreadsheetByDataFilterRequest.setDataFilters(dataFilterList)
                .setIncludeGridData(true);
        Sheets.Spreadsheets.GetByDataFilter getByDataFilter = sheets.spreadsheets().getByDataFilter(SPREADSHEET_ID, getSpreadsheetByDataFilterRequest);

        Spreadsheet spreadsheet = getByDataFilter.execute();
        GridData mData = spreadsheet.getSheets().get(0).getData().get(0);
        List<RowData> mRowDataList = mData.getRowData();
        List<Double> mPeriodsList = new ArrayList<>();

        for (RowData rowData : mRowDataList) {
            mPeriodsList.add(rowData.getValues().get(0).getEffectiveValue().getNumberValue());
        }
        return mPeriodsList;
    }

    private int fetchColumnOnDate(String mDate) throws Exception
    {
        int mColumn = 0;
        GetSpreadsheetByDataFilterRequest getSpreadsheetByDataFilterRequest = new GetSpreadsheetByDataFilterRequest();

        DataFilter dataFilter = new DataFilter().setGridRange(new GridRange()
                                                    .setStartRowIndex(0)
                                                    .setEndRowIndex(1)
                                                    .setStartColumnIndex(0)
                                                    .setEndColumnIndex(getLastColumn())
                                                    .setSheetId(0));
        List<DataFilter> dataFilterList = new ArrayList<>();
        dataFilterList.add(dataFilter);
        getSpreadsheetByDataFilterRequest.setDataFilters(dataFilterList)
                                        .setIncludeGridData(true);
        Sheets.Spreadsheets.GetByDataFilter getByDataFilter = sheets.spreadsheets().getByDataFilter(SPREADSHEET_ID, getSpreadsheetByDataFilterRequest);

        Spreadsheet spreadsheet = getByDataFilter.execute();
        GridData myData = spreadsheet.getSheets().get(0).getData().get(0);
        List<CellData> mCellDataList= myData.getRowData().get(0).getValues();
        for(int i = 0; i < mCellDataList.size() ; i++)
        {
            if(mDate.equals(mCellDataList.get(i).getEffectiveValue().getStringValue()))
            {
                mColumn = i;
                break;
            }
        }
        return mColumn;
    }

    public String updateFormula(String formula) throws Exception {
        int mLastColumn = getLastColumn();
        if(mLastColumn <= mColumnCount)
        {
            addColumn();
        }
        RepeatCellRequest repeatCellRequest = new RepeatCellRequest();
        repeatCellRequest.setCell(new CellData().setEffectiveValue(
                    new ExtendedValue().setFormulaValue(formula)))
                    .setFields("userEnteredValue")
                    .setRange(new GridRange().setSheetId(0).setStartColumnIndex(mLastColumn)
                            .setEndColumnIndex(mLastColumn + 1)
                            .setStartRowIndex(1).setEndRowIndex(58));
        List<Request> requestList = new ArrayList<>();
        requestList.add(new Request().setRepeatCell(repeatCellRequest));
        executeRequest(requestList);
        return "Success";
    }
}