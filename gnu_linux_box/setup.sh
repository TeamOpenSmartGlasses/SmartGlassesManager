#!/usr/bin/env bash

python -m spacy download en_core_web_sm

#setup wordnet
python -c "import nltk;nltk.download('wordnet');nltk.download('omw-1.4')"
