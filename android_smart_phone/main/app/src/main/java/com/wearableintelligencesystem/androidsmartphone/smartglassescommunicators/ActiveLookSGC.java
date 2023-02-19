package com.wearableintelligencesystem.androidsmartphone.smartglassescommunicators;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.util.Pair;
import android.widget.ArrayAdapter;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.Sdk;
import com.activelook.activelooksdk.types.GlassesUpdate;
import com.activelook.activelooksdk.types.Rotation;
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import java.util.ArrayList;
import java.util.Hashtable;


//communicate with ActiveLook smart glasses
public class ActiveLookSGC extends SmartGlassesCommunicator {
    private static final String TAG = "WearableAi_ActivelookSGC";

    private Glasses connectedGlasses;
    private int displayWidthPixels = 304;
    private int displayHeightPixels = 256;
    private Sdk alsdk;
    private ArrayAdapter<DiscoveredGlasses> scanResults;
    private ArrayList<DiscoveredGlasses> discoveredGlasses;

    //map font size to actual size in pixels - specific to Activelook because we have to handle text wrapping manually
    private Hashtable<Integer, Integer> fontToSize;
    private Hashtable<Integer, Float> fontToRatio;

    //scrolling text view state
    Point belowTitleLocScrollingTextView; //where is the y location where we can start populating data below the title?
    Point lastLocScrollingTextView; //where was the end of our last scrolling text y location?
    ArrayList<String> finalScrollingTextStrings;
    int scrollingTextTitleFontSize = LARGE_FONT;
    int scrollingTextTextFontSize = SMALL_FONT;
    double marginRatio = 0.3;

    public ActiveLookSGC(Context context) {
        super();
        //hold all glasses we find
        discoveredGlasses = new ArrayList<>();

        //state information
        mConnectState = 0;
        belowTitleLocScrollingTextView = new Point(0,0);
        lastLocScrollingTextView = new Point(0,0);
        finalScrollingTextStrings = new ArrayList<>();

        this.alsdk = Sdk.init(context,
                "",
                this::onUpdateStart,
                this::onUpdateAvailableCallback,
                this::onUpdateProgress,
                this::onUpdateSuccess,
                this::onUpdateError
        );
    }

    @Override
    protected void setFontSizes(){
        int EXTRA_LARGE_FONT = 3;
        LARGE_FONT = 2;
        MEDIUM_FONT = 1;
        SMALL_FONT = 0;

        fontToSize = new Hashtable<>();
        fontToSize.put(LARGE_FONT, 35);
        fontToSize.put(MEDIUM_FONT, 24);
        fontToSize.put(SMALL_FONT, 24);
        fontToSize.put(EXTRA_LARGE_FONT, 49);

        fontToRatio = new Hashtable<>();
        fontToRatio.put(LARGE_FONT, 2.0f);
        fontToRatio.put(MEDIUM_FONT, 2.1f);
        fontToRatio.put(SMALL_FONT, 2.1f);
        fontToRatio.put(EXTRA_LARGE_FONT, 3.0f);
    }

    private int computeMarginPercent(int fontSize){
        int marginPercent = pixelToPercent(displayHeightPixels, (int)Math.floor(fontToSize.get(fontSize) * marginRatio)); //space between previous line and next line
        return marginPercent;
    }

    @Override
    public void connectToSmartGlasses(){
        Log.d(TAG, "Activelook scanning, trying to connect...");
        this.alsdk.startScan(activeLookDiscoveredGlassesI -> {
            discoveredGlasses.add(activeLookDiscoveredGlassesI);
            tryConnectNewGlasses(activeLookDiscoveredGlassesI);
        });
    }

    private void tryConnectNewGlasses(DiscoveredGlasses device){
        connectionEvent(MessageTypes.TRYNA_CONNECT_GLASSES);
        device.connect(glasses -> {
                    Log.d(TAG, "Activelook connected.");
                    mConnectState = 2;
                    connectedGlasses = glasses;
                    connectionEvent(MessageTypes.CONNECTED_GLASSES);
                    // Power on the glasses
                    connectedGlasses.power(true);
                    // Clear the glasses display
                    connectedGlasses.clear();
                    //clear any fonts to use just default fonts
                    connectedGlasses.fontDeleteAll();
                    // Set the display luminance level
                    glasses.luma((byte) 7);
                },
                discoveredGlasses -> {
                    Log.d(TAG, "Activelook connection failed.");
                    connectionEvent(MessageTypes.TRYNA_CONNECT_GLASSES);
                },
                glasses -> {
                    Log.d(TAG, "Activelook disconnected.");
                    connectionEvent(MessageTypes.DISCONNECTED_GLASSES);
                });
    }

    @Override
    public void destroy(){
       if (connectedGlasses != null){
           connectedGlasses.disconnect();
       }
    }

    private void onUpdateStart(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateStart");
    }

    private void onUpdateAvailableCallback(final android.util.Pair<GlassesUpdate, Runnable> glassesUpdateAndRunnable) {
        Log.d(TAG, "onUpdateAvailableCallback");
        Log.d("GLASSES_UPDATE", String.format("onUpdateAvailableCallback   : %s", glassesUpdateAndRunnable.first));
        glassesUpdateAndRunnable.second.run();
    }
    private void onUpdateProgress(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateProgress");
    }
    private void onUpdateSuccess(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateSuccess");
    }
    private void onUpdateError(final GlassesUpdate glassesUpdate) {
        Log.d(TAG, "onUpdateError");
    }

    public void displayReferenceCardSimple(String title, String body){
        if (isConnected()) {
            ArrayList<Object> stuffToDisplay = new ArrayList<>();
            stuffToDisplay.add(new TextLineSG(title, LARGE_FONT));
            stuffToDisplay.add(new TextLineSG(body, SMALL_FONT));
            displayLinearStuff(stuffToDisplay);
        }
    }

    //takes a list of strings and images and displays them in a line from top of the screen to the bottom
    private void displayLinearStuff(ArrayList<Object> stuffToDisplay){
        if (!isConnected()){
            return;
        }

        //make sure our glasses are powered on and cleared
        connectedGlasses.power(true);
        connectedGlasses.clear();

        //loop through the stuff to display, and display it, moving the next item below the previous item in each loop
        Point currPercentPoint = new Point(0,0);
        int yMargin = 5;
        for (Object thing : stuffToDisplay){
            Point endPercentPoint;
            if (thing instanceof TextLineSG) { //could this be done cleaner with polymorphism? https://stackoverflow.com/questions/5579309/is-it-possible-to-use-the-instanceof-operator-in-a-switch-statement
                endPercentPoint = displayText((TextLineSG) thing, currPercentPoint);
//            } else if (thing instanceof ImageSG) {
                //endPoint = displaySomething((ImageSG) thing, currPoint);
            } else { //unsupported type
                continue;
            }
            currPercentPoint = new Point(0, endPercentPoint.y + yMargin); //move the next thing to display down below the previous thing
        }
    }

    //this inverts things because that's what activelook does
    private Point percentScreenToPixelsLocation(int xLocVw, int yLocVh){
        int xCoord = displayWidthPixels - percentToPixel(displayWidthPixels, xLocVw);
        int yCoord = displayHeightPixels - percentToPixel(displayHeightPixels, yLocVh);
        return new Point(xCoord, yCoord);
    }

    //does not invert
    private int percentToPixel(int pixelLen, int percentLoc){
        int coord = Math.round((((float) percentLoc) / 100) * pixelLen);
        return coord;
    }

    private Point pixelsToPercentScreen(Point point){
        int xPercent = (int)(((float)point.x / displayWidthPixels) * 100);
        int yPercent = (int)(((float)point.y / displayHeightPixels) * 100);
        Point percentPoint = new Point(xPercent, yPercent);
        return percentPoint;
    }

    private int pixelToPercent(int pixelLen, int pixelLoc){
        int percent = (int)(((float)pixelLoc / pixelLen) * 100);
        return percent;
    }

    //handles text wrapping, returns final position of last line printed
    private Point displayText(TextLineSG textLine, Point percentLoc){
        if (!isConnected()){
            return null;
        }

        //get info about the wrapping
        Pair wrapInfo = computeStringWrapInfo(textLine);
        int numWraps = (int)wrapInfo.first;
        int wrapLenNumChars = (int)wrapInfo.second;

        //loop through the text, writing out individual lines to the glasses
        ArrayList<String> chunkedText = new ArrayList<>();
        Point textPoint = percentLoc;
//        int textMarginY = pixelsToPercentScreen(new Point((int)(, 0)).x;
        int textMarginY = computeMarginPercent(textLine.getFontSizeCode()); //(fontToSize.get(textLine.getFontSize()) * 1.3)
        for (int i = 0; i <= numWraps; i++){
            int startIdx = wrapLenNumChars * i;
            int endIdx = Math.min(startIdx + wrapLenNumChars, textLine.getText().length());
            String subText = textLine.getText().substring(startIdx, endIdx).trim();
            chunkedText.add(subText);
            sendTextToGlasses(new TextLineSG(subText, textLine.getFontSizeCode()), textPoint);
            textPoint = new Point(textPoint.x, textPoint.y + pixelToPercent(displayHeightPixels, fontToSize.get(textLine.getFontSizeCode())) + textMarginY); //lower our text for the next loop
        }

        return textPoint;
    }

    private Pair<Integer, Integer> computeStringWrapInfo(TextLineSG textLine){
        //compute information about how to wrap this line
        int fontHeight = fontToSize.get(textLine.getFontSizeCode());
        float fontAspectRatio = fontToRatio.get(textLine.getFontSizeCode()); //this many times as taller than, found by trial and error
        int fontWidth = (int) (fontHeight / fontAspectRatio);
        int textWidth = (int) (textLine.getText().length() * fontWidth); //width in pixels
        int numWraps = ((int) Math.floor(((float) textWidth) / displayWidthPixels));
        int wrapLenNumChars = ((int) Math.floor((displayWidthPixels / (float) fontWidth)));
        return new Pair(numWraps, wrapLenNumChars);
    }

    private void sendTextToGlasses(TextLineSG textLine, Point percentLoc){
        Point pixelLoc = percentScreenToPixelsLocation(percentLoc.x, percentLoc.y);
        connectedGlasses.txt(pixelLoc, Rotation.TOP_LR, (byte) textLine.getFontSizeCode(), (byte) 0x0F, textLine.getText());
    }

    public void startScrollingTextViewMode(String title){
        if (connectedGlasses == null) {
            return;
        }

        //clear the screen
        connectedGlasses.clear();

        //display the title at the top of the screen
        TextLineSG tlTitle = new TextLineSG(title, scrollingTextTitleFontSize);
        belowTitleLocScrollingTextView = displayText(tlTitle, new Point(0,0));
        lastLocScrollingTextView = belowTitleLocScrollingTextView;
    }

    public void scrollingTextViewIntermediateText(String text){

    }

    public void scrollingTextViewFinalText(String text){
        if (!isConnected()){
            return;
        }

        //save to our saved list of final scrolling text strings
        finalScrollingTextStrings.add(text);

        //get the max number of wraps allows
        Log.d(TAG, "margin percent of scrolling text is: " + computeMarginPercent(scrollingTextTextFontSize));
        Log.d(TAG, "margin pixel of scrolling text is: " + percentToPixel(displayHeightPixels, computeMarginPercent(scrollingTextTextFontSize)));
        float allowedTextRows = computeAllowedTextRows(fontToSize.get(scrollingTextTitleFontSize), fontToSize.get(scrollingTextTextFontSize), percentToPixel(displayHeightPixels, computeMarginPercent(scrollingTextTextFontSize)));
        Log.d(TAG, "ALLOWED TEZT ROWS IS: " + allowedTextRows);

        //figure out the maximum we can display
        int totalRows = 0;
        ArrayList<String> finalTextToDisplay = new ArrayList<>();
        for (int i = finalScrollingTextStrings.toArray().length - 1; i >= 0; i--){
            String finalText = finalScrollingTextStrings.get(i);
            Log.d(TAG, finalText);
            //convert to a TextLine type with small font
            TextLineSG tlString = new TextLineSG(finalText, SMALL_FONT);
            //get info about the wrapping of this string
            Pair wrapInfo = computeStringWrapInfo(tlString);
            int numWraps = (int)wrapInfo.first;
            int wrapLenNumChars = (int)wrapInfo.second;
            totalRows += numWraps + 1;

            if (totalRows > allowedTextRows){
                Log.d(TAG, "HIT THE ALLOWED TEXT ROW LIMIT, taking partial string if possible and exiting loop");
                finalScrollingTextStrings = finalTextToDisplay;
                lastLocScrollingTextView = belowTitleLocScrollingTextView;
//                connectedGlasses.clear(); //clear the glasses as we hit our limit and need to redraw
                connectedGlasses.color((byte)0x00);
                connectedGlasses.rectf(percentScreenToPixelsLocation(belowTitleLocScrollingTextView.x, belowTitleLocScrollingTextView.y), percentScreenToPixelsLocation(100, 100));
                break;
            } else {
                finalTextToDisplay.add(0, finalText);
            }
        }

        //display all of the text that we can
        for (String finalString: finalTextToDisplay) {
            TextLineSG tlString = new TextLineSG(finalString, SMALL_FONT);
            //write this text at the last location + margin
            Log.d(TAG, "Writing string: " + tlString.getText());
            lastLocScrollingTextView = displayText(tlString, new Point(0, lastLocScrollingTextView.y));
        }
    }

    private void clearBlock(Point topLeft, Point bottomRight){

    }

    //this works using activelook pixels
    private float computeAllowedTextRows(int titleHeight, int fontHeight, int yMargin){
        Log.d(TAG, "computeAllowedTExtRowsssssssssssssssssssssss");
        Log.d(TAG, "th" + titleHeight);
        Log.d(TAG, "fh" + fontHeight);
        Log.d(TAG, "ymarg" + yMargin);
        int yBox = displayHeightPixels - titleHeight;
        int lineHeight = fontHeight + yMargin;

        float numRows = (float)yBox / lineHeight;

        return numRows;
    }
}
