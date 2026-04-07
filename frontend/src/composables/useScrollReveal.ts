/**
 * useScrollReveal Composable
 * Reveals elements when they enter viewport using IntersectionObserver
 * Library-grade composable with proper cleanup
 */

import { onMounted, onBeforeUnmount, ref } from 'vue'

interface ScrollRevealOptions {
  threshold?: number | number[]
  rootMargin?: string
  revealClass?: string
  observeOnce?: boolean
}

/**
 * Automatically reveal elements with .scroll-reveal class when visible
 * @param options - Configuration options
 * @returns Function to manually observe an element, and unobserve function
 */
export function useScrollReveal(options: ScrollRevealOptions = {}) {
  const {
    threshold = 0.15,
    rootMargin = '0px 0px -50px 0px',
    revealClass = 'revealed',
    observeOnce = true,
  } = options

  let observer: IntersectionObserver | null = null

  const setupObserver = () => {
    if (observer) return

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add(revealClass)
            if (observeOnce) {
              observer?.unobserve(entry.target)
            }
          } else if (!observeOnce) {
            entry.target.classList.remove(revealClass)
          }
        })
      },
      {
        threshold,
        rootMargin,
      }
    )
  }

  const observeElement = (el: Element) => {
    if (!observer) setupObserver()
    observer?.observe(el)
  }

  const unobserveElement = (el: Element) => {
    observer?.unobserve(el)
  }

  const unobserveAll = () => {
    observer?.disconnect()
    observer = null
  }

  onMounted(() => {
    setupObserver()
    // Find all elements with scroll-reveal class
    const elements = document.querySelectorAll('.scroll-reveal')
    elements.forEach((el) => {
      observer?.observe(el)
    })
  })

  onBeforeUnmount(() => {
    unobserveAll()
  })

  return {
    observeElement,
    unobserveElement,
    unobserveAll,
  }
}
