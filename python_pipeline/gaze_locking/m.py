# first neural network with keras make predictions
import numpy as np
from keras.models import Sequential
from keras.layers import Dense
from keras.optimizers import SGD
#from keras.utils import multi_gpu_model
import sys
import os
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import StratifiedShuffleSplit
from sklearn.linear_model import LinearRegression

images_folder = "dataset"
train_percent = 0.85
num_inputs = 1

columns = ["label", "v", "h"]
to_ignore = ["0010_2m_15P_0V_5H.jpg", "0009"] #missing this one, ignore it
data = np.empty((0, num_inputs+1))

def calculateHeadAngle(ratio):
    y_angle_head = (750* ratio) - 750; #this was computed, and then changed through trial and error
    return y_angle_head;

def calculateEyeAngle(ratio, is_left):
    y_angle_eye = (3800* ratio) - 3800; #this was computed, and then changed through trial and error
    if (is_left):
        y_angle_eye = -1 * y_angle_eye;
    return y_angle_eye;

def angle(vectors):
    """
    Feature engineering - find angle of head and eyes relative to camera
    """
    #x angles
    #get head angle using distance from eyes to nose
    #get distance between right eye and nose
    eye_nose_dist_right = vectors[loi["nose_bottom"]*2] - vectors[loi["right_iris_center"]*2];
    eye_nose_dist_left = vectors[loi["left_iris_center"]*2] - vectors[loi["nose_bottom"]*2];
    #now we get ratio of the two. Let's add 2 to both distances to "normalize" (don't let any negative numbers, don't hit a divide by zero,  makes things easier)
    head_turn_ratio = (eye_nose_dist_right + 2) / (eye_nose_dist_left + 2);
    #if head_turn_ratio ~= 1.12 - head turn 75 degrees right - +75deg
    #if head_turn_ratio ~= 0.88 - head turn 75 degrees left  - -75deg
    #if head_turn_ratio ~= 1.00 - head turn straight ahead
    #resting head position looking forward = 0 degrees
    #convert to degrees turn
    #
    #get eye angle using ratio of distance of iris center to outermost point and iris center to innermost point
    #inner
    left_eye_inner_dist = vectors[loi["left_eye_inner_center"]*2] - vectors[loi["left_iris_center"]*2];
    right_eye_inner_dist = vectors[loi["right_iris_center"]*2] - vectors[loi["right_eye_inner_center"]*2];
    #outer
    left_eye_outer_dist = vectors[loi["left_iris_center"]*2] - vectors[loi["left_eye_outer_center"]*2];
    right_eye_outer_dist = vectors[loi["right_eye_outer_center"]*2] - vectors[loi["right_iris_center"]*2];
    #ratios, again add 2 to make it all postive, no divide by 0 issue
    right_eye_turn_ratio =  (right_eye_inner_dist + 2) / (right_eye_outer_dist + 2);
    left_eye_turn_ratio =  (left_eye_inner_dist + 2) / (left_eye_outer_dist + 2);
    #eyes ratio - [0.998,1.002] - straight ahead
    #eyes ratio - 1.02 - full outside 30 degrees
    #     -right - outside is +30deg
    #     -left - outside is -30deg
    #eyes ratio - 0.98 - full inside 30 degrees
    #     -right - inside is -30deg
    #     -left - inside is +30deg
    #now we convert ratios to angles with linear functions - done on paper, y = mx+b and that shit
    head_angle = calculateHeadAngle(head_turn_ratio);
    right_eye_angle = calculateEyeAngle(right_eye_turn_ratio, False);
    left_eye_angle = calculateEyeAngle(left_eye_turn_ratio, True);

    #use whichever eye we can see better. So if head turns right, use left eye. If head turns left, use right eye
    if (head_angle > 0):
        closer_eye_angle = left_eye_angle
    else:
        closer_eye_angle = right_eye_angle
    line_of_sight_angle = head_angle + closer_eye_angle;
    print("Line of sight angle: {}".format(line_of_sight_angle))

    #     if line_of_sight_angle < 9 and line_of_sight_angle > -9):
    #         eye_contact = true
    #     else:
    #         eye_contact = false
    return line_of_sight_angle

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
                    if v == 0 and h == 0:
                        label = 1
#                    elif v == 0 and abs(h) == 5:
#                        label = 1
                    elif abs(v) == 10 and abs(h) == 15:
                        label = 0
#                    elif abs(v) == 0 and abs(h) == 15:
#                        label = 0
#                    elif abs(v) == 10 and abs(h) == 10:
#                        label = 0
                    else:
                        continue
                    #get vectors
                    idx = names.index(image)
                    vectors = df.iloc[idx].iloc[1:].to_numpy()
                    ang = [angle(vectors)]
                    print(image)
                    print(ang)
                    print(label)
                    print("\n")
                    #print(image)
                    #print(vectors)
                    vectors_small = vectors[loi_list].tolist()
                    e = [label]
                    e.extend(ang)
                    data = np.concatenate([data, [e]], axis=0)

sys.exit()
                    
#balance dataset
labels = data[:,0]
smallest_balanced_class = min(np.count_nonzero(labels == 0), np.count_nonzero(labels == 1))
small_idx = np.random.choice(np.argwhere(labels == 0)[:,0], size=smallest_balanced_class)
off_data = data[small_idx]
on_data = data[np.argwhere(labels == 1)[:,0]]
balanced_data = np.concatenate([on_data, off_data])

#split into train and test, balanced
train, test = train_test_split(balanced_data)
#num_total_examples = len(balanced_data)
#train_samples = int(num_total_examples * train_percent)
#test_samples = num_total_examples - train_samples
#X = balanced_data[:,1:]
#y = balanced_data[:,0]
#sss = StratifiedShuffleSplit(train_size=train_samples, n_splits=1,
#                             test_size=test_samples, random_state=0)
#
#for train_index, test_index in sss.split(X, y):
#    X_train, X_test = X[train_index], X[test_index]
#    y_train, y_test = y[train_index], y[test_index]

## split into input (X) and output (y) variables
#X_train = train[:,1:]
#X_test = test[:,1:]
#y_train = train[:,0]
#y_test = test[:,0]

# define the keras model
model = Sequential()
#model = multi_gpu_model(model, gpus=2)
model.add(Dense(200, input_dim=num_inputs, activation='relu'))
model.add(Dense(100))
model.add(Dense(12))
model.add(Dense(1, activation='sigmoid'))

# compile the keras model
model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
# fit the keras model on the dataset
model.fit(train[:,1:], train[:,0], epochs=1024, batch_size=32)
