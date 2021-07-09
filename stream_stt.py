#!/usr/bin/env python

"""
Streams in always on microphone, sends to GCP to be transcribed, receives live transcription, runs results through function to find user defined wake words and commands.

Right now just shoving everything into this file and will modularize as the program form develops

@author: Cayden Pierce
Based on the google speech file provided by Google
"""

# Copyright 2019 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Google Cloud Speech API sample application using the streaming API.

NOTE: This module requires the dependencies `pyaudio` and `termcolor`.
To install using pip:

    pip install pyaudio
    pip install termcolor

Example usage:
    python transcribe_streaming_infinite.py
"""
import requests
import urllib

import subprocess

# Function to find all close matches of
# input string in given list of possible strings
from difflib import get_close_matches
from fuzzysearch import find_near_matches

import re
import sys
import time

from google.cloud import speech
import pyaudio
from six.moves import queue

#config file(s)
wake_words_file = "./wakewords.txt"
voice_memories_file = "./data/voice_memories.csv"
wolfram_api_key_file = "./wolfram_api_key.txt"

#set API key
import os
os.environ["GOOGLE_APPLICATION_CREDENTIALS"]=os.path.join(os.path.dirname(__file__), "creds.json")

# Audio recording parameters
STREAMING_LIMIT = 240000  # 4 minutes
SAMPLE_RATE = 16000
CHUNK_SIZE = int(SAMPLE_RATE / 10)  # 100ms

RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[0;33m"

#define phrases/word that will wake the system to search the current speech for voice commands
#wake_words = ["licklider", "mxt", "mxd", "mxc", "mind extension", "mind expansion", "wearable AI"]
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

#Wolfram API key - this loads from a plain text file containing only one string - your APP id key
wolframApiKey = None
with open(wolfram_api_key_file) as f:
    wolframApiKey = [line.strip() for line in f][0]

#API that returns short form responses
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
        "save memory" : {"function" : save_memory, "voice_sounds" : ["save memory", "save speech", "mxt cache"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "ask Wolfram" : {"function" : ask_wolfram, "voice_sounds" : ["Wolfram", "Wolfram Alpha", "ask Wolfram"]}, #pass command directly to the terminal window we opened #TODO implement this with python `cli` package
        "go to"  : {"function" : None, "voice_sounds" : ["select", "choose", "go to"]}, #start a new program mode (enter different mental upgrade loop, or start a 'suite' of mental upgrades, i.e. "go to social mode"
        }

def find_commands(transcript):
    """
    Search through a transcript for wake words and predefined voice commands - strict mode commands first (not natural language)

    """
    # closest wake word detection
    wake_words_found = [] #list of fuzzysearch Match objects
    transcript_l = transcript.lower()
    for option in wake_words:
        wake_words_found.extend(find_near_matches(option, transcript_l, max_l_dist=1))

    #if we found a wake word, tell the user
    found_wake_word = False
    wake_word = None
    command_match = None
    command_name = None
    command_args = None
    possible_command = None
    if len(wake_words_found) > 0:
        found_wake_word = True
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
    if found_wake_word and not found_command:
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

    if command_match is not None:
        print("RECEIVED COMMAND: {}".format(command_name))

        #run commands funcs
        voice_command_func = voice_commands[command_name]["function"]
        if voice_command_func is not None:
            res = voice_command_func(transcript, command_args)
            if type(res) == int and res == 1:
                print("COMMAND COMPLETED SUCCESSFULLY")
            elif type(res) == str:
                print("COMMAND COMPLETED SUCCESSFULLY")
                print("NOW SAYING: {}".format(res))
                #subprocess.Popen("say {}".format(res), close_fds=True)
                subprocess.call(['say',res])

            else:
                print("COMMAND FAILED")

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))


class ResumableMicrophoneStream:
    """Opens a recording stream as a generator yielding the audio chunks."""

    def __init__(self, rate, chunk_size):
        self._rate = rate
        self.chunk_size = chunk_size
        self._num_channels = 1
        self._buff = queue.Queue()
        self.closed = True
        self.start_time = get_current_time()
        self.restart_counter = 0
        self.audio_input = []
        self.last_audio_input = []
        self.result_end_time = 0
        self.is_final_end_time = 0
        self.final_request_end_time = 0
        self.bridging_offset = 0
        self.last_transcript_was_final = False
        self.new_stream = True
        self._audio_interface = pyaudio.PyAudio()
        self._audio_stream = self._audio_interface.open(
            format=pyaudio.paInt16,
            channels=self._num_channels,
            rate=self._rate,
            input=True,
            frames_per_buffer=self.chunk_size,
            # Run the audio stream asynchronously to fill the buffer object.
            # This is necessary so that the input device's buffer doesn't
            # overflow while the calling thread makes network requests, etc.
            stream_callback=self._fill_buffer,
        )

    def __enter__(self):

        self.closed = False
        return self

    def __exit__(self, type, value, traceback):

        self._audio_stream.stop_stream()
        self._audio_stream.close()
        self.closed = True
        # Signal the generator to terminate so that the client's
        # streaming_recognize method will not block the process termination.
        self._buff.put(None)
        self._audio_interface.terminate()

    def _fill_buffer(self, in_data, *args, **kwargs):
        """Continuously collect data from the audio stream, into the buffer."""

        self._buff.put(in_data)
        return None, pyaudio.paContinue

    def generator(self):
        """Stream Audio from microphone to API and to local buffer"""

        while not self.closed:
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

            # Use a blocking get() to ensure there's at least one chunk of
            # data, and stop iteration if the chunk is None, indicating the
            # end of the audio stream.
            chunk = self._buff.get()
            self.audio_input.append(chunk)

            if chunk is None:
                return
            data.append(chunk)
            # Now consume whatever other data's still buffered.
            while True:
                try:
                    chunk = self._buff.get(block=False)

                    if chunk is None:
                        return
                    data.append(chunk)
                    self.audio_input.append(chunk)

                except queue.Empty:
                    break

            yield b"".join(data)


def listen_print_loop(responses, stream):
    """Iterates through server responses and prints them.

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

        if result.is_final:
            sys.stdout.write(GREEN)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\n")

            stream.is_final_end_time = stream.result_end_time
            stream.last_transcript_was_final = True

            #sent transcription responses to our voice command
            find_commands(transcript)
        else:
            sys.stdout.write(RED)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\r")

            stream.last_transcript_was_final = False


def main():
    """start bidirectional streaming from microphone input to speech API"""

    client = speech.SpeechClient()
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
        sample_rate_hertz=SAMPLE_RATE,
        language_code="en-US",
        max_alternatives=1,
        speech_contexts=[{"phrases" : ["Licklider", "mind expansion", "mind extension", "Cayden", "Cayden Pierce", "BCI", "wearables", "engineering", "bash", "shell"]}] #TODO make this pull from our wake words and voice commands list automatically
    )

    streaming_config = speech.StreamingRecognitionConfig(
        config=config, interim_results=True
    )

    mic_manager = ResumableMicrophoneStream(SAMPLE_RATE, CHUNK_SIZE)
    print(mic_manager.chunk_size)
    sys.stdout.write(YELLOW)
    sys.stdout.write('\nListening, say "Quit" or "Exit" to stop.\n\n')
    sys.stdout.write("End (ms)       Transcript Results/Status\n")
    sys.stdout.write("=====================================================\n")

    with mic_manager as stream:

        while not stream.closed:
            sys.stdout.write(YELLOW)
            sys.stdout.write(
                "\n" + str(STREAMING_LIMIT * stream.restart_counter) + ": NEW REQUEST\n"
            )

            stream.audio_input = []
            audio_generator = stream.generator()

            requests = (
                speech.StreamingRecognizeRequest(audio_content=content)
                for content in audio_generator
            )

            responses = client.streaming_recognize(streaming_config, requests)

            # Now, put the transcription responses to use.
            listen_print_loop(responses, stream)

            if stream.result_end_time > 0:
                stream.final_request_end_time = stream.is_final_end_time
            stream.result_end_time = 0
            stream.last_audio_input = []
            stream.last_audio_input = stream.audio_input
            stream.audio_input = []
            stream.restart_counter = stream.restart_counter + 1


            if not stream.last_transcript_was_final:
                sys.stdout.write("\n")
            stream.new_stream = True


if __name__ == "__main__":
    main()
