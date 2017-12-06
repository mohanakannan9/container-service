#!/usr/bin/env python

"""upload-confluence
Upload a Confluence-formatted HTML document to the XNAT Wiki. Currently for updating only. To create a new post, do it in the web interface first.

Usage:
    upload-confluence.py [-i <postId>] <username> <password> <file> [<message>]...
    upload-confluence.py (-h | --help)
    upload-confluence.py --version

Options:
    -h --help   Show the usage
    --version   Show the version
    -i <postId> The ID of an existing post on the Wiki. If not supplied, the script will
                attempt to find the post ID as a comment on the first line of the post document, as
                <!-- id: <postId> -->
    <username>  XNAT wiki username
    <password>  XNAT wiki password
    <file>      Confluence-HTML file. Possibly generated from Markdown source by md2confluencehtml.sh.
    <message>   The rest of the arguments will be used as the edit message when posting the document.

"""

import requests
import os
import re
import sys
import json
from docopt import docopt
import requests.packages.urllib3
requests.packages.urllib3.disable_warnings()

version = "1.0"
args = docopt(__doc__, version=version)
username = args['<username>']
password = args['<password>']
postId = args.get('<postId>')
filepath = args['<file>']
messageList = args.get('<message>')

if not postId:
    with open(filepath) as f:
        firstline = f.readline()
        m = re.match('<!--.*id: *(.+?) *-->', firstline)
        if not m or not m.group(1):
            sys.exit("ERROR: No post id. Please pass a post ID argument or include it as a comment in the file, matching regex <!--.*[iI][dD]: *(.+?) *-->")
        postId = m.group(1)

if not messageList:
    message = "Posting from update-confluence.py"
else:
    message = " ".join(messageList)

s = requests.Session()
s.auth = (username, password)
host = 'https://wiki.xnat.org'
url = host + '/rest/api/content/{}'.format(postId)

# Load the file to upload
print "Load file"
with open(filepath) as f:
    content = f.read()
print "Ok"

# Get the existing post. We will need some information about it.
print "Get old post"
r = s.get(url, params={"expand": "version,space"})
if not r.ok:
    errorMessage = ''
    if r.text:
        try:
            errorMessageJson = json.loads(r.text)
            errorMessage = ':\n' + errorMessageJson.get('message', '')
        except:
            # Ignored
            pass

    sys.exit("ERROR getting post with id {}{}".format(postId, errorMessage))
print "Ok"

post = r.json()
# post['type']
# post['space']['key']
previousVersionNumber = post.get('version', {}).get('number', '')
if not previousVersionNumber:
    sys.exit("ERROR: Could not get version number of post {}".format(postId))
spaceKey = post.get('space', {}).get('key', '')
if not previousVersionNumber:
    sys.exit("ERROR: Could not get space key of post {}".format(postId))

newPostJson = {
    "id": postId,
    "type": post['type'],
    "title": post['title'],
    "space": {
        "key": spaceKey
    },
    "body": {
        "storage": {
            "representation": "storage",
            "value": content
        }
    },
    "version": {
        "number": previousVersionNumber + 1,
        "message": message
    }
}

print "Uploading..."
r = s.put(url, json=newPostJson)
if not r.ok:
    print "Upload failed"
    try:
        rj = r.json()
        if rj.get("statusCode"):
            print "CODE: %s" % rj.get("statusCode")
        if rj.get("message"):
            print "ERROR: %s" % rj.get("message")
    except:
        print "ERROR: %s" % r.text
    r.raise_for_status()

print "Done"