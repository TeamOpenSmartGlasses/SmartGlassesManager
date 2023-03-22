package com.smartglassesmanager.androidsmartphone.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;

import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;


import com.smartglassesmanager.androidsmartphone.R;

public class PhraseListAdapter extends RecyclerView.Adapter<PhraseListAdapter.PhraseViewHolder> {
    private ItemClickListenerPhrase clickListener;
    private final LayoutInflater mInflater;
    private List<Phrase> mPhrases; // Cached copy of phrases

    class PhraseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView phraseItemView;

        private PhraseViewHolder(View itemView) {
            super(itemView);
            phraseItemView = itemView.findViewById(R.id.textView);
            //disable clicking transcripts
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            if(clickListener != null){
                clickListener.onClick(view, mPhrases.get(getAdapterPosition()));
            }
        }
    }

    public void setClickListener(ItemClickListenerPhrase itemClickListener) {
        this.clickListener = itemClickListener;
    }

    PhraseListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

    @Override
    public PhraseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.phrase_recyclerview_item, parent, false);
        PhraseViewHolder holder = new PhraseViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(PhraseViewHolder holder, int position) {
        if (mPhrases != null) {
            Phrase current = mPhrases.get(position);
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("L-d hh:mma").withZone(ZoneId.systemDefault());
            //SimpleDateFormat formatski = new SimpleDateFormat("L-d hh:mma");
            SimpleDateFormat formatski = new SimpleDateFormat("EEE LLL d, yy, H:mm");
            holder.phraseItemView.setText(formatski.format(current.getTimestamp()) + " - " + current.getPhrase());
        } else {
            // Covers the case of data not being ready yet.
            holder.phraseItemView.setText("No Phrase");
        }
    }

    void setPhrases(List<Phrase> phrases){
        mPhrases = phrases;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mPhrases has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mPhrases != null)
            return mPhrases.size();
        else return 0;
    }
}
