const {Given, When, Then, Before, AfterAll} = require('@cucumber/cucumber')
const assert = require("assert");
const {call, del, post} = require("./common");
const fs = require("fs");

const afm_host = process.env.AFM_HOST;


let body;
let responseToCheck;
let validBundles = [];
let touchpoints = [];

Given('the configuration {string}', async function (filePath) {
    let file = fs.readFileSync('./config/' + filePath);
    let config = JSON.parse(file);
    validBundles = mapToValidBundles(config);

    const result = await post(afm_host + '/configuration/bundles/add', validBundles);
    assert.strictEqual(result.status, 201);

    touchpoints = config["touchpoints"];
    const result2 = await post(afm_host + '/configuration/touchpoint/add', touchpoints);
    assert.strictEqual(result2.status, 201);
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
// AfterAll(async function () {
//     const result = await post(afm_host + '/configuration/bundles/delete', validBundles);
//     assert.strictEqual(result.status, 200);
//
//     const result2 = await post(afm_host + '/configuration/touchpoint/delete', touchpoints);
//     assert.strictEqual(result2.status, 200);
//     return Promise.resolve()
// });
