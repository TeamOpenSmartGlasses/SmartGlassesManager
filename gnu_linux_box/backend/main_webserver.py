# flask
from flask import Flask, render_template, flash, redirect, jsonify
from flask import request
from flask_restful import Api
from flask_cors import CORS
from flask_cors import CORS, cross_origin

# regular stuff
import json
from time import sleep
import os
import threading

# custom - endpoints
from main_tools import Tools
from api.SemanticWebSpeechApi import SemanticWebSpeechApi
from api.NaturalLanguageQueryApi import NaturalLanguageQueryApi
from api.SearchEngineApi import SearchEngineApi
from api.VisualSearchApi import VisualSearchApi
from api.MapStaticImageApi import MapStaticImageApi
from api.TranslateTextSimpleApi import TranslateTextSimpleApi
from api.TranslateReferenceApi import TranslateReferenceApi
from api.FinalTranscriptApi import FinalTranscriptApi
from api.SemanticSearchApi import SemanticSearchApi

# app setup
app = Flask(__name__)
CORS(app)
app.debug = True

api = Api(app)  # flask_restful
CORS(app)

# start/attach everything
tools = Tools()
api.add_resource(SemanticWebSpeechApi, "/semantic_web_speech", resource_class_args=[tools])
api.add_resource(NaturalLanguageQueryApi, "/natural_language_query", resource_class_args=[tools])
api.add_resource(SearchEngineApi, "/search_engine_search", resource_class_args=[tools])
api.add_resource(VisualSearchApi, "/visual_search_search", resource_class_args=[tools])
api.add_resource(MapStaticImageApi, "/get_static_map", resource_class_args=[tools])
api.add_resource(TranslateTextSimpleApi, "/translate_text_simple_query", resource_class_args=[tools])
api.add_resource(TranslateReferenceApi, "/translate_reference_query", resource_class_args=[tools])
api.add_resource(FinalTranscriptApi, "/final_transcript", resource_class_args=[tools])
api.add_resource(SemanticSearchApi, "/semantic_search", resource_class_args=[tools])

# for dev server
def start():
    app.debug = True
    app.run(debug=True, host='0.0.0.0', port = 5000)
    #app.run(debug=True)

if __name__ == "__main__":
    start()
