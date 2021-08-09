import socket
import time
import struct

class ASGSocket:
    def __init__(self, PORT=8989):
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

        #save command ids (CID)
        self.final_transcript_cid = bytearray([12, 2]) 
        self.intermediate_transcript_cid = bytearray([12, 1]) 
        self.switch_mode_cid = bytearray([12, 3]) 
        self.command_output_cid = bytearray([12, 4]) 
        self.mode_ids = {
                            "social" : bytearray([15, 0]),
                            "llc" : bytearray([15, 1]), #live life captions
                            "blank" : bytearray([15, 3]) #blank display
                        }


    def start_conn(self):
        print("Attempting to start connection...")
        self.s.listen()
        print("Socket Listening")
        self.conn, self.addr = self.s.accept()
        print("Connected")
        return self.conn, self.addr

    def send_final_transcript(self, message_str):
        print("SENDING FINAL")
        self.send_bytes(self.final_transcript_cid, bytes(message_str + "\n",'UTF-8'))

    def send_intermediate_transcript(self, message_str):
        self.send_bytes(self.intermediate_transcript_cid, bytes(message_str + "\n",'UTF-8'))

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
        self.conn.send(payload_packet)

if __name__ == "__main__":
    asg_socket = ASGSocket()
    asg_socket.start_conn()

    i = 0
    while True:
        try:
            print("Sending utf-8 string ping...")
            asg_socket.send_string("Hello man {}".format(i))
            print("--Ping sent")
        except BrokenPipeError as e:
            print("Connection broken, listening again")
            asg_socket.start_conn()
            i = 0
        i += 1
        time.sleep(1)

