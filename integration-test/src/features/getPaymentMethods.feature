Feature: Payment Methods - Get List of Payment Methods

  Background: 
	Given the configuration "data.json"

  Scenario: Simple search payment request
	Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "IOS",
			"bin": "string",
			"totalAmount": 0,
			"paymentNotice": [
				{
					"paymentAmount": 1200,
					"primaryCreditorInstitution": "BPPIITRRZZZ",
					"transferList": [
						{
							"creditorInstitution": "BPPIITRRZZZ",
							"transferCategory": "TAX1"
						},
						{
							"creditorInstitution": "77777777778",
							"transferCategory": "TAX2"
						}
					]
				}
			],
			"allCCp": true,
			"targetKey": "string"
	  }
	  """
	When the client send a V2 POST to /payment-methods/search without parameters
	Then check statusCode is 200
#	And check response body is
#      """
#      {
#
#      }
#      """