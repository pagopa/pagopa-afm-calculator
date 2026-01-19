Feature: Payment Methods - Get List of Payment Methods

  Background:
	Given the configuration "data.json"
  	Given the payment methods configuration "payment-methods-total-amount.json"

	Scenario: Search payment request using an amount out of bound for both test payment methods
		Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "IOS",
			"totalAmount": 200,
			"paymentNotice": [
				{
					"paymentAmount": 200,
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
		And the body response contains the added test payment methods but they are both disabled for AMOUNT_OUT_OF_BOUND

  Scenario: Search payment request using an incorrect amount
	  Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "IOS",
			"bin": "309500",
			"totalAmount": -10,
			"paymentNotice": [
				{
					"paymentAmount": 10,
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
	  And the body response contains the added test payment methods but they are both disabled for AMOUNT_OUT_OF_BOUND
	  And the the cart is first and others in alphabetic order



	Scenario: Search payment request using an amount in range for both test payment methods
		Given initial json
	  """
	  {
			"userTouchpoint": "IO",
			"userDevice": "IOS",
			"bin": "309500",
			"totalAmount": 5,
			"paymentNotice": [
				{
					"paymentAmount": 5,
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
		And the body response contains the added test payment methods enabled
		And the the cart is first and others in alphabetic order
