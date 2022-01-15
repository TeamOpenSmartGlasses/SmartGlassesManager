import socket
import struct
import threading
import time
import queue
from utils.encryption.AESpy import AESpy
from .keys.key import aes_key

STREAMING_LIMIT = 300000 #YOU HAVE TO CHANGE THIS IN main.py AS WELL NO TIME TO SETUP SHARED CONFIG FILES (YAML) #also, no more than 300 seconds or GCP will error - cayden

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))


class ASGAudioServer:
    def __init__(self, audio_stream_observable, port=4449, chunk=4096):
        self.chunk = chunk
        self.closed = True
        self.port = port
        self.heart_beat_time = 3 #number seconds to send a heart beat ping
        self.heart_beat_id = bytearray([25, 32])

        #the observable we use to push data to everyone who wants a live stream of audio
        self.audio_stream_observable = audio_stream_observable

        #queue to hold incoming data
        self._buff = queue.Queue()

        # Part of the bridging system
        self.audio_input = []
        self.last_audio_input = []
        self.new_stream = True
        self.bridging_offset = 0
        self.result_end_time = 0
        self.is_final_end_time = 0
        self.last_transcript_was_final = False
        self.restart_counter = 0

        #encryption
        self.aes = AESpy()

        self.socket = None
        self.connected = 0

    def connect(self):
        if self.connected == 2:
            self.socket.close()
        self.connected = 1
        self.closed = True
        self.start_time = get_current_time()
#        if self.socket is not None:
#            self.socket.shutdown(socket.SHUT_RDWR)
#            socket.close()
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind(('', self.port))
        self.socket.listen(5)
        (self.clientsocket, self.address) = self.socket.accept()
        self.connected = 2
        print("AUDIO SOCKET CONNECTED TO ASG")
        self.closed = False
        self.heart_beat() #continues to repeat indefinitly

    def close(self, *args):
        print('Shutting down audio socket')
        self.closed = True
        self.socket.close()

    def fill_buffer(self, in_data):
        """Continuously collect data from the audio stream, into the buffer."""
        pass
        #self._buff.put(in_data)

    def data_receive(self):
        chunk = None
        if not self.closed:
            try:
                encrypted_chunk = self.clientsocket.recv(self.chunk, socket.MSG_WAITALL)
                #decrypt chunk
                chunk = self.aes.decrypt(encrypted_chunk, aes_key)
            except (OSError, ConnectionResetError, BrokenPipeError, BlockingIOError) as e:
                print('ASG Audio Server disconnected, retrying.')
                self.restart_connection()
            except ValueError as e:
                print("Problem with audio data received, reconnecting")
                self.restart_connection()

            #if the received data is none, exit the generator
            if chunk is None:
                print('ASG Audio Server received None, exiting.')
                return

            #send out data fill out local data objects
            self.audio_stream_observable.on_next(chunk)
            self.fill_buffer(chunk)

    def heart_beat(self):
        """
        Runs on repeat, send a heart beat ping to the ASG if we are connected. If ping fails, restart server so ASG can reinitiate connection.
        """
        #if the socket is closed, stop sending heart beats
        if self.closed:
            return

        #try to send heart beat, reconnect if not connected
        try:
            self.send_heart_beat()
        except (ConnectionResetError, BrokenPipeError) as e:
            self.restart_connection() #this will start a new heart beat stream if connect is successful, so return
            return

        #start a new heart beat that will run after this one
        heart_beat_thread = threading.Timer(self.heart_beat_time, self.heart_beat)
        heart_beat_thread.daemon = True
        heart_beat_thread.start()

    def restart_connection(self):
        if self.connected == 2:
            self.connect()

    def send_heart_beat(self):
        self.send_bytes(self.heart_beat_id, bytes("ping"  + "\n",'UTF-8'))

    def send_bytes(self, cid, data):
        """
        cid - byte array
        data - byte array
        """
        #first, send hello
        hello = bytearray([1, 2, 3])
        #then send length of body
        message_len = None
        if data:
             #message_len = my_int_to_bb_be(len(data))
             message_len = struct.pack('>I', len(data))
        else:
            message_len = struct.pack('>I', 0)
        #then send id of message type
        msg_id = cid
        #then send data
        body = data
        #then send end tag - eventually make this unique to the image
        goodbye = bytearray([3, 2, 1])
        #combine those into a payload
        payload_packet = hello + message_len + msg_id + body + goodbye
        self.clientsocket.send(payload_packet)

def run_audio_server(audio_stream_observable):
    print("Starting ASGAudioServer")
    audio_stream = ASGAudioServer(audio_stream_observable)
    audio_stream.connect()

    #get the thread
    t = threading.currentThread()
    t.do_run = True

    while True:
        #check if our thread kill switch has been activated
        if not getattr(t, "do_run", True):
            return
        audio_stream.data_receive()
    audio_stream.close()

if __name__ == "__main__":
    with ASGAudioServer() as stream:
        audio_generator = stream.generator()
        for aud in audio_generator:
            print(aud)
