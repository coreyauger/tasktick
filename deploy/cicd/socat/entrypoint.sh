#!/bin/bash

socat TCP4-LISTEN:5000,fork,reuseaddr TCP4:$REG_IP:$REG_PORT
