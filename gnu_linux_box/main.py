#!/usr/bin/env python

"""
Streams in always on microphone, transcribes (speech (audio) to text), runs results through function to find user defined wake words and commands.

Main components:
    -this file, master file which creates shared queues, starts up threads, kills threads
    -STT - GCP 
    -server - TCP socket
    -voice command system that parses commands and runs the commands

Both of these are IO-bound, both of them need to run all the time. We need be asynchronous, for now that's with multithreading.

@author: Cayden Pierce, Emex Labs
"""

import sys
import time

from queue import Queue, Empty
from threading import Thread
from rx import of, operators as op
import random
from rx.subject import Subject

from utils.gcp_stt import run_google_stt
from utils.gcp_translate import run_google_translate
from utils.voice_command_server import run_voice_command_server, run_voice_command
from utils.ASGAudioServer import run_audio_server

from language_options import language_options

def make_new_thread(thread_q, thread_holder, translate_q, obj_q, audio_stream_observable): #handles making new threads and adding them to the thread holder for the main loop
    try:
        thread_obj = thread_q.get(False)
        if thread_obj:
            print("thread request")
            thread_type = thread_obj["type"]
            if thread_type == "translate":
                if thread_obj["cmd"] == "start":
                    #first, check if translate mode is already running
                    try:
                        translate_stt_thread = thread_holder["translate_stt"]
                        translater_thread = thread_holder["translater"]
                        if translate_stt_thread is not None or translater_thread is not None:
                            return
                    except KeyError:
                        pass

                    language = thread_obj["language"]
                    source_language = language_options[language.lower()]
                    #source_language = "es"
                    translate_stt_thread = Thread(target = run_google_stt, args = (translate_q, audio_stream_observable, source_language)) #transcribes a different language
                    translater_thread = Thread(target = run_google_translate, args = (translate_q, obj_q, source_language)) #takes those translations and converts them into our target language
                    print("STARTING TRANSLATION THREAD")
                    translate_stt_thread.start()
                    translater_thread.start()
                    thread_holder["translate_stt"] = translate_stt_thread
                    thread_holder["translater"] = translater_thread
                else: #if cmd is kill, then stop the translater, if it's running
                    try:
                        translate_stt_thread = thread_holder["translate_stt"]
                        translater_thread = thread_holder["translater"]
                    except KeyError:
                        return
                    if translate_stt_thread:
                        print("attempting to kill (((((((((((((((((((((((((((((((((((((((((")
                        translate_stt_thread.do_run = False
                        translate_stt_thread.join()
                        print("kill success (((((((((((((((((((((((((((((((((((((((((")
                        thread_holder["translate_stt"] = None
                    if translater_thread:
                        print("2attempting to kill (((((((((((((((((((((((((((((((((((((((((")
                        translater_thread.do_run = False
                        translater_thread.join()
                        print("2kill success (((((((((((((((((((((((((((((((((((((((((")
                        thread_holder["translater"] = None
    except Empty:
        pass


def main():
    # Create the shared queue and launch both threads
    transcript_q = Queue() #to share raw transcriptions
    cmd_q = Queue() #to share parsed commands (currently voice command is running in run_google_stt)
    obj_q = Queue() #generic object queue to pass anything, of type: {"type" : "transcript" : "data" : "hello world"} or similiar
    thread_q = Queue() #requests to the main thread to start a new thread
    translate_q = Queue() # holds text to be translated to english
    test_q = Queue() # a test queue that no one will ever read
    audio_stream_observable = Subject() #an observable to share the audio data stream with anyone who subscribes to the stream
    print(dir(audio_stream_observable))

    server_thread = Thread(target = run_voice_command_server, args = (transcript_q, cmd_q, obj_q))
    audio_server_thread = Thread(target = run_audio_server, args = (audio_stream_observable,))
    stt_thread = Thread(target = run_google_stt, args = (transcript_q, audio_stream_observable))
    voice_command_thread = Thread(target = run_voice_command, args = (transcript_q, cmd_q, obj_q, thread_q))

    #set the thread kill boolean to true
    audio_server_thread.do_run = True

    server_thread.start() #create connection with ASG and handle sending it stuff
    audio_server_thread.start()
    stt_thread.start() #convert speech to text, fill queue with transcribed speech
    voice_command_thread.start() #receive output form STT, process it, send results and commands to ASG

    #look for requests to start a new thread (some threads, like a translation service, are only started when requested by the user, and killed when the user requests to kill it
    thread_holder = dict()
    i = 0
    while True:
        make_new_thread(thread_q, thread_holder, translate_q, obj_q, audio_stream_observable)
        i += 1
        time.sleep(0.2)

    #shutdown - kill thread
    audio_server.do_run = False
    stt_thread.do_run = False
    server_thread.kill()
    stt_thread.kill()
    voice_command_thread.kill()
    audio_server_thread.kill()
    server_thread.join()
    stt_thread.join()
    voice_command_thread.join()
    audio_server_thread.join()

if __name__ == "__main__":
    main()
