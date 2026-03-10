#############################
## Product AFM Calculator ##
#############################

locals {
  apim_afm_calculator_service_api = {
    display_name          = "AFM Calculator pagoPA - calculator of advanced fees management service API"
    description           = "Calculator API to support advanced fees management service"
    path                  = "afm/calculator-service"
    product_id            = "afm-calculator"
    subscription_required = true
    service_url           = null
  }

  apim_afm_calculator_service_node_api = {
    display_name          = "AFM Calculator pagoPA for Node - calculator of advanced fees management service API"
    description           = "Calculator API to support advanced fees management service"
    path                  = "afm/node/calculator-service"
    product_id            = "afm-node-calculator"
    subscription_required = true
    service_url           = null
  }
}


###########################
##  API AFM Calculator  ##
###########################

resource "azurerm_api_management_api_version_set" "api_afm_calculator_api" {

  name                = format("%s-afm-calculator-service-api", var.env_short)
  resource_group_name = local.pagopa_apim_rg
  api_management_name = local.pagopa_apim_name
  display_name        = local.apim_afm_calculator_service_api.display_name
  versioning_scheme   = "Segment"
}


module "apim_api_afm_calculator_api_v1" {
  source = "./.terraform/modules/__v3__/api_management_api"

  name                  = format("%s-afm-calculator-service-api", local.project)
  api_management_name   = local.pagopa_apim_name
  resource_group_name   = local.pagopa_apim_rg
  product_ids           = [local.apim_afm_calculator_service_api.product_id]
  subscription_required = local.apim_afm_calculator_service_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_afm_calculator_api.id
  api_version           = "v1"

  description  = local.apim_afm_calculator_service_api.description
  display_name = local.apim_afm_calculator_service_api.display_name
  path         = local.apim_afm_calculator_service_api.path
  protocols    = ["https"]
  service_url  = local.apim_afm_calculator_service_api.service_url

  content_format = "openapi"
  content_value = templatefile(var.env_short != "p" ? "../openapi/openapi-v1-dev-uat.json" : "../openapi/openapi-v1.json", {
    host = local.apim_hostname
  })

  xml_content = templatefile("./api/calculator-service/v1/_base_policy.xml", {
    hostname = local.afm_hostname
  })
}

module "apim_api_afm_calculator_api_v2" {
  source = "./.terraform/modules/__v3__/api_management_api"


  name                  = format("%s-afm-calculator-service-api", local.project)
  api_management_name   = local.pagopa_apim_name
  resource_group_name   = local.pagopa_apim_rg
  product_ids           = [local.apim_afm_calculator_service_api.product_id]
  subscription_required = local.apim_afm_calculator_service_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_afm_calculator_api.id
  api_version           = "v2"

  description  = local.apim_afm_calculator_service_api.description
  display_name = local.apim_afm_calculator_service_api.display_name
  path         = local.apim_afm_calculator_service_api.path
  protocols    = ["https"]
  service_url  = local.apim_afm_calculator_service_api.service_url

  content_format = "openapi"
  content_value = templatefile("../openapi/openapi-v2.json", {
    host    = local.apim_hostname
    service = local.apim_afm_calculator_service_api.product_id
  })

  xml_content = templatefile("./api/calculator-service/v2/_base_policy.xml", {
    hostname = local.afm_hostname
  })
}


##################################
##  API AFM Calculator for Node ##
##################################

resource "azurerm_api_management_api_version_set" "api_afm_calculator_node_api" {
  name                = format("%s-afm-calculator-service-node-api", var.env_short)
  resource_group_name = local.pagopa_apim_rg
  api_management_name = local.pagopa_apim_name
  display_name        = local.apim_afm_calculator_service_node_api.display_name
  versioning_scheme   = "Segment"
}

module "apim_api_afm_calculator_api_node_v1" {
  source = "./.terraform/modules/__v3__/api_management_api"

  name                  = format("%s-afm-calculator-service-node-api", local.project)
  api_management_name   = local.pagopa_apim_name
  resource_group_name   = local.pagopa_apim_rg
  product_ids           = [local.apim_afm_calculator_service_node_api.product_id, local.apim_x_node_product_id]
  subscription_required = local.apim_afm_calculator_service_node_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_afm_calculator_node_api.id
  api_version           = "v1"

  description  = local.apim_afm_calculator_service_node_api.description
  display_name = local.apim_afm_calculator_service_node_api.display_name
  path         = local.apim_afm_calculator_service_node_api.path
  protocols    = ["https"]
  service_url  = local.apim_afm_calculator_service_node_api.service_url

  content_format = "openapi"
  content_value = templatefile("../openapi/openapi-node-v1.json", {
    host = local.apim_hostname
  })

  xml_content = templatefile(var.env_short == "p" ? "./api/calculator-service/node/v1/_base_policy.xml" : "./api/calculator-service/node/v1/_base_policy_test.xml", {
    hostname = local.afm_hostname
  })
}

module "apim_api_afm_calculator_api_node_v2" {
  source = "./.terraform/modules/__v3__/api_management_api"

  name                  = format("%s-afm-calculator-service-node-api", local.project)
  api_management_name   = local.pagopa_apim_name
  resource_group_name   = local.pagopa_apim_rg
  product_ids           = [local.apim_afm_calculator_service_node_api.product_id, local.apim_x_node_product_id]
  subscription_required = local.apim_afm_calculator_service_node_api.subscription_required
  version_set_id        = azurerm_api_management_api_version_set.api_afm_calculator_node_api.id
  api_version           = "v2"

  description  = local.apim_afm_calculator_service_node_api.description
  display_name = local.apim_afm_calculator_service_node_api.display_name
  path         = local.apim_afm_calculator_service_node_api.path
  protocols    = ["https"]
  service_url  = local.apim_afm_calculator_service_node_api.service_url

  content_format = "openapi"
  content_value = templatefile("../openapi/openapi-node-v2.json", {
    host    = local.apim_hostname
    service = local.apim_afm_calculator_service_node_api.product_id
  })

  xml_content = templatefile("./api/calculator-service/node/v2/_base_policy.xml", {
    hostname = local.afm_hostname
  })
}
