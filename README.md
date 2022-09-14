# Wearable Intelligence System

The Wearable Intelligence System (WIS) is the homepage for your smart glasses with a host of built-in apps, voice controls, always available HUD information, app launcher, and more. The WIS makes building smart glasses applications easy. There's a number of powerful and fashionable smart glasses being released (2022-24), and the WIS gives you an interface and apps to make those glasses useful. The WIS is like your phone homescreen or your computer desktop combined with a smart assistant.

## beta Version Video

Coming soon!

### Early alpha Version Video
[![Wearable Intelligence System Demo - Part 1](res/early_demo_thumbnail.png)](http://www.youtube.com/watch?v=O2a6ng9jICE "Wearable Intelligence System Demo - Smart Glasses Apps - Part 1")

## What It Can Do Now

### User Features

- Search - Search the web with voice, see immediate results on your HUD.
- Ask Questions - Ask an intelligent voice assistant any question, see answers on your HUD.
- Live Translation - Translate live foreign language speech into your native language, and silently read the output on the screen.
- Remember More - Memory tools to expand your memory with information recording and recall.
- Visual Search - Define anything that you see. Find images with similiar content as your point-of-view (POV) camera image.
- Name Memorizer - Never forget a name again with HUD notifications when you see a familiar face
- Live Captions - Improve understanding and retention in conversations, meetings, lectures, etc. with live closed captions overlaid on your vision at all times.
- Autociter / Wearable Referencer - Auto-associative voice search through a personal database, send relevant links to conversation partners over SMS.

### Developer Use

The WIS can do much more if you are a researcher, engineer, scientist, or diy hobby maker because it's a software framework that makes it easy to build smart glasses applications and experiments. Checkout the [Documentation](#documentation) for more information.
 
## How To Use 

You will need two pieces of hardware to run the system:  

- ASP - Android Smart Phone
- ASG - Android Smart Glasses (Supported: Vuzix Blade)

### Voice Commands

All voice commands must be preceded by a `wakeword` (the wake word is "hey computer"). A `wakeword` is "hey computer" or any word you choose to "wake up" the system and start listening to commands.
    
##### Wakeword

The wake word is "hey computer".
    
#### Voice Commands

Say "hey computer" to see available commands.

##### Some of the available voice commands:

- `search for <query` - search the web for anything, see the intelligently chosen top result
- `question <query>` - ask a question to an intelligence assistant
- `run visual search` - use a POV image to search the web for anything that you see around you
- `save speech <note>` - save any voice note to your cache of notes. This can be used to save ideas, thoughts, notes, reminders, etc.
- `save speech tag <tagname> <note>` - save any voice note to your cache of notes and to a specific tag bin named <tag>
- `run speech translate <language>` - live translate the given language into english
- `run live life captions` - display live closed captions
- `run blank screen` - blank the screen

## Abbreviations

ASP - Android Smart Phone  
ASG - Android Smart Glasses  
GLBOX - GNU/Linux 'Single Board Computer'/Laptop  

## Install / Use

### First Time Setup

1. On your Android smart phone, download the "Wearable Intelligence System" app:
    - (RECCOMENDED) Play Store: Coming soon
    - [Github latest release](https://github.com/emexlabs/WearableIntelligenceSystem/releases)
2. On your smart glasses, download the "Wearable Intelligence System" app:
    - (RECCOMENDED) Vuzix Store: Coming soon
    - [Github latest release](https://github.com/emexlabs/WearableIntelligenceSystem/releases)
3. 
    * Launch the "Wearable Intelligence System" app on your smart phone
    * Accept permissions.
    * Tap "Start Wifi Hotspot", turn on (configure password if necessary) your wifi hotspot, then go "Back" to return
4. Connect smart glasses WiFi to the smart phone WiFi hotspot
5. Enable mobile data (or wifi sharing) on Android smart phone
6. Start "Wearable Intelligence System" application on smart glasses
7. 
    * The phone connection icon will be green if the glasses are connected to your phone. If you speak, you'll see a live transcript on the smart glasses screen.
    * On the Android smart phone, got to "Memory Tools" -> "Memory Stream" and you will see live transcripts
8. Setup complete.

### Normal Use
    
Here's how to launch the system after you've already done the initial setup above:  

1. Launch "WIS" app on smart phone
2. Enable mobile hotspot on smart phone with the "Start WiFi Hotspot" button
3. Connect Android smart glasses to Android smart phone WiFi hotspot.
4. Launch "WIS" app on smart glasses.
5. Verify system is running by the "Smart Glasses Conection Indicator" icon turning white on the smart glasses HUD.
    
## Documentation / Developers
    
The docs are hosted on this repo's Wiki, [here are the docs](https://github.com/emexlabs/WearableIntelligenceSystem/wiki).
   
## Authors

The system is fully Open Source and built by this growing list of contributors:

- Cayden Pierce - [Emex Labs](https://emexwearables.com)
- Aaditya Vaze - https://thisisvaze.com/
- Jeremy Stairs - https://github.com/stairs1
- Add Your Name Here!

We are actively building a community that is building cognitive augmentation technologies together. 

The Wearable Intelligence System was started at [Emex Labs](https://emexwearables.com) by [Cayden Pierce](https://caydenpierce.com/).


