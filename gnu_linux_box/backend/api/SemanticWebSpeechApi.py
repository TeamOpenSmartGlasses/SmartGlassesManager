from flask import render_template, make_response
from flask_restful import Resource, reqparse, fields, marshal_with
#from db import Database
from datetime import datetime
#from bson.objectid import ObjectId
#from flask_bcrypt import Bcrypt
#from flask_jwt_extended import (
#    JWTManager,
#    jwt_required,
#    create_access_token,
#    get_jwt_identity,
#    set_access_cookies,
#    create_refresh_token,
#)


class SemanticWebSpeechApi(Resource):
    def __init__(self, nlp):
        self.nlp = nlp
        pass

    def send_fail(self):
        resp = dict()
        resp["success"] = False
        resp["payload"] = dict()
        return resp

    def post(self):  # NOTE that this is actually using username and not userId...
        # define args to accepts from post
        parser = reqparse.RequestParser()
        parser.add_argument("text", type=str)
        args = parser.parse_args()

        # get incoming text
        text = args["text"]
        print("Incoming text is: {}".format(text))

        #get semantic speech information
        entity_links = self.nlp.semantic_web_speech(text)

        #build payload
        if (entity_links) != 0:
            resp = dict()
            resp["success"] = True
            resp["payload"] = entity_links
        else:
            return self.send_fail()

        return resp
