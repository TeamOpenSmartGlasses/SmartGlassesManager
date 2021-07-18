import socket
import time

PORT = 8989 #hard set port
HOST = 'localhost' #we have to listen to broadcast events to know the IP of android glasses host

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
print('Socket created')

# Ensure that you can restart your server quickly when it terminates
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

s.bind(('', PORT))
#try:
#    #s.bind((HOST, PORT))
#    s.bind(('', PORT))
#except socket.error as err:
#    print('Bind failed. Error Code : ' .format(err))


s.listen()
print("Socket Listening")
conn, addr = s.accept()
print("Connected")

i = 0
while True:
    try:
        print("Sending utf-8 string ping")
        conn.send(bytes("Hello man {}".format(i)+"\r\n *",'UTF-8'))
        time.sleep(1)
#        print("Message sent")
#        data = conn.recv(1024)
#        print(data.decode(encoding='UTF-8'))
    except BrokenPipeError as e:
        print("Connection broken, listening again")
        s.listen()
        print("Socket Listening")
        conn, addr = s.accept()
        print("Connected")
        i = 0
    i += 1

