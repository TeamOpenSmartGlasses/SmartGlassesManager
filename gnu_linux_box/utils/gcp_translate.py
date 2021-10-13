import os
import sys
from google.cloud import translate

os.environ["GOOGLE_APPLICATION_CREDENTIALS"]=os.path.join(os.path.dirname(__file__), "creds.json")

def translate_text(translation_client, text="hola, Encantado de conocerte", project_id="wearableai", source_language="es", target_language="en"):
    """Translating Text.

    text : single string - text to be translated

    """

    location = "global"

    parent = f"projects/{project_id}/locations/{location}"

    # Detail on supported types can be found here:
    # https://cloud.google.com/translate/docs/supported-formats
    response = translation_client.translate_text(
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

def run_google_translate(translate_q, obj_q):
    #make translation client
    client = translate.TranslationServiceClient()

    while True:
        print("Translate loop started")
        text = translate_q.get()
        result = translate_text(client, text)
        print("Translation result: {}".format(result))
        obj_q.put({"type" : "translate_result", "data" : result})

if __name__ == "__main__":
    if len(sys.argv) > 1:
        print("Translated text: {}".format(translate_text(sys.argv[1])))
    else: 
        print("Translated text: {}".format(translate_text()))
