# first neural network with keras make predictions
import numpy as np
from tensorflow import keras
from keras.models import Sequential
from keras.layers import Dense
from keras.optimizers import SGD
#from keras.utils import multi_gpu_model
import sys
import os
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit
from sklearn.preprocessing import normalize
import matplotlib.pyplot as plt
from keras.utils import to_categorical
from sklearn.metrics import classification_report, confusion_matrix

seed_val = 1999
np.random.seed(seed_val)

categorical = True
images_folder = "dataset"
train_percent = 0.9
trimmed = True
ec = 2**13 #2048*12
if trimmed:
    lr = 0.0001
    num_inputs = 48
else: 
    lr = 0.0001
    num_inputs = 956

columns = ["label", "v", "h"]
to_ignore = ["0010_2m_15P_0V_5H.jpg", "0009"] #missing this one, ignore it
data = np.empty((0, num_inputs+1))

#only consider these landmarks
loi = {
    #NOSE
     "nose_bottom" : 1,
     "nose_mid" : 4,
    #EYES
    #right eye iris
     "right_iris_center" : 473,
     "right_iris_inner" : 474,
     "right_iris_upper" : 475,
     "right_iris_outer" : 476,
     "right_iris_lower" : 477,
    #left eye iris "left_iris_center" : 468,
     "left_iris_center" : 468,
     "left_iris_inner" : 469,
     "left_iris_upper" : 470,
     "left_iris_outer" : 471,
     "left_iris_lower" : 472,
    #right eye surrounding
     "right_eye_outer_center" : 263,
     "right_eye_outer_upper" : 466,
     "right_eye_outer_lower" : 249,
     "right_eye_inner_center" : 362,
     "right_eye_inner_upper" : 398,
     "right_eye_inner_lower" : 382,
    #left eye surrounding
     "left_eye_outer_center" : 33,
     "left_eye_outer_upper" : 246,
     "left_eye_outer_lower" : 7,
     "left_eye_inner_center" : 155,
     "left_eye_inner_upper" : 173,
     "left_eye_inner_lower" : 154,
     }

loi_list = list()
vals = list(loi.values())
for i, value in enumerate(vals):
    x = vals[i] * 2
    y = x + 1
    loi_list.append(x)
    loi_list.append(y)

#load the cvs
df = pd.read_csv("./cave_landmarks_1615067368550.csv", header=None)
names = df.iloc[:,0].to_numpy().tolist()

# load the dataset
for image_folder in os.listdir(images_folder):
    if os.path.isdir(os.path.join(images_folder, image_folder)):
        for image in os.listdir(os.path.join(images_folder, image_folder)):
            if ".jpg" in image:
                if image not in to_ignore:
                    for val in to_ignore:
                        if val in image:
                            continue
                    labels = image[:image.index(".")].split("_")
                    v = int(labels[3][:-1])
                    h = int(labels[4][:-1])
#                    if v == 0 and h == 0:
#                        label = 1
#                    elif v == 0 and abs(h) == 5:
#                        label = 1
#                    elif abs(h) == 15:
#                        label = 0
#                    elif abs(v) == 10 and abs(h) == 10:
#                        label = 0
                    if v == 10 and h == -15:
                        label = 0
                    elif v == 10 and h == -5:
                        label = 1
                    elif v == 10 and h == 0:
                        label = 2
                    elif v == 10 and h == 5:
                        label = 3
                    elif v == 10 and h == 15:
                        label = 4
                    elif v == 0 and h == -15:
                        label = 5
                    elif v == 0 and h == -5:
                        label = 6
                    elif v == 0 and h == 0:
                        label = 7
                    elif v == 0 and h == 5:
                        label = 8
                    elif v == 0 and h == 15:
                        label = 9
                    elif v == -10 and h == -15:
                        label = 10
                    elif v == -10 and h == -5:
                        label = 11
                    elif v == -10 and h == 0:
                        label = 12
                    elif v == -10 and h == 5:
                        label = 13
                    elif v == -10 and h == 15:
                        label = 14
                    else:
                        continue
                    #get vectors
                    idx = names.index(image)
                    vectors = df.iloc[idx].iloc[1:].to_numpy()
                    vectors_small = vectors[loi_list].tolist()
                    e = [label]
                    if trimmed:
                        #normalize
                        mini = min(vectors_small)
                        maxi = max(vectors_small)
                        vectors_small = (vectors_small - mini) / (maxi  - mini)
                        e.extend(vectors_small)
                    else:
                        vectors = vectors.tolist()
                        #normalize
                        mini = min(vectors)
                        maxi = max(vectors)
                        vectors = (vectors - mini) / (maxi  - mini)
                        e.extend(vectors)
                    data = np.concatenate([data, [e]], axis=0)
                    
#balance dataset
if categorical:
    pass
#    labels = data[:,0]
#    smallest_balanced_class = min(np.count_nonzero(labels == 0), np.count_nonzero(labels == 1))
#    off_idx = np.random.choice(np.argwhere(labels == 0)[:,0], size=smallest_balanced_class)
#    on_idx = np.random.choice(np.argwhere(labels == 1)[:,0], size=smallest_balanced_class)
#    off_data = data[off_idx]
#    on_data = data[on_idx]
#    balanced_data = np.concatenate([on_data, off_data])
    balanced_data = data
else:
    labels = data[:,0]
    smallest_balanced_class = min(np.count_nonzero(labels == 0), np.count_nonzero(labels == 1))
    off_idx = np.random.choice(np.argwhere(labels == 0)[:,0], size=smallest_balanced_class)
    on_idx = np.random.choice(np.argwhere(labels == 1)[:,0], size=smallest_balanced_class)
    off_data = data[off_idx]
    on_data = data[on_idx]
    balanced_data = np.concatenate([on_data, off_data])

#split into train and test, balanced
#train, test = train_test_split(balanced_data)
num_total_examples = len(balanced_data)
train_samples = int(num_total_examples * train_percent)
test_samples = num_total_examples - train_samples
X = balanced_data[:,1:]
y = balanced_data[:,0]
sss = StratifiedShuffleSplit(train_size=train_samples, n_splits=1,
                             test_size=test_samples, random_state=0)

for train_index, test_index in sss.split(X, y):
    X_train, X_test = X[train_index], X[test_index]
    y_train, y_test = y[train_index], y[test_index]

# split into input (X) and output (y) variables
#X_train = train[:,1:]
#X_test = test[:,1:]
#y_train = train[:,0]
#y_test = test[:,0]
#
# define the keras model
model = Sequential()
#model = multi_gpu_model(model, gpus=2)
model.add(Dense(12, input_dim=num_inputs, activation='relu'))
model.add(Dense(40, activation='relu'))
model.add(Dense(15, activation='sigmoid'))

# compile the keras model
opt = keras.optimizers.Adam(learning_rate=lr)
if categorical:
    model.compile(loss='categorical_crossentropy', optimizer=opt, metrics=['accuracy'])
else:
    model.compile(loss='binary_crossentropy', optimizer=opt, metrics=['accuracy'])
# fit the keras model on the dataset
#print("TRAINX")
#print(train[:,1:])
#print("TRAINY")
#print(train[:,0])
if categorical:
    history = model.fit(X_train, to_categorical(y_train), epochs=ec, batch_size=32)
else:
    history = model.fit(X_train, y_train, epochs=ec, batch_size=32)
#= model.fit(test.iloc[:,1:9], test.iloc[:,0], epochs=1024, batch_size=32)
#evaluate
if categorical:
    _, accuracy = model.evaluate(X_test, to_categorical(y_test))
else:
    _, accuracy = model.evaluate(X_test, y_test)
model.save("gaze_model_accuracy{}.h5".format(int(accuracy*100)))
print('Accuracy : %.2f' % (accuracy * 100))
 
#plot of learning

fig, axs = plt.subplots(2)
# summarize history for accuracy
axs[0].plot(history.history['accuracy'])
axs[0].set_title('model accuracy')
axs[0].set(xlabel='epoch', ylabel='accuracy')
# summarize history for loss
axs[1].semilogy(history.history['loss'])
axs[1].set_title('model loss')
axs[1].set(xlabel='epoch', ylabel='loss')

plt.show()

#get confusion matrixgg

