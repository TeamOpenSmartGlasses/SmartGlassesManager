from flask import render_template, make_response
from flask_restful import Resource, reqparse, fields, marshal_with
from db import Database
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


class FinalTranscriptApi(Resource):
    def __init__(self, tools):
        self.tools = tools
        self.db = Database()
        self.db.connect()
        pass

    def send_fail(self):
        resp = dict()
        resp["success"] = False
        return resp

    def post(self):  # NOTE that this is actually using username and not userId...
        # define args to accepts from post
        parser = reqparse.RequestParser()
        parser.add_argument("transcript", type=str)
        parser.add_argument("timestamp")
        parser.add_argument("userId") #later this should be verified, for now, just trust the user
        parser.add_argument("id")
        args = parser.parse_args()

        # get incoming text
        print("FINAL_TRANSCRIPT API ARGS:")
        print(args)

        #save transcript to database
        userId = args['userId']
        transcript = args['transcript']
        timestamp = args['timestamp']
        dbResp = self.db.insertTranscript(userId, transcript, timestamp)
        print(dbResp)

        resp = dict()
        resp["success"] = True

        return resp
