/**
 * Navigation Configuration
 * Centralized navigation items used across the app
 */

export interface NavItem {
  id: number
  badge: string
  title: string
  path: string
}

export const NAVIGATION_CONFIG: NavItem[] = [
  {
    id: 1,
    badge: 'IN',
    title: 'Introduction',
    path: '/introduction',
  },
  {
    id: 2,
    badge: 'SV',
    title: 'Services',
    path: '/services',
  },
  {
    id: 3,
    badge: 'CT',
    title: 'Contact',
    path: '/contact',
  },
  {
    id: 4,
    badge: 'LI',
    title: 'Log In',
    path: '/login',
  },
]
