Working on mediapipe Android now:

-pose tracknig
-hand tracking
-iris tracking
-face detection
-face landmark detection
(much of this is in the mediapipe Holistic example)

What we are adding/doing:

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

-enhanced emotional intelligence
-enhanced understanding and retention of information
    -live display showing transcript of the current converstation
-wearable lie detector
-wearable emotion detector
    -stress vs. confidence
    -comfort vs. discomfort
-extended memory
    -all memories are tagged with high level information about the scene
    -recall memory using high level tags - e.g. "we were in the park downtown about two weeks ago, it ws in the afternoon and I was with John and Steve, there were people there doing tai chi" - instant flash back to the raw data of that moment, read the transcipt of the conversation, etc.
    
# HOW TO USE

The system requires three things:

ASP - Android Smart Phone (Tested: OnePlus 7T)
ASG - Android Smart Glasses (Tested: Vuzix Blade, Epson Moverio)
GLBOX - Gnu/Linux Single-Board-Computer/Laptop (Tested: Lenovo Legion Y540 w/ Ubuntu 20) 

1. Turn on the WiFi hotspot on the ASP.
2. Connect the GLBOX and ASG to the ASP WiFi hotspot.
3. Start the life_live_captions Python server on the GLBOX.
```
cd life_cc
source venv/bin/activate #activate virtual environment
python3 main.py
```
5. Start the Mobile Compute app on the ASP.
6. Start the smart glasses app on the ASG.

## Demo

Install `scrcpy`: https://github.com/Genymobile/scrcpy
Run `scrcpy`

## Abbreviations

ASP - Android Smart Phone  
ASG - Android Smart Glasses  
GLBOX - Gnu/Linux 'Single Board Computer'/Laptop  
