#!/usr/bin/env python

"""upload-confluence
Upload a Confluence-formatted HTML document to the XNAT Wiki. Currently for updating only. To create a new post, do it in the web interface first.

Usage:
    upload-confluence.py <username> <password> <postId> <file>
    upload-confluence.py (-h | --help)
    upload-confluence.py --version

Options:
    -h --help   Show the usage
    --version   Show the version
    <username>  XNAT wiki username
    <password>  XNAT wiki password
    <file>      HTML file. Possibly generated from Markdown source by md2confluencehtml.sh.
    <postId>    The ID of an existing post on the Wiki.

"""

import requests
import os
import sys
import json
from docopt import docopt
import requests.packages.urllib3
requests.packages.urllib3.disable_warnings()

version = "1.0"
args = docopt(__doc__, version=version)
username = args['<username>']
password = args['<password>']
postId = args['<postId>']
filepath = args['<file>']

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
r = s.get(url)
if not r.ok:
    sys.exit("No post with id {}".format(postId))
print "Ok"

post = r.json()
# post['type']
# post['space']['key']
previousVersionNumber = post['version']['number']

newPostJson = {
    "id": postId,
    "type": post['type'],
    "title": post['title'],
    "space": {
        "key": post['space']['key']
    },
    "body": {
        "storage": {
            "representation": "storage",
            "value": content
        }
    },
    "version": {
        "number": previousVersionNumber + 1,
        "message": "Posting from update-confluence.py"
    }
}

print "Uploading new post..."
r = s.put(url, json=newPostJson)
if not r.ok:
    print "Upload failed"
    r.raise_for_status()

print "Done"