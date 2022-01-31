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


class TranslateReferenceApi(Resource):
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
        parser.add_argument("query", type=str)
        parser.add_argument("source_language", type=str)
        parser.add_argument("target_language", type=str)
        args = parser.parse_args()

        # get incoming text
        query = args["query"]
        source_language = args["source_language"]
        target_language = args["target_language"]

        #run translation
        translated_reference_response = self.tools.translate_reference(query, source_language=source_language, target_language=target_language)

        #build payload
        if translated_reference_response is not None:
            resp = dict()
            resp["success"] = True
            resp["response"] = translated_reference_response
        else:
            return self.send_fail()

        return resp
