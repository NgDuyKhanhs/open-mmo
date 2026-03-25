/**
 * usePhoneTilt Composable
 * Tracks mouse movement over a phone element and applies 3D tilt effect
 * Library-grade composable using adaptable inputs (MaybeRefOrGetter)
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'
import type { Ref } from 'vue'

interface TiltOptions {
  maxRotate?: number
  enabled?: boolean
}

interface TiltState {
  rotateX: Ref<number>
  rotateY: Ref<number>
  handleMouseMove: (e: MouseEvent) => void
  handleMouseLeave: () => void
}

/**
 * Phone tilt animation based on mouse position
 * @param phoneRef - Reference to the phone container element
 * @param options - Configuration options (maxRotate, enabled)
 * @returns Reactive rotation values and mouse handlers
 */
export function usePhoneTilt(
  phoneRef: Ref<HTMLElement | null>,
  options: TiltOptions = {},
): TiltState {
  const { maxRotate = 15, enabled = true } = options

  // State
  const rotateX = ref(0)
  const rotateY = ref(0)

  // Handlers
  const handleMouseMove = (e: MouseEvent) => {
    if (!enabled || !phoneRef.value) return

    const rect = phoneRef.value.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2

    const mouseX = e.clientX - centerX
    const mouseY = e.clientY - centerY

    // Normalize coordinates to -1 to 1
    const normalizedX = Math.max(-1, Math.min(1, mouseX / (rect.width / 2)))
    const normalizedY = Math.max(-1, Math.min(1, mouseY / (rect.height / 2)))

    // Apply cubic easing for smooth animation
    const easedX = normalizedX * normalizedX * normalizedX
    const easedY = normalizedY * normalizedY * normalizedY

    rotateX.value = -easedY * maxRotate
    rotateY.value = easedX * maxRotate
  }

  const handleMouseLeave = () => {
    rotateX.value = 0
    rotateY.value = 0
  }

  // Lifecycle
  onMounted(() => {
    if (!phoneRef.value) return
    phoneRef.value.addEventListener('mousemove', handleMouseMove)
    phoneRef.value.addEventListener('mouseleave', handleMouseLeave)
  })

  onBeforeUnmount(() => {
    if (!phoneRef.value) return
    phoneRef.value.removeEventListener('mousemove', handleMouseMove)
    phoneRef.value.removeEventListener('mouseleave', handleMouseLeave)
  })

  return {
    rotateX,
    rotateY,
    handleMouseMove,
    handleMouseLeave,
  }
}
