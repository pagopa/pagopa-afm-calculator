Feature: GetFees - Get List of fees by CI, amount, method, touchpoint

  Background: 
    Given the configuration "dataCart.json"

  Scenario: Commission is higher than the sum of the fees
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-1"  |
    | "int-test-9"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "77777777777"  |
    | "77777777777"  |

  Scenario: Commission is lower than the sum of the fees
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-2"  |
    | "int-test-9"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "77777777777"  |
    | "77777777777"  |

  Scenario: No ciBundles present
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |

  Scenario: Multiple ciBundles present but one paymentNoticeItem has EC code not corresponding
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |

  Scenario: Multiple ciBundles present but only one element in the paymentNotice
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-6"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "88888888888"  |

  Scenario: Multiple ciBundles and multiple paymentNoticeItems
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle1  | idCiBundle2  |
    | "int-test-5"  | "int-test-6"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode1  | feeCode2  |
    | "77777777777"  | "88888888888"  |

  Scenario: Multiple ciBundles present with multiple attributes, cartesian product is returned
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
                "paymentAmount": 899999999998989,
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
    When the client send a V2 POST to /fees
    Then check statusCode is 200
    And the body response ordering for the bundleOptions.onUs field is:
    | onUs  |
    | true  |
    | true  |
    | true  |
    | true  |
    | false |
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle1  | idCiBundle2  |
    | "int-test-7"  | "int-test-8"  |
    | "int-test-7"  | "int-test-8"  |
    | "int-test-7"  | "int-test-8"  |
    | "int-test-7"  | "int-test-8"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode1  | feeCode2  |
    | "77777777777"  | "88888888888"  |
    | "77777777777"  | "88888888888"  |
    | "77777777777"  | "88888888888"  |
    | "77777777777"  | "88888888888"  |
