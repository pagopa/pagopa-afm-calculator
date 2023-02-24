import http from 'k6/http';

export function getFeesByPsp(rootUrl, idPsp, payload, params) {
	const url = `${rootUrl}/psps/${idPsp}/fees`

    return http.post(url, JSON.stringify(payload), params);
}

export function getFees(rootUrl, payload, params) {
	const url = `${rootUrl}/fees`

    return http.post(url, JSON.stringify(payload), params);
}

export function addTouchpoints(rootUrl, payload, params) {
	const url = `${rootUrl}/configuration/touchpoint/add`

    return http.post(url, JSON.stringify(payload), params);
}

export function deleteTouchpoints(rootUrl, payload, params) {
	const url = `${rootUrl}/configuration/touchpoint/delete`

    return http.post(url, JSON.stringify(payload), params);
}

export function mapToValidBundles(config) {

    let validbundles = [];

    for (let bundle of config["bundles"]) {
        let validBundle = bundle;
        validBundle.ciBundleList = [];
        for (let cibundle of config["ciBundles"]) {
            if (cibundle.idBundle === bundle.id) {
                validBundle.ciBundleList.push(cibundle);
            }
        }
        validbundles.push(validBundle);
    }
    return validbundles;
}
