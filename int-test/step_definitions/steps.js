const {Given, When, Then} = require('@cucumber/cucumber')
const assert = require("assert");


var body;

Given('bundles are available', async function () {
});

Given(/^CI attributes are available$/, function () {

});
Given(/^initial json$/, function (payload) {
    body = JSON.parse(payload);
});
When(/^the client send (GET|POST|PUT|DELETE) to (.*)$/, function (method, url) {

});
Then(/^check errorCode is (\d+)$/, function (status) {

});
Then(/^check response body is$/, function (payload) {

});
