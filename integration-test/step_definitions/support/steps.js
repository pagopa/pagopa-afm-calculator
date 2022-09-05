const {Given, When, Then} = require('@cucumber/cucumber')
const assert = require("assert");
const {call} = require("./common");


let body;
let responseToCheck;

Given('bundles are available', async function () {
    // TODO
});

Given(/^CI attributes are available$/, function () {
    // TODO
});

Given(/^initial json$/, function (payload) {
    body = JSON.parse(payload);
});

When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/, async function (method, url) {
    responseToCheck = await call(method, url, body)
    console.log(responseToCheck)
});

Then(/^check errorCode is (\d+)$/, function (status) {
    assert.strictEqual(responseToCheck.status, status);

});

Then(/^check response body is$/, function (payload) {
    assert.strictEqual(responseToCheck.data, payload);
});
