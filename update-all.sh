#!/bin/bash
if [ -e dist/calliope.core.jar ]; then
  cp dist/calliope.core.jar ../TimelineConverter/
  cp dist/calliope.core.jar ../TILT2/lib/
  cp dist/calliope.core.jar ../Project/lib/
  cp dist/calliope.core.jar ../MML/lib/
  cp dist/calliope.core.jar ../Pages/lib/
  cp dist/calliope.core.jar ../Annotator/lib/
else
  echo "rebuild calliope.core.jar first!"
fi

