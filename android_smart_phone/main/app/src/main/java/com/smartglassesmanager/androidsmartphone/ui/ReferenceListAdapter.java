package com.smartglassesmanager.androidsmartphone.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.smartglassesmanager.androidsmartphone.R;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileEntity;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileViewModel;
import com.smartglassesmanager.androidsmartphone.utils.BitmapJavaUtils;

import java.text.SimpleDateFormat;
import java.util.List;

public class ReferenceListAdapter extends RecyclerView.Adapter<ReferenceListAdapter.ReferenceViewHolder> {
    private ItemClickListenerReference clickListener;
    private final LayoutInflater mInflater;
    private List<Reference> mReferences; // Cached copy of references

    private MediaFileViewModel mMediaFileViewModel;

    private Context context;

    class ReferenceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView referenceTitle;
        private final TextView referenceStartDate;
        private final TextView referenceStopDate;
        private final TextView referenceSummary;
        private final ImageView referenceImage;

        private ReferenceViewHolder(View itemView) {
            super(itemView);
            referenceTitle = itemView.findViewById(R.id.title_text_view);
            referenceStartDate = itemView.findViewById(R.id.start_date_text_view);
            referenceStopDate = itemView.findViewById(R.id.stop_date_text_view);
            referenceSummary = itemView.findViewById(R.id.summary_text_view);
            referenceImage = itemView.findViewById(R.id.image_image_view);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view){
            if(clickListener != null){
                clickListener.onClick(view, mReferences.get(getAdapterPosition()));
            }
        }
    }

    public void setClickListener(ItemClickListenerReference itemClickListener) {
        this.clickListener = itemClickListener;
    }

    ReferenceListAdapter(Context context, MediaFileViewModel mMediaFileViewModel) {
        this.context = context;
        this.mMediaFileViewModel = mMediaFileViewModel;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public ReferenceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.reference_card_recyclerview_item, parent, false);
        ReferenceViewHolder holder = new ReferenceViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(ReferenceViewHolder holder, int position) {
        if (mReferences != null) {
            Reference current = mReferences.get(position);
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("L-d hh:mma").withZone(ZoneId.systemDefault());
            SimpleDateFormat formatski = new SimpleDateFormat("EEE LLL d, yy, H:mm");
            if (current.getStopTimestamp() != null) {
                holder.referenceStartDate.setText("Start: " + formatski.format(current.getStartTimestamp()));
                holder.referenceStopDate.setText("Stop: " + formatski.format(current.getStopTimestamp()));
            } else {
                holder.referenceStartDate.setText(formatski.format(current.getStartTimestamp()));
                holder.referenceStopDate.setVisibility(View.GONE);
            }
            holder.referenceTitle.setText(current.getTitle());
            if (current.getSummary() != "" && current.getSummary() != null) {
                holder.referenceSummary.setText(current.getSummary());
            } else {
                holder.referenceSummary.setVisibility(View.GONE); //make the summary disapear if there isn't one
            }

            //set the image
            MediaFileEntity currentImage;
            if (current.getStopTimestamp() != null){
                long imageTime = (current.getStartTimestamp() + current.getStopTimestamp()) / 2;
                currentImage = mMediaFileViewModel.getClosestMediaFileSnapshot("image", imageTime);
            } else{
                currentImage = mMediaFileViewModel.getClosestMediaFileSnapshot("image", current.getStartTimestamp());
            }
            //set the image of the image view
            String imagePath = currentImage.getLocalPath();
            Bitmap imageBitmap = BitmapJavaUtils.loadImageFromStorage(imagePath);
            holder.referenceImage.setImageBitmap(imageBitmap);
        } else {
            // Covers the case of data not being ready yet.
            holder.referenceTitle.setText("No Data");
        }
    }

    void setReferences(List<Reference> references){
        mReferences = references;
        notifyDataSetChanged();
    }

    // getItemCount() is called many times, and when it is first called,
    // mReferences has not been updated (means initially, it's null, and we can't return null).
    @Override
    public int getItemCount() {
        if (mReferences != null)
            return mReferences.size();
        else return 0;
    }
}

