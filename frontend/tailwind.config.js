export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#0f0f0f',
        secondary: '#0066ff',
        accent: '#00d4ff',
        dark: '#0a0a0a',
        light: '#f9f9f9',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'avenir', 'Helvetica', 'Arial', 'sans-serif'],
      },
      animation: {
        slide: 'slide 20s linear infinite',
        'fade-in': 'fadeIn 0.6s ease-in',
        'slide-up': 'slideUp 0.6s ease-out',
      },
      keyframes: {
        slide: {
          '0%': { transform: 'translateX(0)' },
          '100%': { transform: 'translateX(-100%)' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { transform: 'translateY(20px)', opacity: '0' },
          '100%': { transform: 'translateY(0)', opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}
