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
    badge: 'TG',
    title: 'Giới Thiệu',
    path: '/introduction',
  },
  {
    id: 2,
    badge: 'DV',
    title: 'Dịch Vụ',
    path: '/services',
  },
  {
    id: 3,
    badge: 'LH',
    title: 'Liên Hệ',
    path: '/contact',
  },
  {
    id: 4,
    badge: 'DN',
    title: 'Đăng Nhập',
    path: '/login',
  },
]
