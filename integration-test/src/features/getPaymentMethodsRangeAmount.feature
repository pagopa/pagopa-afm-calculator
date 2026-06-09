Feature: Payment Methods - Get List of Payment Methods

  Background: 
	Given the configuration "data.json"
  	Given the payment methods configuration "payment-methods-range-amount.json"

	Scenario: Search payment request for range amount
		Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "IOS",
			"totalAmount": 1200,
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
		And the body response contains the added test payment methods, only PAYPAL-test is disabled for AMOUNT_OUT_OF_BOUND
		And the the cart is first and others in alphabetic order
