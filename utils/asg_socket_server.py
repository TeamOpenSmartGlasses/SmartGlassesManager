import socket
import time

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

    def start_conn(self):
        self.s.listen()
        print("Socket Listening")
        self.conn, self.addr = self.s.accept()
        print("Connected")
        return self.conn, self.addr

    def send_string(self, message_str):
        self.conn.send(bytes(message_str + "\n",'UTF-8'))

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

