Feature: GetFeeByPsp

 # Background: 
 #   Given the configuration "data.json"

#  Scenario: Get List of fees by CI, amount, method, touchpoint and single PSP
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "paymentMethod": "CP",
#          "touchpoint": "CHECKOUT",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888888/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions": [
#        {
#          "taxPayerFee": 999999999999978,
#          "primaryCiIncurredFee": 20,
#          "paymentMethod": "ANY",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-1",
#          "bundleName": "pacchetto 1",
#          "bundleDescription": "pacchetto 1",
#          "idCiBundle": "int-test-1",
#          "idPsp": "88888888888",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#         }
#       ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount, method and single PSP
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999988,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "paymentMethod": "PO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888888/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions": [
#        {
#          "taxPayerFee": 999999999999978,
#          "primaryCiIncurredFee": 20,
#          "paymentMethod": "ANY",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-1",
#          "bundleName": "pacchetto 1",
#          "bundleDescription": "pacchetto 1",
#          "idCiBundle": "int-test-1",
#          "idPsp": "88888888888",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        },
#        {
#          "taxPayerFee": 999999999999997,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "PO",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-2",
#          "bundleName": "pacchetto 2",
#          "bundleDescription": "pacchetto 2",
#          "idCiBundle": null,
#          "idPsp": "88888888888",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount and single PSP
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#      "belowThreshold": false,
#      "bundleOptions": [
#        {
#            "taxPayerFee": 999999999999996,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "CP",
#            "touchpoint": "ANY",
#            "idBundle": "int-test-3",
#            "bundleName": "pacchetto 3",
#            "bundleDescription": "pacchetto 3",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": true,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999975,
#            "primaryCiIncurredFee": 20,
#            "paymentMethod": "ANY",
#            "touchpoint": "ANY",
#            "idBundle": "int-test-4",
#            "bundleName": "pacchetto 4",
#            "bundleDescription": "pacchetto 4",
#            "idCiBundle": "int-test-3",
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999991,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-8",
#            "bundleName": "pacchetto 8",
#            "bundleDescription": "pacchetto 8",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999992,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-7",
#            "bundleName": "pacchetto 7",
#            "bundleDescription": "pacchetto 7",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999993,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-6",
#            "bundleName": "pacchetto 6",
#            "bundleDescription": "pacchetto 6",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "touchpoint": "IO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#      "belowThreshold": false,
#      "bundleOptions": [
#        {
#            "taxPayerFee": 999999999999996,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "CP",
#            "touchpoint": "ANY",
#            "idBundle": "int-test-3",
#            "bundleName": "pacchetto 3",
#            "bundleDescription": "pacchetto 3",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": true,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999975,
#            "primaryCiIncurredFee": 20,
#            "paymentMethod": "ANY",
#            "touchpoint": "ANY",
#            "idBundle": "int-test-4",
#            "bundleName": "pacchetto 4",
#            "bundleDescription": "pacchetto 4",
#            "idCiBundle": "int-test-3",
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999991,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-8",
#            "bundleName": "pacchetto 8",
#            "bundleDescription": "pacchetto 8",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999992,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-7",
#            "bundleName": "pacchetto 7",
#            "bundleDescription": "pacchetto 7",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999993,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-6",
#            "bundleName": "pacchetto 6",
#            "bundleDescription": "pacchetto 6",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 2
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "touchpoint": "IO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1",
#              "digitalStamp": true
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2",
#              "digitalStamp": false
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions":[
#        {
#          "taxPayerFee": 999999999999992,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "ANY",
#          "touchpoint": "IO",
#          "idBundle": "int-test-7",
#          "bundleName": "pacchetto 7",
#          "bundleDescription": "pacchetto 7",
#          "idCiBundle": null,
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount, touchpoint and single PSP 3
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "touchpoint": "IO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1",
#              "digitalStamp": true
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2",
#              "digitalStamp": true
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions":[
#        {
#            "taxPayerFee": 999999999999991,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-8",
#            "bundleName": "pacchetto 8",
#            "bundleDescription": "pacchetto 8",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999992,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-7",
#            "bundleName": "pacchetto 7",
#            "bundleDescription": "pacchetto 7",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get List of fees by CI, amount, touchpoint, single PSP and above threshold
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "300000",
#          "touchpoint": "IO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions":[
#        {
#          "taxPayerFee": 999999999999996,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "CP",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-3",
#          "bundleName": "pacchetto 3",
#          "bundleDescription": "pacchetto 3",
#          "idCiBundle": null,
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": true,
#          "abi": "14156"
#        },
#        {
#          "taxPayerFee": 999999999999975,
#          "primaryCiIncurredFee": 20,
#          "paymentMethod": "ANY",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-4",
#          "bundleName": "pacchetto 4",
#          "bundleDescription": "pacchetto 4",
#          "idCiBundle": "int-test-3",
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999991,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-8",
#            "bundleName": "pacchetto 8",
#            "bundleDescription": "pacchetto 8",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999992,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-7",
#            "bundleName": "pacchetto 7",
#            "bundleDescription": "pacchetto 7",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#          "taxPayerFee": 999999999999993,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "ANY",
#          "touchpoint": "IO",
#          "idBundle": "int-test-6",
#          "bundleName": "pacchetto 6",
#          "bundleDescription": "pacchetto 6",
#          "idCiBundle": null,
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        }
#      ]
#      }
#      """
#
#  Scenario: Get fee by psp with non-existing bin
#    Given initial json
#      """
#        {
#          "paymentAmount": 999999999999998,
#          "primaryCreditorInstitution": "77777777777",
#          "bin": "123456789",
#          "touchpoint": "IO",
#          "transferList": [
#            {
#              "creditorInstitution": "77777777777",
#              "transferCategory": "TAX1"
#            },
#            {
#              "creditorInstitution": "77777777778",
#              "transferCategory": "TAX2"
#            }
#          ]
#        }
#      """
#    When the client send POST to /psps/88888888889/fees
#    Then check statusCode is 200
#    And check response body is
#      """
#      {
#        "belowThreshold": false,
#        "bundleOptions":[
#         {
#          "taxPayerFee": 999999999999975,
#          "primaryCiIncurredFee": 20,
#          "paymentMethod": "ANY",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-4",
#          "bundleName": "pacchetto 4",
#          "bundleDescription": "pacchetto 4",
#          "idCiBundle": "int-test-3",
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999991,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-8",
#            "bundleName": "pacchetto 8",
#            "bundleDescription": "pacchetto 8",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#            "taxPayerFee": 999999999999992,
#            "primaryCiIncurredFee": 0,
#            "paymentMethod": "ANY",
#            "touchpoint": "IO",
#            "idBundle": "int-test-7",
#            "bundleName": "pacchetto 7",
#            "bundleDescription": "pacchetto 7",
#            "idCiBundle": null,
#            "idPsp": "88888888889",
#            "idChannel": "88888888899_01_ONUS",
#            "idBrokerPsp": "88888888899",
#            "onUs": false,
#            "abi": "14156"
#        },
#        {
#          "taxPayerFee": 999999999999993,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "ANY",
#          "touchpoint": "IO",
#          "idBundle": "int-test-6",
#          "bundleName": "pacchetto 6",
#          "bundleDescription": "pacchetto 6",
#          "idCiBundle": null,
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        },
#        {
#          "taxPayerFee": 999999999999996,
#          "primaryCiIncurredFee": 0,
#          "paymentMethod": "CP",
#          "touchpoint": "ANY",
#          "idBundle": "int-test-3",
#          "bundleName": "pacchetto 3",
#          "bundleDescription": "pacchetto 3",
#          "idCiBundle": null,
#          "idPsp": "88888888889",
#          "idBrokerPsp": "88888888899",
#          "idChannel": "88888888899_01_ONUS",
#          "onUs": false,
#          "abi": "14156"
#        } 
#      ]
#      }
#      """
