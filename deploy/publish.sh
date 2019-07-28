#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    gradle build publish --settings $GPG_DIR/settings.xml -Dci=true
    exit $?
fi
