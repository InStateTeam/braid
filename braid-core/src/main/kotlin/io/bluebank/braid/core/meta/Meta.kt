package io.bluebank.braid.core.meta

val DEFAULT_API_MOUNT = "/api/"

fun defaultServiceEndpoint(serviceName: String) = "${defaultServiceMountpoint(DEFAULT_API_MOUNT, serviceName)}/braid"
fun defaultServiceEndpoint(rootAPIPath: String, serviceName: String) = "${defaultServiceMountpoint(rootAPIPath, serviceName)}/braid"
fun defaultServiceMountpoint(serviceName: String) = "$DEFAULT_API_MOUNT$serviceName"
fun defaultServiceMountpoint(rootAPIPath: String, serviceName: String) = "$rootAPIPath$serviceName"

data class ServiceDescriptor(val endpoint: String, val documentation: String) {
  companion object {
    /**
     * Creates a map of [ServiceDescriptor] given the REST root api path and a collection of serviceNames
     *
     * * rootAPIPath - this must terminate with '/' e.g. /api/
     */
    fun createServiceDescriptors(rootAPIPath: String, serviceNames: Collection<String>) : Map<String, ServiceDescriptor> {
      assert(rootAPIPath.endsWith('/')) {
        "$rootAPIPath doesn't end with '/'"
      }
      return serviceNames.map {
        it to ServiceDescriptor(defaultServiceEndpoint(rootAPIPath, it), "$rootAPIPath$it")
      }.toMap()
    }
  }
}



