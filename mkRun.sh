#!/bin/bash

# Script that makes and runs the programs on the command line
javac -cp Packages/jars/\* *.java && clear && \
java -cp .:Packages/jars/\* TwitterAnalysis
