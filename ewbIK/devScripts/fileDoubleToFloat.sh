#!/bin/bash

inFile=$1
outFile=$2
 
    
    #note, theis script is brittle, as it just assumes any thing ending  in d.java should be renamed to f.java in the output directory
    #make sure to manually cleanup any failed instances
    sed -e 's/sceneGraph.math.doubleV/sceneGraph.math.floatV/g' \
-e 's/Matrix4d/Matrix4f/g' \
-e 's/Matrix3d/Matrix3f/g' \
-e 's/SGVec_3d/SGVec_3f/g' \
-e 's/SGVec_2d/SGVec_2f/g' \
-e 's/sgRayd/sgRayf/g' \
-e 's/Vec2d/Vec2f/g' \
-e 's/Vec3d/Vec3f/g' \
-e 's/Vecd/Vecf/g' \
-e 's/double/float/g' \
-e 's/Double/Float/g' \
-e 's/\([0-9]\)d/\1f/g' \
-e 's/\.\([0-9]+\)/\.\1f/g' \
-e 's/FastMath\./MathUtils\./g' \
-e 's/Math\./MathUtils\./g' \
-e 's/Quaternion/Quaternionf/g' $inFile > $outFile
