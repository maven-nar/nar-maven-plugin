#!/bin/sh
curl -fsLO https://raw.githubusercontent.com/scijava/scijava-scripts/master/travis-build.sh
sh travis-build.sh $encrypted_f76761764219_key $encrypted_f76761764219_iv &&
if [ ! -f release.properties ]
then
  # Not a release -- also perform integration tests.
  mvn -Dinvoker.debug=true -Prun-its
fi

