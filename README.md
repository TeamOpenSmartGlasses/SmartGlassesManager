# Live Life Captions

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
