The android pipeline is quickly becoming the pipeline of choice. 

Why? Because I would rather make something that is useful than dick around training new models that can't run in the real world. The mediapipe models run super fast on my Oneplus 7T, and I would love to keep using the mediapipe framework to make perception pipelines, as it's beautiful and exactly what I was planning on building myself.

So, what do I have working on my Android now?

-pose tracknig
-hand tracking
-iris tracking
-face detection
-face landmark detection
(much of this is in the mediapipe Holistic example)

What I want to add:

-voice recognition - deep speech (easy to get going on android)
    -live display showing transcript of the current converstation
        -bluetooth ring to scroll back in the conversation if you missed a point/word, or forget where you were at
        -combine with face rec to make speech bubble appear when looking at someone
    -pipe this into some NLP/text classification (like https://www.tensorflow.org/lite/models/text_classification/overview) to understand emotinn/tone behind what is being said (and pair with facial emotion, body language (from pose), etc).
-scene recognition (Places365 is pretty insane, see https://github.com/CSAILVision/places365 and here for a tf port: https://github.com/GKalliatakis/Keras-VGG16-places365)
-face recognition + personal face database (mediapipe has face detection, but not recognition, need to use face_detection in mediapipe to crop ROI and feed to face rec net that is ported into mediapipe)
    -search linkedin/twitter/facebook for people/names/faces, live display overlay of the conversation partner's posts, information, occupation, etc
-object recognition (already in mediapipe, but kinda shit. Start with mediapipe's for now and find something better in tf that can be brought into mediapipe)
-emotion recognition
    -from face landmakrs
    -from hand movements
    -from pose
    -from voice sounds
    -from voice content (speech)
    -combine all the above together
-gender recognition
    -useful for memory extension tags
    -useful for higher level network training
-race recognition
    -useful for memory extension tags
    -useful for higher level network training
-age recognition 
    -useful on its own - I want to know how old someone is
    -useful for memory extension tags
    -useful for higher level network training
-raw data: weather, time, gps, accelerometer, raw audio, raw video, temperature, magnetometer raw, gyroscope raw, barometer raw 

Use cases:

-wearable lie detector
-wearable emotion detector
    -stress vs. confidence
    -comfort vs. discomfort
    -
-extended memory
    -all memories are tagged with high level information about the scene
    -recall memory using high level tags - e.g. "we were in the park downtown about two weeks ago, it ws in the afternoon and I was with John and Steve, there were people there doing tai chi" - instant flash back to the raw data of that moment, read the transcipt of the conversation, etc.
    
# HOW TO USE

WearableAiDisplayMoverio - runs on Epson Moverio BT 200. Takes images repeatedly, send them over a socket to the compute module, receives a response and displays that response on the Moverio screen
WearableAiComputeModule - runs on Oneplus7T, or other modern Android with good CPU+GPU. Receives images from Moverio, puts them through processing pipeline, and sends back results

## Demo

Install `scrcpy`: https://github.com/Genymobile/scrcpy
Run `scrcpy`
