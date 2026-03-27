// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {getFeesMulti} from './helpers/calculator_helper.js';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`${__ENV.VARS}`)).environment;
});

// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const rootUrl = `${vars.hostV2}`;
const cartPathApi = `${vars.cartPathApi}`;

export default function calculator() {

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
    };

    // to give randomness to request in order to avoid caching
    const paymentAmount = Math.floor(Math.random() * (100 + __VU) % 100);
    //const primaryCreditorInstitution1 = '7777777777' + Math.floor(Math.random() * 10);
    //const primaryCreditorInstitution2 = '7777777777' + Math.floor(Math.random() * 10);
    const primaryCreditorInstitution = 'fiscalCode-' + Math.floor(Math.random() * 2) + 1;

    let payload = {
        "bin": "1005066",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": [],
        "paymentNotice": [
            {
                "primaryCreditorInstitution": primaryCreditorInstitution,
                "paymentAmount": paymentAmount/2,
                "transferList": [
                    {
                        "creditorInstitution": "fiscalCode-1",
                        "transferCategory": "TAX1"
                    }
                ]
            },
            {
                "primaryCreditorInstitution": primaryCreditorInstitution,
                "paymentAmount": paymentAmount/2,
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
