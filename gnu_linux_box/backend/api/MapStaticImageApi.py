from flask import render_template, make_response
from flask_restful import Resource, reqparse, fields, marshal_with
#from db import Database
from datetime import datetime
import json
import base64
import ast
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


class MapStaticImageApi(Resource):
    def __init__(self, tools):
        self.tools = tools
        pass

    def send_fail(self):
        resp = dict()
        resp["success"] = False
        return resp

    def post(self):  # NOTE that this is actually using username and not userId...
        # define args to accepts from post
        parser = reqparse.RequestParser()
        parser.add_argument("params", type=str)
        args = parser.parse_args()

        # get incoming params
        params_string = args["params"]
        print(params_string)
        print(type(params_string))

        #parse to dict
        #params = json.loads(params_string)
        params = ast.literal_eval(params_string) # use this to handle Android JSONObject INCORRECT use of single quotes on key and value strings in JSON
        print(params)
        print(type(params))

        #get map image
        map_img = self.tools.get_map(params) 
        print("Type of map img:")
        print(type(map_img))

        #encode to string
        #map_img_string = base64.b64encode(map_img)
        map_img_string = base64.b64encode(map_img).decode('ascii')

        #build payload
        if map_img is not None:
            resp = dict()
            resp["success"] = True
            resp["image_b64"] = map_img_string
        else:
            return self.send_fail()

        return resp
