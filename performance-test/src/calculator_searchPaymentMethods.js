// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {searchPaymentMethods, getValidBundle} from './helpers/calculator_helper.js';
import { createDocument, deleteDocument } from "./helpers/cosmosdb_client.js";

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`${__ENV.VARS}`)).environment;
});

const vars = varsArray[0];
const rootUrl = `${vars.hostV1}`;
const cosmosDBURI = `${vars.cosmosDBURI}`;
const databaseID = `${vars.databaseID}`;
const validBundlesNum = `${vars.validBundlesNum}`;

const cosmosPrimaryKey = `${__ENV.COSMOS_SUBSCRIPTION_KEY}`;

export function setup() {
    for (let i = 0; i < validBundlesNum; i++) {
        let validBundle = getValidBundle("int-test-"+i);
        let response = createDocument(cosmosDBURI, databaseID, "validbundles", cosmosPrimaryKey, validBundle, validBundle['idPsp']);
        check(response, { "status is 201": (res) => (res.status === 201) });
    }
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
    const primaryCreditorInstitution = 'fiscalCode-' + Math.floor(Math.random() * 2) + 1;
    const touchpoints = ["IO", "CHECKOUT", "CHECKOUT_CART"]
    const userDevices = ["IOS", "ANDROID", "WEB"]
    const touchpointIndex = Math.floor(Math.random() * (2 + 1));
    const userDeviceIndex = Math.floor(Math.random() * (2 + 1));
    let payload = {
      "userTouchpoint": touchpoints[touchpointIndex],
      "userDevice": userDevices[userDeviceIndex],
      "totalAmount": paymentAmount,
      "paymentNotice": [
          {
              "paymentAmount": paymentAmount,
              "primaryCreditorInstitution": "77777777777",
              "transferList": [
                  {
                      "creditorInstitution": "777777777"
                  }
              ]
          }
      ],
      "allCCp": true
    }

    let response = searchPaymentMethods(rootUrl, payload, params);

    check(response, {
        'check status is 200': (r) => r.status === 200,
        'check payment method is not null': (r) => JSON.parse(r.body).paymentMethods != null,
    });

}

export function teardown() {
    for (let i = 0; i < validBundlesNum; i++) {
        let validBundle = getValidBundle("int-test-"+i);
        let response = deleteDocument(cosmosDBURI, databaseID, "validbundles", cosmosPrimaryKey, validBundle['id'], validBundle['idPsp']);
        check(response, { "status is 204": (res) => (res.status === 204) });
    }
}
