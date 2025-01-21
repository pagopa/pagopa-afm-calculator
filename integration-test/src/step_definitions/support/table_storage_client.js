const { odata, TableClient, AzureNamedKeyCredential } = require("@azure/data-tables");


// azurite storage connection
/*
const account = "devstoreaccount1";   
const accountKeyAzurite = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="; 
const tableName = "issuerrangetable";
const credential = new AzureNamedKeyCredential(account, accountKeyAzurite);
const connectionString = "http://127.0.0.1:10002/devstoreaccount1";
const tableClient = new TableClient(connectionString, tableName, credential, { allowInsecureConnection: true });
*/

// storage account connection
const tableName = process.env.ISSUER_RANGE_TABLE // es. "issuerrangetable";
const connectionString = process.env.AFM_SA_CONNECTION_STRING
const tableClient = TableClient.fromConnectionString(connectionString, tableName);

async function setup (entity){
  await deleteByPK (entity.partitionKey);
  await createEntity (entity);
} 

async function deleteByPK(partitionKey) {
  if (partitionKey) {

    const entities = tableClient.listEntities({
      queryOptions: {
        filter: odata`PartitionKey eq ${partitionKey}`,
      },
    });

    for await (const entity of entities) {
      await tableClient.deleteEntity(entity.partitionKey, entity.rowKey);
    }
  }
}

async function createEntity(entity) {
  await tableClient.createEntity(entity);
}

module.exports = {
  deleteByPK, createEntity, setup
}