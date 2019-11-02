#!/bin/bash

inFile=$1
outFile=$2
 
    
    #note, theis script is brittle, as it just assumes any thing ending  in d.java should be renamed to f.java in the output directory
    #make sure to manually cleanup any failed instances
    sed -e 's/sceneGraph.math.floatV/sceneGraph.math.doubleV/g' \
-e 's/Matrix4f/Matrix4d/g' \
-e 's/Matrix3f/Matrix3d/g' \
-e 's/SGVec_3f/SGVec_3d/g' \
-e 's/SGVec_2f/SGVec_2d/g' \
-e 's/sgRayf/sgRayd/g' \
-e 's/Vec2f/Vec2d/g' \
-e 's/Vec3f/Vec3d/g' \
-e 's/Vecf/Vecd/g' \
-e 's/float/double/g' \
-e 's/Float/Double/g' \
-e 's/\([0-9]\)f/\1d/g' \
-e 's/\.\([0-9]+\)/\.\1d/g' $inFile > $outFile

