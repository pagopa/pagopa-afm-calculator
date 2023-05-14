locals {
  github = {
    org        = "pagopa"
    repository = "pagopa-afm-calculator"
  }

  prefix  = "pagopa"
  location_short = "weu"
  product = "${local.prefix}-${var.env_short}"

  app_name        = "github-${var.github.org}-${var.github.repository}-${var.prefix}-${var.domain}-${var.env}-aks"

  aks_cluster = {
    name           = "${local.product}-${local.location_short}-${var.env}-aks"
    resource_group = "${local.product}-${local.location_short}-${var.env}-aks-rg"
  }

  container_app_environment = {
    name           = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-cae",
    resource_group = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-rg",
  }
}

variable "env" {
  type = string
}

variable "env_short" {
  type = string
}

variable "domain" {
  type        = string
  description = "Domain name"
}

variable "github_repository_environment" {
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = list(string)
  })
  description = "GitHub Continuous Integration roles"
  default     = {
    protected_branches     = false
    custom_branch_policies = true
    reviewers_teams        = ["pagopa-tech"]
  }
}
