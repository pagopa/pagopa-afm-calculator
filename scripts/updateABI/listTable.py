from azure.data.tables import TableClient
from azure.data.tables import UpdateMode
import os
import sys

connection_string = os.environ['CONNECTION_STRING']
table_name = os.environ['TABLE_NAME']
table_client = TableClient.from_connection_string(conn_str=connection_string, table_name=table_name)

abi = sys.argv[1]
my_filter = (f"ABI eq '{abi}'")
entities = table_client.query_entities(my_filter)
for entity in entities:
    print(entity)