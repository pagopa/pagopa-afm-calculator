data "azurerm_storage_account" "tfstate_app" {
  name                = "pagopainfraterraform${var.env}"
  resource_group_name = "io-infra-rg"
}

data "azurerm_storage_account" "storage_account_afm" {
  name                = "pagopa${var.env_short}weuafmsa"
  resource_group_name = "pagopa-${var.env_short}-weu-${local.domain}-rg"
}

data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

#
# secrets
#

data "azurerm_key_vault" "key_vault" {
  count = var.env_short == "d" ? 1 : 0

  name                = "pagopa-${var.env_short}-kv"
  resource_group_name = "pagopa-${var.env_short}-sec-rg"
}

data "azurerm_key_vault" "domain_key_vault" {
  count = var.env_short != "p" ? 1 : 0

  name                = "pagopa-${var.env_short}-${local.domain}-kv"
  resource_group_name = "pagopa-${var.env_short}-${local.domain}-sec-rg"
}

data "azurerm_key_vault_secret" "key_vault_sonar" {
  count = var.env_short == "d" ? 1 : 0

  name         = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault[0].id
}

data "azurerm_key_vault_secret" "key_vault_bot_token" {
  count = var.env_short == "d" ? 1 : 0

  name         = "bot-token-github"
  key_vault_id = data.azurerm_key_vault.key_vault[0].id
}

data "azurerm_key_vault_secret" "key_vault_cucumber_token" {
  count = var.env_short == "d" ? 1 : 0

  name         = "cucumber-token"
  key_vault_id = data.azurerm_key_vault.key_vault[0].id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_subkey" {
  count        = var.env_short != "p" ? 1 : 0
  name         = "integration-test-subkey"
  key_vault_id = data.azurerm_key_vault.key_vault[0].id
}

data "azurerm_key_vault_secret" "key_vault_connection_string" {
  count = var.env_short != "p" ? 1 : 0

  name         = "afm-d-sa-connection-string"
  key_vault_id = data.azurerm_key_vault.domain_key_vault[0].id
}
