// 1. init code (once per VU)
// prepares the script: loading files, importing modules, and defining functions

import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { getFeesByPsp, getFees } from './helpers/calculator_helper.js';

// read configuration
// note: SharedArray can currently only be constructed inside init code
// according to https://k6.io/docs/javascript-api/k6-data/sharedarray
const varsArray = new SharedArray('vars', function () {
	const data = JSON.parse(open(`./${__ENV.VARS}`))
	return [data.environment[0], data.rampingVusNode];
});
// workaround to use shared array (only array should be used)
const vars = varsArray[0];
const optsConfiguration = varsArray[1];
const rootUrl = `${vars.host}`;

export const options = {
	discardResponseBodies: true,
	scenarios: {
		rampingVus: optsConfiguration,
	},
};

export default function calculator() {

	const params = {
		headers: {
			'Content-Type': 'application/json',
		},
	};

	// to give randomness to request in order to avoid caching
    const paymentAmount = Math.floor(Math.random() * __VU % 100);
    const primaryCreditorInstitution = 'fiscalCode-' + Math.floor(Math.random() * 2) + 1;

    let payload = {
        "paymentAmount": paymentAmount,
        "primaryCreditorInstitution": primaryCreditorInstitution,
		"paymentMethod": "CP",
		"touchpoint": "CHECKOUT",
		"idPspList": [],
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
	}
    response = getFees(rootUrl, payload, params);
	check(response, {
		'getFees': (r) => r.status === 200,
	});

}


export function handleSummary(data) {
	return { 'raw-data.json': JSON.stringify(data)};
}
