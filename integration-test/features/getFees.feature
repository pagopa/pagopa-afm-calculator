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
      "touchPoint": "CHECKOUT",
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 0.30,
        "primaryCiIncurredFee": 0.20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "4",
        "name": "pacchetto 4",
        "description": "pacchetto 4",
        "idCiBundle": "3",
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 0.90,
        "primaryCiIncurredFee": 0.00,
        "paymentMethod": "CP",
        "touchpoint": null,
        "idBundle": "3",
        "name": "pacchetto 3",
        "description": "pacchetto 3",
        "idCiBundle": null,
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 1.30,
        "primaryCiIncurredFee": 0.20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "1",
        "name": "pacchetto 1",
        "description": "pacchetto 1",
        "idCiBundle": "1",
        "idPsp": "88888888888"
      }
    ]
    """

  Scenario: Execute a GetFees request
    Given initial json
    """
    {
      "paymentAmount": 70,
      "primaryCreditorInstitution": "77777777777",
      "paymentMethod": "CP",
      "touchPoint": null,
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 0.30,
        "primaryCiIncurredFee": 0.20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "4",
        "name": "pacchetto 4",
        "description": "pacchetto 4",
        "idCiBundle": "3",
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 0.90,
        "primaryCiIncurredFee": 0.00,
        "paymentMethod": "CP",
        "touchpoint": null,
        "idBundle": "3",
        "name": "pacchetto 3",
        "description": "pacchetto 3",
        "idCiBundle": null,
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 1.30,
        "primaryCiIncurredFee": 0.20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "1",
        "name": "pacchetto 1",
        "description": "pacchetto 1",
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
      "touchPoint": null,
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 0.30,
        "primaryCiIncurredFee": 0.20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "4",
        "name": "pacchetto 4",
        "description": "pacchetto 4",
        "idCiBundle": "3",
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 0.90,
        "primaryCiIncurredFee": 0.00,
        "paymentMethod": "CP",
        "touchpoint": null,
        "idBundle": "3",
        "name": "pacchetto 3",
        "description": "pacchetto 3",
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
      "touchPoint": "IO",
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
    Then check errorCode is 200
    And check response body is
    """
    [
    ]
    """
