# check the frequency of all words in a list, return list of words with frequency below constant
import pandas as pd
from nltk.corpus import wordnet

df_word_freq = pd.read_csv("./english_word_freq_list/unigram_freq.csv")
idx_dict_word_freq = df_word_freq.groupby(by='word').apply(lambda x: x.index.tolist()).to_dict()
low_freq_constant = 250000 #relative to dataset used, up for debate, maybe user-settable (example english not you first language? Want to be higher)... in general, frequency can't be the only metric - maybe you say a rare word every day, we shouldn't keep defining it - cayden

#load text indices
print("Loading word frequency index...")
df_word_freq.iloc[idx_dict_word_freq["golgw"]] # run once to build the index
print("     Word frequency index loaded.")

print("Loading word definitions index...")
syns = wordnet.synsets("dog") #run once to build index
print("     Word definitionsindex loaded.")

#we use this funny looking thing for fast string search, thanks to: https://stackoverflow.com/questions/44058097/optimize-a-string-query-with-pandas-large-data
def find_low_freq_words(text):
    low_freq_words = list()
    for word in text.split(' '):
        print(word)
        word_freq = df_word_freq.iloc[idx_dict_word_freq[word]]
        if len(word_freq) > 0 and word_freq["count"].iloc[0] < low_freq_constant: #we use iloc[0] because our dataset should only have one of each word
            word = word_freq["word"].iloc[0]
            low_freq_words.append(word)
    return low_freq_words

def define_word(word):
    # lookup the word
    syns = wordnet.synsets(word)
    try:
        definition = syns[0].definition()
    except IndexError as e:
        print("Definition unknown for: {}".format(word))
        return {word : None}
    return {word : definition}

import time
ctime = time.time()
words = find_low_freq_words("spectroscopy this is a test and preposterous people might amicably proliferate tungsten arcane ark botanical bonsai gynecologist esoteric")
definitions = [define_word(w) for w in words]
print("Time taken: {}".format(time.time() - ctime))
print(definitions)



ctime = time.time()
words = find_low_freq_words("gulfaircom is a nasty gttool and the old gpollo is amicably preposterous in the desca of my yurok")
definitions = [define_word(w) for w in words]
print(definitions)
print("Time taken: {}".format(time.time() - ctime))
