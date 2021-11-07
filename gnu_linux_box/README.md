This is the main/only (Python3) program the runs on the GLBOX.

# SETUP


1. Clone this repo
2. Make a virtualenv
3. `sudo apt install portaudio19-dev python3-pyaudio #for ubuntu 20/21`
4. Install requirements (`pip3 install -r requirements.txt`)
5. Install spacy NLP artifact: `python3 -m spacy download en_core_web_trf`
6. Make file `./utils/wolfram_api_key.txt` and paste your WolframOne App Id in there
7. Setup GCP to allow Speech to Text, make credentials for it, and place your JSON credentials at `./utils/creds.json`
8. Setup Microsoft Azure to allow Bing search API, get the key and paste just the key string in `./utils/azure_key.txt`
9. CHANGE THE KEY IN key.txt TO YOUR PRIVATE AES KEY
10. Run `main.py #dont do this without changing AES key in key.py`


If you follow the above steps above, follow the steps in the main README (connect GLBOX and ASG to ASP hotspot), and run Android apps on the ASG and ASP, you will be running the WIS.

## Software Engineering

- make the main.py just start up our services, shared queueus, and run a loop looking for new thread to start
    - should move `run_voice_command` into its own transcript/voice command processing class
    - should move `run_server` and associated ASG code into an ASG class - abstract away socket stuff from main
    - 

- need some kind of way to synchronize state between GLBOX and ASG. Some commands should only work in a specific mode/state, and we don't record the mode in the GLBOX right now - maybe GLBOX should be in charge of the mode and the ASG will ask for current mode on start

This has been thought through and attempted to make modular and customizable. However some things have become a bit too dependent (not modular enough.

The overall layout with the main thread simply starting up queues and threads is good. The communication scheme is ok, and could be improved. The modularity is mostly there, but moving functions into their own classes to further classify/modularize will help just to simplify the layout. The asg_socket_server should become just the ASG object, and that itself should import an asg_socket_server which doesn't understand what it's sending, just handles connetion, reconnetion, sending raw data.

None of these things are life and death and at some point it will be worthwhile to return to fix these things. For now, keep building on what we have, which is completely acceptable, and what this turns into will help us better understand the "perfect" way to structure in a few months time.
