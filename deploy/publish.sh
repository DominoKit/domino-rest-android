#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    ../gradlew build publish
    exit $?
fi
