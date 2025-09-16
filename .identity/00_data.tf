data "azurerm_user_assigned_identity" "identity_cd" {
  resource_group_name = "${local.product}-identity-rg"
  name                = "${local.product}-${local.domain}-01-github-cd-identity"
}

data "azurerm_user_assigned_identity" "identity_ci" {
  count               = var.env_short == "p" ? 0 : 1
  name                = "${local.product}-${local.domain}-01-github-ci-identity"
  resource_group_name = "${local.product}-identity-rg"
}

data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "azurerm_user_assigned_identity" "workload_identity_clientid" {
  name                = "afm-workload-identity"
  resource_group_name = "pagopa-${var.env_short}-${local.location_short}-${var.env}-aks-rg"
}

data "azurerm_user_assigned_identity" "identity_cd_01" {
  resource_group_name = "${local.product}-identity-rg"
  name                = "${local.product}-${local.domain}-job-01-github-cd-identity"
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_key_vault" "key_vault" {
  name                = "pagopa-${var.env_short}-kv"
  resource_group_name = "pagopa-${var.env_short}-sec-rg"
}

data "azurerm_key_vault" "domain_key_vault" {
  name                = "pagopa-${var.env_short}-${local.domain}-kv"
  resource_group_name = "pagopa-${var.env_short}-${local.domain}-sec-rg"
}

data "azurerm_key_vault_secret" "key_vault_sonar" {
  name         = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_bot_cd_token" {
  name         = "pagopa-platform-domain-github-bot-cd-pat"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_cucumber_token" {
  name         = "cucumber-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_sa_connection_string" {
  name         = "afm-${var.env_short}-sa-connection-string"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_afm_calculator_subkey" {
  count        = var.env_short != "p" ? 1 : 0

  name         = "integration-test-afm-calculator-subkey"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_afm_marketplace_subkey" {
  count        = var.env_short != "p" ? 1 : 0

  name         = "integration-test-afm-marketplace-subkey"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_deploy_slack_webhook" {
  name         = "pagopa-pagamenti-deploy-slack-webhook"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_slack_webhook" {
  name         = "pagopa-pagamenti-integration-test-slack-webhook"
  key_vault_id = data.azurerm_key_vault.domain_key_vault.id
}