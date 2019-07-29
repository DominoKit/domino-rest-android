#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    ./gradlew build upload
    exit $?
fi
