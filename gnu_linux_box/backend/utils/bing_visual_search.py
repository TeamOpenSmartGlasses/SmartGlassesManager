import json
import requests
from PIL import Image
from io import BytesIO

#BASE_URI = 'https://api.cognitive.microsoft.com/bing/v7.0/images/visualsearch'
#BASE_URI = 'https://api.bing.microsoft.com' + "/v7.0/images/visualsearch"
#BASE_URI = 'https://api.cognitive.microsoft.com/bing/v7.0/images/visualsearch'

imagePath = '/home/cayden/Documents/to_rec/IMG_20211012_221002.jpg' #toilet paper
#imagePath = '/home/cayden/Documents/to_rec/IMG_20211014_235712.jpg' #12V power supply
#imagePath = '/home/cayden/Documents/to_rec/IMG_20211012_221016.jpg' #oculus quest 2
#imagePath = '/home/cayden/Documents/to_rec/nano.jpg' #arduino nano

def bing_visual_search(img_bytes):
    BASE_URI = "https://api.bing.microsoft.com/v7.0/images/visualsearch"

    #set azure API key
    with open('./keys/azure_key.txt') as f:
        SUBSCRIPTION_KEY = f.readline().strip()

    HEADERS = {'Ocp-Apim-Subscription-Key': SUBSCRIPTION_KEY}

    file = {'image' : ('myfile', img_bytes)}
    params = {"mkt" : "en-us"}

    try:
        response = requests.post(BASE_URI, params=params, headers=HEADERS, files=file)
        response.raise_for_status()

        #get the visual search response
        resp_dict = response.json()
        visual_search_result = None
        for action in resp_dict["tags"][0]["actions"]:
            if action['actionType'] == "VisualSearch":
                visual_search_result = action
                break

#        for data in visual_search_result["data"]["value"]:
#            print(data["name"])
#            print(data["hostPageDisplayUrl"])
#            thumb_img_url = data['thumbnailUrl']
            #get image and display it
    #        thumb_img_bytes = requests.get(thumb_img_url).content
    #        thumb_img = Image.open(BytesIO(thumb_img_bytes))
    #        thumb_img.show()

        return visual_search_result["data"]["value"]
    except Exception as ex:
        print(ex)
        return None

def bing_visual_search_file(filepath):
    BASE_URI = "https://api.bing.microsoft.com/v7.0/images/visualsearch"
    SUBSCRIPTION_KEY = "53cf054028cb4bae98713c5be4afc8f9" 
    HEADERS = {'Ocp-Apim-Subscription-Key': SUBSCRIPTION_KEY}

    file = {'image' : ('myfile', open(filepath, 'rb'))}
    params = {"mkt" : "en-us"}

    try:
        response = requests.post(BASE_URI, params=params, headers=HEADERS, files=file)
        response.raise_for_status()

        #get the visual search response
        resp_dict = response.json()
        visual_search_result = None
        for action in resp_dict["tags"][0]["actions"]:
            if action['actionType'] == "VisualSearch":
                visual_search_result = action
                break

#        for data in visual_search_result["data"]["value"]:
#            print(data["name"])
#            print(data["hostPageDisplayUrl"])
#            thumb_img_url = data['thumbnailUrl']
            #get image and display it
    #        thumb_img_bytes = requests.get(thumb_img_url).content
    #        thumb_img = Image.open(BytesIO(thumb_img_bytes))
    #        thumb_img.show()

        return visual_search_result["data"]["value"]
    except Exception as ex:
        print(ex)
        return None

if __name__ == "__main__":
    res = bing_visual_search_file(imagePath)
    print(res)
