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
        "touchPoint": "CHECKOUT",
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 130,
        "primaryCiIncurredFee": 20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "1",
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 80,
        "primaryCiIncurredFee": 0,
        "paymentMethod": "PO",
        "touchpoint": null,
        "idBundle": "2",
        "idCiBundle": null,
        "idPsp": "88888888888"
      },
      {
        "taxPayerFee": 130,
        "primaryCiIncurredFee": 20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "1",
        "idCiBundle": "1",
        "idPsp": "88888888888"
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 30,
        "primaryCiIncurredFee": 20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "4",
        "idCiBundle": "3",
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
        "touchPoint": "IO",
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
    Then check errorCode is 200
    And check response body is
    """
    [
      {
        "taxPayerFee": 30,
        "primaryCiIncurredFee": 20,
        "paymentMethod": null,
        "touchpoint": null,
        "idBundle": "4",
        "idCiBundle": "3",
        "idPsp": "88888888889"
      },
      {
        "taxPayerFee": 100,
        "primaryCiIncurredFee": 0,
        "paymentMethod": null,
        "touchpoint": "IO",
        "idBundle": "6",
        "idCiBundle": null,
        "idPsp": "88888888889"
      }
    ]
    """
