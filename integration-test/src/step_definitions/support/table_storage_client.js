const { odata, TableClient, AzureNamedKeyCredential } = require("@azure/data-tables");


// azurite storage connection
/* const account = "devstoreaccount1";   
   const accountKey = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="; 
   const tableName = "issuerrangetable";
   const credential = new AzureNamedKeyCredential(account, accountKey);
   const tableClient = new TableClient(connectionString, tableName, credential, { allowInsecureConnection: true });
*/

// storage account connection
const tableName = process.env.ISSUER_RANGE_TABLE // es. "issuerrangetable";
const connectionString = process.env.AFM_SA_CONNECTION_STRING
console.log("********* connectionString:" + connectionString);
const tableClient = TableClient.fromConnectionString(connectionString, tableName);

async function setup (entity){
  console.log("*** start setup: " + entity.partitionKey + "***")
  await deleteByPK (entity.partitionKey);
  await createEntity (entity);
  console.log("*** end setup ***")
} 

async function deleteByPK(partitionKey) {
  if (partitionKey) {

    const entities = tableClient.listEntities({
      queryOptions: {
        filter: odata`PartitionKey eq ${partitionKey}`,
      },
    });

    for await (const entity of entities) {
      console.log("*** found entity to delete ["+entity.partitionKey+","+entity.rowKey+"] ***")
      await tableClient.deleteEntity(entity.partitionKey, entity.rowKey);
    }
  }
}

async function createEntity(entity) {
  console.log("*** entity to create ["+entity.partitionKey+","+entity.rowKey+"] ***")
  await tableClient.createEntity(entity);
}

module.exports = {
  deleteByPK, createEntity, setup
}