#!/usr/bin/env python

import pyaudio
import socket
import select
import time

FORMAT = pyaudio.paInt16
CHANNELS = 1
#RATE = 16000
RATE = 44100
CHUNK = 1024 * 2

audio = pyaudio.PyAudio()

serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
serversocket.bind(('', 4449))
serversocket.listen(5)

def current_milli_time():
    return round(time.time() * 1000)

counter = 0
def callback(in_data, frame_count, time_info, status):
    global counter
    counter = (counter + 1) % 256
    print("cid: {}, time: {}".format(counter, current_milli_time()))
    in_data = bytes([counter]) + in_data[:-1]
    for s in read_list[1:]:
        s.send(in_data)
    return (None, pyaudio.paContinue)

# start Recording
stream = audio.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK, stream_callback=callback)
# stream.start_stream()

read_list = [serversocket]
print("recording...")

try:
    while True:
        readable, writable, errored = select.select(read_list, [], [])
        for s in readable:
            if s is serversocket:
                (clientsocket, address) = serversocket.accept()
                read_list.append(clientsocket)
                print("Connection from {}".format(address))
            else:
                print("Received at {}".format(current_milli_time()))
                data = s.recv(1024)
                if not data:
                    read_list.remove(s)
except KeyboardInterrupt:
    pass


print("finished recording")

serversocket.close()
# stop Recording
stream.stop_stream()
stream.close()
audio.terminate()
