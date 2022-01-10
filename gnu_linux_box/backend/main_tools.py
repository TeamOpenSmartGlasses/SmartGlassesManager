import spacy
from duckduckgo_search import ddg
import time
import opengraph_py3 as opengraph
import wikipedia
import requests
import json
import urllib
import base64

#function structured into their classes and/or modules
from utils.bing_visual_search import bing_visual_search

#for lack of a better structure, this is all the functions, the tools
class Tools:
    def __init__(self):
        #import nlp
        self.spacey_nlp = spacy.load("en_core_web_sm") # if not found, download with python -m spacy download en_core_web_sm
        self.og_limit = 3 #only open up this many pages to check for open graph, or it will take too long
        self.summary_limit = 35 #word limit on summary
        self.check_wiki_limit = 5 #don't use wiki unless it's in the top n results

        #setup wolfram
        self.wolfram_api_key_file = "./utils/wolfram_api_key.txt"
        self.wolfram_api_key = self.get_wolfram_key(self.wolfram_api_key_file)

    def run_ner(self, text):
        #run nlp
        doc = self.spacey_nlp(text)
        return doc

    def search_duckduckgo(self, entity_name):
        #duckduckgosearch for entities
        #don't run this more than once every two seconds, or there will be an error
        results = ddg(entity_name, region='wt-wt', safesearch='Moderate', time='y', max_results=8)
        return results

    def check_links_for(self, search_results, tag):
        for site in search_results:
            for k in site.keys():
                if tag in site[k].lower():
                    return site

    def semantic_web_speech(self, text):
        #run named entity recognition
        nes = self.run_ner(text).ents

        # for each thing (name entity) search engine (duckduckgo) to find the top result for that thing
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

    def get_best_link_info(self, search_results):
        #takes in a list of link, returns one with the best info - most easy to parse

        #first, check for wikipedia
        link = self.check_links_for(search_results[:self.check_wiki_limit], "wikipedia")
        if link is not None:
            currTime = time.time()
            wiki_res = self.wikipedia_search(link)
            print("Wikipedia time was: {}".format(time.time() - currTime))
            if wiki_res is not None and wiki_res["image"] is not None: #wikipedia api, for some pages, doesn't return image. If not, first try looking for opengraph page
                return wiki_res

        #then, check for OG compatible
        counter = 0
        for site in search_results:
            if "youtube" in site["href"].lower(): # youtube doesn't give good info, but is often high in search results, so ignore it
                continue
            try:
                currTime = time.time()
                page = opengraph.OpenGraph(url=site["href"], scrape=True)
                counter += 1
                if page.is_valid():
                    print("Found OG for: {}".format(site["href"]))
                    image_url = page.get('image', None)
                    if not image_url.startswith('http'):
                        image_url = urljoin(page['_url'], page['image'])
                    summary = page.get('description', None)
                    site["image"] = image_url
                    site["summary"] = summary
                    return site
                if counter > self.og_limit:
                    break
                print("OPgraph time was: {}".format(time.time() - currTime))
            except Exception as e:
                continue

        #all has failed, return the first result
        return search_results[0]

    def wikipedia_search(self, ent):
        #TODO limit this to one search
        #parse query from url (hilarious)
        query = ent["href"].split("/")[-1].replace("_", " ")
        print("Wikipedia query: " + query)

        #make wikipedia search request (will fail if ambiguous)
        try:
            wiki_res = wikipedia.page(query, auto_suggest=False)
        except wikipedia.exceptions.DisambiguationError as e:
            return None


        #get first paragraph of summary
        summary_start = wiki_res.summary.split("\n")[0]

        #get MAIN image url
        WIKI_REQUEST = 'http://en.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&piprop=original&titles='
        response  = requests.get(WIKI_REQUEST+wiki_res.title)
        json_data = json.loads(response.text)
        try:
            img_url = list(json_data['query']['pages'].values())[0]['original']['source']
        except Exception as e:
            img_url = None
        
        #pack result
        ent["image"] = img_url
        ent["summary"] = summary_start

        return ent

    def get_wolfram_key(self, key_file):
        #Wolfram API key - this loads from a plain text file containing only one string - your APP id key
        wolfram_api_key = None
        with open(key_file) as f:
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
        result = bing_visual_search(img_bytes)
        return result

    def search_engine(self, query):
        currTime = time.time()
        links = self.search_duckduckgo(query)

        if links is None:
            return None

        #get the best link and its first image
        best_link = self.get_best_link_info(links)
        if best_link is None:
            return None

        #limit summary
        best_link["summary"] = " ".join(best_link["summary"].split(" ")[:self.summary_limit]) + "..."

        return best_link



