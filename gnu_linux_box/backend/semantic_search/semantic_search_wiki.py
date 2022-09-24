#from datasets import load_dataset
import os
import sys


from txtai.embeddings import Embeddings
from txtai.pipeline import Similarity

import pandas as pd

import time

#csv path 
csv_path = 'enwiki-20220901-pages-articles-multistream.csv'

#globals
stream_index = 0
finished = False

def stream(dataset):
  global stream_index, finished
  for row in dataset:
    wiki_title = str(row)
    #print("Embedding: {}".format(row))
    yield (wiki_title, wiki_title, None)
    stream_index += 1
    if stream_index >= numero:
        finished = True
        return

def search(query):
  return [(result["score"], result["text"]) for result in embeddings.search(query, limit=50)]

def ranksearch(query):
  results = [text for _, text in search(query)]
  return [(score, results[x]) for x, score in similarity(query, results)]

# Load HF dataset
#dataset = load_dataset("ag_news", split="train")

numero = 100000000
print("Loading CSV...")
#df = pd.read_csv('enwiki-20220901-pages-articles-multistream.csv', quotechar='|', index_col = False, nrows=numero)

#load the qrank top n csv to compare
qrank = pd.read_csv("qrank_top_n_pandas.csv")

def estimate_df_chunk_number(df_path):
    full_size = os.path.getsize(df_path)  # get size of file
    linecount = None
    with open(df_path,'rb') as f:
            next(f)                              # skip header
            line_size = len(f.readline())        # get size of one line, assuming 1 byte encoding
            linecount = full_size // line_size + 1   # ~count of lines
    return linecount

embeddings = Embeddings({"path": "sentence-transformers/paraphrase-MiniLM-L3-v2", "content": True})
def process(df):
    global finished
    df['timestamp'] = pd.to_datetime(df['timestamp'],format='%Y-%m-%dT%H:%M:%SZ')

    print("\n\n")
    print("BEFORE")
    print(df)

    #filter the chunk down to just pages which are included in the top n qrank
    df = df[df["page_id"].isin(qrank["Entity"])]
    print("AFTER")
    print(df)
    print("\n\n")
    dataset = df['page_title'].tolist()

    # Create embeddings model, backed by sentence-transformers & transformers, enable content storage
    print("Starting making embeddings...")
    embeddings.upsert(stream(dataset))
    print("Done making embeddings.")

    return finished

    # Create similarity instance for re-ranking
    #similarity = Similarity("valhalla/distilbart-mnli-12-3")

    #queries = ["what a wonderful world", "Donald Trump", "big war in the 60s", "hippy music festival 1969", "opposite of day", "dog"]
    #for query in queries:
    #    print("Running similiarity search.")
    #    print(ranksearch(query)[:2])

#chunk through the CSV so we don't load ~86Gb at once
chunksize = 10 ** 4
curr_chunk_idx = 0
total_chunks_to_proc = estimate_df_chunk_number(csv_path)
for chunk in pd.read_csv('enwiki-20220901-pages-articles-multistream.csv', quotechar='|', index_col = False, chunksize=chunksize):
    curr_chunk_idx += 1
    curr_percent_embedded = (curr_chunk_idx / total_chunks_to_proc) * 100
    print("Embedding progress: {}% complete".format(curr_percent_embedded))
    print("Loaded CSV chunk.")
    proc_finished_flag = process(chunk)
    print("Processed CSV chunk.");
    if proc_finished_flag:
        break
embeddings.save("./current_wikipedia_title_embedding_numero_{}_time_{}.txtai".format(numero, time.time()))
