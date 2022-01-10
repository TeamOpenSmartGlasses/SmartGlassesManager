import wikipedia
import sys

query = sys.argv[1]

wiki_res = wikipedia.page(query, auto_suggest=False)

print(dir(wiki_res))
print(wiki_res.title)
#first paragraph of summary
summary_start = wiki_res.summary.split("\n")[0]
print(summary_start)
print(wiki_res.images[0])
