package com.smartglassesmanager.androidsmartphone.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.opencsv.CSVWriter;
import com.smartglassesmanager.androidsmartphone.R;
import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;
import com.smartglassesmanager.androidsmartphone.database.phrase.PhraseRepository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class ExportDataUi extends Fragment {
    private  final String TAG = "WearableAi_ExportDataUiFragment";

    private final String fragmentLabel = "Export Data";

    private NavController navController;
    private PhraseRepository mPhraseRepository;

    private MaterialDatePicker materialDatePicker;
    private Long startDate;
    private Long stopDate;
    private String dateRange;

    public ExportDataUi() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.export_data_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        mPhraseRepository = new PhraseRepository(getActivity().getApplication());

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        // now register the text view and the button with
        // their appropriate IDs
        Button mPickDateButton = view.findViewById(R.id.pick_date_button);
        Button mExportDataButton = view.findViewById(R.id.export_data_button);
        TextView mDateRangeDisplayTextView = view.findViewById(R.id.export_date_range_text_view);

        //setup date range picker
        // now create instance of the material date picker
        // builder make sure to add the "dateRangePicker"
        // which is material date range picker which is the
        // second type of the date picker in material design
        // date picker we need to pass the pair of Long
        // Long, because the start date and end date is
        // store as "Long" type value
        MaterialDatePicker.Builder<Pair<Long, Long>> materialDateBuilder = MaterialDatePicker.Builder.dateRangePicker();

        // now define the properties of the
        // materialDateBuilder
        materialDateBuilder.setTitleText("SELECT A DATE");

        // now create the instance of the material date
        // picker
        materialDatePicker = materialDateBuilder.build();

        // handle select date button which opens the
        // material design date picker
        mPickDateButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // getSupportFragmentManager() to
                        // interact with the fragments
                        // associated with the material design
                        // date picker tag is to get any error
                        // in logcat
                        materialDatePicker.show(getActivity().getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
                    }
                });

        // now handle the positive button click from the
        // material design date picker
        materialDatePicker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onPositiveButtonClick(Object selection) {

                        // if the user clicks on the positive
                        // button that is ok button update the
                        // selected date
//                        mShowSelectedDateText.setText("Selected Date is : " + materialDatePicker.getHeaderText());
                        Log.d(TAG, "Selected Date is : " + materialDatePicker.getHeaderText());
                        mDateRangeDisplayTextView.setText(materialDatePicker.getHeaderText());
                        mExportDataButton.setActivated(true);
                        mExportDataButton.setAlpha(1.0f);
                        mExportDataButton.setEnabled(true);
                        Pair<Long, Long> p_selection = (Pair<Long, Long>) selection;
                        startDate = p_selection.first;
                        stopDate = p_selection.second;
                        dateRange = materialDatePicker.getHeaderText();
                        // in the above statement, getHeaderText
                        // will return selected date preview from the
                        // dialog
                    }
                });

        mExportDataButton.setActivated(false);
        mExportDataButton.setAlpha(0.5f);
        mExportDataButton.setEnabled(false);

        mExportDataButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startDataExport();
                        //sendDataEmail();
                    }
                });

    }

//    private void createExportDialog(){
//        LayoutInflater inflater = getLayoutInflater();
//        View alertLayout = inflater.inflate(R.layout.face_rec_naming_modal, null);
//        final TextInputEditText etPersonName = alertLayout.findViewById(R.id.tiet_personName);
//
//        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
//        alert.setTitle("Who is this?");
//        // this is set the view from XML inside AlertDialog
//        alert.setView(alertLayout);
//        // disallow cancel of AlertDialog on click of back button and outside touch
//        alert.setCancelable(false);
//
//        alert.setNegativeButton("Cancel", (dialog, which) -> {
//        });
//
//        AlertDialog dialog = alert.create();
//        dialog.show();
//    }

    //this export is ugly and we should rewrite it to better tie into room, especially when we add more data
    //not exactly sure best way to do it yet
    private void startDataExport(){
        List<Phrase> phrases = mPhraseRepository.getPhraseRangeSnapshot(startDate, stopDate);


        try {
            String prettyDate = Calendar.getInstance().getTime().toString().replaceAll("\\s", "_").replaceAll(":", "-");
            String fileName = "WIS_data_export_" + prettyDate + ".csv";
            File csvfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName);
            String filePath = csvfile.toString();
            File f = new File(filePath);
            CSVWriter writer;

            // File exist
            if(f.exists()&&!f.isDirectory())
            {
                FileWriter mFileWriter = new FileWriter(filePath, true);
                writer = new CSVWriter(mFileWriter, ',', CSVWriter.NO_QUOTE_CHARACTER);
            }
            else
            {
                writer = new CSVWriter(new FileWriter(filePath), ',', CSVWriter.NO_QUOTE_CHARACTER);
            }

            //write the phrases to a csv
            String first_row = "id,timestamp,latitude,longitude,phrase";
            writer.writeNext(first_row.split(","));
            for (Phrase p : phrases) {
                String row = p.getId() + "," + p.getTimestamp() + "," + Double.toString(p.getLocation().getLatitude()) + "," + Double.toString(p.getLocation().getLongitude()) + "," + p.getPhrase();
                Log.d(TAG, "writing row: " + row);
                String [] row_s = row.split(",");
                writer.writeNext(row_s);
            }

            //close the writer
            writer.close();

            //send as email
            sendDataEmail(filePath, dateRange);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendDataEmail(String filePath, String dateRange){
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
//        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"email@example.com"});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "WIS Data, " + dateRange);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Attached is your WIS data for " + dateRange);
        File root = Environment.getExternalStorageDirectory();
        String pathToMyAttachedFile = filePath;
        File file = new File(pathToMyAttachedFile);
        if (!file.exists() || !file.canRead()) {
            Log.d(TAG, "FILE DOESN'T EXIST");
            return;
        }
        //Uri uri = Uri.fromFile(file);
        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", file);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(emailIntent, "Pick an email provider to send the data with."));
    }


}
