import socket
import time
import struct
import json
import threading

class ASGSocket:
    def __init__(self, PORT=8989):
        self.ADV_PORT = 8891 #port to send out advertising broadcast on
        PORT = 8989 #hard set port

        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        print('Socket created')

        # Ensure that you can restart your server quickly when it terminates
        self.s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        print("Binding socket")
        try:
            self.s.bind(('', PORT))
        except socket.error as err:
            print('Bind failed. Error Code : ' .format(err))

        self.heart_beat_time = 3 #number seconds to send a heart beat ping
        #self.heart_beat() #continues to repeat indefinitly

        self.heart_beat_id = bytearray([25, 32])

        #save command ids (CID)
        self.final_transcript_cid = bytearray([12, 2]) 
        self.intermediate_transcript_cid = bytearray([12, 1]) 
        self.switch_mode_cid = bytearray([12, 3]) 
        self.command_output_cid = bytearray([12, 4]) 
        self.wikipedia_result_cid = bytearray([12, 5]) 
        self.translation_cid = bytearray([12, 6]) 
        self.visual_search_images_result_cid = bytearray([12, 7]) 
        self.affective_summary_result_cid = bytearray([12, 8]) 
        self.mode_ids = {
                            "social" : bytearray([15, 0]),
                            "llc" : bytearray([15, 1]), #live life captions
                            "blank" : bytearray([15, 2]), #blank display
                            "translate" : bytearray([15, 3]), #mode to translate text
                            "visualsearchviewfind" : bytearray([15, 4])
                        }
        
        self.connected = 0
        self.conn = None

    def advertise_and_connect_glbox(self):
        print("advertiseglbox")
        #advertise we are a glbox so the ASG can find us
        adv_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)

        # Enable port reusage so we will be able to run multiple clients and servers on single (host, port).
        # Do not use socket.SO_REUSEADDR except you using linux(kernel<3.9): goto https://stackoverflow.com/questions/14388706/how-do-so-reuseaddr-and-so-reuseport-differ for more information.
        # For linux hosts all sockets that want to share the same address and port combination must belong to processes that share the same effective user ID!
        # So, on linux(kernel>=3.9) you have to run multiple servers and clients under one user to share the same (host, port).
        # Thanks to @stevenreddie
        adv_sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)

        # Enable broadcasting mode
        adv_sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)

        # Set a timeout so the socket does not block
        # indefinitely when trying to receive data.
        adv_sock.settimeout(0.2)
        self.s.settimeout(0.5)
        message = b"WearableAiCyborgGLBOX"
        #in this loop, we advertise who we are, then wait for a connection, and repeat until an ASG connects
        while True:
            print("send adv and try to connect to ASG")
            adv_sock.sendto(message, ('<broadcast>', self.ADV_PORT))
            try:
                self.conn, self.addr = self.s.accept()
                self.conn.settimeout(None)
                break
            except socket.timeout as e:
                pass
            time.sleep(0.5)

    def start_conn(self):
        if self.connected == 2:
            self.conn.close()
        elif self.connected == 1:
            return
        self.connected = 1
        print("Attempting to start connection...")
        self.s.listen()
        print("Socket Listening")
        #self.advertise_and_connect_glbox() # we no longer advertise on local network because we are running in the cloud
        while True:
            try:
                self.conn, self.addr = self.s.accept()
                break
            except socket.timeout as e:
                pass
        self.connected = 2
        self.heart_beat()
        print("DATA SOCKET CONNECTED TO ASG")
        return self.conn, self.addr

    def heart_beat(self):
        """
        Runs on repeat, send a heart beat ping to the ASG if we are connected. If ping fails, restart server so ASG can reinitiate connection.
        """
        #if connected, try to send heart beat, reconnect if not connected
        if self.connected == 2:
            try:
                self.send_heart_beat()
            except (OSError, ConnectionResetError, BrokenPipeError) as e:
                self.restart_connection() #this will start a new heart beat stream if connect is successful, so return
                return
        else: #if not connected, that means no point in checking connection
            return

        #start a new heart beat that will run after this one
        heart_beat_thread = threading.Timer(self.heart_beat_time, self.heart_beat)
        heart_beat_thread.daemon = True
        heart_beat_thread.start()

    def restart_connection(self):
        print("RESTARTING ASG_SOCKET")
        if self.connected == 2:
            self.start_conn()

    def send_heart_beat(self):
        print("ASG HEART BEAT")
        self.send_bytes(self.heart_beat_id, bytes("ping"  + "\n",'UTF-8'))

    def send_final_transcript_object(self, t_obj):
        encoded_message = json.dumps(t_obj).encode('utf-8')
        #s.sendall(b)
        self.send_bytes(self.final_transcript_cid, encoded_message)

    def send_translated_text(self, text):
        self.send_bytes(self.translation_cid, bytes(text + "\n",'UTF-8'))

    def send_intermediate_transcript(self, message_str):
        self.send_bytes(self.intermediate_transcript_cid, bytes(message_str + "\n",'UTF-8'))

    def send_wikipedia_result(self, result):
        result_encoded = json.dumps(result).encode('utf-8')
        self.send_bytes(self.wikipedia_result_cid, result_encoded)

    def send_visual_search_images_result(self, result):
        result_encoded = json.dumps(result).encode('utf-8')
        self.send_bytes(self.visual_search_images_result_cid, result_encoded)

    def send_affective_search_result(self, result):
        self.send_bytes(self.affective_summary_result_cid, bytes(result,'UTF-8'))

    def send_command_output(self, message_str):
        self.send_bytes(self.command_output_cid, bytes(message_str + "\n",'UTF-8'))

    def send_switch_mode(self, mode):
        """
        mode (str): Name of the mode user wants to enter
        """
        mode_id = self.mode_ids[mode]
        self.send_bytes(self.switch_mode_cid, mode_id) #send the mode_id to ASG and the ASG changes to that mode

    def send_intermediate_transcript(self, message_str):
        self.send_bytes(self.intermediate_transcript_cid, bytes(message_str + "\n",'UTF-8'))

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
        goodbye = bytearray([3, 2, 1]);
        #combine those into a payload
        payload_packet = hello + message_len + msg_id + body + goodbye
        prev_timeout = self.conn.gettimeout()
        self.conn.settimeout(None)
        self.conn.send(payload_packet)
        self.conn.settimeout(prev_timeout)

if __name__ == "__main__":
    asg_socket = ASGSocket()
    asg_socket.start_conn()
    print("conn done been started son")

    i = 0
    while True:
        try:
            print("Sending utf-8 string ping...")
            asg_socket.send_string("Hello man {}".format(i))
            print("--Ping sent")

            #listen for data
            s.settimeout(1.0)
            fragments = list()
            print("getting frags")
            while True:
                print("Loop in asg")
                chunk = s.recv(1024)
                if not chunk:
                    print("parse cuz no chunkz:")
                    break
                fragments.append(chunk)
            message = "".join(fragments)
            print("message received")
            print(message)
        except (ConnectionResetError, BrokenPipeError) as e:
            print("Connection broken, listening again")
            asg_socket.start_conn()
            i = 0
        i += 1

