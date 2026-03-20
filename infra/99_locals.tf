locals {
  project = "${var.prefix}-${var.env_short}-${var.location_short}-${var.domain}"
  product = "${var.prefix}-${var.env_short}"
  
  pagopa_apim_name = "${local.product}-apim"
  pagopa_apim_rg   = "${local.product}-api-rg"

  apim_hostname = "api.${var.apim_dns_zone_prefix}.${var.external_domain}"
  afm_hostname  = var.env == "prod" ? "weuprod.afm.internal.platform.pagopa.it" : "weu${var.env}.afm.internal.${var.env}.platform.pagopa.it"
    
  apim_x_node_product_id = "apim_for_node"
}
