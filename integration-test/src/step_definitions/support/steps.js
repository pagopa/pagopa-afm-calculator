const {Given, When, Then} = require('@cucumber/cucumber')
const {AfterAll, BeforeAll} = require('@cucumber/cucumber');
const assert = require("assert");
const {call, get, post} = require("./common");
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
    assert.strictEqual(result.status, 201);
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
    console.log(responseToCheck.data)

    assert.deepStrictEqual(responseToCheck.data, JSON.parse(payload));
});


// Synchronous
BeforeAll(async function () {
    // perform some shared setup
    const result = await get(afm_data_host + '/configuration')
    console.log(result.data);
    fs.writeFile('./config/saved.json', JSON.stringify(result.data), function (err) {
        if (err) {
            return console.log(err);
        }
        console.log("The file was saved!");
    });

});

// Asynchronous Promise
AfterAll(async function () {
    // perform some shared teardown
    let file = fs.readFileSync('./config/saved.json');
    let config = JSON.parse(file);
    await post(afm_data_host + '/configuration', config);
    fs.unlinkSync('./config/saved.json');

    return Promise.resolve()
});
