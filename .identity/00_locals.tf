locals {
  github = {
    org        = "pagopa"
    repository = "pagopa-afm-calculator"
  }
  product = "${var.prefix}-${var.env_short}"
}
