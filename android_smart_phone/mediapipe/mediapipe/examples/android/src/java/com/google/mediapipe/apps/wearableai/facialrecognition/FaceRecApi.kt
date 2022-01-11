/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use requireContext() file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.apps.wearableai.facialrecognition

import androidx.lifecycle.LifecycleService;
import android.util.Log;
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.lifecycle.Observer;
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.method.ScrollingMovementMethod
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.apps.wearableai.facialrecognition.model.FaceNetModel
import com.google.mediapipe.apps.wearableai.facialrecognition.model.Models
import com.google.mediapipe.apps.wearableai.facialrecognition.Prediction
import java.io.*
import java.util.concurrent.Executors

import androidx.lifecycle.LiveData;

//rxjava
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException

import com.google.mediapipe.apps.wearableai.comms.MessageTypes

//database
import com.google.mediapipe.apps.wearableai.database.person.PersonCreator;
import com.google.mediapipe.apps.wearableai.database.person.PersonRepository;
import com.google.mediapipe.apps.wearableai.database.person.PersonEntity;
import com.google.mediapipe.apps.wearableai.database.person.PersonViewModel;

import androidx.lifecycle.ViewModelProvider;

import com.google.mediapipe.apps.wearableai.R;

class FaceRecApi ( private var context: LifecycleService, private var mPersonRepository: PersonRepository ){

//            .setTargetResolution(Size( 480, 640 ) )
    private var isSerializedDataStored = false
    
    private val TAG = "WearableAi_FaceRecApi"

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<Long,FloatArray>>()

    public var isApiSetup = false

    // Serialized data will be stored ( in app's private storage ) with requireContext() filename.
    private val SERIALIZED_DATA_FILENAME = "image_data"

    // Shared Pref key to check if the data was stored.
    private val SHARED_PREF_IS_DATA_STORED_KEY = "is_data_stored"

//    private lateinit var previewView : PreviewView
    private lateinit var frameAnalyser  : FaceRecFrameAnalyser
    private lateinit var model : FaceNetModel
    private lateinit var fileReader : FileReader
//    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var sharedPreferences: SharedPreferences

    //data object to pass around object
    private lateinit var dataObservable : PublishSubject<JSONObject>

    companion object {

        lateinit var logTextView : TextView

        fun setMessage( message : String ) {
            logTextView.text = message
        }

    }

    //setup face rec system on launch
    fun setup() {
        frameAnalyser = FaceRecFrameAnalyser( context , Models.FACENET_512 )
        fileReader = FileReader( context , Models.FACENET_512 )

        //load our saved face recognition ids and embeddings - don't load if we still haven't saved any faces
        sharedPreferences = context.getSharedPreferences( context.getString( R.string.app_name ) , Context.MODE_PRIVATE )
        isSerializedDataStored = sharedPreferences.getBoolean( SHARED_PREF_IS_DATA_STORED_KEY , false )
        if ( isSerializedDataStored ) {
            Logger.log( "Serialized data was found.")
            faceList = loadSerializedImageData()
            frameAnalyser.faceList = faceList
        }

        //eventually we need to constantly watch the database for face rec changes and remove any references to deleted things, that's what we were trying to do below. We can't run observe in non main ui, so for now, let's just delete an entry whenever we come across a "deleted" entitiy (in onPredictionResults)
//        //setup the live data which will watch for updates to our face/person database so we have live update here
//        setupFaceRecDbWatcher()
        removeDeletedPeople()
        isApiSetup = true
    }

//    fun setupFaceRecDbWatcher(){
//        val badImagesList: LiveData<List<PersonEntity>> = mPersonRepository.getBadForFaceRec()
//
//
//        badImagesList.observe(context, Observer { badImages ->
//            Log.d(TAG, "bad images size: " + badImages.size);
//            val toDrop = mutableListOf<Int>();
//            for (i in badImages.indices){
//                Log.d(TAG, "bad images index" + i)
//                Log.d(TAG, "bad image person id is: " + badImages.get(i).getPersonId())
//                for (j in faceList.indices){
//                    Log.d(TAG, "face list index" + j)
//                    if (faceList[j].first == badImages.get(i).getPersonId()){
//                        toDrop.add(j);
//                    }
//                }
//            }
//            var count = 0; // must have this count, as each time we remove, we decrease all indicies by 1
//            for (k in toDrop.listIterator()){
//                Log.d(TAG, "to drop item " + k)
//                faceList.removeAt(k - count);
//                count = count + 1;
//            }
//
//        })
//    }

    fun removeDeletedPeople(){
    }

    fun analyze(image: Bitmap, imageTime : Long, imageId : Long){
        if (isApiSetup){
            frameAnalyser.analyze(image, imageTime, imageId, ::onPredictionResults)
        }
    }

    // Get the image as a Bitmap from given Uri and fix the rotation using the Exif interface
    // Source -> https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    private fun getFixedBitmap( imageFileUri : Uri ) : Bitmap {
        var imageBitmap = BitmapUtils.getBitmapFromUri( context.contentResolver , imageFileUri )
        val exifInterface = ExifInterface( context.contentResolver.openInputStream( imageFileUri )!! )
        imageBitmap =
            when (exifInterface.getAttributeInt( ExifInterface.TAG_ORIENTATION ,
                ExifInterface.ORIENTATION_UNDEFINED )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> BitmapUtils.rotateBitmap( imageBitmap , 90f )
                ExifInterface.ORIENTATION_ROTATE_180 -> BitmapUtils.rotateBitmap( imageBitmap , 180f )
                ExifInterface.ORIENTATION_ROTATE_270 -> BitmapUtils.rotateBitmap( imageBitmap , 270f )
                else -> imageBitmap
            }
        return imageBitmap
    }


    // ---------------------------------------------- //


//    private val fileReaderCallback = object : FileReader.ProcessCallback {
//        override fun onProcessCompleted(data: ArrayList<Pair<Long, FloatArray>>, numImagesWithNoFaces: Int) {
//            frameAnalyser.faceList = data
//            faceList = data
//            saveSerializedImageData( data )
//            Logger.log( "Images parsed. Found $numImagesWithNoFaces images with no faces." )
//        }
//    }


    private fun saveSerializedImageData(data : ArrayList<Pair<Long,FloatArray>> ) {
        val serializedDataFile = File( context.filesDir , SERIALIZED_DATA_FILENAME )
        ObjectOutputStream( FileOutputStream( serializedDataFile )  ).apply {
            writeObject( data )
            flush()
            close()
        }
        sharedPreferences.edit().putBoolean( SHARED_PREF_IS_DATA_STORED_KEY , true ).apply()
    }


    private fun loadSerializedImageData() : ArrayList<Pair<Long,FloatArray>> {
        val serializedDataFile = File( context.filesDir , SERIALIZED_DATA_FILENAME )
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<Long,FloatArray>>
        objectInputStream.close()
        return data
    }

    public fun setDataObservable(observable : PublishSubject<JSONObject>){
        dataObservable = observable
    }

    //next steps - 
    //make a new id for each person - save to person database as "newperson event"
    //edit the faceList to include id, or even just change name to id for simplicicity
    //make a person database table
    //work out how to structure the person database so it can handle all kinds of "people" events, not just sightings - like "had conversation with", "hugged", "talked to", "walked past", "saw on screen"
    //save person sighting to database
    //give ui to name people without names
    private fun onPredictionResults(predictions : ArrayList<Pair<Prediction, FloatArray>>, imageTime : Long, imageId : Long){
        Logger.log("onPredictionResults has been called in facial rec")
        for (i in predictions.indices) {
            var personId = predictions[i].first.label
            Logger.log("Saw personId: " + personId)

            var displayName : String? = null
            if (personId == null){
                // if face is unknown, add a new encoding to our faceList object, save it, and update it in frameanalyzer
                Log.d(TAG, "person id was null")
                displayName = "Unknown"
                personId = addNewPersonToDatabase(imageTime, imageId);
                addToEncodingList(personId, predictions[i].second)
            } else{
                // if face has been seen before, get the name of the person (note - we may have seen the face before but not saved a name, if so it returns null, and thus is still "Unknown"
                displayName = mPersonRepository.getPersonsName(personId);
                if (displayName == null || displayName == "Unknown"|| displayName == "unknown"){                    Log.d(TAG, "diplayName: null or unknown or deleted")
                    displayName = "Unknown"
                } else if (displayName == "deleted"){ //for now, we remove the reference here if the person is seen, we should never see delted because if a person is deleted, we should remove them from face rec object
                    displayName = "Unknown"
                    //delete that face encoding from our face list
                    dropIdFromFaceList(personId);
                } else {
                    Log.d(TAG, "diplayName: " + displayName)
                }
            }

            // save face sighting to database
            saveFaceSightingToDatabase(personId, imageTime, imageId)

            // send face sighting to ASG
            sendAsgFaceSighting(displayName, imageTime)
        }
    }

    private fun sendAsgFaceSighting(name : String, imageTime : Long){
        //send rxjava to websocket to be sent to the ASG
        try {
            val toSendObj = JSONObject()
            toSendObj.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.FACE_SIGHTING_EVENT)
            toSendObj.put(MessageTypes.FACE_NAME, name)
            toSendObj.put(MessageTypes.TIMESTAMP, imageTime)
            dataObservable.onNext(toSendObj)
        } catch (e : JSONException){
            e.printStackTrace()
        }
    }

    private fun dropIdFromFaceList(personId : Long){
        Log.d(TAG, "dropping id from face list");
        for (i in faceList.indices){
            if (faceList.get(i).first == personId){
                faceList.removeAt(i)
                return
            }
        }
    }

    //after we make our encodings, the user goes through and labels images with names. If the image was too dark/blurry/etc., it will be labelled as a bad image. Here, each time we load/reload, we can check for all bad images, and drop the encoding if it links to a bad image
    private fun dropBadImages(){
    }

    private fun saveFaceSightingToDatabase(personId : Long, imageTime : Long, imageId : Long){
        //save 'seen' event to Person database
        PersonCreator.create(personId, "event", "seen", imageTime, imageId, mPersonRepository);
    }
    
    private fun addNewPersonToDatabase(imageTime : Long, imageId : Long) : Long {
        //save new to Person database
        val id = PersonCreator.create(null, "new_face_name", "unknown", imageTime, imageId, mPersonRepository);
        mPersonRepository.updatePersonId(id); //update the personId to match the row id, only do this once when we are adding a new person
        return id
    }

    private fun addToEncodingList(personId: Long, subject : FloatArray){
        //add encoding to encoding list
        //update encoding list in FaceRecFrameAnalyzer
        //save new encoding list to file - Dispatch
        faceList.add(Pair(personId, subject));
        frameAnalyser.faceList = faceList
        saveSerializedImageData( faceList )
    }
}
