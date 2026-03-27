/**
 * Services Configuration
 * All services organized by category for easy management
 */

export interface BaseService {
  title: string
  desc: string
  icon: string
}

export interface EarningService extends BaseService {
  revenue: string
}

export interface SkillService extends BaseService {
  level: string
}

export interface ToolService extends BaseService {
  use: string
}

export const SERVICES_CONFIG = {
  earning: [
    {
      title: 'Affiliate Marketing',
      desc: 'Comprehensive guidance on affiliate marketing, product selection, audience building, and conversion optimization.',
      icon: '🔗',
      revenue: 'Unlimited',
    },
    {
      title: 'Content Creation',
      desc: 'Monetization strategies for YouTube, blogs, TikTok, and Instagram through ads, sponsorships, and your own products.',
      icon: '📺',
      revenue: '$500-$5000/month',
    },
    {
      title: 'Freelancing',
      desc: 'Start freelancing with confidence, set project pricing, manage clients, and grow your income over time.',
      icon: '💼',
      revenue: '$10-$100+/hour',
    },
    {
      title: 'E-commerce',
      desc: 'Build online stores, run dropshipping and print-on-demand models, and optimize digital sales performance.',
      icon: '🛍️',
      revenue: '$1000-$10000/month',
    },
  ] as EarningService[],

  skills: [
    {
      title: 'Digital Marketing',
      desc: 'Master SEO, SEM, email marketing, and content marketing to grow traffic and revenue.',
      icon: '📈',
      level: 'Beginner to advanced',
    },
    {
      title: 'Web Development',
      desc: 'Learn web development, WordPress, and Shopify to build scalable online income systems.',
      icon: '💻',
      level: 'Beginner to expert',
    },
    {
      title: 'Social Media Marketing',
      desc: 'Manage social channels, create viral content, build communities, and monetize effectively.',
      icon: '📱',
      level: 'Beginner to expert',
    },
    {
      title: 'Copywriting & SEO',
      desc: 'Write SEO-optimized content, sales copy, and landing pages that convert visitors into customers.',
      icon: '✍️',
      level: 'Beginner to advanced',
    },
  ] as SkillService[],

  tools: [
    {
      title: 'Analytics & Tracking',
      desc: 'Use Google Analytics, Facebook Pixel, and Hotjar to analyze data and optimize campaigns.',
      icon: '📊',
      use: 'Performance tracking',
    },
    {
      title: 'Automation Tools',
      desc: 'Use Zapier, Make, and Airtable to automate workflows and save time.',
      icon: '🤖',
      use: 'Workflow automation',
    },
    {
      title: 'Hosting & Domain',
      desc: 'Choose hosting, CDNs, and domains to build fast, secure, and stable websites.',
      icon: '🌐',
      use: 'Web infrastructure',
    },
    {
      title: 'Design & Graphics',
      desc: 'Use Canva, Figma, and Adobe tools to create graphics, landing pages, and professional branding.',
      icon: '🎨',
      use: 'Visual design',
    },
  ] as ToolService[],

  support: [
    {
      title: 'Mentoring 1-on-1',
      desc: 'Get private coaching from online business experts with a tailored growth roadmap.',
      icon: '👨‍🏫',
    },
    {
      title: 'Community & Network',
      desc: 'Join a 10,000+ member community to share experience and collaborate on growth.',
      icon: '👥',
    },
    {
      title: 'Resources & Courses',
      desc: 'Access course libraries, tutorial videos, successful case studies, and ready-to-use templates.',
      icon: '📚',
    },
    {
      title: 'Technical Support',
      desc: 'Get answers to technical questions, tooling advice, bug fixes, and progress guidance.',
      icon: '🔧',
    },
  ] as BaseService[],
}

export type ServiceTab = keyof typeof SERVICES_CONFIG
