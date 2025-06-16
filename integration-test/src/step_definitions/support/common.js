const axios = require("axios");

axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY // for all requests
if (process.env.canary) {
  axios.defaults.headers.common['X-CANARY'] = 'canary' // for all requests
}

function get(url, config) {
  return axios.get(url, config)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function post(url, body, config) {
  return axios.post(url, body, config)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function put(url, body, config) {
  return axios.put(url, body, config)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function del(url, config) {
  return axios.delete(url, config)
  .then(res => {
    return res;
  })
  .catch(error => {
    return error.response;
  });
}

function call(method, url, body, config) {
  if (method === 'GET') {
    return get(url, config)
  }
  if (method === 'POST') {
    return post(url, body, config)
  }
  if (method === 'PUT') {
    return put(url, body, config)
  }
  if (method === 'DELETE') {
    return del(url, config)
  }

}

module.exports = {get, post, put, del, call}
