Feature: GetFeeByPsp

  Background: 
    Given the configuration "data.json"

  Scenario: Get List of fees by CI, amount, method, touchpoint and single PSP
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
          "paymentMethod": "CP",
          "touchpoint": "CHECKOUT",
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
    When the client send POST to /psps/PPAYITR1XXX/fees
    Then check statusCode is 200
    And check response body is
      """
      {
        "belowThreshold": false,
        "bundleOptions": [
        {
          "taxPayerFee": 999999999999978,
          "primaryCiIncurredFee": 20,
          "paymentMethod": "ANY",
          "touchpoint": "ANY",
          "idBundle": "int-test-1",
          "bundleName": "pacchetto 1",
          "bundleDescription": "pacchetto 1",
          "idCiBundle": "int-test-1",
          "idPsp": "PPAYITR1XXX",
          "idBrokerPsp": "88888888899",
          "idChannel": "88888888899_01_ONUS",
          "onUs": false,
          "abi": "14156",
          "pspBusinessName": "psp business name int-test-1"
         }
       ]
      }
      """

  Scenario: Get List of fees by CI, amount, method and single PSP
    Given initial json
      """
        {
          "paymentAmount": 799999999999988,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
          "paymentMethod": "PO",
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
    When the client send POST to /psps/PPAYITR1XXX/fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
      | onUs  |
      | false |
      | false |

  Scenario: Get List of fees by CI, amount and single PSP
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
      | onUs  |
      | true  |
      | false |
      | false |
      | false |
      | false |

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
          "touchpoint": "IO",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
      | onUs  |
      | true  |
      | false |
      | false |
      | false |
      | false |

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 2
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
          "touchpoint": "IO",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And check response body is
      """
      {
        "belowThreshold": false,
        "bundleOptions":[
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
          "idBrokerPsp": "88888888899",
          "idChannel": "88888888899_01_ONUS",
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
          "touchpoint": "IO",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
      | onUs  |
      | false |
      | false |

  Scenario: Get List of fees by CI, amount, touchpoint, single PSP and above threshold
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "309500",
          "touchpoint": "IO",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
      | onUs  |
      | true  |
      | false |
      | false |
      | false |
      | false |

  Scenario: Get fee by psp with non-existing bin
    Given initial json
      """
        {
          "paymentAmount": 799999999999998,
          "primaryCreditorInstitution": "77777777777",
          "bin": "123456789",
          "touchpoint": "IO",
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
    When the client send POST to /psps/BPPIITRRXXX/fees
    Then check statusCode is 200
    And check response body is
      """
      {
        "belowThreshold": false,
        "bundleOptions":[]
      }
      """

  Scenario: Get List of fees by CI, amount, method, touchpoint and single PSP for AMEX payment
    Given initial json
      """
      {
        "paymentAmount": 799999999999990,
        "primaryCreditorInstitution": "77777777777",
        "bin": "340000",
        "paymentMethod": "CP",
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
    When the client send POST to /psps/UNCRITMM/fees
    Then check statusCode is 200
    And check response body is
      """
      {
        "belowThreshold": false,
        "bundleOptions": [
        {
          "taxPayerFee": 999999999999990,
          "primaryCiIncurredFee": 0,
          "paymentMethod": "CP",
          "touchpoint": "ANY",
          "idBundle": "int-test-10",
          "bundleName": "pacchetto 10",
          "bundleDescription": "pacchetto 10",
          "idCiBundle": null,
          "idPsp": "UNCRITMM",
          "idBrokerPsp": "88888888899",
          "idChannel": "UNCRITMM_ONUS",
          "onUs": true,
          "abi": "AMREX",
          "pspBusinessName": "psp business name int-test-10"
         }
       ]
      }
      """
