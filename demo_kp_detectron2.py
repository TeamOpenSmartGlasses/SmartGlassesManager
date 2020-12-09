# Copyright (c) Facebook, Inc. and its affiliates.
import argparse
import glob
import multiprocessing as mp
import os
import time
import cv2
import tqdm
import pathlib
import sys

from detectron2.config import get_cfg 
from detectron2.data.detection_utils import read_image
from detectron2.utils.logger import setup_logger

from predictor import VisualizationDemo

class DetectronPrebuilt:
    def __init__(self):
        self.frame = None

    def setup_cfg(self, config_file, opts, confidence_threshold):
        # load config from file and command-line arguments
        cfg = get_cfg()
        # To use demo for Panoptic-DeepLab, please uncomment the following two lines.
        # from detectron2.projects.panoptic_deeplab import add_panoptic_deeplab_config  # noqa
        # add_panoptic_deeplab_config(cfg)
        #Tensormask support 
        cfg.merge_from_file(config_file)
        cfg.merge_from_list(opts)
        # Set score_threshold for builtin models
        cfg.MODEL.RETINANET.SCORE_THRESH_TEST = confidence_threshold
        cfg.MODEL.ROI_HEADS.SCORE_THRESH_TEST = confidence_threshold
        cfg.MODEL.PANOPTIC_FPN.COMBINE.INSTANCES_CONFIDENCE_THRESH = confidence_threshold
        cfg.freeze()
        return cfg

    def setup_keypoints(self):
        mp.set_start_method("spawn", force=True)
        path = pathlib.Path(__file__).parent.absolute()
        config_file = os.path.join(path, "detectron2/configs/COCO-Keypoints/keypoint_rcnn_R_101_FPN_3x.yaml")
        opts = ["MODEL.WEIGHTS", os.path.join(path, "detectron2/models/keypoints_R101_FPN.pkl")]
        confidence_threshold = 0.5
        print(opts)
        setup_logger(name="fvcore")
        self.logger = setup_logger()

        self.cfg = self.setup_cfg(config_file, opts, confidence_threshold)
        self.demo = VisualizationDemo(self.cfg)

    def pass_frame(self, img):
        self.frame = img

    def get_keypoints(self):
        start_time = time.time()
        predictions, visualized_output = self.demo.run_on_image(self.frame)
        self.logger.info(
            "{} in {:.2f}s".format(
                "detected {} instances".format(len(predictions["instances"]))
                if "instances" in predictions
                else "finished",
                time.time() - start_time,
            )
        )
        return predictions, visualized_output

if __name__ == "__main__":
    WINDOW_NAME = "keypoint_demo"
    dt2 = DetectronPrebuilt()
    dt2.setup_keypoints()
    img = cv2.imread(sys.argv[1])
    dt2.pass_frame(img)
    keypoints, visualized_output = dt2.get_keypoints()
    print(keypoints)
    print(len(keypoints))
    cv2.namedWindow(WINDOW_NAME, cv2.WINDOW_NORMAL)
    cv2.imshow(WINDOW_NAME, visualized_output.get_image()[:, :, ::-1])
    while cv2.waitKey(0) != 27:
        pass # esc to quit
    #cv2.imshow("frame", visualized_output)
    #cv2.waitKey()
