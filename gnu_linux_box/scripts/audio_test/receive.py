#!/usr/bin/env python

import pyaudio
import socket
import sys
import time

FORMAT = pyaudio.paInt16
CHANNELS = 1
#RATE = 16000
RATE = 44100
# CHUNK = 1024 * 4
CHUNK = 1024
# CHUNK = 1000 * 2
# CHUNK = 1200

def current_milli_time():
    return round(time.time() * 1000)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((sys.argv[1], int(sys.argv[2])))
#audio = pyaudio.PyAudio()
#stream = audio.open(format=FORMAT, channels=CHANNELS, rate=RATE, output=True, frames_per_buffer=CHUNK)

try:
    while True:
        data = s.recv(CHUNK, socket.MSG_WAITALL)
        cid = data[0]
        print("cid : {}, received at: {}".format(cid, current_milli_time()))
        print(len(data))
        #stream.write(data)
except KeyboardInterrupt:
    pass

print('Shutting down')
s.close()
#stream.close()
#audio.terminate()
