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


class VisualSearchApi(Resource):
    def __init__(self, tools):
        self.tools = tools
        self.img_limit = 15
        pass

    def send_fail(self):
        resp = dict()
        resp["success"] = False
        return resp

    def post(self):  # NOTE that this is actually using username and not userId...
        # define args to accepts from post
        parser = reqparse.RequestParser()
        parser.add_argument("timestamp", type=int)
        parser.add_argument("image", type=str)
        args = parser.parse_args()

        # get incoming text
        timestamp = args["timestamp"]
        img_b64 = args["image"]
        print("Incoming visual search image")

        #convert image from b64 encoding to raw bytes
        img_bytes = self.tools.b64_to_bytes(img_b64)

        #run visual search
        visual_search_response = self.tools.visual_search(img_bytes)
        print("Visual search response:")
        print(visual_search_response)

        #build payload
        if visual_search_response is not None:
            resp = dict()
            resp["success"] = True
            resp["response"] = visual_search_response
        else:
            return self.send_fail()

        return resp
