// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { getFeesByPsp, getFees } from './helpers/calculator_helper.js';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
	return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const optsConfiguration = varsArray[1];
const rootUrl = `${vars.host}`;

export default function calculator_getFeesByPsp() {

	const params = {
		headers: {
			'Content-Type': 'application/json',
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
