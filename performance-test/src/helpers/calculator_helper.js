import http from 'k6/http';

export function getFeesByPsp(rootUrl, idPsp, payload, params) {
	const url = `${rootUrl}/psps/${idPsp}/fees`

    return http.post(url, JSON.stringify(payload), params);
}

export function getFees(rootUrl, payload, params) {
	const url = `${rootUrl}/fees`

    return http.post(url, JSON.stringify(payload), params);
}

export function getFeesByPspMulti(rootUrl, cartPathApi, idPsp, payload, params) {
	const url = `${rootUrl}/psps/${idPsp}/fees`.concat(cartPathApi)

    return http.post(url, JSON.stringify(payload), params);
}

export function getFeesMulti(rootUrl, cartPathApi, payload, params) {
	const url = `${rootUrl}/fees`.concat(cartPathApi)

    return http.post(url, JSON.stringify(payload), params);
}

export function searchPaymentMethods(rootUrl, payload, params) {
	const url = `${rootUrl}/payment-methods/search`

    return http.post(url, JSON.stringify(payload), params);
}

export function getPaymentMethodById(rootUrl, methodId, params) {
	const url = `${rootUrl}/payment-methods/${methodId}`

    return http.get(url, params);
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

export function getValidBundle(id) {
  const r = Math.random();
  return {
      "id": id,
      "idPsp": "88888888888",
      "idBrokerPsp": "88888888899",
      "idChannel": "88888888899_01",
      "abi": "BNLIITRR",
      "digitalStamp": false,
      "digitalStampRestriction": false,
      "name": "pacchetto 1",
      "description": "pacchetto 1",
      "paymentAmount": Math.floor(r * 150),
      "minPaymentAmount": 0,
      "maxPaymentAmount": Math.floor(r * 10000),
      "type": "GLOBAL",
      "validityDateFrom": null,
      "validityDateTo": null,
      "touchpoint": "ANY",
      "paymentType": null,
      "ciBundleList": [
          {
              "id": id,
              "ciFiscalCode": "77777777777",
              "idBundle": id,
              "attributes": [
                  {
                      "id": id,
                      "maxPaymentAmount": Math.floor(Math.random() * 500),
                      "transferCategory": "TAX1",
                      "transferCategoryRelation": "EQUAL"
                  }
              ]
          },
          {
              "id": id,
              "ciFiscalCode": "77777777777",
              "idBundle": id,
              "attributes": [
                  {
                      "id": id,
                      "maxPaymentAmount": Math.floor(r * 500),
                      "transferCategory": "TAX1",
                      "transferCategoryRelation": "EQUAL"
                  }
              ]
          }
      ],
  }
}
