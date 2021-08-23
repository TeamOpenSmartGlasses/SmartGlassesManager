# Wearable Intelligence System

The Wearable Intelligence System is a personal [intelligence amplification](https://en.wikipedia.org/wiki/Intelligence_amplification) tool and [humanistically intelligent](https://en.wikipedia.org/wiki/Humanistic_intelligence) system. The system runs on a pair of AR/smart glasses and presents the user with intelligent audio and visual information.

Users benefit from a suite of tools that can be used in the real world while developers benefit from a system that provides a smart glasses app backend. 

This project and codebase is currently in stage alpha, is not stable, and is subject to change. Right now, the system is most valuable to super users, developers, and individuals in the XR and wearables industry. 

## What is this?

The Wearable Intelligence System is:

1. **Smart Glasses Apps - User Tool**  

A fully functional, wearable, intelligence amplification system featuring a number of intelligence tools:

- understand your social-emotional environment with emotional intelligence tools
- remember every names, idea, and conversation with memory expansion tools
- improve understanding and retention in conversations, meetings, lectures, etc. with live closed captions overlaid on your vision at all times
- voice control with command and natural language interface

2. **Software Framework - Developer Tool**  

A software framework for developing smart glasses based intelligence systems on the the coming wave of consumer ready Android smart glasses. 

There are three pieces of hardware involved, each with it's own standalone app. All hardware connects to the same WiFi network and connect to each other when the apps (at the top level of this repo, with the folders named after which hardware device they are for) are run. The system already handles a number of backend smart glasses system functionalities:  
- Android smart glasses connect to and communicate with any external Android device (normally a smart phone with a powerful SOC) to send sensory data, run compute on the external Android device, receive results back from the external Android device, and render those results on the AR display
- Android Smart Glasses connect to and communicate with any GNU/Linux computer, receive commands from the GNU/Linux computer, and render those results on the glasses' AR display
- Android smart phone (or any Android device) receives sensory data from the smart glasses, runs compute-heavy machine-learning inference (neural networks) and signal processing using a [MediaPipe Perception Pipeline](https://arxiv.org/abs/1906.08172), and pushes results back to the glasses
- GNU/Linux computer can run the voice commmand system, which runs live live Speech-To-Text, processes the data for commands, runs those commands, and send a stream of raw transcriptions and voice command query results the glasses.
- System setup that adding new functions/tools is as simple as writing a new program that can run in Linux - use the prebuilt, simple voice command interface to run arbitrary code based on an arbitrary command name
 
## Technical Description

This system provides the foundation for a wearable computing suite consisting of connected Android Smart Glasses (ASG), Android Smart Phone (ASP) and a GNU/Linux box (GLBOX).

The ASG acts as wearable sensors (camera, microphone, etc.) and wearable display (waveguides, birdbath, etc.). The ASP is running a server which the ASG connects to and streams POV camera video over Wifi. The ASP also is running a MediaPipe machine learning pipeline on incoming sensory data. The GLBOX also connects to the ASG using a TCP socket and it handles transcription, voice command, and programmatic control of the ASG from within a Linux development environment.

# How To Use

The system *currently* requires three pieces of hardware:

- ASP - Android Smart Phone (Tested: OnePlus 7T)
- ASG - Android Smart Glasses (Tested: Vuzix Blade, Epson Moverio)
- GLBOX - GNU/Linux Single-Board-Computer/Laptop (Tested: Lenovo Legion Y540 w/ Ubuntu 20) 

#### **Please see the "Subcomponents" section for more details on how to complete each step.**

1. Turn on the WiFi hotspot on the ASP.
2. Connect the GLBOX and ASG to the ASP WiFi hotspot.
3. Start the life_live_captions Python server on the GLBOX.
5. Start the Mobile Compute app on the ASP.
6. Start the smart glasses app on the ASG.

## System Components  

### GNU/Linux Box

1. Follow *Setup* in gnu_linux_box/glbox_main_app/README.md
2. Activate virtualenv and launch app

```
cd gnu_linux_box
source venv/bin/activate #activate virtualenv
python3 main.py
```

The GLBOX (GNU/Linux Box (a computer running a GNU/Linux distribution operation system)) is part of the the Wearable Intelligence System that handles transcription, running commands, and saving memories.

Live Linux programmatic control of Android smart glasses running the Wearable Intelligence System app.

Run this on any laptop or single-board-computer running GNU/Linux.

### Android Smart Glasses

1. Open android_smart_glasses/smart_glasses_app Android app in Android studio.
2. Plug in Android Smart Glasses and build + flash to device using Android Studio.

The latest system is developed on a Vuzix Blade 1.5.

Running on another pair of Android AR/MR glasses could work without issue or porting could happen in 48 hours if the hardware is supplied.

### Android Smart Phone

1. Follow commands here to setup MediaPipe: <https://google.github.io/mediapipe/getting_started/install.html#installing-on-debian-and-ubuntu>
2. 
Run the following commands to build and run the app for the ASP:
```
cd android_smart_phone/mediapipe
./build_single_android.sh mediapipe/examples/android/src/java/com/google/mediapipe/apps/wearableai
```
3. Run the android_smart_phone/mobile_compute_app on any modern-ish Android smart phone (a good CPU/GPU is reccomended for MediaPipe graph) that can make a WiFi hotspot.

## Voice Commands

All voice commands must be preceded by a `wakeword`. A `wakeword` is any word you choose to "wake up" the system and start listening to commands, commands will only be run if they follow a wakeword. Set your own wakeword by adding it to wakewords.txt, or just use an existing `wakeword` from wakewords.txt.  Choose a `wakeword` that the Speech-To-Text system can reliably recognize.

`ask Wolfram <query>` - ask Wolfram Alpha a natural language <query>  
 
`add wake word <wakeword>` - add a new <wake word> to wakewords.txt  
 
`save speech` - save the transcribed speech to a file. This can be used to save ideas, thoughts, notes, reminders, etc.  
 
`switch mode <arg>` - switch the current mode of the smart glasses app.  
Currently available modes:  
- live life captions
- blank screen
- social mode
 
 ## Modes
 
 ### Live Life Captions
 
 Closed captions of everything you and those around you say. Live view of commands and commmand output. Nouns in transcripts are highlighted. Soon to be extended to give definition, summary, encylopedia, and NLP functionalities.
 
 ### Social Mode
 
 A social-emotional intelligence tool to be used in social situations. Live metrics about the social environment (eye-contact, facial emotion, high-level psychological metrics (e.g. stress, confidence, etc.)) overlaid on the users vision. Soon to extended with facial recognition (tie in to memory stream "Personal Person Database"), amalgamation (combine social information about everyone in the room), more metrics (drowsiness, believability (both ways), interest, etc.).
 
 ### Blank Mode
 
 Blanks out the screen, sleep mode.

## Demo

1. Install `scrcpy`: https://github.com/Genymobile/scrcpy
2. Run `scrcpy`

## Abbreviations

ASP - Android Smart Phone  
ASG - Android Smart Glasses  
GLBOX - GNU/Linux 'Single Board Computer'/Laptop  

## Authors

Cayden Pierce - [emexwearables.com](https://emexwearables.com)
