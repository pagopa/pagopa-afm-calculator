Feature: Payment Methods - Get List of Payment Methods

  Background: 
	Given the configuration "data.json"
  	Given the payment methods configuration "payment-methods-user-device-ok.json"

	Scenario: Search payment request using userDevice WEB, which is present in the payment methods configuration
		Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "WEB",
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
		When the client send POST to /payment-methods/search
		Then check statusCode is 200
		And the body response contains the added test payment methods