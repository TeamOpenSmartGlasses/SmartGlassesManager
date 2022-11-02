import xml.etree.ElementTree as etree
import codecs
import csv
import time
import os
import sys
import re

from wiki_mapper import WikiMapper

# http://www.ibm.com/developerworks/xml/library/x-hiperfparse/

PATH_WIKI_XML = "./"
FILENAME_WIKI = "enwiki-20220901-pages-articles-multistream.xml"
FILENAME_ARTICLES = 'articles_with_text.csv'
FILENAME_REDIRECT = 'articles_redirect.csv'
FILENAME_TEMPLATE = 'articles_template.csv'
ENCODING = "utf-8"


# Nicely formatted time string
def hms_string(sec_elapsed):
    h = int(sec_elapsed / (60 * 60))
    m = int((sec_elapsed % (60 * 60)) / 60)
    s = sec_elapsed % 60
    return "{}:{:>02}:{:>05.2f}".format(h, m, s)


def strip_tag_name(t):
    t = elem.tag
    idx = k = t.rfind("}")
    if idx != -1:
        t = t[idx + 1:]
    return t


pathWikiXML = os.path.join(PATH_WIKI_XML, FILENAME_WIKI)
pathArticles = os.path.join(PATH_WIKI_XML, FILENAME_ARTICLES)
pathArticlesRedirect = os.path.join(PATH_WIKI_XML, FILENAME_REDIRECT)
pathTemplateRedirect = os.path.join(PATH_WIKI_XML, FILENAME_TEMPLATE)

totalCount = 0
articleCount = 0
redirectCount = 0
templateCount = 0
title = None
abstract = None
start_time = time.time()
skip = False

# non-greedy regex failed, so took this from SO: https://stackoverflow.com/questions/14596884/remove-text-between-and
def remove_text_inside_brackets(text, brackets="{}"):
    count = [0] * (len(brackets) // 2) # count open/close brackets
    saved_chars = []
    for character in text:
        for i, b in enumerate(brackets):
            if character == b: # found bracket
                kind, is_close = divmod(i, 2)
                count[kind] += (-1)**is_close # `+1`: open, `-1`: close
                if count[kind] < 0: # unbalanced bracket
                    count[kind] = 0  # keep it
                else:  # found bracket to remove
                    break
        else: # character is not a [balanced] bracket
            if not any(count): # outside brackets
                saved_chars.append(character)
    return ''.join(saved_chars)

from bs4 import BeautifulSoup

#this is a nasty parser to deal with the nasty format we receive the page in. I'm sure somewhere there is an official wikipedia parser that would do a better job, we should switch to that if we ever find it
def get_abstract(text):
    #remove html tags
    #print(text)
    cleantext = BeautifulSoup(text, "lxml").text

    stripped_text = cleantext.replace("\n", " ").strip()

    #drop everything after first header start, so we only take the abstract
    drop_post = stripped_text[:stripped_text.find("==")]

    #get rid of special types File: and Image:, etc.
    #drop_Image = re.sub(r'\[\[Image.+?\]\]', '', drop_post)
    #drop_File = re.sub(r'\[\[File.+?\]\]', '', drop_Image)
    #drop_media = re.sub(r'/\[\[(File|Image|Media):[^[\]\]]*(\[\[.*]])?[^[\]\]]*]]/gi', '', drop_post) # https://stackoverflow.com/questions/34717096/how-to-remove-all-files-from-wiki-string
    drop_File = re.sub(r'\[\[File:[^[\]]*(?:\[\[[^[\]]*]][^[\]]*)*]]', '', drop_post)
    drop_Image = re.sub(r'\[\[Image:[^[\]]*(?:\[\[[^[\]]*]][^[\]]*)*]]', '', drop_File)
    drop_Media = re.sub(r'\[\[Media:[^[\]]*(?:\[\[[^[\]]*]][^[\]]*)*]]', '', drop_Image) #https://stackoverflow.com/questions/34717096/how-to-remove-all-files-from-wiki-string

    drop_headers = remove_text_inside_brackets(drop_Media)#re.sub(r'{{.*?}}', '', drop_post)

    drop_links = drop_headers.replace("[[", "").replace("]]", "")

    split_links = drop_links.replace("|", " ")

    drop_parans = re.sub(r'\([^()]*\)', '', split_links)

    drop_special = drop_parans.replace("'", "").replace("\"", "").strip()

    #drop nots
    drop_notes = remove_text_inside_brackets(drop_special, brackets="[]")#re.sub(r'{{.*?}}', '', drop_post)

    #fix spaces and newlines
    single_spaces = re.sub(' +', ' ', drop_notes)
    no_newlines = re.sub('\n+', ' ', single_spaces)
    no_newlines_two = re.sub('\r+', ' ', no_newlines)

    abstract = no_newlines_two
    #print(abstract)
    #print("\n\n")
    return abstract


mapper = WikiMapper("index_enwiki_latest_ossg.db")
with codecs.open(pathArticles, "w", ENCODING) as articlesFH, \
        codecs.open(pathArticlesRedirect, "w", ENCODING) as redirectFH, \
        codecs.open(pathTemplateRedirect, "w", ENCODING) as templateFH:
    articlesWriter = csv.writer(articlesFH, quoting=csv.QUOTE_MINIMAL)
    redirectWriter = csv.writer(redirectFH, quoting=csv.QUOTE_MINIMAL)

    articlesWriter.writerow(['id', 'title', 'redirect', 'wikidata_id', 'abstract'])

    for event, elem in etree.iterparse(pathWikiXML, events=('start', 'end')):
        tname = strip_tag_name(elem.tag)

        if event == 'start':
            if tname == 'page':
                title = ''
                id = -1
                redirect = ''
                inrevision = False
                ns = 0
                wikidata_id = -1
                abstract = ""
            elif tname == 'revision':
                # Do not pick up on revision id's
                inrevision = True
        else:
            if tname == 'title':
                title = elem.text
                wikidata_id = mapper.title_to_id(title.replace(" ", "_"))
                if wikidata_id is None:
                    print("{} not found.".format(title))
                    skip = True
            elif tname == 'id' and not inrevision:
                id = int(elem.text)
            elif tname == 'redirect':
                redirect = elem.attrib['title']
            elif tname == 'ns':
                ns = int(elem.text)
            elif tname == 'text':
                text = elem.text
                if len(redirect) == 0 and text != None:
                    abstract = get_abstract(text)
            elif tname == 'page':
                totalCount += 1
#                if totalCount>=250:
#                    sys.exit()

                if ns == 10:
                    templateCount += 1
                elif len(redirect) == 0:
                    articleCount += 1
                else:
                    redirectCount += 1
                    #redirectWriter.writerow([id, title, redirect])

                #articlesWriter.writerow([id, title, redirect, text])
                if not skip and len(redirect) == 0:
                    articlesWriter.writerow([id, title, redirect, wikidata_id, abstract])
                skip = False

#                if totalCount > 1000:
#                    break

                if totalCount > 1 and (totalCount % 100000) == 0:
                    print("{:,}".format(totalCount))
            else:
                pass

            elem.clear()

elapsed_time = time.time() - start_time

print("Total pages: {:,}".format(totalCount))
print("Template pages: {:,}".format(templateCount))
print("Article pages: {:,}".format(articleCount))
print("Redirect pages: {:,}".format(redirectCount))
print("Elapsed time: {}".format(hms_string(elapsed_time)))
