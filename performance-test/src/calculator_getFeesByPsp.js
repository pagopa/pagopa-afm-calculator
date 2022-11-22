// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import { check } from 'k6';
import { SharedArray } from 'k6/data';
import {getFeesByPsp, getFees, addTouchpoints, deleteTouchpoints} from './helpers/calculator_helper.js';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
	return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const rootUrl = `${vars.host}`;


export function setup() {
    // 2. setup code (once)
    // The setup code runs, setting up the test environment (optional) and generating data
    // used to reuse code for the same VU
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
    };
    const response = addTouchpoints(rootUrl, [{
        "id": "perf-test-1",
        "name": "CHECKOUT"
    }], params);

    check(response, {
        'setup': (r) => r.status === 201,
    });

    // precondition is moved to default fn because in this stage
    // __VU is always 0 and cannot be used to create env properly
}

export default function calculator_getFeesByPsp() {

	const params = {
		headers: {
			'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
	};

    // to give randomness to request in order to avoid caching
	const paymentAmount = Math.floor(Math.random() * (100 + __VU) % 100);
	const primaryCreditorInstitution = 'fiscalCode-' + Math.floor(Math.random() * 2) + 1;

	let payload = {
        "paymentAmount": paymentAmount,
        "primaryCreditorInstitution": primaryCreditorInstitution,
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "transferList": [
            {
                "creditorInstitution": "fiscalCode-1",
                "transferCategory": "TAX1"
            },
            {
                "creditorInstitution": "fiscalCode-2",
                "transferCategory": "TAX2"
            }
        ]
    };

    const idPsp = String(Math.floor(Math.random() * 10) + 1).padStart(11, '0');

	let response = getFeesByPsp(rootUrl, idPsp, payload, params);

	check(response, {
		'getFeesByPsp': (response) => response.status === 200,
	});
}

export function teardown() {
    // After All
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Ocp-Apim-Subscription-Key': __ENV.API_SUBSCRIPTION_KEY
        },
    };
    const response = deleteTouchpoints(rootUrl, [{
        "id": "perf-test-1",
        "name": "CHECKOUT"
    }], params);

    check(response, {
        'teardown': (r) => r.status === 200,
    });
}
