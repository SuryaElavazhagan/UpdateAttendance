package com.example.kingslayer.updateattendance;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by kingslayer on 20/11/17.
 */

public class RecyclerViewFetchAdapter extends RecyclerView.Adapter<RecyclerViewFetchAdapter.ViewHolder>{

    private Context context;
    private List<Students> mStudentsList;

    RecyclerViewFetchAdapter(Context context, List<Students> mStudentsList)
    {
        this.context = context;
        this.mStudentsList = mStudentsList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fetch_cards_list,parent,false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Students students = mStudentsList.get(position);

        holder.mName.setText(students.getRollNo() + " : " + students.getStudentName());
        holder.mPeriods.setText(": " + students.getPeriods());
    }

    @Override
    public int getItemCount() {
        return mStudentsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mName;
        TextView mPeriods;
        public ViewHolder(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name_of_student_2);
            mPeriods = itemView.findViewById(R.id.periods_attended);
        }
    }
}
