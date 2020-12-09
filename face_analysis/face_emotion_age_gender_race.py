import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), "Face_info"))
import f_Face_info
import cv2
import time

def face_analyze(img):
    #get passed in frame
    if type(img) == str:
        img = cv2.imread(sys.argv[1])
    # obtenego info del frame
    out = f_Face_info.get_face_info(img)
    # pintar imagen
    res_img = f_Face_info.bounding_box(out,img)
    return out, res_img

if __name__ == "__main__":
    img = cv2.imread(sys.argv[1])
    res, res_img = face_analyze(img)
    print(res)
