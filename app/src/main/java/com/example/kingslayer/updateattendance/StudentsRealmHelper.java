package com.example.kingslayer.updateattendance;

import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.RowData;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by kingslayer on 19/11/17.
 */

public class StudentsRealmHelper {
    private Realm realm;
    StudentsRealmHelper(Realm realm)
    {
        this.realm = realm;
    }

    public void loadIntoDB(final Realm realm) throws Exception
    {
        final List<Students> mStudentList = new ArrayList<Students>();

        int rolls[] = {58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
                71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
                81, 82, 83,84,85,86,87,89,90,
                91,92,94,95,96,97,98,99,100,
                101,102,103,104,105,106,107,108,109,110,
                112,113,114,115,701,305};
        String studs[] = {"NAVEEN S","NAVEEN NANDA M","NITHYA R","NITIN R","NIVEDHA SATHYANARAYANAN",
                "NIVEDITA SURESH KUMAR","ODURI BINDU MADHAV","OVIYAA B","PADMANAYANA N V","PETCHI PRAKASH S",
                "POOJA S","POORNIMA K","POTTI ADITYA SWAROOP","PRAVEEN KUMAR M","PRIYA S","PRIYADHARSHINI U",
                "PRIYADHARSHNI R","CHIRUMAMILLA PRUDHVI","RAGHURAAMON RAA",
                "RAHUL KIRON C","RAKSHANDA RAGHUPATHI","RAMASUBRAMANIAN R","RISHI KUMAR S",
                "ROOPIKA G","RUPA RAMACHANDRAN","RUPIKA R G","SAAGAR S","SAGARIGA SUNDAR",
                "SANDYA G","SANJAY S","SATHYA NARAYANAN G","SEETHAI SELVI M",
                "SHARADH R","SHARMILA S","SHRIRAAM P","SHRIYA SURESH",
                "SIDDHARTH M","SOWMIYA P","SOWMYA K","SRIRAM G R",
                "SRUTHI KANNAN","SUMALATHA P","SUNDARI SWATHY MEENA H","SUNIL RAJAN S",
                "SURYA E","TAMIL SELVI T","THILAKH R","VAIBHAV M",
                "VAISSHNAVI V G","VANITHA G","VIDYA S","VINITHA R",
                "VISHNUKUMAR M","VISHNU PRIYA R","VISHNUVARTHAN M","JAYASHREE H","SURYA PRAKASH T" };


        for(int i=0;i<studs.length;i++)
        {
            Students students = new Students();
            students.setStudentName(studs[i]);
            students.setRollNo(rolls[i]);
            students.setPeriods(0);
            mStudentList.add(students);
        }

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.insertOrUpdate(mStudentList);
            }
        });
    }

    public List<Students> getAllStudents()
    {
        RealmResults<Students> mStudentList = realm.where(Students.class).between("rollNo",58,701).findAllAsync();
        List<Students> mStudentsList = new ArrayList<Students>();
        for(int i = 0; i < 57 ; i++)
        {
            Students stud = mStudentList.get(i);
            mStudentsList.add(stud);
        }
        return mStudentsList;
    }

    public void updatePeriods(List<Double> mPeriodsList)
    {
        RealmResults<Students> mStudentList = realm.where(Students.class).between("rollNo",58,701).findAllAsync();
        for(int i = 0; i < 57 ; i++)
        {
            Students stud = mStudentList.get(i);
            if (stud != null) {
                realm.beginTransaction();
                stud.setPeriods(mPeriodsList.get(i));
                realm.commitTransaction();
            }
        }
    }

    /*public List<Integer> getPeriods()
    {
        List<Students> studentsList = getAllStudents();
        List<Integer> mPeriodsList = new ArrayList<Integer>();

        for(int i = 0; i < 57 ; i++)
        {
            mPeriodsList.add(studentsList.get(i).getPeriods());
        }
        return mPeriodsList;
    }*/

    public List<RowData> getData(String mDate)
    {
        List<RowData> rowDataList = new ArrayList<>();
        List<Students> studentsList = getAllStudents();
        List<CellData> cellData = new ArrayList<>();
        cellData.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue(mDate)));
        rowDataList.add(new RowData().setValues(cellData));

        for(int i = 0; i < 57; i++)
        {
                List<CellData> mCellData = new ArrayList<>();
            mCellData.add(new CellData().setUserEnteredValue(new ExtendedValue().
                    setNumberValue(studentsList.get(i).getPeriods())));
            rowDataList.add(new RowData().setValues(mCellData));
        }
        return rowDataList;
    }

    public void addPeriods(int roll) throws Exception
    {
        RealmResults<Students> students = realm.where(Students.class).equalTo("rollNo",roll).findAllAsync();
        Students stud = students.first();
        if (stud != null && !(stud.getPeriods() >= 8)) {
            realm.beginTransaction();
            stud.setPeriods(stud.getPeriods() + 1);
            realm.commitTransaction();
        }
    }

    public void subPeriods(int roll) throws Exception
    {
        RealmResults<Students> students = realm.where(Students.class).equalTo("rollNo",roll).findAllAsync();
        Students stud = students.first();
        if (stud != null && !(stud.getPeriods() <= 0)) {
            realm.beginTransaction();
            stud.setPeriods(stud.getPeriods() - 1);
            realm.commitTransaction();
        }
    }

}
