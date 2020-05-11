#!/bin/sh

path="../logs/"
files=$(ls $path)
for filename in $files
do
    `truncate -s 0 $path$filename`
done
