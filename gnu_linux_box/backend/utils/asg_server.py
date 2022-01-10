import socket
from queue import Queue, Empty
import spacy
import struct 

from utils.asg_socket_server import ASGSocket
from utils.english_pronouns_list import english_pronouns

# NLP - Load English tokenizer, tagger, parser, NER and word vectors
nlp = spacy.load("en_core_web_trf")

def check_message(message, cmd_q):
    img_id = [0x01, 0x10] #id for images
    #make sure header is verified
    if (message[0] != 0x01 or message[1] != 0x02 or message[2] != 0x03):
        print("Hello is no good")
        return False
    else:
        print("hello is good to go")

    body_len, = struct.unpack_from(">i", message, offset=3)
    print("Body length is: {}".format(body_len))

    if (message[-3] != 0x03 or message[-2] != 0x02 or message[-1] != 0x01):
        print("Goodbye is no good")
        return False
    else:
        print("goodbye is good to go")

    message_body = message[9:-3]

    #now process the data that was sent to us
    if ((message[7] == img_id[0]) and (message[8] == img_id[1])): #this means the message is an image
        print("The message is an image")
        process_image(message_body, cmd_q)
    else:
        print("The message is NOT an image")

def get_nlp(string):
    doc = nlp(string)

    # Analyze syntax
    nouns = [chunk for chunk in doc.noun_chunks if chunk.text not in english_pronouns] #don't include pronouns
    #print("Verbs:", [token.lemma_ for token in doc if token.pos_ == "VERB"])

    # Find named entities, phrases and concepts
#    for entity in doc.ents:
#        print(entity.text, entity.label_)

    nouns_list = []
    for noun in nouns:
        new_noun = {"noun" : noun.text, "start" : noun.start_char, "end" : noun.end_char}
        nouns_list.append(new_noun)

    nlp_out = { "nouns" : nouns_list}

    return nlp_out

def run_asg_server(transcript_q, cmd_q, obj_q):

    def GUI_receive_final_transcript_object(transcript_object): #soon run_server will be a class, and this will be a method
            asg_socket.send_final_transcript_object(transcript_object)

    def GUI_receive_intermediate_transcript(transcript): #soon run_server will be a class, and this will be a method
        #pass
        asg_socket.send_intermediate_transcript(transcript)

    def GUI_receive_command_output(output): #soon run_server will be a class, and this will be a method
            asg_socket.send_command_output(output)

    #start a socket connection to the android smart glasses
    asg_socket = ASGSocket()
    asg_socket.start_conn()
    while True: #main server loop
        try:
            try:
                cmd, args = cmd_q.get(timeout=0.001)
                #a switch block here for different commands and their associated function
                if cmd == "switch mode":
                    asg_socket.send_switch_mode(args)
                    print("SENT SWITCH MODE")
                elif cmd == "wikipedia search":
                    print("sending wikipedia search results")
                    asg_socket.send_wikipedia_result(args)
                elif cmd == "visual search":
                    print("sending visual search image response")
                    asg_socket.send_visual_search_images_result(args)
            except Empty as e:
                pass
            #check for command responses
            try:
                obj = obj_q.get(timeout=0.001)
                if obj["type"] == "transcript":
                    transcript = obj["transcript"]
                    if obj["is_final"] == True:
                        #run nlp on the final transcript
                        nlp_out = get_nlp(transcript)
                        obj["nlp"] = nlp_out
                        GUI_receive_final_transcript_object(obj)
                    else:
                        GUI_receive_intermediate_transcript(transcript)
                elif obj["type"] == "cmd_response":
                    command_output = obj["data"]
                    print("CMD RESPONSE RECEIVEEEDDDD************************************** {}".format(command_output))
                    GUI_receive_command_output(command_output)
                    print("SENT COMMAND OUTPUT ")
                elif obj["type"] == "cmd_success":
                    if obj["data"]:
                        GUI_receive_command_output("COMMAND SUCCESS")
                    else:
                        GUI_receive_command_output("COMMAND FAILED")
                elif obj["type"] == "translate_result":
                    print("sending translation results")
                    asg_socket.send_translated_text(obj["data"])
            except Empty as e:
                pass

            #listen for data
            asg_socket.conn.settimeout(0.001)
            fragments = list()
            while True:
                try:
                    chunk = asg_socket.conn.recv(1024)
                    if not chunk:
                        break
                    asg_socket.conn.settimeout(0.05) #increase it here after receiving part of a stream so we don't seperate messages
                except socket.timeout as e:
                    break
                fragments.append(chunk)
            message = b"".join(fragments)
            if message != b'':
                print("message received")
                check_message(message, cmd_q)

        except (ConnectionResetError, BrokenPipeError) as e: #if error on socket at any point, restart connection and return to loop
            print("Connection broken, listening again")
            asg_socket.start_conn()
