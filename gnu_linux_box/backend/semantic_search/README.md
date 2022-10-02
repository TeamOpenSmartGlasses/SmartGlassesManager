# Live Contextual Semantic Search - Work in progress

### Data sources:

Wikipedia dump: https://dumps.wikimedia.org/enwiki/
Qrank dump: https://qrank.wmcloud.org/
Wikipedia title converter: https://github.com/jcklie/wikimapper

### How to run

0. 
```
pip install wiki_dump_parser
python3 -m wiki_dump_parser enwiki-20220901-pages-articles-multistream.xml
```
1. Download enwiki dataset
2. Install wikimapper and run commands in README to generate datastore
3. Run wiki_parser.py
4. Run semantic_search_wiki.py
5. Run search.py
