Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background: 
    Given the configuration "data.json"
    And the poste bundles configuration "bundle_poste.json"

  Scenario: Execute a GetFees request with allCcp flag set to false
    Given initial json
      """
      {
        "paymentAmount": 670000000000000,
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
    And the body response does not contain the Poste bundles

  Scenario: Execute a GetFees request with allCcp flag set to true
    Given initial json
      """
      {
        "paymentAmount": 670000000000000,
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
    When the client send POST to /fees?maxOccurrences=10&allCcp=true
    Then check statusCode is 200
    And the body response contain the Poste bundles
