// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {addTouchpoints, deleteTouchpoints, getFeesMulti, mapToValidBundles, getValidBundle} from './helpers/calculator_helper.js';
import { createDocument, deleteDocument } from "./helpers/cosmosdb_client.js";

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`${__ENV.VARS}`)).environment;
});

const data = JSON.parse(open('./helpers/data.json'));
const touchpoints = data["touchpoints"];
const paymenttypes = data["paymenttypes"];

// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const rootUrl = `${vars.hostV2}`;
const cartPathApi = `${vars.cartPathApi}`;
const cosmosDBURI = `${vars.cosmosDBURI}`;
const databaseID = `${vars.databaseID}`;
const validBundlesNum = `${vars.validBundlesNum}`;

const cosmosPrimaryKey = `${__ENV.COSMOS_SUBSCRIPTION_KEY}`;


export function setup() {
    // 2. setup code (once)
    // The setup code runs, setting up the test environment (optional) and generating data
    // used to reuse code for the same VU

    for (let i = 0; i < touchpoints.length; i++) {
        let response = createDocument(cosmosDBURI, databaseID, "touchpoints", cosmosPrimaryKey, touchpoints[i], touchpoints[i]['name']);
        check(response, { "status is 201": (res) => (res.status === 201) });
    }

    for (let i = 0; i < paymenttypes.length; i++) {
        let response = createDocument(cosmosDBURI, databaseID, "paymenttypes", cosmosPrimaryKey, paymenttypes[i], paymenttypes[i]['name']);
        check(response, { "status is 201": (res) => (res.status === 201) });
    }

    for (let i = 0; i < validBundlesNum; i++) {
        let validBundle = getValidBundle("int-test-"+i);
        let response = createDocument(cosmosDBURI, databaseID, "validbundles", cosmosPrimaryKey, validBundle, validBundle['idPsp']);
        check(response, { "status is 201": (res) => (res.status === 201) });
    }

    // precondition is moved to default fn because in this stage
    // __VU is always 0 and cannot be used to create env properly
}

export default function calculator() {

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
    };

    // to give randomness to request in order to avoid caching
    const paymentAmount = Math.floor(Math.random() * (100 + __VU) % 100);
    //const primaryCreditorInstitution = '7777777777' + Math.floor(Math.random() * 10);
    const primaryCreditorInstitution = 'fiscalCode-' + Math.floor(Math.random() * 2) + 1;

    let payload = {
        "bin": "1005066",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": [],
        "paymentNotice": [
            {
                "primaryCreditorInstitution": primaryCreditorInstitution,
                "paymentAmount": paymentAmount,
                "transferList": [
                    {
                        "creditorInstitution": "fiscalCode-1",
                        "transferCategory": "TAX1"
                    }
                ]
            }
        ]
    }

    let response = getFeesMulti(rootUrl, cartPathApi, payload, params);

    check(response, {
        'getFeesMulti': (r) => r.status === 200,
    });

}

export function teardown() {
    // After All
    for (let i = 0; i < touchpoints.length; i++) {
        let response = deleteDocument(cosmosDBURI, databaseID, "touchpoints", cosmosPrimaryKey, touchpoints[i]['id'], touchpoints[i]["name"]);
	      check(response, { "status is 204": (res) => (res.status === 204) });
    }

    for (let i = 0; i < paymenttypes.length; i++) {
        let response = deleteDocument(cosmosDBURI, databaseID, "paymenttypes", cosmosPrimaryKey, paymenttypes[i]['id'], paymenttypes[i]["name"]);
        check(response, { "status is 204": (res) => (res.status === 204) });
    }

    for (let i = 0; i < validBundlesNum; i++) {
        let validBundle = getValidBundle("int-test-"+i);
        let response = deleteDocument(cosmosDBURI, databaseID, "validbundles", cosmosPrimaryKey, validBundle['id'], validBundle['idPsp']);
        check(response, { "status is 204": (res) => (res.status === 204) });
    }
}
