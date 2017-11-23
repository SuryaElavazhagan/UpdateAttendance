package com.example.kingslayer.updateattendance;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by kingslayer on 19/11/17.
 */

public class Students extends RealmObject {

    @PrimaryKey
    private int rollNo;
    private String studentName;
    private double periods;

    public int getRollNo() {
        return rollNo;
    }

    public void setRollNo(int rollNo) {
        this.rollNo = rollNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public double getPeriods() {
        return periods;
    }

    public void setPeriods(double periods) {
        this.periods = periods;
    }
}
