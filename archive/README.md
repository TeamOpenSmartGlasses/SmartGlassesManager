# Wearable Intelligence Enhancement 

## PLAN

AI is getting really, really good at interpreting sensor data. There seems to be a ton of networks that people make, and they work really well, but there are not many applications of those networks.

### 1 - Use

Epson Moverio 200 BT running an assortment of AI models on an Android phone. Overload vision of machine intellgience extracting insights about the environment we are in and providing us with a live stream of information taht is immediatly useful to us. Examples include: lie detection, emotion detection, stress and confidence responses, comfort and discomfort responses, personal memory expansion (memorize everything and recall it naturally).

## 2 - Learning
My plan is to take many, many networks and run them on a wearable computers sensor stream. As well, we should take information that isn't a direct sensor reading, but easy to pull in (i.e. time of day, time of year, weather, light levels, etc. etc.). Make a giant output vector with as much environment/contextual information as possible, and train another deep NN on that, on the high level input vector... this could make possible many high level metric classification that currently is not possible when pulling it out form the low level data. This approaches somewhat Kurzweils theory of mind and how our cognition is models on models.

VIDEO

Object Recognitin - > 
Dense Object Recogniton - > TensorMask -> https://github.com/facebookresearch/detectron2/tree/master/projects/TensorMask
Pose estimation - DensePose - https://github.com/facebookresearch/detectron2/tree/master/projects/DensePose

3D object mesh estimation: Mesh R-CNN -> https://github.com/facebookresearch/meshrcnn

Image segmentation: PointRend -> https://github.com/facebookresearch/detectron2/tree/master/projects/PointRend

NOT ACTUALLY OPEN, DON't USE, THEY OWN ALL COPIES OF THE PROGRAM -> Face landmark detection : OpenFace -> https://github.com/TadasBaltrusaitis/OpenFace

Room classification: https://github.com/bartkowiaktomasz/microsoft-cntk-room-classifier

Gender classification: https://github.com/arunponnusamy/gender-detection-keras
Gender + race classification: https://github.com/wondonghyeon/face-classification
Speech from video and audio: https://github.com/astorfi/lip-reading-deeplearning 
Speech from video: https://github.com/afourast/deep_lip_reading

DeepFace -> EMotino, age, reace, and gender from image: https://github.com/serengil/deepface

Heart rate from video: https://github.com/erdewit/heartwave
Heart rate from video: https://github.com/habom2310/Heart-rate-measurement-using-camera

AUDIO

Gender recogntion: https://github.com/SuperKogito/Voice-based-gender-recognition
VOice recognition: DeepSpeech : https://github.com/mozilla/DeepSpeech

OTHER

Encodings/vectorization in general: https://github.com/vector-ai/vectorhub
Other data to use: IMU, GPS, time (of day, day of week, time of year, seperate), weather, temperature, barometer, 

INTERESTING CHAINS

-voice recognition engine -> Language encoding network (BERT)
-

WHAT IS THE OUTPUT?

-lie detection
-danger detection
-"awkwardness"?
-emotion detection - better than face emotion detection
    -in individuals (via facial recognition)
    -in a scene (general mood of the group of people, often called the "environment" when reffering to the social environment)

PLAN

1. Get a few pre-trained nets doing inference on images or audio chunks
x -Deepspeech
x-object recognition
x-face information
x-pose estimation
-vectorize 
    -image with vector hub
    -audioo with vector hub
    -make one class to wrap vectorhub funcs? Or just do in main...
-room detection - MS CLNK
    -install CNTK
    -clone : https://github.com/TreeLLi/CNTK-Hotel-pictures-classificator
    -get running in main.py

2. Get them all running on the same data stream

3. Run mobile (laptop with USB camera).
4.  -run in cloud with GPUs?
5. -run on ANdroid somehow?
6. -run on smart glasses? WHich ones? - not vuzix
 
7. Train network on them.



## Instructions
RUNNING TENSORMASK

```
cd detectron2
python projects/TensorMask/train_net.py 0 --config-file projects/TensorMask/configs/tensormask_R_50_FPN_6x.yaml --eval-only MODEL.WEIGHTS projects/TensorMask/models/model_final_higher_ap.pkl
```

# Install

There's a lot of pretrained models here that you'll need to download. If one is missing, go find it on Github and make a README.md PR.

Detectron2 prebuilt models, to be saved in detectron2/models: https://github.com/facebookresearch/detectron2/blob/master/MODEL_ZOO.md

# Ideas
-when walking down street, read every sign and look it up on google maps with current GPS coordinates
