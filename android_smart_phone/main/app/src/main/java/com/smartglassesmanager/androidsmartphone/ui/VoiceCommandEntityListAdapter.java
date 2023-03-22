package com.smartglassesmanager.androidsmartphone.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.smartglassesmanager.androidsmartphone.database.voicecommand.VoiceCommandEntity;

import java.text.SimpleDateFormat;
import java.util.List;

import com.smartglassesmanager.androidsmartphone.R;

public class VoiceCommandEntityListAdapter extends RecyclerView.Adapter<VoiceCommandEntityListAdapter.VoiceCommandEntityViewHolder> {
    private ItemClickListenerVoiceCommandEntity clickListener;
    private final LayoutInflater mInflater;
    private List<VoiceCommandEntity> mVoiceCommandEntitys; // Cached copy of commands

    class VoiceCommandEntityViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView phraseItemView;

        private VoiceCommandEntityViewHolder(View itemView) {
            super(itemView);
            phraseItemView = itemView.findViewById(R.id.textView);
//            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            if(clickListener != null){
                clickListener.onClick(view, mVoiceCommandEntitys.get(getAdapterPosition()));
            }
        }
    }

    public void setClickListener(ItemClickListenerVoiceCommandEntity itemClickListener) {
        this.clickListener = itemClickListener;
    }

    VoiceCommandEntityListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public VoiceCommandEntityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.phrase_recyclerview_item, parent, false);
        VoiceCommandEntityViewHolder holder = new VoiceCommandEntityViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(VoiceCommandEntityViewHolder holder, int position) {
        if (mVoiceCommandEntitys != null) {
            VoiceCommandEntity current = mVoiceCommandEntitys.get(position);
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("L-d hh:mma").withZone(ZoneId.systemDefault());
            //SimpleDateFormat formatski = new SimpleDateFormat("L-d hh:mma");
            SimpleDateFormat formatski = new SimpleDateFormat("EEE LLL d, yy, H:mm");
            holder.phraseItemView.setText(formatski.format(current.getTimestamp()) + " - " + current.getPostArgs());
        } else {
            // Covers the case of data not being ready yet.
            holder.phraseItemView.setText("No VoiceCommandEntity");
        }
    }

    void setVoiceCommandEntitys(List<VoiceCommandEntity> commands){
        mVoiceCommandEntitys = commands;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mVoiceCommandEntitys has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mVoiceCommandEntitys != null)
            return mVoiceCommandEntitys.size();
        else return 0;
    }
}

