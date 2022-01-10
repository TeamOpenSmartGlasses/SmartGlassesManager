import struct 
import requests
import urllib
from playsound import playsound
import subprocess
from fuzzysearch import find_near_matches
import spacy
import wikipedia
import base64
from queue import Queue, Empty
import socket
import time
import json

from utils.asg_socket_server import ASGSocket
from language_options import language_options
from utils.bing_visual_search import bing_visual_search, bing_visual_search_file
from utils.affective_summarizer import affective_summarize

from utils.english_pronouns_list import english_pronouns

# NLP - Load English tokenizer, tagger, parser, NER and word vectors
nlp = spacy.load("en_core_web_trf")

#config files
wake_words_file = "./wakewords.txt"
voice_memories_file = "./data/voice_memories.csv"
wolfram_api_key_file = "./utils/wolfram_api_key.txt"

#pre-generated text to speech sound files
command_success_sound = "./speech_pre_rendered/command_success.wav"
generic_failure_sound = "./speech_pre_rendered/command_failed.wav"
wolfram_failure_sound = "./speech_pre_rendered/wolfram_query_failed.wav"

#define phrases/word that will wake the system to search the current speech for voice commands
wake_words = []
with open(wake_words_file) as f:
    #wake_words = [word for line in f for word in line.split()]
    wake_words = [line.strip() for line in f]
print("Active wake words: {}".format(wake_words))

#define voice commands functions
def add_wake_word(transcript, args, cmd_q, thread_q):
    try: 
        wake_word = args
        wake_words.append(args) #add it to our locally loaded object
        with open(wake_words_file, "a") as f: #add it to the wake words file for next load too
            # Append new wake word at the end of file
            f.write(args + "\n")
        return 1
    except Exception as e:
        print(e)
        return False

def visual_search(transcript, args, cmd_q, thread_q):
    #sneaky - use switch mode because the GUI updates the display... even though it's not really a new mode - or is it?
    cmd_q.put(("switch mode", "visualsearchviewfind"))
    return True

def wikipedia_search(transcript, args, cmd_q, thread_q):
    try:
        print("WIKIPEDIA SEARCHING NOW...")
        #make wikipedia search request (will fail if ambiguous)
        wiki_res = wikipedia.page(args, auto_suggest=False)
        #get first paragraph of summary
        summary_start = wiki_res.summary.split("\n")[0]
        #get url or image, download image
        img_url = wiki_res.images[0]
        headers = {'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11',
           'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
           'Accept-Charset': 'ISO-8859-1,utf-8;q=0.7,*;q=0.3',
           'Accept-Encoding': 'none',
           'Accept-Language': 'en-US,en;q=0.8',
           'Connection': 'keep-alive'}
        img_data = requests.get(img_url, headers=headers).content
        with open('image_name.jpg', 'wb') as handler:
            handler.write(img_data)
        img_string = base64.b64encode(img_data).decode('utf-8')
        #img_string = img_data.decode('utf-8') #turn image into utf-8 string so it can be sent over the socket
        #pack results into dict/json
        result = {
                "title" : wiki_res.title,
                "summary" : summary_start,
                "image" : img_string
                }
        print(wiki_res.title)
        print(summary_start)
        print(type(img_data))
        cmd_q.put(("wikipedia search", result))
        return 1
    except Exception as e:
        print(e)
        return False

def save_memory(transcript, args, cmd_q, thread_q):
    try: 
        ctime = time.time()
        memory = args
        with open(voice_memories_file, "a") as f:
            # Append new wake word at the end of file
            f.write(str(ctime) + ",\"" + transcript + "\"\n")
        return 1
    except Exception as e:
        print(e)
        return False

def switch_mode(transcript, args, cmd_q, thread_q):
    print("RUNNING SWITCH MODE")
    modes = {"social" : ["social", "social mode"],
            "llc" : ["LLC", "live life captions"],
            "blank" : ["blank mode", "blank screen", "blank", "Blanc"],
            "translate" : ["translate mode", "translate", "translated"],
            }

    transcript_l = transcript.lower()
    for k in modes:
        for voice_command in modes[k]:
            matches = find_near_matches(voice_command, transcript_l, max_l_dist=1)
            print(matches)
            if len(matches) > 0:
                args = transcript_l[matches[0].end+1:]
                print("USER COMMAND: SWITCH TO MODE: {}".format(k))
                if (k == "translate"):
                    thread_q.put({"type" : "translate", "cmd" : "start", "language" : args})
                else: #if we are changing to any mode other than translate, then stop translation services
                    thread_q.put({"type" : "translate", "cmd" : "kill"})
                cmd_q.put(("switch mode", k))
                break
    return 1

def ask_wolfram(transcript, args, cmd_q, thread_q):
    print("ASKING WOLFRAM: {}".format(args))
    result, convo_id = wolfram_conversational_query(args)
    print("WOLFRAM RESPONSE:")
    print(result)
    if result is None:
        return 0
    return result

def wolfram_failed():
    pass
    #playsound(wolfram_failure_sound) #playsound(command_success_sound) #disable as we are now a headless cloud server

#Wolfram API key - this loads from a plain text file containing only one string - your APP id key
wolframApiKey = None
with open(wolfram_api_key_file) as f:
    wolframApiKey = [line.strip() for line in f][0]

#API that returns short conversational responses - can be extended in future to be conversational using returned "conversationID" key
def wolfram_conversational_query(query):
    #encode query
    query_enc = urllib.parse.quote_plus(query)

    #build request
    getString = "https://api.wolframalpha.com/v1/conversation.jsp?appid={}&i={}".format(wolframApiKey, query_enc)
    response = requests.get(getString)

    if response.status_code == 200:
        parsed_res = response.json()
        if "error" in parsed_res:
            return None, None
        return parsed_res["result"], parsed_res["conversationID"]
    else:
        return None, None

#define the possible voice commands (only found if the wake word is detected)
voice_commands = {
        "exit" : {"function" : None, "voice_sounds" : ["exit loop", "quit loop"]}, #end the program loop running (voice rec continues to run)
        "start" : {"function" : None, "voice_sounds" : ["start loop", "begin loop"]}, #start the program loop running
        "stop listening" : {"function" : None, "voice_sounds" : ["stop listening", "go deaf"]}, #end the program loop running (voice rec continues to run)
        "shell" : {"function" : None, "voice_sounds" : ["CLI", "computer shell", "zee shell", "z shell"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "add wake word" : {"function" : add_wake_word, "voice_sounds" : ["add wake word", "new wake word"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "save memory" : {"function" : save_memory, "voice_sounds" : ["save memory", "save speech", "mxt cache", "mxt remember", "remember speech"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "ask wolfram" : {"function" : ask_wolfram, "fail_function" : None, "voice_sounds" : ["Wolfram Alpha", "ask Wolfram"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        #"go to"  : {"function" : None, "voice_sounds" : ["select", "choose", "go to"]}, #start a new program mode (enter different mental upgrade loop, or start a 'suite' of mental upgrades, i.e. "go to social mode"
        "switch mode"  : {"function" : switch_mode, "voice_sounds" : ["switch mode"]}, #start a new program mode (enter different mental upgrade loop, or start a 'suite' of mental upgrades, i.e. "go to social mode"
        "wikipedia"  : {"function" : wikipedia_search, "voice_sounds" : ["wikipedia"]}, #search wikipedia for a query
        "visual search"  : {"function" : visual_search, "voice_sounds" : ["what am I looking at", "visual search"]}, #search using the current POV image
        }

def find_commands(transcript, cmd_q, obj_q, thread_q):
    """
    Search through a transcript for wake words and predefined voice commands - strict mode commands first (not natural language)

    """
    #stop listening while we parse command and TTS (say) the result
    #stream.deaf = True

    # closest wake word detection
    wake_words_found = [] #list of fuzzysearch Match objects
    transcript_l = transcript.lower()
    for option in wake_words:
        try:
            wake_words_found.extend(find_near_matches(option, transcript_l, max_l_dist=1))
        except ValueError as e:
            pass

    #if we found a wake word, tell the user
    wake_word = None
    command_match = None
    command_name = None
    command_args = None
    possible_command = None
    if len(wake_words_found) > 0:
        wake_word = transcript_l[wake_words_found[-1].start:wake_words_found[-1].end]
        print("DETECTED WAKE WORD: {}".format(wake_word))

        #first, parse the wake word to see if it overlaps with a command
        looping = True
        found_command = False
        for comm_key in voice_commands.keys():
            for voice_sound in voice_commands[comm_key]["voice_sounds"]:
                matched = find_near_matches(voice_sound, wake_word, max_l_dist=1)
                if len(matched) > 0:
                    command_match = matched[-1]
                    command_name = comm_key
                    found_command = True
                    command_args = transcript_l[wake_words_found[-1].end:]
                    looping = False
                    break
            if not looping:
                break

        #if wake word was found but wasn't a command, try to parse the text after the wake word for a command    
        if not found_command:
            possible_command = transcript_l[wake_words_found[-1].end+1:] #+1 removes following space

            #run through possible commands
            #stop at first match - that must be our command
            looping = True
            for comm_key in voice_commands.keys():
                for voice_sound in voice_commands[comm_key]["voice_sounds"]:
                    matched = find_near_matches(voice_sound, possible_command, max_l_dist=1)
                    if len(matched) > 0:
                        command_match = matched[-1]
                        command_name = comm_key
                        looping = False
                        found_command = True
                        command_args = possible_command[command_match.end + 1:] #+1 removes following space
                        break
                if not looping:
                    break

        #if we found a wake word but no hard coded command was found, pass the query to wolfram
        if command_match is None:
            command_name = "ask wolfram"
            command_args = transcript_l[wake_words_found[-1].end:]
            
        print("RECEIVED COMMAND: {}".format(command_name))

        #run commands funcs
        voice_command_func = voice_commands[command_name]["function"]

        if voice_command_func is not None:
            #run the voice command
            res = voice_command_func(transcript, command_args, cmd_q, thread_q)
            if (type(res) == bool and res) or (type(res) == int and res == 1):
                print("COMMAND COMPLETED SUCCESSFULLY")
                obj_q.put({"type" : "cmd_success", "data" : True})
                #playsound(command_success_sound) #disable as we are now a headless cloud server
            elif type(res) == str:
                print("COMMAND COMPLETED SUCCESSFULLY")
                print("COMMAND OUTPUT SAVING TO QUEUE")
                obj_q.put({"type" : "cmd_response", "data" : res})
                #print("NOW SAYING: {}".format(res))
                #subprocess.call(['say',res])
            else:
                if "fail_function" in voice_commands[command_name] and voice_commands[command_name]["fail_function"] is not None:
                    voice_commands[command_name]["fail_function"]()
                else:
                    #playsound(generic_failure_sound) #playsound(command_success_sound) #disable as we are now a headless cloud server
                    obj_q.put({"type" : "cmd_success", "data" : False})
                    print("COMMAND FAILED")

    #start listening again after we have parsed command, run command, and given user response with TTS
    #stream.deaf = False

def run_voice_command(transcript_q, cmd_q, obj_q, thread_q):
    while True:
        transcript_obj = transcript_q.get()
        transcript = transcript_obj["transcript"]
        #send it to the ASG
        obj_q.put({"type" : "transcript", "is_final" : transcript_obj["is_final"], "transcript" : transcript})
        if transcript_obj["is_final"]:
            #analyze latest transcription for voice commands
            find_commands(transcript, cmd_q, obj_q, thread_q)

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

def check_message(message, cmd_q):
    img_id = [0x01, 0x10] #id for images
    affective_conversation_id = [0xD, 0x01] #bytearray([13, 1]) #{0xD, 0x01};

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
    elif ((message[7] == affective_conversation_id[0]) and (message[8] == affective_conversation_id[1])): #this means the message is an affective conversation
        process_affective_conversation(message_body, cmd_q)
    else:
        print("The message is NOT an image")

def process_affective_conversation(json_bytes, cmd_q):
    convo_obj = json.loads(json_bytes)
    summary = affective_summarize(convo_obj)
    cmd_q.put(("affective summary", summary))

def process_image(img_bytes, cmd_q):
    #for now, this is just for visual search mode, se will visually search for 
#    imagePath = '/home/cayden/Documents/to_rec/IMG_20211012_221002.jpg' #toilet paper
#    result = bing_visual_search_file(imagePath)
    print("ASKING BING")
    result = bing_visual_search(img_bytes)
    print("BING VISUAL SEARCH RESULT")
    print(type(result))

    print("size of result in bytes is: {}".format(len(result)))
    cmd_q.put(("visual search", result[0:25]))

def run_voice_command_server(transcript_q, cmd_q, obj_q):

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
        if asg_socket.connected != 2:
            print("Pass while disconnected")
            continue
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
                elif cmd == "affective summary":
                    print("sending affective summary response")
                    asg_socket.send_affective_search_result(args)
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
                    print("CMD RESPONSE RECEIVED")
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
                check_message(message, cmd_q)
        except (OSError, ConnectionResetError, BrokenPipeError) as e: #if error on socket at any point, restart connection and return to loop
            print("ASG Socket connection broken found in voice_command_server, listening again")
            asg_socket.restart_connection()
    print("EXITING VOICE COMMAND SERVER LOOP NOW")
