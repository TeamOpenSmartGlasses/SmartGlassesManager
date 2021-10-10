This is the main/only (Python3) program the runs on the GLBOX.

# SETUP

1. Clone this repo
2. Make a virtualenv
3. `sudo apt install portaudio19-dev python3-pyaudio #for ubuntu 20/21`
4. Install requirements (`pip3 install -r requirements.txt`)
5. Make file `./wolfram_api_key.txt` and paste your WolframOne App Id in there
6. Setup GCP to allow Speech to Text, make credentials for it, and place your JSON credentials at `./utils/creds.json`
7. Run `main.py`

If you follow the above steps above, follow the steps in the main README (connect GLBOX and ASG to ASP hotspot), and run Android apps on the ASG and ASP, you will be running the WIS.
