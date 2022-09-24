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
embeddings.load("current_wikipedia_title_embedding_numero_100000000_time_1663965972.4657254.txtai")
print("Embeddings loaded.")

#Create similarity instance for re-ranking
#similarity = Similarity("valhalla/distilbart-mnli-12-3")

def search(query):
  return [(result["score"], result["text"]) for result in embeddings.search(query, limit=50)]

def ranksearch(query):
  results = [text for _, text in search(query)]
  return [(score, results[x]) for x, score in similarity(query, results)]

#queries = ["what a wonderful world", "Steve Jobs", "Donald Trump", "vietnam big war in the 60s", "hippy music festival 1969 woodstock", "opposite of day", "dog", "Voltage divider"]
queries = ["USA", "founder of dell", "founder of tesla", "glass bottle of beer", "laptop"]
# Create similarity instance for re-ranking
similarity = Similarity("valhalla/distilbart-mnli-12-3")
for query in queries:
    print("Running similiarity search.")
    print("Query: {}".format(query))
    print(ranksearch(query))


