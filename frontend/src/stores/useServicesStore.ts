/**
 * Services Store
 * Centralized services management and future API integration
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { SERVICES_CONFIG, SERVICES_CONFIG as ServicesData } from '@/config'
import type { ServiceTab } from '@/config'

export const useServicesStore = defineStore('services', () => {
  // State
  const activeTab = ref<ServiceTab>('earning')
  const services = ref(ServicesData)
  const isLoading = ref(false)

  // Computed
  const currentTabServices = computed(() => {
    return services.value[activeTab.value]
  })

  const earningServices = computed(() => services.value.earning)
  const skillServices = computed(() => services.value.skills)
  const toolServices = computed(() => services.value.tools)
  const supportServices = computed(() => services.value.support)

  const allServices = computed(() => [
    ...services.value.earning,
    ...services.value.skills,
    ...services.value.tools,
    ...services.value.support,
  ])

  const serviceStats = computed(() => ({
    earning: services.value.earning.length,
    skills: services.value.skills.length,
    tools: services.value.tools.length,
    support: services.value.support.length,
    total: allServices.value.length,
  }))

  // Actions
  const setActiveTab = (tab: ServiceTab) => {
    activeTab.value = tab
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  /**
   * Future: Fetch services from backend API
   * Replace static SERVICES_CONFIG with dynamic data
   */
  const fetchServices = async () => {
    setLoading(true)
    try {
      // TODO: Replace with actual API call
      // const response = await fetch('/api/services')
      // services.value = await response.json()

      // For now, keep static data
      services.value = ServicesData
    } catch (error) {
      console.error('Failed to fetch services:', error)
    } finally {
      setLoading(false)
    }
  }

  /**
   * Initialize store on app startup
   */
  const initialize = async () => {
    await fetchServices()
  }

  return {
    // State
    activeTab,
    services,
    isLoading,

    // Computed
    currentTabServices,
    earningServices,
    skillServices,
    toolServices,
    supportServices,
    allServices,
    serviceStats,

    // Actions
    setActiveTab,
    setLoading,
    fetchServices,
    initialize,
  }
})
