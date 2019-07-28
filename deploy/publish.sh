#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    ./gradlew build publish --settings $GPG_DIR/settings.xml -Dci=true
    exit $?
fi
