# importing csv module
import csv
import pandas as pd
import os

# csv file name
filename = "wikidata-qrank.csv"

# initializing the titles and rows list
#fields = []
#rows = []
#
## reading csv file
#with open(filename, 'r') as csvfile:
#    # creating a csv reader object
#    csvreader = csv.reader(csvfile)
#
#    # extracting field names through first row
#    fields = next(csvreader)
#
#    # extracting each data row one by one
#    for row in csvreader:
#        rows.append(row)
#
## get total number of rows
#print("Total no. of rows: %d"%(csvreader.line_num))
#
## printing the field names
#print('Field names are:' + ', '.join(field for field in fields))
#
## printing first 5 rows
#print('\nFirst 5 rows are:\n')
#for row in rows[:5]:
#    # parsing each column of a row
#    for col in row:
#        print("%10s"%col,end=" "),
#        print('\n')
#

#def estimate_df_chunk_number(df_path):
#    full_size = os.path.getsize(df_path)  # get size of file
#    linecount = None
#    with open(df_path,'rb') as f:
#        next(f)                              # skip header
#        line_size = len(f.readline())        # get size of one line, assuming 1 byte encoding
#        linecount = full_size // line_size + 1   # ~count of lines
#    return linecount
#
#def process(chunk):
#    for line in chunk:
#        print(line)
#
##chunk through the CSV so we don't load ~86Gb at once
#chunksize = 10 ** 3
#curr_chunk_idx = 0
#total_chunks_to_proc = estimate_df_chunk_number(filename)
#for chunk in pd.read_csv(filename, index_col = False, chunksize=chunksize):
#    curr_chunk_idx += 1
#    curr_percent_embedded = (curr_chunk_idx / total_chunks_to_proc) * 100
#    print("Loaded CSV chunk.")
#    print(chunk)
#    proc_finished_flag = process(chunk)
#    print("Processed CSV chunk.");
#    if proc_finished_flag:
#        break
#

num_entries = 10000
df = pd.read_csv(filename)
#df = df.sort_values("QRank", ascending=False) #they are already sorted by QRank, so this is just wasted compute
df = df.head(num_entries)
#df['Entity'] = df['Entity'].str[1:].astype(int) #previously removed the "Q", but that was a mistake, not needed
df.to_csv("./qrank_top_n_pandas_tenthousand_2.csv")
#print(df)
