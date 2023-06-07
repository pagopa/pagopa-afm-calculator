const { Given, When, Then, BeforeAll, AfterAll, setDefaultTimeout } = require('@cucumber/cucumber')

const assert = require("assert");
const { call, post } = require("./common");
const fs = require("fs");
const tableStorageClient = require("./table_storage_client");

const afm_host = process.env.AFM_HOST;

/*increased the default timeout of the promise to allow
the correct execution of the smoke tests*/
setDefaultTimeout(15000);

let body;
let responseToCheck;
let validBundles = [];
let touchpoints = [];
let paymenttypes = [];

let isseuerEntity1 = {
  partitionKey: "300000",
  rowKey: "1005066",
  LOW_RANGE: "3095000000000000000",
  HIGH_RANGE: "3095999999999999999",
  CIRCUIT: "DINERS",
  PRODUCT_CODE: "N",
  PRODUCT_TYPE: "2",
  PRODUCT_CATEGORY: "C",
  ISSUER_ID: "100",
  ABI: "14156"
};
let isseuerEntity2 = {
  partitionKey: "309500",
  rowKey: "1005067",
  LOW_RANGE: "3095000000000000000",
  HIGH_RANGE: "3095999999999999999",
  CIRCUIT: "DINERS",
  PRODUCT_CODE: "N",
  PRODUCT_TYPE: "2",
  PRODUCT_CATEGORY: "C",
  ISSUER_ID: "100",
  ABI: "14156"
};

// Synchronous
BeforeAll(function() {
  tableStorageClient.setup(isseuerEntity1);
  tableStorageClient.setup(isseuerEntity2);
});

Given('the configuration {string}', async function(filePath) {
  let file = fs.readFileSync('./config/' + filePath);
  let config = JSON.parse(file);

  validBundles = mapToValidBundles(config);

  let result = await post(afm_host + '/configuration/bundles/add',
    validBundles);
  assert.strictEqual(result.status, 201);

  touchpoints = config["touchpoints"];
  let result2 = await post(afm_host + '/configuration/touchpoint/add',
    touchpoints);
  assert.strictEqual(result2.status, 201);

  paymenttypes = config["paymenttypes"];
  let result3 = await post(afm_host + '/configuration/paymenttypes/add',
    paymenttypes);
  assert.strictEqual(result3.status, 201);

});

Given(/^initial json$/, function(payload) {
  body = JSON.parse(payload);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/,
  async function(method, url) {
    responseToCheck = await call(method, afm_host + url, body)
  });

Then(/^check statusCode is (\d+)$/, function(status) {
  assert.strictEqual(responseToCheck.status, status);

});

Then(/^check response body is$/, function(payload) {
  assert.deepStrictEqual(responseToCheck.data, JSON.parse(payload));
});

Then('the body response ordering for the bundleOptions.onUs field is:', function (dataTable) {
  for (let i=0; i<responseToCheck.data.bundleOptions.length; i++){
    let bodyOnUs = responseToCheck.data.bundleOptions[i].onUs;
    let checkOnUs = JSON.parse(dataTable.rows()[i][0]);
    assert.equal(bodyOnUs, checkOnUs)
  }
});

Then('the body response does not contain the Poste idPsp', function () {
  for (let i=0; i<responseToCheck.data.bundleOptions.length; i++){
    let bodyPsp = responseToCheck.data.bundleOptions[i].idPsp;
    assert.notEqual(bodyPsp, process.env.ID_PSP_POSTE);
  }
});

function mapToValidBundles(config) {

  let validbundles = [];

  for (let bundle of config["bundles"]) {
    let validBundle = bundle;
    validBundle.ciBundleList = [];
    for (let cibundle of config["ciBundles"]) {
      if (cibundle.idBundle === bundle.id) {
        validBundle.ciBundleList.push(cibundle);
      }
    }
    validbundles.push(validBundle);
  }
  return validbundles;
}


// Asynchronous Promise
AfterAll(async function() {

   let result = await post(afm_host + '/configuration/bundles/delete',
     validBundles);
   assert.strictEqual(result.status, 200);

   let result2 = await post(afm_host + '/configuration/touchpoint/delete',
     touchpoints);
   assert.strictEqual(result2.status, 200);

   let result3 = await post(afm_host + '/configuration/paymenttypes/delete',
     paymenttypes);
   assert.strictEqual(result3.status, 200);

  return Promise.resolve()
});
