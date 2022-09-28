#from datasets import load_dataset
import os
import sys


from txtai.embeddings import Embeddings
from txtai.pipeline import Similarity

import pandas as pd

import time

# Load embeddings
print("Loading embeddings...")
embeddings = Embeddings({"path": "sentence-transformers/paraphrase-MiniLM-L3-v2", "content": True})
embeddings.load("current_wikipedia_title_embedding_numero_100000000_time_1664298053.0403905.txtai")
print("Embeddings loaded.")

#Create similarity instance for re-ranking
#similarity = Similarity("valhalla/distilbart-mnli-12-3")

def search(query):
  return [(result["score"], result["text"]) for result in embeddings.search(query, limit=5)]

def ranksearch(query):
  results = [text for _, text in search(query)]
  return [(score, results[x]) for x, score in similarity(query, results)]

#queries = ["what a wonderful world", "Steve Jobs", "Donald Trump", "vietnam big war in the 60s", "hippy music festival 1969 woodstock", "opposite of day", "dog", "Voltage divider"]
queries = ["USA", "founder of dell", "founder of tesla", "divide a potential difference", "most common keyboard layout", "glass bottle of beer", "laptop"]
#queries = ["Rocket engine", "Personal computer", "Dog"]
# Create similarity instance for re-ranking
#similarity = Similarity("valhalla/distilbart-mnli-12-3")
for query in queries:
    print("Running similiarity search.")
    print("Query: {}".format(query))
    #print(ranksearch(query))
    print(embeddings.search(query, 10))
    print("\n\n")


