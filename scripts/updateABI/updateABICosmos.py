from azure.cosmos import exceptions, CosmosClient, PartitionKey

import os
import sys

url = os.environ["ACCOUNT_URI"]
key = os.environ["ACCOUNT_KEY"]
db = os.environ["ACCOUNT_DB"]
client = CosmosClient(url, key)
database = client.get_database_client(db)
container = database.get_container_client("validbundles")

old_abi = sys.argv[1]
new_abi = sys.argv[2]

queryText = "SELECT * FROM collection b WHERE b.abi = @abi"

results = container.query_items(
    query=queryText,
    parameters=[
        dict(
            name="@abi",
            value=old_abi,
        )
    ],
    enable_cross_partition_query=True,
)

num = 0
for result in results:
    print(result['id'])
    result['abi'] = new_abi
    container.replace_item(item=result, body=result)
    print("Successfull replacement")
    num +=1

print(num)





