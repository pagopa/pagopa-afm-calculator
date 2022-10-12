import http from 'k6/http';
import { check, group, sleep } from 'k6';
import {calculator} from "./calculator";

import {calculator} from "./calculator.js";

// export const options = {
// 	stages: [
// 		{ duration: '5m', target: 100 }, // simulate ramp-up of traffic from 1 to 100 users over 5 minutes.
// 		{ duration: '10m', target: 100 }, // stay at 100 users for 10 minutes
// 		{ duration: '5m', target: 0 }, // ramp-down to 0 users
// 	],
// 	thresholds: {
// 		'http_req_duration': ['p(99)<150'], // 99% of requests must complete below 150ms
// 	},
// };


export const options = {
	scenarios: {
		load_scenario: {
			// name of the executor to use
			executor: 'per-vu-iterations',

			// common scenario configuration
			startTime: '10s',
			gracefulStop: '5s',

			// executor-specific configuration
			vus: 1,
			iterations: 1,
			maxDuration: '10s',
		},
	},
};

export default function () {
	calculator();
}
