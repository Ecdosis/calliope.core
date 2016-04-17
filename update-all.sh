#!/bin/bash
if [ -e dist/calliope.core.jar ]; then
  cp dist/calliope.core.jar ../TimelineConverter/
  cp dist/calliope.core.jar ../TILT2/lib/
  cp dist/calliope.core.jar ../Project/lib/
  cp dist/calliope.core.jar ../MML/lib/
  cp dist/calliope.core.jar ../Pages/lib/
  cp dist/calliope.core.jar ../Annotator/lib/
  cp dist/calliope.core.jar ../Compare/lib/
  cp dist/calliope.core.jar ../Importer/lib/
  cp dist/calliope.core.jar ../Formatter/lib/
  cp dist/calliope.core.jar ../Psef/lib/
  cp dist/calliope.core.jar ../Search/lib/
  cp dist/calliope.core.jar ../Shared/lib/
  cp dist/calliope.core.jar ../MVD/lib/
  cp dist/calliope.core.jar ../Misc/lib/
  cp dist/calliope.core.jar ../Tree/lib/
  cp dist/calliope.core.jar ../Ratings/lib/
else
  echo "rebuild calliope.core.jar first!"
fi

