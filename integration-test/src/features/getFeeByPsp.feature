Feature: GetFeeByPsp

  Background:
    Given the configuration "data.json"

  Scenario: Get List of fees by CI, amount, method, touchpoint and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
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
    When the client send POST to /psps/88888888888/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 130,
        "primaryCiIncurredFee": 20,
        "paymentMethod": "ANY",
        "touchpoint": "ANY",
        "idBundle": "1",
        "bundleName": "pacchetto 1",
        "bundleDescription": "pacchetto 1",
        "idCiBundle": "1",
        "idPsp": "88888888888",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      }
    ]
    """

  Scenario: Get List of fees by CI, amount, method and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
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
    When the client send POST to /psps/88888888888/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 80,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "PO",
        "touchpoint": "ANY",
        "idBundle": "2",
        "bundleName": "pacchetto 2",
        "bundleDescription": "pacchetto 2",
        "idCiBundle": null,
        "idPsp": "88888888888",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      },
      {
        "taxPayerFee": 130,
        "primaryCiIncurredFee": 20,
        "paymentMethod": "ANY",
        "touchpoint": "ANY",
        "idBundle": "1",
        "bundleName": "pacchetto 1",
        "bundleDescription": "pacchetto 1",
        "idCiBundle": "1",
        "idPsp": "88888888888",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      }
    ]
    """

  Scenario: Get List of fees by CI, amount and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
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
    When the client send POST to /psps/88888888889/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 30,
        "primaryCiIncurredFee": 20,
        "paymentMethod": "ANY",
        "touchpoint": "ANY",
        "idBundle": "4",
        "bundleName": "pacchetto 4",
        "bundleDescription": "pacchetto 4",
        "idCiBundle": "3",
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      },
      {
        "taxPayerFee": 90,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "CP",
        "touchpoint": "ANY",
        "idBundle": "3",
        "bundleName": "pacchetto 3",
        "bundleDescription": "pacchetto 3",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01",
        "onUS": false
      },
      {
        "taxPayerFee": 100,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "ANY",
        "touchpoint": "IO",
        "idBundle": "6",
        "bundleName": "pacchetto 6",
        "bundleDescription": "pacchetto 6",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      }
    ]
    """

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
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
    When the client send POST to /psps/88888888889/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 30,
        "primaryCiIncurredFee": 20,
        "paymentMethod": "ANY",
        "touchpoint": "ANY",
        "idBundle": "4",
        "bundleName": "pacchetto 4",
        "bundleDescription": "pacchetto 4",
        "idCiBundle": "3",
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      },
      {
        "taxPayerFee": 90,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "CP",
        "touchpoint": "ANY",
        "idBundle": "3",
        "bundleName": "pacchetto 3",
        "bundleDescription": "pacchetto 3",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01",
        "onUS": false
      },
      {
        "taxPayerFee": 100,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "ANY",
        "touchpoint": "IO",
        "idBundle": "6",
        "bundleName": "pacchetto 6",
        "bundleDescription": "pacchetto 6",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01"
      }
    ]
    """
  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "touchpoint": "IO",
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1",
            "marcaBolloDigitale": true
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2",
            "marcaBolloDigitale": false
          }
        ]
      }
    """
    When the client send POST to /psps/88888888889/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 100,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "CP",
        "touchpoint": "IO",
        "idBundle": "7",
        "bundleName": "pacchetto 7",
        "bundleDescription": "pacchetto 7",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01",
        "onUS": false
      }
    ]
    """
  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
    """
      {
        "paymentAmount": 70,
        "primaryCreditorInstitution": "77777777777",
        "touchpoint": "IO",
        "transferList": [
          {
            "creditorInstitution": "77777777777",
            "transferCategory": "TAX1",
            "marcaBolloDigitale": true
          },
          {
            "creditorInstitution": "77777777778",
            "transferCategory": "TAX2",
            "marcaBolloDigitale": true
          }
        ]
      }
    """
    When the client send POST to /psps/88888888889/fees
    Then check statusCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 100,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "CP",
        "touchpoint": "IO",
        "idBundle": "8",
        "bundleName": "pacchetto 8",
        "bundleDescription": "pacchetto 8",
        "idCiBundle": null,
        "idPsp": "88888888889",
        "idBrokerPsp": "88888888899",
        "idChannel": "88888888899_01",
        "onUS": false
      }
    ]
    """