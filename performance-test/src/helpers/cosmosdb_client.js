import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

var authorizationType      = "master"
var authorizationVersion   = "1.0";
var cosmosDBApiVersion     = "2018-12-31";

export function getDocumentById(cosmosDbURI, databaseId, containerId, authorizationSignature, id) {
	let path = `dbs/${databaseId}/colls/${containerId}/docs`;
	let resourceLink = `dbs/${databaseId}/colls/${containerId}`;
	// resource type (colls, docs...)
    let resourceType = "docs";
	let date = new Date().toUTCString();
	// request method (a.k.a. verb) to build text for authorization token
  let verb = 'post';
	let authorizationToken = getCosmosDBAuthorizationToken(verb,authorizationType,authorizationVersion,authorizationSignature,resourceType,resourceLink,date);
	
	let partitionKeyArray = [];
	let headers = getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, 'application/query+json');

  const body = {
    "query": "SELECT * FROM c where c.id=\"" + id + "\"",
    "parameters": []
  }

  return http.post(cosmosDbURI+path, body, {headers});
}

export function createDocument(cosmosDbURI, databaseId, containerId, authorizationSignature, document, pk) {
	let path = `dbs/${databaseId}/colls/${containerId}/docs`;
	let resourceLink = `dbs/${databaseId}/colls/${containerId}`;
	// resource type (colls, docs...)
	let resourceType = "docs"
	let date = new Date().toUTCString();
	// request method (a.k.a. verb) to build text for authorization token
  let verb = 'post';
	let authorizationToken = getCosmosDBAuthorizationToken(verb,authorizationType,authorizationVersion,authorizationSignature,resourceType,resourceLink,date);

	let partitionKeyArray = "[\""+pk+"\"]";
	let headers = getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, 'application/json');

	const body = JSON.stringify(document);

	let params = {
		headers: headers,
	};

  return http.post(cosmosDbURI+path, body, params)
}

export function deleteDocument(cosmosDbURI, databaseId, containerId, authorizationSignature, id, pk) {
	let path = `dbs/${databaseId}/colls/${containerId}/docs/${id}`;
	let resourceLink = path;
	// resource type (colls, docs...)
	let resourceType = "docs"
	let date = new Date().toUTCString();
	// request method (a.k.a. verb) to build text for authorization token
    let verb = 'delete';
	let authorizationToken = getCosmosDBAuthorizationToken(verb,authorizationType,authorizationVersion,authorizationSignature,resourceType,resourceLink,date);
	
	let partitionKeyArray = "[\""+pk+"\"]";
	let headers = getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, 'application/json');
	
	let params = {
		headers: headers,
	};
	
  return http.del(cosmosDbURI+path, null, params);
}



function getCosmosDBAPIHeaders(authorizationToken, date, partitionKeyArray, contentType){
	 
   return {'Accept': 'application/json',
   		 'Content-Type': contentType,
		 'Authorization': authorizationToken,
		 'x-ms-version': cosmosDBApiVersion,
		 'x-ms-date': date,
         'x-ms-documentdb-isquery': 'true',
         'x-ms-query-enable-crosspartition': 'true',
         'x-ms-documentdb-partitionkey': partitionKeyArray}; 
}

function getCosmosDBAuthorizationToken(verb,autorizationType,autorizationVersion,authorizationSignature,resourceType,resourceLink,dateUtc){ 
	// Decode authorization signature
	let key = encoding.b64decode(authorizationSignature);
    let text = (verb || "").toLowerCase() + "\n" +
            (resourceType || "").toLowerCase() + "\n" +
            (resourceLink || "") + "\n" +
            dateUtc.toLowerCase() + "\n\n";
    let hmacSha256 = crypto.createHMAC("sha256", key); 
    hmacSha256.update(text);
    // Build autorization token, encode it and return 
    return encodeURIComponent("type=" + autorizationType + "&ver=" + autorizationVersion + "&sig=" + hmacSha256.digest("base64"));  
}
