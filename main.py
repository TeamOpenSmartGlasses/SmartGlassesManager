#!/usr/bin/env python

"""
Streams in always on microphone, transcribes (speech (audio) to text), runs results through function to find user defined wake words and commands.

Main components:
    -STT - GCP 
    -server - TCP socket

Both of these are IO-bound, both of them need to run all the time. We need be asynchronous, for now that's with multithreading.

STT fills a queue of transcripts - both intermediate and final transcripts
server goes through that queue and processes/send to the wearable

@author: Cayden Pierce, Emex Labs
"""
import requests
import urllib
from playsound import playsound
import subprocess
from fuzzysearch import find_near_matches
import sys
import time

from queue import Queue
from threading import Thread

from utils.gcp_stt import run_google_stt
from utils.asg_socket_server import ASGSocket

#config files
wake_words_file = "./wakewords.txt"
voice_memories_file = "./data/voice_memories.csv"
wolfram_api_key_file = "./wolfram_api_key.txt"

#pre-generated text to speech sound files
command_success_sound = "./speech_pre_rendered/command_success.wav"
generic_failure_sound = "./speech_pre_rendered/command_failed.wav"
wolfram_failure_sound = "./speech_pre_rendered/wolfram_query_failed.wav"

# Audio recording parameters
STREAMING_LIMIT = 3 * 5 * 240000 # 1 hour
SAMPLE_RATE = 16000
CHUNK_SIZE = int(SAMPLE_RATE / 10)  # 100ms

#terminal printing colors
RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[0;33m"

#define phrases/word that will wake the system to search the current speech for voice commands
wake_words = []
with open(wake_words_file) as f:
    #wake_words = [word for line in f for word in line.split()]
    wake_words = [line.strip() for line in f]
print("Active wake words: {}".format(wake_words))

#define voice commands functions
def add_wake_word(transcript, args):
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

def save_memory(transcript, args):
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

def ask_wolfram(transcript, args):
    print("ASKING WOLFRAM: {}".format(args))
    result, convo_id = wolfram_conversational_query(args)
    print("WOLFRAM RESPONSE:")
    print(result)
    return result

def wolfram_failed():
    playsound(wolfram_failure_sound)

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
        print(parsed_res)
        return parsed_res["result"], parsed_res["conversationID"]
    else:
        return None, None

#define the possible voice commands (only found if the wake word is detected)
voice_commands = {
        "exit" : {"function" : None, "voice_sounds" : ["exit loop", "quit loop"]}, #end the program loop running (voice rec continues to run)
        "start" : {"function" : None, "voice_sounds" : ["start loop", "begin loop"]}, #start the program loop running
        "stop listening" : {"function" : None, "voice_sounds" : ["stop listening", "go deaf"]}, #end the program loop running (voice rec continues to run)
        "shell" : {"function" : None, "voice_sounds" : ["CLI", "shell", "bash", "zee shell", "zsh", "z shell"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "add wake word" : {"function" : add_wake_word, "voice_sounds" : ["add wake word", "new wake word"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "save memory" : {"function" : save_memory, "voice_sounds" : ["save memory", "save speech", "mxt cache", "mxt remember", "remember speech"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "ask wolfram" : {"function" : ask_wolfram, "fail_function" : wolfram_failed, "voice_sounds" : ["Wolfram", "Wolfram Alpha", "ask Wolfram"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "go to"  : {"function" : None, "voice_sounds" : ["select", "choose", "go to"]}, #start a new program mode (enter different mental upgrade loop, or start a 'suite' of mental upgrades, i.e. "go to social mode"
        }

def find_commands(transcript, stream):
    """
    Search through a transcript for wake words and predefined voice commands - strict mode commands first (not natural language)

    """
    #stop listening while we parse command and TTS (say) the result
    stream.deaf = True

    # closest wake word detection
    wake_words_found = [] #list of fuzzysearch Match objects
    transcript_l = transcript.lower()
    for option in wake_words:
        wake_words_found.extend(find_near_matches(option, transcript_l, max_l_dist=1))

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
            res = voice_command_func(transcript, command_args)
            if type(res) == int and res == 1:
                print("COMMAND COMPLETED SUCCESSFULLY")
                playsound(command_success_sound)
            elif type(res) == str:
                print("COMMAND COMPLETED SUCCESSFULLY")
                print("NOW SAYING: {}".format(res))
                subprocess.call(['say',res])
            else:
                if "fail_function" in voice_commands[command_name]:
                    voice_commands[command_name]["fail_function"]()
                else:
                    playsound(generic_failure_sound)
                    print("COMMAND FAILED")

    #start listening again after we have parsed command, run command, and given user response with TTS
    stream.deaf = False

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))

def receive_transcriptions(transcript_q, responses, stream):
    """Iterates through STT server responses.

    First sends them to whatever GUI we are using (ASG, send over a socket).
    Then, if the request is a "final request", parse for wake words and commands

    The responses passed is a generator that will block until a response
    is provided by the server.

    Each response may contain multiple results, and each result may contain
    multiple alternatives; for details, see https://goo.gl/tjCPAU.  Here we
    print only the transcription for the top alternative of the top result.

    In this case, responses are provided for interim results as well. If the
    response is an interim one, print a line feed at the end of it, to allow
    the next result to overwrite it, until the response is a final one. For the
    final one, print a newline to preserve the finalized transcription.
    """

    for response in responses:

        if get_current_time() - stream.start_time > STREAMING_LIMIT:
            stream.start_time = get_current_time()
            break

        if not response.results:
            continue

        result = response.results[0]

        if not result.alternatives:
            continue

        transcript = result.alternatives[0].transcript

        result_seconds = 0
        result_micros = 0

        if result.result_end_time.seconds:
            result_seconds = result.result_end_time.seconds

        if result.result_end_time.microseconds:
            result_micros = result.result_end_time.microseconds

        stream.result_end_time = int((result_seconds * 1000) + (result_micros / 1000))

        corrected_time = (
            stream.result_end_time
            - stream.bridging_offset
            + (STREAMING_LIMIT * stream.restart_counter)
        )
        # Display interim results, but with a carriage return at the end of the
        # line, so subsequent lines will overwrite them.

        print("--- " + transcript)

        #send transcription responses to our GUI
        #GUI_receive(transcript)
        #add transcript to queue
        print("PUTTING INTO TRANSCRIPT Q")
        transcript_q.put(transcript)

        if result.is_final:
            sys.stdout.write(GREEN)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\n")

            stream.is_final_end_time = stream.result_end_time
            stream.last_transcript_was_final = True

            #send transcription responses to our voice command
            find_commands(transcript, stream)
        else:
            sys.stdout.write(RED)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\r")

            stream.last_transcript_was_final = False

def run_server(transcript_q):

    def GUI_receive(transcript): #soon run_server will be a class, and this will be a method
            asg_socket.send_string(transcript)

    #start a socket connection to the android smart glasses
    asg_socket = ASGSocket()
    asg_socket.start_conn()
    while True: #main server loop
        try:
            #blocking call, only run event when we have a transript, in future this might just check and continue if there are other data streams to check from
            transcript = transcript_q.get()
            GUI_receive(transcript)
        except BrokenPipeError as e: #if error on socket at any point, restart connection and return to loop
            print("Connection broken, listening again")
            asg_socket.start_conn()

def main():
    # Create the shared queue and launch both threads
    transcript_q = Queue()
    server_thread = Thread(target = run_server, args = (transcript_q, ))
    stt_thread = Thread(target = run_google_stt, args = (transcript_q, receive_transcriptions))
    server_thread.start()
    #run speech to text, pass every result to the callback function passed in
    #in the future if we use different STT libs, we can just change that right here
    stt_thread.start()

    #debug loop
    i = 0
    while True:
        print("--- EHLHO WHARLD * {}".format(i))
        i += 1
        time.sleep(1)

if __name__ == "__main__":
    main()
