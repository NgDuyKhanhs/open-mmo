/**
 * Chat Assistant Configuration
 * Bot responses and chat settings
 */

export const BOT_RESPONSES = [
  'Tôi rất vui được giúp bạn! Bạn cần tư vấn gì?',
  'Chúng tôi chuyên tạo những trải nghiệm kỹ thuật số tuyệt vời. Bạn có dự án nào không?',
  'Bạn muốn biết thêm về dịch vụ của chúng tôi không? Hãy khám phá danh mục của bạn!',
  'Đội ngũ bạn kết hợp sự đổi mới với thiết kế lấy con người làm trung tâm. Chúng tôi có thể giúp gì?',
  'Tôi có thể giúp bạn với thông tin về dịch vụ, danh mục và đội ngũ của chúng tôi.',
]

export const CHAT_CONFIG = {
  initialMessage: 'Tôi có thể giúp gì cho bạn hôm nay ?',
  placeholder: 'Nhập tin nhắn của bạn...',
  header: 'Bot',
  responseDelay: 600, // milliseconds
}

export function getRandomBotResponse(): string {
  return BOT_RESPONSES[Math.floor(Math.random() * BOT_RESPONSES.length)]!
}
