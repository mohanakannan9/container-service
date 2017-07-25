#!/bin/bash

MDFILE=$1
if [[ -z "$MDFILE" ]]; then
    echo "Missing arg: path to Markdown file" &>2
    exit 1
fi

HTMLFILE=${MDFILE%.md}.html

pandoc -o $HTMLFILE $MDFILE




gawk '/<pre><code>/,/<\/code><\/pre>/ {
    gsub(/&quot;/, "\"");
    gsub(/&lt;/, "<");
    gsub(/&gt;/, ">");
    gsub(/&#39;/, "");
    sub(/<pre><code>/, "<ac:structured-macro ac:name=\"code\"><ac:plain-text-body><![CDATA[");
    sub(/<\/code><\/pre>/, "]]></ac:plain-text-body></ac:structured-macro>");
}
{
    r = gensub(/<a href="https:\/\/issues\.xnat\.org\/browse\/([^"]+)">[^<]+<\/a>/, "<ac:structured-macro ac:name=\"jira\"><ac:parameter ac:name=\"server\">Neuroinformatics Research Group JIRA</ac:parameter><ac:parameter ac:name=\"columns\">key,summary,type,created,updated,due,assignee,reporter,priority,status,resolution</ac:parameter><ac:parameter ac:name=\"serverId\">cd48cfbe-36e3-3ab6-af43-5d0331c561fb</ac:parameter><ac:parameter ac:name=\"key\">\\1</ac:parameter></ac:structured-macro>", "g");
    print r;
} ' $HTMLFILE > $HTMLFILE.confluence

