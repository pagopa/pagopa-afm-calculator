Feature: Payment Methods - Get List of Payment Methods

  Background: 
	Given the configuration "data.json"
  	Given the payment methods configuration "payment-methods-1.json"

	Scenario: Search payment request using userTouchpoint IO, which is not present in the payment methods configuration
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
		And the body response does not contain the added test payment methods

  Scenario: Search payment request using incorrect userTouchpoint value
	Given initial json
	  """
	  {
			"userTouchpoint": "IOp",
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
	Then check statusCode is 400
	And check response body is
      """
	   {
		"title": "BAD REQUEST",
		"status": 400,
		"detail": "Invalid input format"
	   }
      """