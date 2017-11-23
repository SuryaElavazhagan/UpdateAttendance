package com.example.kingslayer.updateattendance;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import io.realm.Realm;

/**
 * Created by kingslayer on 19/11/17.
 */

public class RecyclerViewUpdateAdapter extends RecyclerView.Adapter<RecyclerViewUpdateAdapter.ViewHolder> {

    private Context context;
    private List<Students> mStudentsList;
    private Realm realm;
    private StudentsRealmHelper studentsRealmHelper;

    RecyclerViewUpdateAdapter(Context context, List<Students> mStudentsList, Realm realm)
    {
        this.context = context;
        this.mStudentsList = mStudentsList;
        this.realm = realm;
        studentsRealmHelper = new StudentsRealmHelper(realm);
        //Toast.makeText(context, "Hello World", Toast.LENGTH_SHORT).show();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("Adapter:","Started");
        //Toast.makeText(context, "Hey", Toast.LENGTH_SHORT).show();
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.update_cards_list,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Students students = mStudentsList.get(position);

        try {
            holder.mName.setText(students.getRollNo() + " : " + students.getStudentName());
            holder.mResult.setText(" " + students.getPeriods() + " ");

        holder.mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d("mAdd.Click","Entered");
                try {
                    studentsRealmHelper.addPeriods(students.getRollNo());
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                RecyclerViewUpdateAdapter.this.notifyDataSetChanged();
            }
        });

        holder.mSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    studentsRealmHelper.subPeriods(students.getRollNo());
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                RecyclerViewUpdateAdapter.this.notifyDataSetChanged();
            }
        });
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public int getItemCount() {
        return mStudentsList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mAdd;
        TextView mResult;
        TextView mSub;
        public ViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name_of_student);
            mAdd = itemView.findViewById(R.id.add_tv);
            mResult = itemView.findViewById(R.id.result_tv);
            mSub = itemView.findViewById(R.id.sub_tv);
        }
    }
}
