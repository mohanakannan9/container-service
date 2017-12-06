#!/bin/bash

die(){
    popd > /dev/null
    echo >&2 "$@"
    exit -1
}

username=${1?Please pass XNAT Wiki username and password as arguments to this script}
password=${2?Please pass XNAT Wiki username and password as arguments to this script}

docsdir=$(dirname "$0")
pushd $docsdir > /dev/null

mostRecentReleaseTag=$(git describe --abbrev=0 --tags)

# Make container-service-api.md from template and swagger.json
echo "Making container-service-api.md from template"
./make-container-service-api-md.py

# For each markdown file...
#     Convert it to HTML and confluence HTML
#     Upload it to wiki
for mdFilePath in $(ls *.md); do
    mdName=$(basename $mdFilePath)
    base=${mdName%.md}
    htmlName="${base}.html"
    confluenceName="${htmlName}.confluence"

    echo
    echo "Next file: $mdName"

    # Check if the file should be skipped
    if $(head -1 $mdName | grep 'NO UPLOAD' > /dev/null); then
        echo "${mdName} has requested NO UPLOAD. Skipping."
        continue
    fi

    if [[ "$base" == "container-service-api" ]]; then
        fileToCompare="swagger.json"
    else
        fileToCompare=${mdName}
    fi
    d=$(git diff ${mostRecentReleaseTag} ${fileToCompare}) || (echo "Failed to check if $fileToCompare has changed since last release. Skipping."; continue)

    if [[ -n "$d" ]]; then
        echo "$fileToCompare has changed since last release. Proceeding with conversion and upload."
    else
        echo "$fileToCompare has not changed since last release. Skipping."
        continue
    fi

    echo "Converting ${mdName} from markdown to confluence HTML"
    ./md2confluencehtml.sh ${mdName} || die "Failed to convert ${mdName} from markdown to confluence HTML"

    echo "Uploading converted ${mdName} to wiki"
    ./upload-confluence.py ${username} ${password} ${confluenceName} || die "Failed to upload ${confluenceName} to wiki"

done

echo "Cleaning up"
rm *.html
rm *.html.confluence
rm container-service-api.md

popd > /dev/null
