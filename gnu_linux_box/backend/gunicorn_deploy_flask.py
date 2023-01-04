#! /usr/bin/python3.8

import logging
import sys
import os

logging.basicConfig(stream=sys.stderr)
sys.path.insert(0, '/var/www/html/WearableIntelligenceSystem/gnu_linux_box/backend')

print("****************** exec is {}".format(sys.executable))

from main_webserver import app

if __name__ == "__main__":
    app.run(host='0.0.0.0')
