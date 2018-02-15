#!/bin/sh

die(){
    echo >&2 "$@"
    exit 1
}

find /input > /output/found-files.txt || die "FAILED find /input > /output/found-files.txt"
