#when we processed the data, we missed one
#this finds that one lol

import pandas as pd
import sys
import os

images_folder = "./dataset"

#load the cvs
df = pd.read_csv("./cave_landmarks_1615067368550.csv", header=None)
names = df.iloc[:,0].to_numpy().tolist()

# load the dataset
imgs = list()
for image_folder in os.listdir(images_folder):
    if os.path.isdir(os.path.join(images_folder, image_folder)):
        for image in os.listdir(os.path.join(images_folder, image_folder)):
            if ".jpg" in image:
                if image not in names:
                    print(image)
