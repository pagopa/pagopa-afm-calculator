Feature: GetFeeByPsp - Get fee by CI, amount, method, touchpoint with specified PSP using V2 APIs

  Background: 
    Given the configuration "data.json"

  Scenario: Commission is higher than the sum of the fees (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-cart-1"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "77777777777"  |

  Scenario: Commission is lower than the sum of the fees (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-cart-2"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "77777777777"  |

  Scenario: No ciBundles present (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |

  Scenario: Multiple ciBundles present but one paymentNoticeItem has EC code not corresponding (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |

  Scenario: Multiple ciBundles present but only one element in the paymentNotice (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle  |
    | "int-test-cart-6"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode  |
    | "88888888888"  |

  Scenario: Multiple ciBundles and multiple paymentNoticeItems (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle1  | idCiBundle2  |
    | "int-test-cart-5"  | "int-test-cart-6"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode1  | feeCode2  |
    | "77777777777"  | "88888888888"  |

  Scenario: Multiple ciBundles present with multiple attributes, cartesian product is returned (psp id specified)
    Given initial json
      """
      {
        "bin": "309500",
        "paymentMethod": "CP",
        "touchpoint": "CHECKOUT",
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
    When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
    Then check statusCode is 200
    And the body response has one bundle for each psp
    And the body response for the bundleOptions.idsCiBundle field is:
    | idCiBundle1  | idCiBundle2  |
    | "int-test-cart-7"  | "int-test-cart-8"  |
    And the sum of the fees is correct and the EC codes are:
    | feeCode1  | feeCode2  |
    | "77777777777"  | "88888888888"  |

  Scenario: Multiple bundles are available, but only one is returned
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
                  "paymentAmount": 899999999999987,
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
      When the client send a V2 POST to /psps/PPAYITR1XXX/fees without parameters
      Then check statusCode is 200
      And the body response has one bundle for each psp
      And the body response ordering for the bundleOptions.onUs field for the "V2" API is:
      | onUs  |
      | true  |
      And the body response for the bundleOptions.idsCiBundle field is:
      | idCiBundle |
      | "int-test-cart-10" |
      And the sum of the fees is correct and the EC codes are:
      | feeCode  |
      | "77777777777"  |
