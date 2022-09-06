const {Given, When, Then} = require('@cucumber/cucumber')
const assert = require("assert");
const {call, post} = require("./common");
const fs = require("fs");

let rawdata = fs.readFileSync('./config/properties.json');
let properties = JSON.parse(rawdata);
const afm_host = properties.afm_host;
const afm_data_host = properties.afm_data_host;

let body;
let responseToCheck;

Given('the configuration {string}', async function (filePath) {
    let file = fs.readFileSync('./config/' + filePath);
    let config = JSON.parse(file);
    const result = await post(afm_data_host + '/configuration', config);
    console.log(result)
    assert.strictEqual(result.status, 201);
});

Given(/^initial json$/, function (payload) {
    body = JSON.parse(payload);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/, async function (method, url) {
    responseToCheck = await call(method, afm_host + url, body)
    console.log(responseToCheck)
});

Then(/^check errorCode is (\d+)$/, function (status) {
    assert.strictEqual(responseToCheck.status, status);

});

Then(/^check response body is$/, function (payload) {
    console.log(responseToCheck.data)
    assert.strictEqual(responseToCheck.data, JSON.parse(payload));
});
