from youtube_transcript_api import YouTubeTranscriptApi
import sys
import pandas as pd
import time
  
# assigning srt variable with the list
# of dictonaries obtained by the get_transcript() function
video_id = sys.argv[1]
srt = YouTubeTranscriptApi.get_transcript(video_id)
print(dir(srt))
  
# prints the result
#print("Raw transcript data: ")
#print(srt)

df = pd.DataFrame.from_dict(srt)
print(df)
df.to_csv("transcripts_video_{}_{}.csv".format(video_id, time.time()))
