import json

f = open('openapi.json')
data = json.load(f)

filteredPath = {}
for path in data['paths']:
    if '/fees' in path:
        filteredPath.update({path: data['paths'][path]})
data['paths'] = filteredPath

filteredTags = []
for tag in data['tags']:
    if 'Calculator' == tag['name']:
        filteredTags.append(tag)
data['tags'] = filteredTags

data['info']['title'] = 'PagoPA API Spontaneous Payment for Node'

with open('openapi-node.json', 'w') as f:
    json.dump(data, f, indent=2)
f.close()
