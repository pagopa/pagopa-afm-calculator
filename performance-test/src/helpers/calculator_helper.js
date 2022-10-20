import http from 'k6/http';

export function getFeesByPsp(rootUrl, idPsp, payload, params) {
	const url = `${rootUrl}/psps/${idPsp}/fees`

    return http.post(url, JSON.stringify(payload), params);
}

export function getFees(rootUrl, payload, params) {
	const url = `${rootUrl}/fees`

    return http.post(url, JSON.stringify(payload), params);
}
