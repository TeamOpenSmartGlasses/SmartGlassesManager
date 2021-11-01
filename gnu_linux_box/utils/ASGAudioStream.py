import socket
import struct
import threading
import time


STREAMING_LIMIT = 300000 #YOU HAVE TO CHANGE THIS IN main.py AS WELL NO TIME TO SETUP SHARED CONFIG FILES (YAML) #also, no more than 300 seconds or GCP will error - cayden

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))


class ASGAudioStream:
    def __init__(self, port=4449, chunk=1024):
        self.chunk = chunk
        self.closed = True
        self.port = port
        self.heart_beat_time = 3 #number seconds to send a heart beat ping
        self.heart_beat_id = bytearray([25, 32])

        # Part of the bridging system
        self.audio_input = []
        self.last_audio_input = []
        self.new_stream = True
        self.bridging_offset = 0
        self.result_end_time = 0
        self.is_final_end_time = 0
        self.last_transcript_was_final = False
        self.restart_counter = 0

    def __enter__(self):

        self.start_time = get_current_time()
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind(('', self.port))
        self.socket.listen(5)
        (self.clientsocket, self.address) = self.socket.accept()
        self.closed = False
        self.heart_beat() #continues to repeat indefinitly
        return self

    def __exit__(self, *args):
        print('Shutting down audio socket')
        self.closed = True
        self.socket.close()

    def generator(self):
        while not self.closed:
            # non-bridging
            # yield self.clientsocket.recv(self.chunk, socket.MSG_WAITALL)

            #bridging below
            try:
                received = self.clientsocket.recv(self.chunk, socket.MSG_WAITALL)
            except (ConnectionResetError, BrokenPipeError) as e:
                print('lost audio connection, retrying')
                self.end_connection()

            data = []

            if self.new_stream and self.last_audio_input:

                chunk_time = STREAMING_LIMIT / len(self.last_audio_input)

                if chunk_time != 0:

                    if self.bridging_offset < 0:
                        self.bridging_offset = 0

                    if self.bridging_offset > self.final_request_end_time:
                        self.bridging_offset = self.final_request_end_time

                    chunks_from_ms = round(
                        (self.final_request_end_time - self.bridging_offset)
                        / chunk_time
                    )

                    self.bridging_offset = round(
                        (len(self.last_audio_input) - chunks_from_ms) * chunk_time
                    )

                    for i in range(chunks_from_ms, len(self.last_audio_input)):
                        data.append(self.last_audio_input[i])

                self.new_stream = False

            chunk = received 
            self.audio_input.append(chunk)

            if chunk is None:
                return
            data.append(chunk)

            # print(b"".join(data))
            yield b"".join(data)


    def heart_beat(self):
        """
        Runs on repeat, send a heart beat ping to the ASG if we are connected. If ping fails, restart server so ASG can reinitiate connection.
        """
        #start a new heart beat that will run after this one
        if self.closed:
            return

        heart_beat_thread = threading.Timer(self.heart_beat_time, self.heart_beat)
        heart_beat_thread.daemon = True
        heart_beat_thread.start()
        try:
            self.send_heart_beat()
        except (ConnectionResetError, BrokenPipeError) as e:
            print('lost audio connection, retrying')
            self.end_connection()

    def end_connection(self):
        self.__exit__()

    def send_heart_beat(self):
        print("SENDING HEART BEAT")
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
        self.clientsocket.settimeout(None)
        self.clientsocket.send(payload_packet)



if __name__ == "__main__":
    with ASGAudioStream() as stream:
        audio_generator = stream.generator()
        for aud in audio_generator:
            print(aud)