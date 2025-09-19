// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import {check} from 'k6';
import {SharedArray} from 'k6/data';
import {getPaymentMethodById} from './helpers/calculator_helper.js';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
    return JSON.parse(open(`${__ENV.VARS}`)).environment;
});

const vars = varsArray[0];
const rootUrl = `${vars.hostV1}`;

export function setup() {}

export default function calculator() {

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
    };

    let response = getPaymentMethodById(rootUrl, 'PAYPAL', params);

    check(response, {
        'check status is 200': (r) => r.status === 200
    });

}

export function teardown() {}
