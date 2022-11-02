# importing csv module
import csv
import pandas as pd
import os

from wiki_mapper import WikiMapper

#map wikidata id to title
print("Loading mapper...")
mapper = WikiMapper("index_enwiki_latest_ossg.db")
print("Mapper loaded.")
def convert_id_to_title(row):
    global mapper
    wikidata_id = row["Entity"]
    titles = mapper.id_to_titles(str(wikidata_id))
    if len(titles) > 0:
        title = titles[0]
    else:
        title = None
    return title

# csv file name
filename = "wikidata-qrank.csv"

num_entries = 100000
print("Loading wikidata-qrank.csv.")
df = pd.read_csv(filename)
print("CSV loaded.")

#df = df.sort_values("QRank", ascending=False) #they are already sorted by QRank, so this is just wasted compute
df = df.head(num_entries)
print(df)
df['title'] = df.apply(lambda row: convert_id_to_title(row), axis=1)
print(df)
#df.apply(lambda row: convert_id_to_title(row), axis=1)
#df['Entity'] = df['Entity'].str[1:].astype(int) #previously removed the "Q", but that was a mistake, not needed
df.to_csv("./qrank_listed_hundredthousand.csv")
