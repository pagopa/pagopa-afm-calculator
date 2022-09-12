Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background:
    Given the configuration "data.json"

  Scenario: Execute a GetFees request
    Given initial json
    """
    {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
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
        "idPsp": "88888888889"
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
        "idPsp": "88888888889"
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
        "idPsp": "88888888888"
      }
    ]
    """

  Scenario: Execute a GetFees request 2
    Given initial json
    """
    {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
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
        "idPsp": "88888888889"
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
        "idPsp": "88888888889"
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
        "idPsp": "88888888888"
      }
    ]
    """

  Scenario: Get List of fees by CI, amount, method and single PSP
    Given initial json
    """
    {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
      "paymentMethod": "CP",
      "touchpoint": null,
      "idPspList": ["88888888889"],
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
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 90,
        "primaryCiIncurredFee": 00,
        "paymentMethod": "CP",
        "touchpoint": "ANY",
        "idBundle": "3",
        "bundleName": "pacchetto 3",
        "bundleDescription": "pacchetto 3",
        "idCiBundle": null,
        "idPsp": "88888888889"
      }
    ]
    """

  Scenario: Get List of fees by CI, amount, touchpoint and single PSP
    Given initial json
    """
    {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
      "paymentMethod": null,
      "touchpoint": "IO",
      "idPspList": ["88888888888"],
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
    [
    ]
    """
