from azure.data.tables import TableClient
from azure.data.tables import UpdateMode
import os
import sys

connection_string = os.environ['CONNECTION_STRING']
table_name = os.environ['TABLE_NAME']
table_client = TableClient.from_connection_string(conn_str=connection_string, table_name=table_name)

old_abi = sys.argv[1]
new_abi = sys.argv[2]
filter_before_update = (f"ABI eq '{old_abi}'")
filter_after_update = (f"ABI eq '{new_abi}'")

entities = table_client.query_entities(filter_before_update)
for entity in entities:
    print(entity)
    entity["ABI"] = new_abi
    table_client.update_entity(mode=UpdateMode.MERGE, entity=entity)

entitiesAfterChange = table_client.query_entities(filter_after_update)
for entityAfterChange in entitiesAfterChange:
    print(entityAfterChange)