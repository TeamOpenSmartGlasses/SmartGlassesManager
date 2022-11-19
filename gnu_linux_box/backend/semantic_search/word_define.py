from nltk.corpus import wordnet

# lookup the word
syns = wordnet.synsets("game")
print(syns[0].definition())
syns = wordnet.synsets("hacker")
print(syns[0].definition())
syns = wordnet.synsets("cat")
print(syns[0].definition())
syns = wordnet.synsets("amicable")
print(syns[0].definition())
syns = wordnet.synsets("tablessss")
print(syns[0].definition())
