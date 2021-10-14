#Emex Labs, cayden

#Much code borrwed from https://github.com/googleapis/python-speech/blob/master/samples/microphone/transcribe_streaming_infinite.py
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
from utils.ResumableMicrophoneStream import ResumableMicrophoneStream

#STREAMING_LIMIT = 240000 # 4 minutes
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


def run_google_stt(transcript_q, cmd_q, obj_q, parse_cb, thread_q, translate_mode=False, language_code="en-US"):
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

    mic_manager = ResumableMicrophoneStream(SAMPLE_RATE, CHUNK_SIZE)
    sys.stdout.write(YELLOW)
    sys.stdout.write('\nListening, begin entering voice commands now.\n\n')
    sys.stdout.write("End (ms)       Transcript Results/Status\n")
    sys.stdout.write("=====================================================\n")

    with mic_manager as stream:
        while not stream.closed:
            sys.stdout.write(YELLOW)
#            sys.stdout.write(
#                "\n" + str(STREAMING_LIMIT * stream.restart_counter) + ": NEW REQUEST\n"
#            )

            stream.audio_input = []
            audio_generator = stream.generator()

            requests = (
                try_transcribe(content)
                for content in audio_generator
            )

            responses = client.streaming_recognize(streaming_config, requests)

            parse_cb(transcript_q, cmd_q, obj_q, responses, stream, thread_q)
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
