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

from utils.ASGAudioStream import ASGAudioStream
from google.cloud import speech
from google.cloud import translate
import os
import sys
import time
import threading
from utils.ResumableMicrophoneStream import ResumableMicrophoneStream

# Audio recording parameters
STREAMING_LIMIT = 300000 #YOU HAVE TO CHANGE THIS IN ./utils/ResumableMicInput.py AS WELL NO TIME TO SETUP SHARED CONFIG FILES (YAML) #also, no more than 300 seconds or GCP will error - cayden
SAMPLE_RATE = 16000
CHUNK_SIZE = int(SAMPLE_RATE / 10)  # 100ms

import webrtcvad
vad = webrtcvad.Vad(3)
vad_time = 30 #ms
vad_num = 320 # (vad_time * SAMPLE_RATE) / 1000

#terminal printing colors
RED = "\033[0;31m"
GREEN = "\033[0;32m"
YELLOW = "\033[0;33m"

def try_transcribe(content):
    #vad_frame = bytes(bytearray(content)[-vad_num:])
    vad_frame = content[-vad_num:]
    valid_vad = webrtcvad.valid_rate_and_frame_length(SAMPLE_RATE, len(vad_frame))

    if valid_vad:
        try:
            speech_detected = vad.is_speech(vad_frame, SAMPLE_RATE)
        except Exception as e:
            print(e)

    return speech.StreamingRecognizeRequest(audio_content=content)

def get_current_time():
    """Return Current Time in MS."""

    return int(round(time.time() * 1000))

def receive_transcriptions(responses, stream, transcript_q):
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

        #check if our thread kill switch has been activated
        t = threading.currentThread()
        if not getattr(t, "do_run", True):
            return

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

        #send transcription responses to our GUI
        #GUI_receive(transcript)
        #add transcript to queue
        transcript_obj = dict()
        transcript_obj["transcript"] = transcript
        transcript_obj["is_final"] = False

        if result.is_final:
            transcript_obj["is_final"] = True
            sys.stdout.write(GREEN)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\n")

            stream.is_final_end_time = stream.result_end_time
            stream.last_transcript_was_final = True
        else:
            sys.stdout.write(RED)
            sys.stdout.write("\033[K")
            sys.stdout.write(str(corrected_time) + ": " + transcript + "\r")

            stream.last_transcript_was_final = False
        #now that we've done all of the processing, add this transcript to the transcription queue, so that the threads that process transcription can access it
        transcript_q.put(transcript_obj)

def run_google_stt(transcript_q, language_code="en-US"):
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

    while True:
        audio_stream = ASGAudioStream()
        with audio_stream as stream:
            while not stream.closed:
                #check if our thread kill switch has been activated
                if not getattr(t, "do_run", True):
                    return
                sys.stdout.write(YELLOW)

                stream.audio_input = []
                audio_generator = stream.generator()

                requests = (
                    try_transcribe(content)
                    for content in audio_generator
                )

                responses = client.streaming_recognize(streaming_config, requests)

                try:
                    receive_transcriptions(responses, stream, transcript_q)
                except Exception as e:
                    print(e)
                    print('silence time exceeded, opening new connection to google')
                    audio_stream.end_connection()
                    break
    #            if not translate_mode:
    #                # Now, put the transcription responses to use.
    #                parse_cb(transcript_q, cmd_q, obj_q, responses, stream, translate_q)
    #            else: #if we are in translate mode
    #                print("FOREIGN LANGUAGE TEXT")
    #                print(responses[0].results[0].alternatives[0].transcript)
    #
                if stream.result_end_time > 0:
                    stream.final_request_end_time = stream.is_final_end_time
                stream.result_end_time = 0
                stream.last_audio_input = []
                stream.last_audio_input = stream.audio_input
                stream.audio_input = []
                stream.restart_counter = stream.restart_counter + 1

    #            if not stream.last_transcript_was_final:
    #                sys.stdout.write("\n")
                stream.new_stream = True
