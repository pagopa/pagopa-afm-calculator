const {Given, When, Then, After} = require('@cucumber/cucumber')
const assert = require("assert");
const {call} = require("./common");
const fs = require("fs");
const CosmosClient = require('@azure/cosmos').CosmosClient

const afm_host = process.env.AFM_HOST;
const cosmosdb_host = process.env.COSMOSDB_HOST;

const comsosdb_key = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
const client = new CosmosClient({endpoint: cosmosdb_host, key: comsosdb_key})


let body;
let responseToCheck;

Given('the configuration {string}', async function (filePath) {
    let file = fs.readFileSync('./config/' + filePath);
    let config = JSON.parse(file);

    const {database} = await client.databases.createIfNotExists({id: "db"});
    const {container} = await database.containers.createIfNotExists({id: "validbundles"});

    for (let bundle of config["bundles"]) {
        let validBundle = bundle;
        validBundle.ciBundleList = [];
        for (let cibundle of config["ciBundles"]) {
            if (cibundle.idBundle === bundle.id) {
                validBundle.ciBundleList.push(cibundle);
            }
        }
        container.items.create(validBundle);
    }

});

Given(/^initial json$/, function (payload) {
    body = JSON.parse(payload);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/, async function (method, url) {
    responseToCheck = await call(method, afm_host + url, body)
});

Then(/^check statusCode is (\d+)$/, function (status) {
    assert.strictEqual(responseToCheck.status, status);

});

Then(/^check response body is$/, function (payload) {
    assert.deepStrictEqual(responseToCheck.data, JSON.parse(payload));
});


// Asynchronous Promise
After(async function () {
    // perform some shared teardown
    const {database} = await client.databases.createIfNotExists({id: "db"});
    const {container} = await database.containers.createIfNotExists({id: "validbundles"});
    const { resources } = await container.items
        .query("SELECT * from validbundles WHERE validbundles.id like 'int-test-%'")
        .fetchAll();
    for (const bundle of resources) {
        await container.item(bundle.id).delete();
    }
    return Promise.resolve()
});
