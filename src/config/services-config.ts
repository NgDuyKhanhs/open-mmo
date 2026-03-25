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
      desc: 'Hướng dẫn toàn diện về marketing liên kết, lựa chọn sản phẩm, xây dựng đối tượng và tối ưu hóa tỷ lệ chuyển đổi.',
      icon: '🔗',
      revenue: 'Vô hạn',
    },
    {
      title: 'Tạo Nội Dung',
      desc: 'Chiến lược kiếm tiền từ YouTube, Blog, TikTok, Instagram qua ads, sponsorship và sản phẩm riêng.',
      icon: '📺',
      revenue: '$500-$5000/tháng',
    },
    {
      title: 'Freelancing',
      desc: 'Bước đầu vào thế giới freelance, lập giá dự án, quản lý khách hàng và tăng thu nhập theo cấp độ.',
      icon: '💼',
      revenue: '$10-$100+/giờ',
    },
    {
      title: 'E-commerce',
      desc: 'Xây dựng cửa hàng online, dropshipping, print-on-demand và tối ưu hóa bán hàng trực tuyến.',
      icon: '🛍️',
      revenue: '$1000-$10000/tháng',
    },
  ] as EarningService[],

  skills: [
    {
      title: 'Digital Marketing',
      desc: 'Thành thạo SEO, SEM, Email Marketing, Content Marketing để tăng traffic và doanh số.',
      icon: '📈',
      level: 'Từ cơ bản đến nâng cao',
    },
    {
      title: 'Web Development',
      desc: 'Lập trình web, WordPress, shopify để xây dựng hệ thống kiếm tiền trực tuyến.',
      icon: '💻',
      level: 'Từ cơ bản đến chuyên sâu',
    },
    {
      title: 'Social Media Marketing',
      desc: 'Quản lý mạng xã hội, tạo nội dung viral, xây dựng community và monetization.',
      icon: '📱',
      level: 'Từ beginner đến expert',
    },
    {
      title: 'Copywriting & SEO',
      desc: 'Viết content tối ưu SEO, copywriting bán hàng, landing page để chuyển đổi khách hàng.',
      icon: '✍️',
      level: 'Từ cơ bản đến nâng cao',
    },
  ] as SkillService[],

  tools: [
    {
      title: 'Analytics & Tracking',
      desc: 'Google Analytics, Facebook Pixel, Hotjar để phân tích dữ liệu và tối ưu hóa chiến dịch.',
      icon: '📊',
      use: 'Theo dõi hiệu suất',
    },
    {
      title: 'Automation Tools',
      desc: 'Zapier, Make, Airtable để tự động hóa quy trình và tiết kiệm thời gian.',
      icon: '🤖',
      use: 'Tự động hóa công việc',
    },
    {
      title: 'Hosting & Domain',
      desc: 'Lựa chọn hosting, CDN, domains để xây dựng website nhanh, an toàn và ổn định.',
      icon: '🌐',
      use: 'Cơ sở hạ tầng web',
    },
    {
      title: 'Design & Graphics',
      desc: 'Canva, Figma, Adobe Suite để tạo graphics, landing page và branding chuyên nghiệp.',
      icon: '🎨',
      use: 'Thiết kế hình ảnh',
    },
  ] as ToolService[],

  support: [
    {
      title: 'Mentoring 1-on-1',
      desc: 'Tư vấn riêng từ các chuyên gia kiếm tiền trực tuyến, lập kế hoạch chi tiết cho bạn.',
      icon: '👨‍🏫',
    },
    {
      title: 'Cộng Đồng & Network',
      desc: 'Tham gia cộng đồng 10000+ thành viên, chia sẻ kinh nghiệm và hợp tác kiếm tiền.',
      icon: '👥',
    },
    {
      title: 'Tài Liệu & Khóa Học',
      desc: 'Truy cập thư viện khóa học, video hướng dẫn, case study thành công và templates sẵn dùng.',
      icon: '📚',
    },
    {
      title: 'Hỗ Trợ Kỹ Thuật',
      desc: 'Giải đáp mọi câu hỏi kỹ thuật, tư vấn công cụ, fix lỗi và theo dõi tiến độ.',
      icon: '🔧',
    },
  ] as BaseService[],
}

export type ServiceTab = keyof typeof SERVICES_CONFIG
