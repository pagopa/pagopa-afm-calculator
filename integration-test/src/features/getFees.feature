Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background: 
    Given the configuration "data.json"

  Scenario: Execute a GetFees request
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true  |
    | false |
    | false |
    
    

  Scenario: Execute a GetFees request 2
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": null,
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true  |
    | true  |
    | false |
    | false |
    | false |
    | false |
    | false |
    | false |

  Scenario: Get List of fees by CI, amount, method and single PSP
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": null,
        "idPspList": [{"idPsp":"PPAYITR1XXX"}],
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true |
    | false |
    

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": null,
        "touchpoint": "IO",
        "idPspList": [{"idPsp":"PPAYITR1XXX"}],
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true |
    | false |
    | false |
    

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 2
    Given initial json
      """
      {
      "paymentAmount": 799999999999998,
      "primaryCreditorInstitution": "77777777777",
      "bin": "309500",
      "paymentMethod": null,
      "touchpoint": "IO",
      "idPspList": [{"idPsp":"BPPIITRRXXX"}],
      "transferList": [
      {
        "creditorInstitution": "77777777777",
        "transferCategory": "TAX1",
        "digitalStamp": true
      },
      {
        "creditorInstitution": "77777777778",
        "transferCategory": "TAX2",
        "digitalStamp": false
      }
      ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And check response body is
      """
      {
      "belowThreshold": false,
      "bundleOptions": [
        {
            "taxPayerFee": 999999999999992,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "BPPIITRRXXX",
            "idChannel": "88888888899_01_ONUS",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156",
            "pspBusinessName": "psp business name int-test-7"
        }
      ]
      }
      """

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 3
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": null,
        "touchpoint": "IO",
        "idPspList": [{"idPsp":"BPPIITRRXXX"}],
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1",
            "digitalStamp": true
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2",
            "digitalStamp": true
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | false |
    | false |
   

  Scenario: Execute a GetFees request and above threshold
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": null,
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true  |
    | true  |
    | false |
    | false |
    | false |
    | false |
    | false |
    | false |
    

  Scenario: Execute a GetFees request with non-existing bin
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "123456789",
        "paymentMethod": "CP",
        "touchpoint": null,
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |

  Scenario: Execute a GetFees request with allCcp flag set to false
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": null,
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10&allCcp=false
    Then check statusCode is 200
    And the body response does not contain the Poste idPsp
    
    Scenario: Execute a GetFees request for AMEX payment
    Given initial json
      """
      {
        "paymentAmount": 799999999999998,
        "primaryCreditorInstitution": "77777777777",
        "bin": "340000",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
        "idPspList": null,
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1"
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2"
          }
        ]
      }
      """
    When the client send POST to /fees?maxOccurrences=10
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field for the "V1" API is:
    | onUs  |
    | true  |
