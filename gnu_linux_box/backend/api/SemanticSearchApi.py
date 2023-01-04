from flask import render_template, make_response
import time
from flask_restful import Resource, reqparse, fields, marshal_with
from db import Database
from datetime import datetime
import requests
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


from semantic_search.wiki_mapper import WikiMapper

mapper = WikiMapper("index_enwiki_latest_ossg.db")

class SemanticSearchApi(Resource):
    def __init__(self, tools):
        self.tools = tools
        #self.db = Database()
        #self.db.connect()
        pass

    def send_fail(self):
        resp = dict()
        resp["success"] = False
        return resp

    def post(self):  # NOTE that this is actually using username and not userId...
        # define args to accepts from post
        parser = reqparse.RequestParser()
        parser.add_argument("transcript")
        parser.add_argument("timestamp")
        parser.add_argument("userId") #later this should be verified, for now, just trust the user
        args = parser.parse_args()

        # get incoming text
        print("SEMANTIC SEARCH API ARGS:")
        print(args)

        #save transcript to database
        userId = args['userId']
        transcript = args['transcript']
        timestamp = args['timestamp']
        if timestamp is None:
            timestamp = time.time()

        #run word frequency on transcript
        low_freq_words = self.tools.find_low_freq_words(transcript)
        definitions = [self.tools.define_word(lfw) for lfw in low_freq_words]
        print("LOW FREQUENCY WORDS TO DEFINE: {}".format(definitions))

        #run semantic search on wikipedia
        res = self.tools.run_semantic_wiki(transcript)
        resp = dict()
        resp["success"] = True
        if len(res) == 0:
            return resp

        for item in res:
            resp["result"] = False
            print(item['id'])
            print(item['score'])
            print('\n')
        else:
            resp["result"] = True

        title = res[0]['id']
        resp["title"] = title #"Test from backend server"
        #body = res[0]['text']
        body = res[0]['text'][len(title)+1:] #get ride of first word, which is title of article
        body_length = 380
        body_short = body[:min(len(body), body_length)]
        resp["body"] = body_short #"This is that big old test stuff, where we test it all, let's let the testing happen.\nOh yeah, good test stuff man."

        #get image from wikidata page
        image_width = 480
        #wikidata_id = #"Q95724881"
        #wikidata_id = mapper.title_to_id(str(title).replace(" ", "_"))
        wikidata_id = res[0]['tags']
        image_name_url = "https://www.wikidata.org/w/api.php?action=query&prop=images&format=json&titles={}".format(wikidata_id)
        try:
            image_name = requests.get(url=image_name_url, timeout=5).json()
            image_names = image_name["query"]["pages"][list(image_name["query"]["pages"].keys())[0]]["images"]
            image_idx = 0
            image_name = image_names[0]["title"]
            #sometimes we get a weird image, like the wave file icon, so make sure we don't here
            banned_image_names = [".wav", ".ogg"]
            bad_name = True
            while bad_name:
                bad_name = False
                for b in banned_image_names:
                    if b in image_name:
                        bad_name = True
                        image_idx += 1
                        image_name = image_names[image_idx]["title"]
            print(image_name)
            image_raw_url = "https://commons.wikimedia.org/w/index.php?title=Special:Redirect/file/{}&width={}".format(image_name, image_width).replace (" ","%20")
            resp["image"] = image_raw_url
        except Exception as e:
            print("Error:")
            print(e)

        return resp
