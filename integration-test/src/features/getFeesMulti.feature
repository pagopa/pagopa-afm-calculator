Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background: 
    Given the configuration "dataCart.json"

  Scenario: Execute a GetFeesMulti request
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "77777777777",
                "paymentAmount": 899999999999999,
                "transferList": [
                    {
                        "creditorInstitution": "77777777777",
                        "transferCategory": "TAX1"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200

  Scenario: Execute a GetFeesMulti request 2
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "77777777777",
                "paymentAmount": 899999999999997,
                "transferList": [
                    {
                        "creditorInstitution": "77777777777",
                        "transferCategory": "TAX1"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200

  Scenario: Execute a GetFeesMulti request 3
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "77777777777",
                "paymentAmount": 899999999998995,
                "transferList": [
                    {
                        "creditorInstitution": "77777777777",
                        "transferCategory": "TAX1"
                    }
                ]
            },
            {
                "primaryCreditorInstitution": "77777777778",
                "paymentAmount": 1000,
                "transferList": [
                    {
                        "creditorInstitution": "77777777778",
                        "transferCategory": "TAX2"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200

  Scenario: Execute a GetFeesMulti request 4
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "77777777777",
                "paymentAmount": 899999999998993,
                "transferList": [
                    {
                        "creditorInstitution": "77777777777",
                        "transferCategory": "TAX1"
                    }
                ]
            },
            {
                "primaryCreditorInstitution": "77777777778",
                "paymentAmount": 1000,
                "transferList": [
                    {
                        "creditorInstitution": "77777777778",
                        "transferCategory": "TAX2"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200

  Scenario: Execute a GetFeesMulti request 5
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "88888888888",
                "paymentAmount": 899999999999991,
                "transferList": [
                    {
                        "creditorInstitution": "88888888888",
                        "transferCategory": "TAX1"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200

  Scenario: Execute a GetFeesMulti request 6
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "paymentNotice": [
            {
                "primaryCreditorInstitution": "77777777777",
                "paymentAmount": 899999999998991,
                "transferList": [
                    {
                        "creditorInstitution": "77777777777",
                        "transferCategory": "TAX1"
                    }
                ]
            },
            {
                "primaryCreditorInstitution": "88888888888",
                "paymentAmount": 1000,
                "transferList": [
                    {
                        "creditorInstitution": "88888888888",
                        "transferCategory": "TAX1"
                    }
                ]
            }
        ]
      }
      """
    When the client send POST to /fees/multi?maxOccurrences=10
    Then check statusCode is 200
