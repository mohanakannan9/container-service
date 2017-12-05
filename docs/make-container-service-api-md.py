#!/usr/bin/env python

import json

swaggerJsonFile = 'swagger.json'
mdTemplateFile = 'container-service-api.md.template'
mdFile = 'container-service-api.md'

with open(swaggerJsonFile, 'r') as f:
    swaggerJson = json.load(f)
swaggerJsonString = json.dumps(swaggerJson).replace('"', '\\"')

with open(mdTemplateFile, 'r') as f:
    mdTemplate = f.read()

md = mdTemplate.replace('%SWAGGER_JSON_HERE%', swaggerJsonString)

with open(mdFile, 'w') as f:
    f.write(md)
