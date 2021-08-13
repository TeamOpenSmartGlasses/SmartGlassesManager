# Wearable Intelligence System

The Wearable Intelligence System is a personal intelligence amplification tool. This project and codebase is currently in stage alpha and is most valuable to developers, super users, and individuals in the XR and wearables industry. 

The Wearable Intelligence System is:

1. A fully functional, wearable, intelligence amplification system featuring a number of intelligence tools:

- social/emotional intelligence amplification system
- memory expansion tools
- conversational intelligence enhancement
- voice control with command and natural language interface

2. A software framework for developing cognitive extension systems on the the coming wave of consumer ready Android smart glasses. 
 
## Technical Description

This system provides the foundation for a wearable computing suite consisting of connected Android Smart Glasses (ASG), Android Smart Phone (ASP) and a Gnu/Linux box (GLBOX).

The ASG acts as wearable sensors (camera, microphone, etc.) and wearable display (waveguides, birdbath, etc.). The ASP is running a MediaPipe machine learning pipeline on wearable POV video. The GLBOX handles transcription, voice command, and programmatic control of the ASG from within a Linux development environment.

# How To Use

The system *currently* requires three pieces of hardware:

ASP - Android Smart Phone (Tested: OnePlus 7T)
ASG - Android Smart Glasses (Tested: Vuzix Blade, Epson Moverio)
GLBOX - Gnu/Linux Single-Board-Computer/Laptop (Tested: Lenovo Legion Y540 w/ Ubuntu 20) 

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

The GLBOX (Gnu/Linux Box (a computer running a Gnu/Linux distribution operation system)) is part of the the Wearable Intelligence System that handles transcription, running commands, and saving memories.

Live Linux programmatic control of Android smart glasses running the Wearable Intelligence System app.

Run this on any laptop or single-board-computer running Gnu/Linux.

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

## Demo

1. Install `scrcpy`: https://github.com/Genymobile/scrcpy
2. Run `scrcpy`

## Abbreviations

ASP - Android Smart Phone  
ASG - Android Smart Glasses  
GLBOX - Gnu/Linux 'Single Board Computer'/Laptop  

## Authors

Cayden Pierce - <caydenpierce.com> <emexwearables.com>
