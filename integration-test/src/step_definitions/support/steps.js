const { Given, When, Then, BeforeAll, AfterAll, setDefaultTimeout } = require('@cucumber/cucumber')

const assert = require("assert");
const { call, post, del} = require("./common");
const fs = require("fs");
const tableStorageClient = require("./table_storage_client");

const afm_host = process.env.AFM_HOST;
const afm_host_V2 = process.env.AFM_HOST_V2;
const afm_api_extension_V2 = process.env.AFM_API_EXTENSION_V2;
const afm_marketplace_host = process.env.AFM_MARKETPLACE_HOST;

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
let isseuerEntity3 = {
  partitionKey: "340000",
  rowKey: "321087",
  LOW_RANGE: "3400000000000000000",
  HIGH_RANGE: "3499999999999999999",
  CIRCUIT: "AMEX",
  PRODUCT_CODE: "99",
  PRODUCT_TYPE: "3",
  PRODUCT_CATEGORY: "C",
  ISSUER_ID: "999999",
  ABI: "AMREX"
};

// Synchronous
BeforeAll(function() {
  tableStorageClient.setup(isseuerEntity1);
  tableStorageClient.setup(isseuerEntity2);
  tableStorageClient.setup(isseuerEntity3);
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

  // let paypalpaymentmethod = config["paymentmethods"][0];
  // let result4 = await post(afm_marketplace_host + '/payment-methods',
  //       paypalpaymentmethod);
  //   assert.strictEqual(result4.status, 201);
  //
  // let cardspaymentmethod = config["paymentmethods"][1];
  // let result5 = await post(afm_marketplace_host + '/payment-methods',
  //     cardspaymentmethod);
  // assert.strictEqual(result5.status, 201);

});

Given('the payment methods configuration {string}', async function(filePath) {
  let file = fs.readFileSync('./config/' + filePath);
  let config = JSON.parse(file);

  let paypalpaymentmethod = config["paymentmethods"][0];
  let result4 = await post(afm_marketplace_host + '/payment-methods',
      paypalpaymentmethod);
  assert.strictEqual(result4.status, 201);

  let cardspaymentmethod = config["paymentmethods"][1];
  let result5 = await post(afm_marketplace_host + '/payment-methods',
      cardspaymentmethod);
  assert.strictEqual(result5.status, 201);

});

Given(/^initial json$/, function(payload) {
  body = JSON.parse(payload);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/,
  async function(method, url) {
    responseToCheck = await call(method, afm_host + url, body)
  });

When(/^the client send a V2 (GET|POST|PUT|DELETE) to (.*) without parameters$/,
  async function(method, url) {
    responseToCheck = await call(method, afm_host_V2 + url + afm_api_extension_V2, body, {})
  });

When(/^the client send a V2 (GET|POST|PUT|DELETE) to (.*) with onUsFirst (true|false) and orderBy (random|fee|pspname)$/,
  async function(method, url, onUsFirst, orderBy) {
    const params = {}
    params.onUsFirst = onUsFirst
    params.orderBy = orderBy
    responseToCheck = await call(method, afm_host_V2 + url + afm_api_extension_V2, body, {params})
  });

Then(/^check statusCode is (\d+)$/, function(status) {
  assert.strictEqual(responseToCheck.status, status);

});

Then(/^check response body is$/, function(payload) {
  assert.deepStrictEqual(responseToCheck.data, JSON.parse(payload));
});

Then('the body response ordering for the bundleOptions.onUs field for the {string} API is:', function (version, dataTable) {
  // force the obtained list to be sorted by onUs field value if API version is V1
  if(version === "V1") {
    responseToCheck.data.bundleOptions.sort(function (a, b) {
  	    // true values first
      return (a.onUs === b.onUs)? 0 : a.onUs? -1 : 1;
    });
  }
  for (let i=0; i<dataTable.rows().length; i++){
    let bodyOnUs = responseToCheck.data.bundleOptions[i].onUs;
    let checkOnUs = JSON.parse(dataTable.rows()[i][0]);
    assert.equal(bodyOnUs, checkOnUs)
  }
});

Then('the body response has one bundle for each psp', function () {
  const idPsps = [];
  for (let i=0; i<responseToCheck.data.bundleOptions.length; i++){
    assert(!idPsps.includes(responseToCheck.data.bundleOptions[i].idPsp));
    idPsps.push(responseToCheck.data.bundleOptions[i].idPsp);
  }
});

Then('the body response does not contain the Poste idPsp', function () {
  for (let i=0; i<responseToCheck.data.bundleOptions.length; i++){
    let bodyPsp = responseToCheck.data.bundleOptions[i].idPsp;
    assert.notEqual(bodyPsp, process.env.ID_PSP_POSTE);
  }
});

Then('the body response for the bundleOptions.idsCiBundle field is:', function (dataTable) {
  for (let i=0; i<dataTable.rows().length; i++){
    for(let j=0; j<dataTable.rows()[i].length; j++){
      let bodyIdCiBundle = responseToCheck.data.bundleOptions[i].idsCiBundle[j];
      let checkIdCiBundle = JSON.parse(dataTable.rows()[i][j]);
      assert.equal(bodyIdCiBundle, checkIdCiBundle)
    }
  }
});

Then('the sum of the fees is correct and the EC codes are:', function (dataTable) {
  let sumFee = 0;
  for (let i=0; i<dataTable.rows().length; i++){
    responseToCheck.data.bundleOptions[i].fees.sort(function (a, b) {
        // alphabetical order
      return (a.creditorInstitution > b.creditorInstitution) ? 1 : ((b.creditorInstitution > a.creditorInstitution) ? -1 : 0);
    });
    for(let j=0; j<dataTable.rows()[i].length; j++){
      let bodyFeeCode = responseToCheck.data.bundleOptions[i].fees[j].creditorInstitution;
      let checkFeeCode = JSON.parse(dataTable.rows()[i][j]);
      assert.equal(bodyFeeCode, checkFeeCode);
      sumFee += responseToCheck.data.bundleOptions[i].fees[j].actualCiIncurredFee;
    }
    assert.equal(responseToCheck.data.bundleOptions[i].taxPayerFee - responseToCheck.data.bundleOptions[i].actualPayerFee, sumFee);
    sumFee = 0;
  }
});

Then('the body response for the bundleOptions.bundleId field is:', function (dataTable) {
  for (let i=0; i<dataTable.rows().length; i++){
    let idBundle = responseToCheck.data.bundleOptions[i].idBundle;
    let checkIdBundle = dataTable.rows()[i];
    assert.equal(idBundle, checkIdBundle)
  }
});

Then('the body response does not contain the added test payment methods', function () {
  for (let i=0; i<responseToCheck.data.paymentMethods.length; i++){
    let bodyPM = responseToCheck.data.paymentMethods[i].paymentMethodId;
    assert.notEqual(bodyPM, process.env.PAYPAL_PAYMENT_METHOD_TEST_NAME);
    assert.notEqual(bodyPM, process.env.CP_PAYMENT_METHOD_TEST_NAME);
  }
});

Then('the body response contains the added test payment methods', function () {
  // for (let i=0; i<responseToCheck.data.paymentMethods.length; i++){
  //   let bodyPM = responseToCheck.data.paymentMethods[i].paymentMethodId;
  //   assert.notEqual(bodyPM, process.env.PAYPAL_PAYMENT_METHOD_TEST_NAME);
  //   assert.notEqual(bodyPM, process.env.CP_PAYMENT_METHOD_TEST_NAME);
  // }
    assert(responseToCheck.data.paymentMethods.some(pm => pm.paymentMethodId === process.env.PAYPAL_PAYMENT_METHOD_TEST_NAME));
    assert(responseToCheck.data.paymentMethods.some(pm => pm.paymentMethodId === process.env.CP_PAYMENT_METHOD_TEST_NAME));
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

  let result2 = await del(afm_marketplace_host + '/payment-methods/CARDS-test' );
  //assert.strictEqual(result2.status, 200);

  let result3 = await del(afm_marketplace_host + '/payment-methods/PAYPAL-test' );
  //assert.strictEqual(result3.status, 200);

  return Promise.resolve()
});
