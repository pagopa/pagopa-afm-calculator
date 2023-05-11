locals {
  github = {
    org        = "pagopa"
    repository = "pagopa-afm-calculator"
  }

  prefix  = "pagopa"
  location_short = "weu"
  domain = "afm"
  product = "${local.prefix}-${var.env_short}"

  app_name = "github-${local.github.org}-${local.github.repository}-${var.env}"

  aks_cluster = {
    name           = "${local.product}-${local.location_short}-${var.env}-aks"
    resource_group = "${local.product}-${local.location_short}-${var.env}-aks-rg"
  }

  container_app_environment = {
    name           = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-cae",
    resource_group = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-rg",
  }
}
