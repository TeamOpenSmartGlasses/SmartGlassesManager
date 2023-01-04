This is the main/only (Python3) program the runs on the GLBOX.

# SETUP for socket system, webserver setup coming soon


1. Clone this repo
2. cd to ./backend
3. Make a virtualenv
4. `sudo apt install portaudio19-dev python3-pyaudio #for ubuntu 20/21`
5. Install requirements (`pip3 install -r requirements.txt`)
6. Install spacy NLP artifact: `python3 -m spacy download en_core_web_trf`
7. Make file `./utils/wolfram_api_key.txt` and paste your WolframOne App Id in there
8. Setup GCP to allow Speech to Text, make credentials for it, and place your JSON credentials at `./utils/creds.json`
9. Setup Microsoft Azure to allow Bing search API, get the key and paste just the key string in `./utils/azure_key.txt`
10. CHANGE THE KEY IN key.txt TO YOUR PRIVATE AES KEY
11. Run `main.py #dont do this without changing AES key in key.py`


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

## Testing Backend

#### Visual Search
Change url and image location as required
```
 (echo -n '{"image": "'; base64 ~/Pictures/grp.jpg; echo '"}') |
curl -H "Content-Type: application/json" -d @-  http://localhost:5000/visual_search_search
```

#### Text based queries
Change url and query text as required
```
curl -X POST -F "query=who's the president of the us" http://127.0.0.1:5000/natural_language_query -vvv
```

## Deploy your own server

1. Setup a cloud connected Linux box (tested on Ubuntu 18 LTS AWS EC2)
2. Install nginx
3. Clone the repo at /var/www/html
4. Add the two .conf files at `gnu_linux_box/backend/deploy` to `/etc/nginx/site-available` and activate them with:
```
sudo rm /etc/nginx/sites-enabled/default
sudo ln /etc/nginx/sites-available/wis_backend.conf /etc/nginx/sites-enabled/
sudo ln /etc/nginx/sites-available/wis_ssl.conf /etc/nginx/sites-enabled/
sudo systemctl restart nginx
```
5. Setup the backend to run by setting up virtualenv and installing requirements (discussed above)
6. Copy the service service file to `/etc/systemd/system`
7. Enable the service file with `sudo systemctl start wis_gunicorn && sudo systemctl enable wis_gunicorn`
8. Use certbot to setup SSL for your domain, and point the /etc/nginx/site-enabled/*conf files to point to that SSL config file
9. Restart Nginx `sudo systemctl restart nginx`
