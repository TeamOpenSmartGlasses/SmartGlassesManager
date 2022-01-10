#Emex Labs, cayden

#Some code borrwed from https://github.com/googleapis/python-speech/blob/master/samples/microphone/transcribe_streaming_infinite.py
#Copyright for the Google STT streaming code
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

from google.cloud import speech
from google.cloud import translate
import os
import sys
import time
import threading
from utils.ResumableMicrophoneStream import ResumableMicrophoneStream
from queue import Queue, Empty
import collections
import traceback

#THIS NEEDS TO BE A CLASS - cayden

# Audio recording parameters
STREAMING_LIMIT = 300000 #YOU HAVE TO CHANGE THIS IN ./utils/ResumableMicInput.py AS WELL NO TIME TO SETUP SHARED CONFIG FILES (YAML) #also, no more than 300 seconds or GCP will error - cayden
SAMPLE_RATE = 16000
CHUNK_SIZE = int(SAMPLE_RATE / 10)  # 100ms

import webrtcvad
vad = webrtcvad.Vad(2) #aggresiveness of 2 based on loose testing - the VAD is easily tricked
vad_time = 30 #ms
#vad_num = 320 #int((vad_time / 1000) * SAMPLE_RATE)
vad_num = int((vad_time / 1000) * SAMPLE_RATE)
vad_num_bytes = vad_num * 2  #*2 because PCM 16 = 16 bit samples
vad_slider = 5 #holds data for last n seconds of speech for VAD to slide across
vad_slider_bytes = int(( vad_slider * SAMPLE_RATE ) * 2) #number of bytes our slider should be
vad_ring_buffer = bytes([0] * vad_slider_bytes) #don't keep as global

#globals that will be class variables soon
#audio data in counter
last_chunk_time = 0
quickstart_stream = True
stream_kill_time = 3

#terminal printing colors
RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[0;33m"

def vad_filter(content):
    global vad_ring_buffer
    #append our new content to the ring buffer
    vad_ring_buffer = vad_ring_buffer + content
    vad_ring_buffer = vad_ring_buffer[-vad_slider_bytes:]

    valid_vad = webrtcvad.valid_rate_and_frame_length(SAMPLE_RATE, vad_num)

    vad_frame = vad_ring_buffer[-vad_num_bytes:]

    if valid_vad:
        frame_count = 0
        speech_detected_count = 0
        for i in range(0, len(vad_ring_buffer) - vad_num_bytes, vad_num_bytes):
            frame_count += 1
            chunk = vad_ring_buffer[i:i+vad_num_bytes]
            speech_detected = vad.is_speech(chunk, SAMPLE_RATE)
            if speech_detected:
                speech_detected_count += 1
        return content
    else: 
        print("Not valid VAD rate and frame length.")
        return content

def try_transcribe(content):
    return speech.StreamingRecognizeRequest(audio_content=content)

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))

def receive_transcriptions(responses, transcript_q):
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
    start_time = get_current_time() #the time we start streaming

    t = threading.currentThread()

    for response in responses:

        #check if our thread kill switch has been activated
        if not getattr(t, "do_run", True):
            return

        #check if we are over our streaming limit - if so, break so we can start a new connection
        if (get_current_time() - start_time) > STREAMING_LIMIT:
            break

        if not response.results:
            continue

        result = response.results[0]

        if not result.alternatives:
            continue

        transcript = result.alternatives[0].transcript

        # Display interim results, but with a carriage return at the end of the
        # line, so subsequent lines will overwrite them.
        #send transcription responses to our GUI
        #GUI_receive(transcript)
        #add transcript to queue
        transcript_obj = dict()
        transcript_obj["transcript"] = transcript
        transcript_obj["is_final"] = False

        if result.is_final:
            transcript_obj["is_final"] = True
            sys.stdout.write(GREEN)
            print(transcript)
        else:
            sys.stdout.write(YELLOW)
            print(transcript)

        #now that we've done all of the processing, add this transcript to the transcription queue, so that the threads that process transcription can access it
        transcript_q.put(transcript_obj)

def new_chunk_time_update():
    global last_chunk_time
    last_chunk_time = time.time()

def new_chunk(queue, data):
    new_chunk_time_update()
    queue.put(data)

#check if there is audio
def audio_checker_func(audio_stream_observable):
    print("Waiting for audio...")
    tmp_queue = Queue()

    #first, subscribe to the observable
    sub = audio_stream_observable.subscribe(
            #put the value into our local audio queue
            lambda data: new_chunk(tmp_queue, data)
        )
    
    #wait for data to come through and update the timer
    t = threading.currentThread()
    while True:
        try:
            chunk = tmp_queue.get(timeout=0.2)
            break
            if not getattr(t, "do_run", True):
                # on end, Signal the generator to terminate so that the client's
                # streaming_recognize method will not block the process termination.
                return
        except Empty as e:
            pass


    sub.dispose()
    print("New audio chunk received, returning.")

def audio_generator_func(audio_stream_observable):
    kill_stream = False
    current_data = None
    audio_queue = Queue()
    #first, subscribe to the observable
    audio_stream_observable.subscribe(
            #put the value into our local audio queue
            lambda data: new_chunk(audio_queue, data)
        )
    
    t = threading.currentThread()
    while True:
        if not getattr(t, "do_run", True):
            # on end, Signal the generator to terminate so that the client's
            # streaming_recognize method will not block the process termination.
            return

        yield audio_queue.get()

def run_google_stt(transcript_q, audio_stream_observable, language_code="en-US"):
    global last_chunk_time, new_stream, stream_kill_time
    """start bidirectional streaming from microphone input to speech API"""
    #set gcloud API key
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"]=os.path.join(os.path.dirname(__file__), "creds.json")

    client = speech.SpeechClient()
    config = speech.RecognitionConfig(
        encoding=speech.RecognitionConfig.AudioEncoding.LINEAR16,
        sample_rate_hertz=SAMPLE_RATE,
        language_code=language_code,
        max_alternatives=1,
        speech_contexts=[{"phrases" : ["Licklider", "mind expansion", "mind extension", "Cayden", "Cayden Pierce", "BCI", "wearables", "engineering", "bash", "shell"]}] #TODO make this pull from our wake words and voice commands list automatically
    )

    streaming_config = speech.StreamingRecognitionConfig(
        config=config, interim_results=True
    )

    # mic_manager = ResumableMicrophoneStream(SAMPLE_RATE, CHUNK_SIZE)
    sys.stdout.write(YELLOW)
    sys.stdout.write('\nListening, begin entering voice commands now.\n\n')
    sys.stdout.write("End (ms)       Transcript Results/Status\n")
    sys.stdout.write("=====================================================\n")

    #a way to exit when threaded, in future this entire thing could be inside a threaded class that handles the stopping of the thread
    t = threading.currentThread()
    t.do_run = True

    #vad_ring_buffer = collections.deque([0] * vad_num, maxlen=vad_num)
    quickstart_stream = False
    while True:
        #if we aren't receiving a live audio stream, don't restart google connection until we are
        if not quickstart_stream and (time.time() - last_chunk_time) > stream_kill_time:
            audio_checker_func(audio_stream_observable) #block until we have audio streaming
            
        vad_ring_buffer = bytes([0] * vad_slider_bytes) #reset to empty
        #check if our thread kill switch has been activated
        if not getattr(t, "do_run", True):
            return
        sys.stdout.write(YELLOW)
        audio_generator = audio_generator_func(audio_stream_observable) #audio_stream.generator()

        requests = (
            try_transcribe(vad_filter(content))
            for content in audio_generator
        )

        streaming_config = speech.StreamingRecognitionConfig(
            config=config, interim_results=True
        )
        responses = client.streaming_recognize(streaming_config, requests)

        try:
            receive_transcriptions(responses, transcript_q)
        except Exception as e:
#            print(e)
#            tb = traceback.format_exc()
#            print(tb)
            print('Google STT API error, opening new streaming connection to Google Speech to Text API')
