const { odata, TableClient, AzureNamedKeyCredential } = require("@azure/data-tables");

// storage account name and key
const account = "devstoreaccount1";
const accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
const tableName = "issuerrangetable";
//const connectionString = "DefaultEndpointsProtocol=https;AccountName=pagopadweuafmsa;AccountKey=RRDygFmNKKHpX+icQN4F3UdNzaoCgbbS4K8QDjFq0vJUCUKPpBv2DykLYm22OlqsIbZCPYNQJdEO+AStoX9jzw==;EndpointSuffix=core.windows.net"
const connectionString = "http://127.0.0.1:10002/devstoreaccount1";
// Use AzureNamedKeyCredential with storage account and account key
// AzureNamedKeyCredential is only available in Node.js runtime, not in browsers
const credential = new AzureNamedKeyCredential(account, accountKey);
//const tableClient = TableClient.fromConnectionString(connectionString, "myTable");
//const devConnectionString = "UseDevelopmentStorage=true";
//const tableClient = new TableClient(devConnectionString, "issuerrangetable");
const tableClient = new TableClient(connectionString, tableName, credential, { allowInsecureConnection: true });
//const tableStorage = createTableService(connectionString);


async function deleteByPK(partitionKey) {
  if (partitionKey) {

    const entities = tableClient.listEntities({
      queryOptions: {
        filter: odata`PartitionKey eq ${partitionKey}`,
      },
    });

    for await (const entity of entities) {
      console.log(entity.partitionKey, entity.rowKey);
      tableClient.deleteEntity(entity.partitionKey, entity.rowKey);
    }
  }
}

async function createEntity(entity) {
  await tableClient.createEntity(entity);
}

module.exports = {
  deleteByPK, createEntity
}