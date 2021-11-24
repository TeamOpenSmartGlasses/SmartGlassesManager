#Thanks to
import nltk
import pandas as pd
from nltk.cluster import KMeansClusterer
import numpy as np
from sentence_transformers import SentenceTransformer
from scipy.spatial import distance_matrix
import random
nltk.download('punkt')
model = SentenceTransformer('all-MiniLM-L6-v2')

def affective_summarize(json_convo, out_num=8):
#    sentences=nltk.sent_tokenize(article)
#    sentences = [sentence.strip() for sentence in sentences]
    data = pd.DataFrame(json_convo["phrases"])
    data = data.sort_values('timestamp', ignore_index=True)

    def get_sentence_embeddings(sentence):
        embedding = model.encode([sentence])
        return embedding[0]

    data['embeddings'] = data['phrase'].apply(get_sentence_embeddings)

    NUM_CLUSTERS=out_num

    X = np.array(data['embeddings'].tolist())

    rng = random.Random()
    rng.seed(123)
    kclusterer = KMeansClusterer(
            NUM_CLUSTERS, distance=nltk.cluster.util.cosine_distance,
            repeats=25,avoid_empty_clusters=True, rng=rng)

    assigned_clusters = kclusterer.cluster(X, assign_clusters=True)
    data['cluster'] = pd.Series(assigned_clusters, index=data.index)
    data['centroid'] = data['cluster'].apply(lambda x: kclusterer.means()[x])

    def distance_from_centroid(row):
        # type of emb and centroid is different, hence using tolist below
        return distance_matrix([row['embeddings']], [row['centroid'].tolist()])[0][0]

    # Compute centroid distance to the data
    data['distance_from_centroid'] = data.apply(distance_from_centroid, axis=1)
    data['importance'] = data['distance_from_centroid'] * data["affective_engagement"]
    summary='. '.join(data.sort_values('importance',ascending = True).groupby('cluster').head(1).sort_index()['phrase'].tolist())
    return summary
