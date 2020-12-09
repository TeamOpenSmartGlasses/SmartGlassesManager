from deepface import DeepFace
import sys
import cv2
import numpy as np

class FaceAnalyze:
    def __init__(self):
        self.frame = None
        self.attributes = None

    def pass_frame(self, img):
        self.frame = img

    def get_attributes(self):
        #face detection and alignment
        obj = DeepFace.analyze(self.frame, actions = ['age', 'gender', 'race', 'emotion']) #find all attributes
        self.attributes = obj
        print(obj["age"]," years old ",obj["dominant_race"]," ",obj["dominant_emotion"]," ", obj["gender"])
        return self.attributes

#    def get_attributes(self):
#        #face detection and alignment
#        imgs_pixels, img_bbs = DeepFace.detectFaces(self.frame)
#        for face in imgs_pixels:
#            obj = DeepFace.analyze(face, actions = ['age', 'gender', 'race', 'emotion']) #find all attributes
#            print(obj["age"]," years old ",obj["dominant_race"]," ",obj["dominant_emotion"]," ", obj["gender"])

if __name__ == "__main__":
    fa = FaceAnalyze()
    img = cv2.imread(sys.argv[1])
    fa.pass_frame(img)
    res = fa.get_attributes()
    print(res)

