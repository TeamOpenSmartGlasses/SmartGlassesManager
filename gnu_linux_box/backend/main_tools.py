import spacy
from duckduckgo_search import ddg
import time
import opengraph_py3 as opengraph
import wikipedia
import requests
import json
import urllib
import base64
import os
import sys
import threading
from google.cloud import translate

from txtai.embeddings import Embeddings
from txtai.pipeline import Similarity
import pandas as pd

#word frequency + definitions
from nltk.corpus import wordnet

#function structured into their classes and/or modules
from utils.bing_visual_search import bing_visual_search

wiki_score_threshold = 0.40
class Tools:
    def __init__(self):
        #import nlp
        self.spacey_nlp = spacy.load("en_core_web_sm") # if not found, download with python -m spacy download en_core_web_sm
        self.og_limit = 3 #only open up this many pages to check for open graph, or it will take too long
        self.summary_limit = 90 #word limit on summary
        self.check_wiki_limit = 15 #don't use wiki unless it's in the top n results

        #setup gcp translate client
        self.gcp_api_key_file = "gcp_creds.json"
        os.environ["GOOGLE_APPLICATION_CREDENTIALS"]=os.path.join("keys", self.gcp_api_key_file)
        self.gcp_translate_client = translate.TranslationServiceClient()
        self.gcp_project_id = json.load(open(os.path.join("keys", self.gcp_api_key_file)))["project_id"]

        #setup wolfram key
        #Wolfram API key - this loads from a plain text file containing only one string - your APP id key
        self.wolfram_api_key_file = "wolfram_api_key.txt"
        self.wolfram_api_key = self.get_key(self.wolfram_api_key_file)

        #setup map quest key
        self.map_quest_api_key_file = "map_quest_key.txt"
        self.map_quest_api_key = self.get_key(self.map_quest_api_key_file)

        #duckduckgo languages
        self.ddg_langs = {
                "en" : "wt-wt",
                 "fr" : "fr-fr"
                     }

        #load and hold semantic search index
        # Load embeddings
        print("Loading embeddings...")
        self.embeddings = Embeddings({"path": "sentence-transformers/paraphrase-MiniLM-L3-v2", "content": True})
        embeddings_folder = "./semantic_search/current_wikipedia_title_embedding_articletext_numero_100000000_time_1664807043.2603853.txtai"
        self.embeddings.load(embeddings_folder)
        dir(self.embeddings)
        print("Embeddings loaded.")

        #setup word frequency + definitions
        self.df_word_freq = pd.read_csv("./semantic_search/english_word_freq_list/unigram_freq.csv")
        self.idx_dict_word_freq = self.df_word_freq.groupby(by='word').apply(lambda x: x.index.tolist()).to_dict()
        self.low_freq_constant = 250000 #relative to dataset used, up for debate, maybe user-settable (example english not you first language? Want to be higher)... in general, frequency can't be the only metric - maybe you say a rare word every day, we shouldn't keep defining it - cayden

        #load text indices
        print("Loading word frequency index...")
        self.df_word_freq.iloc[self.idx_dict_word_freq["golgw"]] # run once to build the index
        print("--- Word frequency index loaded.")

        print("Loading word definitions index...")
        syns = wordnet.synsets("dog") #run once to build index
        print("--- Word definitions index loaded.")

    def semantic_wiki_filters(self, data):
        data_filtered = list()
        for item in data:
            if item['score'] > wiki_score_threshold:
                data_filtered.append(item)
        return data_filtered

    def run_semantic_wiki(self, query, filters=True, n=3):
        #res = self.embeddings.search(query, n)
        res = self.embeddings.search("select id, text, score, tags from txtai where similar('{}')".format(query))
        res_filtered = self.semantic_wiki_filters(res)
        return res_filtered

    def run_ner(self, text):
        #run nlp
        doc = self.spacey_nlp(text)
        return doc

    def search_duckduckgo(self, entity_name, language='en'):
        #duckduckgosearch for entities
        #don't run this more than once every two seconds, or there will be an error
        region = self.ddg_langs[language]
        results = ddg(entity_name, region=region, safesearch='Moderate', max_results=8)

        return results

    def check_links_for(self, search_results, tag):
        for site in search_results:
            for k in site.keys():
                if tag in site[k].lower():
                    return site

    def semantic_web_speech(self, text):
        #run named entity recognition
        nes = self.run_ner(text).ents

        entities_results= list()
        for ne in nes:
            print("Entity found: " + ne.text)
            print(dir(ne))
            currTime = time.time()
            links = self.search_duckduckgo(ne.text)
            print("DuckDuckGo time was: {}".format(time.time() - currTime))
            if links is not None:
                entities_results.append(links)

        #get the best link and its first image
        entity_list = list()
        for entity_links in entities_results:
            best_entity = self.get_best_link_info(entity_links)
            if best_entity is not None:
                entity_list.append(best_entity)

        #limit summary
        for entity in entity_list:
            entity["summary"] = " ".join(entity["summary"].split(" ")[:self.summary_limit]) + "..."

        return entity_list

    def get_best_link_info(self, search_results, language="en"):
        #check if our response has all necesarry info, and should be sent
        reference_contents = ["title", "body", "image"]
        def is_response_full(response):
            print("CHECKING RESPONES:")
            print(response)
            for rc in reference_contents:
                if rc not in response.keys():
                    return False
                elif response[rc] is None:
                    return False
            return True

        def add_to_response(response, key, value):
            if value is None:
                return
            if (key not in response.keys()) or (response[key] is None):
                response["image"] = value


        #takes in a list of link, returns one with the best info - most easy to parse

        #response being built:
        response = dict()

        #first, check for wikipedia
        wiki_link = self.check_links_for(search_results[:self.check_wiki_limit], "wikipedia")
        if wiki_link is not None:
            currTime = time.time()
            #parse query from url (hilarious)
            print("WIKI TITLE")
            print(wiki_link["href"])
            #response["title"] = wiki_link["href"].split("/")[-1].replace("_", " ")
            response["title"] = wiki_link["title"].replace(" - Wikipedia", "")
            #wiki_res = self.get_wikipedia_data_from_page_title(response["title"])
            response["body"] = wiki_link["body"]
            #response["image"] = wiki_res["image"]

            #get image
            response["image"] = self.get_wikipedia_image_link_from_page_title(wiki_link["href"].split("/")[-1].replace("_", " "), language=language)
            
            if is_response_full(response): #wikipedia api, for some pages, doesn't return image. If not, first try looking for opengraph page
                return response

        print("response after wiki:")
        print(response)
        #then, check for OG compatible
        counter = 0
        for site in search_results:
            print("CHECKING: ")
            print(site)
            if "youtube" in site["href"].lower(): # youtube doesn't give good info, but is often high in search results, so ignore it
                continue
            try:
                currTime = time.time()
                page = opengraph.OpenGraph(url=site["href"], scrape=True)
                counter += 1
                if page.is_valid():
                    print("Found OG for: {}".format(site["href"]))

                    #get image
                    print(page)
                    image_url = page.get('image', None)
                    if (image_url is not None) and (not image_url.startswith('http')):
                        image_url = urljoin(page['_url'], page['image'])

                    add_to_response(response, "image", image_url)

                    #response["body"] = page.get('description', None)
                    add_to_response(response, "body", page.get('body', None))
                    print("PRECHECK RESPONES")
                    print(response)
                    if is_response_full(response): #wikipedia api, for some pages, doesn't return image. If not, first try looking for opengraph page
                        return response
                if counter > self.og_limit:
                    break
                print("OPgraph time was: {}".format(time.time() - currTime))
            except Exception as e:
                print("ERROR")
                print(e)
                continue

        if "title" in response.keys():
            return response
        else:
            #all has failed, return the first result
            return search_results[0]

    def get_wikipedia_data_from_page_title(self, title, language="en"):
        #TODO limit this to one search
        print("Wikipedia query: " + title)

        #set language of wikipedia search
        wikipedia.set_lang(language)

        #make wikipedia search request (will fail if ambiguous)
        try:
            wiki_search = wikipedia.search(title)
            wiki_page_name = wiki_search[0]
            wiki_res = wikipedia.page(wiki_page_name, auto_suggest=False)
        except Exception as e:
            return None

        #get first paragraph of summary
        summary_start = wiki_res.summary.split("\n")[0]

        #get MAIN image url
        img_url = self.get_wikipedia_image_link_from_page_title(wiki_res.title, language=language)
        
        #pack result
        res = dict()
        res = dict()
        res["title"] = wiki_res.title
        res["image"] = img_url
        res["body"] = summary_start

        return res

    def get_wikipedia_image_link_from_page_title(self, page_title, language="en"):
        #do search on wikipedia to get proper title
        #get_wikipedia_image_link_from_page_title(raw_page_title, language=language)

        #get MAIN image url
        WIKI_REQUEST = 'http://{}.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles={}'.format(language, page_title)
        response  = requests.get(WIKI_REQUEST)
        json_data = json.loads(response.text)
        try:
            img_url = list(json_data['query']['pages'].values())[0]['original']['source']
            max_size = 480
            print(img_url)
            image_wikipedia_name = img_url.split("/")[-1]
            img_url = img_url.replace("/commons/", "/commons/thumb/")
            img_url = img_url.replace(f"/{language}/", f"/{language}/thumb/")
            img_url = img_url + f"/{max_size}px-{image_wikipedia_name}"
            if img_url[-3:] == "svg":
                img_url = img_url + ".png"
            elif img_url[-3:] == "ogv":
                img_url = None
#            img_url = f"https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/{image_wikipedia_name}/{max_size}px-{image_wikipedia_name}"
#            print("IMAGE WIKI NAME")
#            print(image_wikipedia_name)
        except Exception as e:
            print(e)
            img_url = None
        print("got wiki image url: {}".format(img_url))
        return img_url

    def get_key(self, key_file):
        wolfram_api_key = None
        with open("./keys/" + key_file) as f:
            wolfram_api_key = [line.strip() for line in f][0]
        return wolfram_api_key

    def natural_language_query(self, query):
        """
        A wrapper for natural langauge queries.
        In the future, will use much more than just wolfram alpha for everything.
        """
        return self.wolfram_conversational_query(query)

    def wolfram_conversational_query(self, query):
        #API that returns short conversational responses - can be extended in future to be conversational using returned "conversationID" key
        #encode query
        query_enc = urllib.parse.quote_plus(query)

        #build request
        getString = "https://api.wolframalpha.com/v1/conversation.jsp?appid={}&i={}".format(self.wolfram_api_key, query_enc)
        response = requests.get(getString)

        if response.status_code == 200:
            parsed_res = response.json()
            if "error" in parsed_res:
                return None, None
            return parsed_res["result"], parsed_res["conversationID"]
        else:
            return None, None

    def b64_to_bytes(self, b64_string):
        b64_bytes = b64_string.encode('ascii')
        message_bytes = base64.b64decode(b64_bytes)
        return message_bytes

    def visual_search(self, img_bytes):
        raw_result = bing_visual_search(img_bytes)
        keepKeys = ["thumbnailUrl", "name"]
        result = list()
        for raw_res in raw_result:
            res = dict()
            for key in keepKeys:
                res[key] = raw_res[key]
            result.append(res)
        return result

    def search_engine(self, query, language="en"):
        currTime = time.time()
        links = self.search_duckduckgo(query, language=language)
        print("DDG links:")
        print(links)

        if links is None:
            return None

        #get the best link and its first image
        for i, link in enumerate(links):
            print("{} -- {}".format(i, link))

        best_link = self.get_best_link_info(links, language=language)
        print("BEST SEARCH LINK OVERALL:")
        print(best_link)
        if best_link is None:
            return None

        #limit summary
        best_link["body"] = " ".join(best_link["body"].split(" ")[:self.summary_limit]) + "..."

        return best_link

    def get_map(self, params):
        map_url = "https://open.mapquestapi.com/staticmap/v5/map?"
        for param_key in params.keys():
            param_value = params[param_key]
            map_url = map_url + param_key + "=" + param_value + "&"

        map_url = map_url + "key=" + self.map_quest_api_key

        response = requests.get(map_url)
        img_bytes = response.content
        return img_bytes

    def translate_text_simple(self, text, source_language="fr", target_language="en"):
        """Translating Text.

        text : single string - text to be translated

        """
        location = "global"

        parent = f"projects/{self.gcp_project_id}/locations/{location}"

        # Detail on supported types can be found here:
        # https://cloud.google.com/translate/docs/languages
        # https://cloud.google.com/translate/docs/supported-formats
        response = self.gcp_translate_client.translate_text(
            request={
                "parent": parent,
                "contents": [text],
                "mime_type": "text/plain",  # mime types: text/plain, text/html
                "source_language_code": source_language,
                "target_language_code": target_language,
            }
        )

        # return the translation for each input text provided
        return response.translations[0].translated_text

    def translate_reference(self, text, source_language="en", target_language="fr"):
        translated_text = self.translate_text_simple(text, source_language=source_language, target_language=target_language)
        translated_text = self.translate_text_simple(text, source_language=source_language, target_language=target_language)
        print("Translated text is: {}".format(translated_text))
        res = self.get_wikipedia_data_from_page_title(translated_text, language=target_language)
        if res is not None:
            res["body"] = " ".join(res["body"].split(" ")[:self.summary_limit]) + "..."
            res["language"] = target_language
        return res

    #we use this indexing for fast string search, thanks to: https://stackoverflow.com/questions/44058097/optimize-a-string-query-with-pandas-large-data
    def find_low_freq_words(self, text):
        low_freq_words = list()
        for word in text.split(' '):
            print(word)
            try:
                word_freq = self.df_word_freq.iloc[self.idx_dict_word_freq[word]]
            except KeyError as e:
                print(e) #word doesn't exist in frequency list
                continue
            if len(word_freq) > 0 and word_freq["count"].iloc[0] < self.low_freq_constant: #we use iloc[0] because our dataset should only have one of each word
                word = word_freq["word"].iloc[0]
                low_freq_words.append(word)
        return low_freq_words

    def define_word(self, word):
        # lookup the word
        syns = wordnet.synsets(word)
        try:
            definition = syns[0].definition()
        except IndexError as e:
            print("Definition unknown for: {}".format(word))
            return {word : None}
        return {word : definition}

