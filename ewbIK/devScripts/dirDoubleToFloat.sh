#!/bin/bash

inDir=$1
outDir=$2

for file in $inDir*
do

  outFile=$(basename $file)
  outFile=${outFile/d.java/f.java}
  echo $outFile
  
  if [ "$outFile" != "MathUtils.java" ]; then
  	if [ "$outFile" != "QCP.java" ]; then
  
    
    #note, theis script is brittle, as it just assumes any thing ending  in d.java should be renamed to f.java in the output directory
    #make sure to manually cleanup any failed instances
    #echo $file
   	bash fileDoubleToFloat.sh $file $outDir/$outFile
   fi
  fi
 
done