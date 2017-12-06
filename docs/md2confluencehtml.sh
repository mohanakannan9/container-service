#!/bin/bash

die(){
    popd > /dev/null
    echo >&2 "$@"
    exit -1
}

BASEDIR=$(dirname "$0")

MDFILE=$1
if [[ -z "$MDFILE" ]]; then
    echo "Missing arg: path to Markdown file" &>2
    exit 1
fi

HTMLFILE=${MDFILE%.md}.html

pandoc -o $HTMLFILE $MDFILE || die "Failed to convert $MDFILE from markdown to HTML"

gawk '/<pre><code>/,/<\/code><\/pre>/ {
    gsub(/&quot;/, "\"");
    gsub(/&lt;/, "<");
    gsub(/&gt;/, ">");
    gsub(/&#39;/, "");
    sub(/<pre><code>/, "<ac:structured-macro ac:name=\"code\"><ac:plain-text-body><![CDATA[");
    sub(/<\/code><\/pre>/, "]]></ac:plain-text-body></ac:structured-macro>");
}
{print}' $HTMLFILE > $HTMLFILE.confluence || die "Failed to convert HTML code blocks to Confluence macros"

$BASEDIR/html2confluence.py $HTMLFILE.confluence || die "Failed to run python HTML to Confluence HTML script"
