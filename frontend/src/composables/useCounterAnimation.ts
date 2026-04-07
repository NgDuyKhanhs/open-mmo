/**
 * useCounterAnimation Composable
 * Animates counter values from 0 to target with easing
 * Library-grade composable with flexible delay configuration
 */

import { ref, onMounted, onBeforeUnmount } from 'vue'
import type { Ref } from 'vue'

interface CounterAnimationOptions {
  duration?: number // Total animation duration in ms
  stepDuration?: number // Duration of each step in ms
  staggerDelay?: number // Delay between each counter (ms)
  easing?: (progress: number) => number
}

/**
 * Animate numeric values counting up from 0 to target
 * @param targets - Array of target values
 * @param options - Animation configuration
 * @returns Array of current animated values
 */
export function useCounterAnimation(
  targets: number[],
  options: CounterAnimationOptions = {}
): Ref<number[]> {
  const { duration = 1200, stepDuration = 30, staggerDelay = 200, easing = easeOutQuad } = options

  const animatedValues = ref<number[]>(targets.map(() => 0))
  const timers: (ReturnType<typeof setTimeout> | ReturnType<typeof setInterval>)[] = []

  /**
   * Ease-out quadratic easing function
   */
  function easeOutQuad(progress: number): number {
    return 1 - (1 - progress) * (1 - progress)
  }

  const animateCounter = (index: number, target: number, startDelay: number) => {
    const delay = setTimeout(() => {
      let progress = 0
      const steps = Math.ceil(duration / stepDuration)
      let currentStep = 0

      const timer = setInterval(() => {
        currentStep++
        progress = Math.min(currentStep / steps, 1)
        const easedProgress = easing(progress)
        animatedValues.value[index] = Math.round(target * easedProgress)

        if (progress >= 1) {
          animatedValues.value[index] = target
          clearInterval(timer)
        }
      }, stepDuration)

      timers.push(delay)
    }, startDelay)

    timers.push(delay)
  }

  const cleanup = () => {
    timers.forEach((timer) => clearTimeout(timer))
    timers.length = 0
  }

  onMounted(() => {
    targets.forEach((target, index) => {
      animateCounter(index, target, index * staggerDelay)
    })
  })

  onBeforeUnmount(() => {
    cleanup()
  })

  return animatedValues
}
