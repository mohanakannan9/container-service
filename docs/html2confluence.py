#!/usr/bin/env python

import re
import sys

inputAndOutputFile = sys.argv[1]

xnatIssueLinkRe = re.compile(r'<a href="https:\/\/issues\.xnat\.org\/browse\/([^"]+?)">.+?<\/a>')
xnatIssueLinkReplacement = "<ac:structured-macro ac:name=\"jira\"><ac:parameter ac:name=\"server\">Neuroinformatics Research Group JIRA</ac:parameter><ac:parameter ac:name=\"columns\">key,summary,type,created,updated,due,assignee,reporter,priority,status,resolution</ac:parameter><ac:parameter ac:name=\"serverId\">cd48cfbe-36e3-3ab6-af43-5d0331c561fb</ac:parameter><ac:parameter ac:name=\"key\">\\1</ac:parameter></ac:structured-macro>"

headerAnchorRe = re.compile(r'<h(\d) id="(.*?)">')
headerAnchorReplacement = "<h\\1><ac:structured-macro ac:name=\"anchor\" ac:schema-version=\"1\" ac:macro-id=\"45f1c881-92ce-4c4d-a738-10958361db24\"><ac:parameter ac:name=\"\">\\2</ac:parameter></ac:structured-macro>"

anchorLinkRe = re.compile(r'<a href="#([^"]+?)">(.*?)<\/a>')
anchorLinkReplacement = "<ac:link ac:anchor=\"\\1\"><ac:plain-text-link-body><![CDATA[\\2]]></ac:plain-text-link-body></ac:link>"

wikiLinkRe = re.compile(r'<a href="https://wiki\.xnat\.org/display/[^/]+?/([^"]+?)">(.+?)</a>')
def wikiLinkReplacement(matchobj):
    return "<ac:link ac:tooltip=\"{0}\"><ri:page ri:content-title=\"{0}\" /><ac:plain-text-link-body><![CDATA[{1}]]></ac:plain-text-link-body></ac:link>".format(matchobj.group(1).replace('+', ' '), matchobj.group(2))

regexAndReplacements = ((xnatIssueLinkRe, xnatIssueLinkReplacement),
                        (headerAnchorRe, headerAnchorReplacement),
                        (anchorLinkRe, anchorLinkReplacement),
                        (wikiLinkRe, wikiLinkReplacement))

with open(inputAndOutputFile, "r") as f:
    confluenceOriginal = [line.strip() for line in f.readlines()]

confluenceProcessed = []
for line in confluenceOriginal:
    for regex, replacement in regexAndReplacements:
        line = regex.sub(replacement, line)
    confluenceProcessed.append(line)

with open(inputAndOutputFile, "w") as f:
    f.write('\n'.join(confluenceProcessed))
