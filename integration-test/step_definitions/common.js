const axios = require("axios");
const fs = require('fs');

let rawdata = fs.readFileSync('./config/properties.json');
let properties = JSON.parse(rawdata);
const afm_host = properties.afm_host;

function get(url) {
    return axios.get(afm_host + url)
         .then(res => {
             return res;
         })
         .catch(error => {
             return error.response;
         });
}

function post(url, body) {
    return axios.post(afm_host + url, config, body)
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function put(url, body) {
    return axios.put(afm_host + url, body)
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}


function del(url) {
    return axios.delete(afm_host + url)
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

function call(method, url, body){
    return axios.call(method, afm_host + url, body)
        .then(res => {
            return res;
        })
        .catch(error => {
            return error.response;
        });
}

module.exports = {get, post, put, del, call}
