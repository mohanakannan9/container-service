#!/bin/sh

die(){
    echo >&2 "$@"
    exit 1
}

cp /input/* /output || die "FAILED cp /input/* /output"
