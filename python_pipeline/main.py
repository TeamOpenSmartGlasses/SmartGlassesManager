import sys
import cv2

from face_analysis.face_emotion_age_gender_race import face_analyze
from detectron2_prebuilt_wrapper import Detectron

"""
NOTE: This main script is all working in BGR (that's what detectron2 is using). If we use another network in RGB or something else, then we will convert it in the pass_frame function of the wrapping class for the model's inference.
"""

#start ML model wrappers
detectron = Detectron()

#open video file/stream
video_file = sys.argv[1]
vid = cv2.VideoCapture(video_file)
fps = vid.get(cv2.CAP_PROP_FPS)
period = 1 / fps
video_time = 0

def get_vec(img, demo=False):
    """
    img : cv2 image, ussually a frame from video

    Returns:
    output of a number of computer vision / hearing algorithms
    """
    #pass into facial analysis class, get facial attributes
    res, res_img = face_analyze(img)
    if demo:
        cv2.imshow(res, "demo")
        cv2.waitKey(0)

    #pass into detectron2, get object recognition masks and human pose keypoints

    return res

#loop through frames of video
while True:
    ret, frame = vid.read()
    if not ret:
        break
    video_time = video_time + period
    #pass through pipeline
    hl_vec = get_vec(frame)
    print("HL Vec is: ")
    print(hl_vec)
    print("At video time: {}".format(video_time))
