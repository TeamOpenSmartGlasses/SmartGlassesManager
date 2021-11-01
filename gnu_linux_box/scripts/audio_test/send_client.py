
import pyaudio
import socket
import sys
import time

FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 16000
CHUNK = 1024

     
audio = pyaudio.PyAudio()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((sys.argv[1], int(sys.argv[2])))

def callback(in_data, frame_count, time_info, status):
    s.send(in_data)
    return (None, pyaudio.paContinue)

stream = audio.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK, stream_callback=callback)

try:
    while True:
        print('just chillin, sending audio till you press crtl-c')
        data = s.recv(CHUNK)
        print(f'oowee we got somethin: {data}')
except KeyboardInterrupt:
    pass

print('Shutting down')
s.close()
audio.terminate()
