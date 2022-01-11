/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mediapipe.apps.wearableai.facialrecognition.model.FaceNetModel
import com.google.mediapipe.apps.wearableai.facialrecognition.model.MaskDetectionModel
import com.google.mediapipe.apps.wearableai.facialrecognition.model.ModelInfo
import com.google.mediapipe.apps.wearableai.facialrecognition.model.Models
import com.google.mediapipe.apps.wearableai.facialrecognition.Prediction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

// Analyser class to process frames and produce detections.
class FaceRecFrameAnalyser( private var context: Context , private var whichModel: ModelInfo) {

    private val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
            .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    // You may the change the models here.
    // Use the model configs in Models.kt
    // Default is Models.FACENET ; Quantized models are faster
    private val model = FaceNetModel( context , whichModel )

    private val nameScoreHashmap = HashMap<Long,ArrayList<Float>>()
    private var subject = FloatArray( model.embeddingDim )

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<Long,FloatArray>>()

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "cosine"

    @SuppressLint("UnsafeOptInUsageError")
    fun analyze(image: Bitmap, imageTime : Long, imageId : Long, onPredictionResultsCallback: (predictions : ArrayList<Pair<Prediction, FloatArray>>, imageTime : Long, imageId : Long) -> Unit) {
        // If the previous frame is still being processed, then skip this frame
        if ( isProcessing ) {
            return
        }
        else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap = image

            val inputImage = InputImage.fromBitmap( frameBitmap, 0 )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope( Dispatchers.Default ).launch {
                        runModel( faces , frameBitmap, imageTime, imageId, onPredictionResultsCallback )
                    }
                }
                .addOnCompleteListener {
                }
        }
    }

    private suspend fun runModel( faces : List<Face> , cameraFrameBitmap : Bitmap, imageTime: Long, imageId : Long, onPredictionResultsCallback: (predictions : ArrayList<Pair<Prediction, FloatArray>>, imageTime : Long, imageId : Long) -> Unit ){
        withContext( Dispatchers.Default ) {
            val predictions = ArrayList<Pair<Prediction,FloatArray>>()
            for (face in faces) {
                Logger.log("Detected face, running face rec model for that face");
                // Crop the frame using face.boundingBox.
                // Convert the cropped Bitmap to a ByteBuffer.
                // Finally, feed the ByteBuffer to the FaceNet model.
                val croppedBitmap = BitmapUtils.cropRectFromBitmap( cameraFrameBitmap , face.boundingBox )
                subject = model.getFaceEmbedding( croppedBitmap )

                //if we have no saved faces, just return unknown
                try {
                    if (faceList.size == 0) {
                        Logger.log("Adding unknown prediction because face list is empty");
                        predictions.add(
                                Pair( Prediction(
                                        face.boundingBox,
                                        null
                                    ),
                                    subject
                                )
                        )
                    } else {
                        // Perform clustering ( grouping )
                        // Store the clusters in a HashMap. Here, the key would represent the 'name'
                        // of that cluster and ArrayList<Float> would represent the collection of all
                        // L2 norms/ cosine distances.
                        for ( i in 0 until faceList.size ) {
                            // If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                            // initialize a new one.
                            if ( nameScoreHashmap[ faceList[ i ].first ] == null ) {
                                // Compute the L2 norm and then append it to the ArrayList.
                                val p = ArrayList<Float>()
                                if ( metricToBeUsed == "cosine" ) {
                                    p.add( cosineSimilarity( subject , faceList[ i ].second ) )
                                }
                                else {
                                    p.add( L2Norm( subject , faceList[ i ].second ) )
                                }
                                nameScoreHashmap[ faceList[ i ].first ] = p
                            }
                            // If this cluster exists, append the L2 norm/cosine score to it.
                            else {
                                if ( metricToBeUsed == "cosine" ) {
                                    nameScoreHashmap[ faceList[ i ].first ]?.add( cosineSimilarity( subject , faceList[ i ].second ) )
                                }
                                else {
                                    nameScoreHashmap[ faceList[ i ].first ]?.add( L2Norm( subject , faceList[ i ].second ) )
                                }
                            }
                        }
                        Logger.log("Got predictions: " + predictions.size);

                        // Compute the average of all scores norms for each cluster.
                        val avgScores = nameScoreHashmap.values.map{ scores -> scores.toFloatArray().average() }
                        Logger.log( "Average score for each user : $nameScoreHashmap" )

                        val names = nameScoreHashmap.keys.toTypedArray()
                        nameScoreHashmap.clear()

                        // Calculate the minimum L2 distance from the stored average L2 norms.
                        val bestScoreUserName: Long? = if ( metricToBeUsed == "cosine" ) {
                            // In case of cosine similarity, choose the highest value.
                            if ( avgScores.maxOrNull()!! > model.model.cosineThreshold ) {
                                names[ avgScores.indexOf( avgScores.maxOrNull()!! ) ]
                            }
                            else {
                                null
                            }
                        } else {
                            // In case of L2 norm, choose the lowest value.
                            if ( avgScores.minOrNull()!! > model.model.l2Threshold ) {
                                null
                            }
                            else {
                                names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                            }
                        }
                        predictions.add(
                            Pair( Prediction(
                                    face.boundingBox,
                                    bestScoreUserName
                                ),
                                subject
                            )
                        )
                    }
            } catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Logger.log( "Exception in FaceRecFrameAnalyser : ${e.message}" )
                    continue
            }
        }
        withContext( Dispatchers.Main ) {
            // Send predictions to call class
            if (predictions.size > 0){
                Logger.log( "Calling callaback" )
                onPredictionResultsCallback(predictions, imageTime, imageId)
            }

            // record that the processing is complete
            isProcessing = false
        }
        }
    }


    // Compute the L2 norm of ( x2 - x1 )
    private fun L2Norm( x1 : FloatArray, x2 : FloatArray ) : Float {
        return sqrt( x1.mapIndexed{ i , xi -> (xi - x2[ i ]).pow( 2 ) }.sum() )
    }


    // Compute the cosine of the angle between x1 and x2.
    private fun cosineSimilarity( x1 : FloatArray , x2 : FloatArray ) : Float {
        val mag1 = sqrt( x1.map { it * it }.sum() )
        val mag2 = sqrt( x2.map { it * it }.sum() )
        val dot = x1.mapIndexed{ i , xi -> xi * x2[ i ] }.sum()
        return dot / (mag1 * mag2)
    }

}
