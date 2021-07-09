## TODO

- clean code so it's modular
- provide interface that always pushes the latest live captions
- make server in python that will connect to the Android Vuzix WearableAI server
- System.out log the closed captions in vuzix blade
- make GUI in vuzix blade to display the captions
- get running on pocket worn SBC with bluetooth headset microphone and speaker


## Improve
-get good open source TTS running
    -coqui tts works great but can we get it running in a script real time on the GPU?
    -other faster option?
    
# SETUP

1. Clone this repo
2. Make a virtualenv
3. Install requirements (`pip3 install -r requirements.txt`)
4. Make file `./wolfram_api_key.txt` and paste your WolframOne App Id in there
5. Setup GCP to allow Speech to Text, make credentials for it, and place your JSON credentials at `./utils/creds.json`
6. Run `main.py`
