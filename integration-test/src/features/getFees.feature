Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background: 
    Given the configuration "data.json"

  Scenario: Execute a GetFees request
    Given initial json
      """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "bin": "1005066",
        "paymentMethod": "CP-test-1",
        "touchpoint": "CHECKOUT-test-1",
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
    And check response body is
      """
       {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 30,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-4",
            "bundleName": "pacchetto 4",
            "bundleDescription": "pacchetto 4",
            "idCiBundle": "int-test-3",
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 90,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "ANY",
            "idBundle": "int-test-3",
            "bundleName": "pacchetto 3",
            "bundleDescription": "pacchetto 3",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 130,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-1",
            "bundleName": "pacchetto 1",
            "bundleDescription": "pacchetto 1",
            "idCiBundle": "int-test-1",
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

  Scenario: Execute a GetFees request 2
    Given initial json
      """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "bin": "1005066",
        "paymentMethod": "CP-test-1",
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
    And check response body is
      """
      {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 30,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-4",
            "bundleName": "pacchetto 4",
            "bundleDescription": "pacchetto 4",
            "idCiBundle": "int-test-3",
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
          
            "taxPayerFee": 60,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-5",
            "bundleName": "pacchetto 5",
            "bundleDescription": "pacchetto 5",
            "idCiBundle": null,
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 90,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "ANY",
            "idBundle": "int-test-3",
            "bundleName": "pacchetto 3",
            "bundleDescription": "pacchetto 3",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-6",
            "bundleName": "pacchetto 6",
            "bundleDescription": "pacchetto 6",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-8",
            "bundleName": "pacchetto 8",
            "bundleDescription": "pacchetto 8",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 130,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-1",
            "bundleName": "pacchetto 1",
            "bundleDescription": "pacchetto 1",
            "idCiBundle": "int-test-1",
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

  Scenario: Get List of fees by CI, amount, method and single PSP
    Given initial json
      """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "bin": "1005066",
        "paymentMethod": "CP-test-1",
        "touchpoint": null,
        "idPspList": [{"idBrokerPsp":"88888888899","idChannel":"88888888899_01","idPsp":"88888888889"}],
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
    And check response body is
      """
      {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 30,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-4",
            "bundleName": "pacchetto 4",
            "bundleDescription": "pacchetto 4",
            "idCiBundle": "int-test-3",
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 90,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "ANY",
            "idBundle": "int-test-3",
            "bundleName": "pacchetto 3",
            "bundleDescription": "pacchetto 3",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-6",
            "bundleName": "pacchetto 6",
            "bundleDescription": "pacchetto 6",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-8",
            "bundleName": "pacchetto 8",
            "bundleDescription": "pacchetto 8",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """
  
  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
      """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "bin": "1005066",
        "paymentMethod": null,
        "touchpoint": "IO-test-2",
        "idPspList": [{"idBrokerPsp":"88888888899","idChannel":"88888888899_01","idPsp":"88888888888"}],
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
    And check response body is
      """
      {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 60,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-5",
            "bundleName": "pacchetto 5",
            "bundleDescription": "pacchetto 5",
            "idCiBundle": null,
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 80,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "PO-test-2",
            "touchpoint": "ANY",
            "idBundle": "int-test-2",
            "bundleName": "pacchetto 2",
            "bundleDescription": "pacchetto 2",
            "idCiBundle": null,
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 130,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-1",
            "bundleName": "pacchetto 1",
            "bundleDescription": "pacchetto 1",
            "idCiBundle": "int-test-1",
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 2
    Given initial json
      """
      {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
      "bin": "1005066",
      "paymentMethod": null,
      "touchpoint": "IO-test-2",
      "idPspList": [{"idBrokerPsp":"88888888899","idChannel":"88888888899_01","idPsp":"88888888889"}],
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
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 3
    Given initial json
      """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "bin": "1005066",
        "paymentMethod": null,
        "touchpoint": "IO-test-2",
        "idPspList": [{"idBrokerPsp":"88888888899","idChannel":"88888888899_01","idPsp":"88888888889"}],
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
    And check response body is
      """
     {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-8",
            "bundleName": "pacchetto 8",
            "bundleDescription": "pacchetto 8",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

  Scenario: Execute a GetFees request and above threshold
    Given initial json
      """
      {
        "paymentAmount": 70000,
        "primaryCreditorInstitution": "77777777777",
        "bin": "300000",
        "paymentMethod": "CP-test-1",
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
    And check response body is
      """
      {
    "belowThreshold": false,
    "bundleOptions": [
        {
            "taxPayerFee": 30,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-4",
            "bundleName": "pacchetto 4",
            "bundleDescription": "pacchetto 4",
            "idCiBundle": "int-test-3",
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 60,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-5",
            "bundleName": "pacchetto 5",
            "bundleDescription": "pacchetto 5",
            "idCiBundle": null,
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 90,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "CP-test-1",
            "touchpoint": "ANY",
            "idBundle": "int-test-3",
            "bundleName": "pacchetto 3",
            "bundleDescription": "pacchetto 3",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-6",
            "bundleName": "pacchetto 6",
            "bundleDescription": "pacchetto 6",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-7",
            "bundleName": "pacchetto 7",
            "bundleDescription": "pacchetto 7",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 100,
            "primaryCiIncurredFee": 0,
            "paymentMethod": "ANY",
            "touchpoint": "IO-test-2",
            "idBundle": "int-test-8",
            "bundleName": "pacchetto 8",
            "bundleDescription": "pacchetto 8",
            "idCiBundle": null,
            "idPsp": "88888888889",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        },
        {
            "taxPayerFee": 130,
            "primaryCiIncurredFee": 20,
            "paymentMethod": "ANY",
            "touchpoint": "ANY",
            "idBundle": "int-test-1",
            "bundleName": "pacchetto 1",
            "bundleDescription": "pacchetto 1",
            "idCiBundle": "int-test-1",
            "idPsp": "88888888888",
            "idChannel": "88888888899_01",
            "idBrokerPsp": "88888888899",
            "onUs": false,
            "abi": "14156"
        }
    ]
}
      """

#  Scenario: Execute a GetFees request with non-existing bin
#    Given initial json
#      """
#      {
#        "paymentAmount": 70000,
#        "primaryCreditorInstitution": "77777777777",
#        "bin": "123456789",
#        "paymentMethod": "CP-test-1",
#        "touchpoint": null,
#        "idPspList": null,
#        "transferList": [
#          {
#            "creditorInstitution": "77777777777",
#            "transferCategory": "TAX1"
#          },
#          {
#            "creditorInstitution": "77777777778",
#            "transferCategory": "TAX2"
#          }
#        ]
#      }
#      """
#    When the client send POST to /fees?maxOccurrences=10
#    Then check statusCode is 404