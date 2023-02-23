data "azurerm_storage_account" "tfstate_app" {
  name                = "tfapp${lower(replace(data.azurerm_subscription.current.display_name, "-", ""))}"
  resource_group_name = "terraform-state-rg"
}

data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_api_management_product" "product" {
  product_id          = "afm-calculator"
  api_management_name = "${local.product}-apim"
  resource_group_name = "${local.product}-api-rg"
}
