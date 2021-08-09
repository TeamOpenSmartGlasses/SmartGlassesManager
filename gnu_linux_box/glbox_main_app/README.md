# Wearable Intelligence System

The Wearable Intelligence System is a smart glasses tool to upgrade users' intelligence. The Wearable Intelligence System is designed to be a foundation upon which to build human cognitive extensions. 

Alongside the technical foundation, the current system features a number of intelligence tools:

- social/emotional intelligence amplification system
- memory expansion tools
- conversational intelligence enhancement
- command and natural language voice control

## Technical Description

This system provides the foundation for a wearable computing suite consisting of connected Android Smart Glasses (ASG), Android Smart Phone (ASP) and a Gnu/Linux box (GLBOX).

The ASG acts as wearable sensors (camera, microphone, etc.) and wearable display (waveguides, birdbath, etc.). The ASP is running a MediaPipe machine learning pipeline on wearable POV video. The GLBOX handles transcription, voice command, and programmatic control of the ASG from within a Linux development environment.

## System Components  

### GNU/Linux Box

```
cd gnu_linux_box
source venv/bin/activate #activate virtualenv
python3 main.py
```

The GLBOX (Gnu/Linux Box (a computer running a Gnu/Linux distribution operation system)) is part of the the Wearable Intelligence System that handles transcription, running commands, and saving memories.

Live Linux programmatic control of Android smart glasses running the Wearable Intelligence System app.

Run this on any laptop or single-board-computer running Gnu/Linux and see the main repo:  for instructions on how to get the system running on the accompanying required Android Smart Glasses (ASG) and Android Smart Phone (ASP).

### Android Smart Glasses

Run the android_smart_glasses/smart_glasses_app Android app on Android Smart Glasses.

The latest system is developed on a Vuzix Blade 1.5.

Running on another pair of Android AR/MR glasses could work without issue or porting could happen in 48 hours if the hardware is supplied.

### Android Smart Phone

Run the android_smart_phone/mobile_compute_app on any modern-ish Android smart phone (a good CPU/GPU is reccomended for MediaPipe graph) that can make a WiFi hotspot.

## TODO

x - clean code so it's modular
x - provide interface that always pushes the latest live captions
x - make server in python that will connect to the Android Vuzix WearableAI server
x - System.out log the closed captions in vuzix blade
x - make GUI in vuzix blade to display the captions
- get running on pocket worn SBC with bluetooth headset microphone and speaker
- add VAD so we don't waste our time, bandwidth, and money transcriving non-speech
    - Google webRTC VAD is one of the best and well supported for python 4 : https://github.com/wiseman/py-webrtcvad


## Improve
-get good open source TTS running
    -coqui tts works great but can we get it running in a script real time on the GPU?
    -other faster option?
    
# SETUP

1. Clone this repo
2. Make a virtualenv
3. `sudo apt install portaudio19-dev python3-pyaudio`
4. Install requirements (`pip3 install -r requirements.txt`)
5. Make file `./wolfram_api_key.txt` and paste your WolframOne App Id in there
6. Setup GCP to allow Speech to Text, make credentials for it, and place your JSON credentials at `./utils/creds.json`
7. Run `main.py`

# Authors

Cayden Pierce - caydenpierce.com, emexwearables.com
